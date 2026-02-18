package com.example.tvreceiver;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class GoServerManager {
    private static final String TAG = "GoServerManager";
    private static final String BINARY_NAME = "libserver.so";
    private static final String BINARY_ASSET_PATH = "server/" + BINARY_NAME;

    private final Context context;
    private Process goProcess;
    private String videoDir;
    private int port = 8080;
    private ServerCallback callback;

    public GoServerManager(Context context) {
        this.context = context.getApplicationContext();
        this.videoDir = context.getFilesDir().getAbsolutePath();
    }

    public void setCallback(ServerCallback callback) {
        this.callback = callback;
    }

    private String getDeviceArchitecture() {
        String[] supportedAbis = android.os.Build.SUPPORTED_ABIS;
        for (String abi : supportedAbis) {
            if (abi.startsWith("arm64")) {
                return "arm64";
            } else if (abi.startsWith("armeabi-v7a") || abi.startsWith("arm")) {
                return "arm32";
            } else if (abi.startsWith("x86_64")) {
                return "x64";
            } else if (abi.startsWith("x86")) {
                return "x86";
            }
        }
        return "arm64";
    }

    private String getBinaryAssetPath(String arch) {
        switch (arch) {
            case "arm32":
                return "server/libserver_arm32.so";
            case "x64":
                return "server/libserver_x64.so";
            case "arm64":
            default:
                return "server/libserver_arm64.so";
        }
    }

    public interface ServerCallback {
        void onServerStarted(String ip, int port);
        void onServerFailed(String error);
        void onArchitectureDetected(String arch);
    }

    public synchronized void startServer(ServerCallback callback) {
        if (isRunning()) {
            Log.w(TAG, "Server is already running");
            if (callback != null) {
                callback.onServerStarted(getLocalIP(), port);
            }
            return;
        }

        new Thread(() -> {
            try {
                String binaryPath = prepareBinary();
                if (binaryPath == null) {
                    String error = "Failed to prepare binary. Please check device architecture and try again.";
                    Log.e(TAG, error);
                    if (callback != null) {
                        callback.onServerFailed(error);
                    }
                    return;
                }

                ProcessBuilder pb = new ProcessBuilder(binaryPath, videoDir);
                pb.redirectErrorStream(true);
                pb.directory(new File(videoDir));

                goProcess = pb.start();

                Thread logThread = new Thread(() -> {
                    try {
                        InputStream is = goProcess.getInputStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            Log.i(TAG, "[GoServer] " + line);
                            if (line.contains("Server starting at")) {
                                parseServerInfo(line);
                            }
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading server output", e);
                    }
                });
                logThread.setDaemon(true);
                logThread.start();

                Thread.sleep(500);

                if (isRunning()) {
                    String ip = getLocalIP();
                    if (callback != null) {
                        callback.onServerStarted(ip, port);
                    }
                } else {
                    String error = "Server process died immediately. Check logs for details.";
                    Log.e(TAG, error);
                    if (callback != null) {
                        callback.onServerFailed(error);
                    }
                }
            } catch (Exception e) {
                String error = "Failed to start server: " + e.getMessage();
                Log.e(TAG, error, e);
                if (callback != null) {
                    callback.onServerFailed(error);
                }
            }
        }).start();
    }

    public synchronized void stopServer() {
        if (goProcess != null) {
            goProcess.destroy();
            goProcess = null;
            Log.i(TAG, "Server stopped");
        }
    }

    public synchronized boolean isRunning() {
        if (goProcess == null) {
            return false;
        }
        try {
            goProcess.exitValue();
            return false;
        } catch (IllegalThreadStateException e) {
            return true;
        }
    }

    public String getVideoPath() {
        return new File(videoDir, "video.mp4").getAbsolutePath();
    }

    public String getVideoDir() {
        return videoDir;
    }

    private String prepareBinary() {
        String arch = getDeviceArchitecture();
        String assetPath = getBinaryAssetPath(arch);
        File binaryFile = new File(context.getFilesDir(), BINARY_NAME);
        String binaryPath = binaryFile.getAbsolutePath();

        Log.i(TAG, "Device architecture: " + arch);
        Log.i(TAG, "Asset path: " + assetPath);
        Log.i(TAG, "Binary path: " + binaryPath);

        if (callback != null) {
            callback.onArchitectureDetected(arch);
        }

        try {
            boolean needCopy = !binaryFile.exists() || isBinaryOutdated(assetPath);
            Log.i(TAG, "Need copy: " + needCopy + ", binary exists: " + binaryFile.exists());

            if (needCopy) {
                copyBinaryFromAssets(binaryFile, assetPath);
            }

            if (!binaryFile.canExecute()) {
                Log.w(TAG, "Binary not executable, setting permission...");
                if (!binaryFile.setExecutable(true, false)) {
                    Log.e(TAG, "Failed to set executable permission");
                    return null;
                }
            }

            Log.i(TAG, "Binary prepared successfully: " + binaryPath);
            return binaryPath;
        } catch (Exception e) {
            Log.e(TAG, "Failed to prepare binary", e);
            return null;
        }
    }

    private boolean isBinaryOutdated(String assetPath) {
        try {
            long assetLastModified = getAssetLastModified(assetPath);
            File binaryFile = new File(context.getFilesDir(), BINARY_NAME);
            if (!binaryFile.exists()) {
                return true;
            }
            return binaryFile.lastModified() < assetLastModified;
        } catch (Exception e) {
            return true;
        }
    }

    private long getAssetLastModified(String assetPath) {
        try {
            android.content.res.AssetFileDescriptor afd = context.getAssets().openFd(assetPath);
            afd.close();
            return System.currentTimeMillis();
        } catch (IOException e) {
            return 0;
        }
    }

    private void copyBinaryFromAssets(File destFile, String assetPath) throws IOException {
        Log.i(TAG, "Copying binary from assets: " + assetPath);
        
        InputStream is = null;
        try {
            is = context.getAssets().open(assetPath);
        } catch (IOException e) {
            Log.e(TAG, "Asset file not found: " + assetPath);
            throw new IOException("Asset file not found: " + assetPath, e);
        }
        
        try (FileOutputStream fos = new FileOutputStream(destFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytes = 0;
            while ((bytesRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;
            }
            fos.flush();
            Log.i(TAG, "Copied " + totalBytes + " bytes");
        } finally {
            if (is != null) {
                is.close();
            }
        }

        if (!destFile.setExecutable(true, false)) {
            throw new IOException("Failed to set executable permission");
        }
        if (!destFile.setReadable(true, false)) {
            throw new IOException("Failed to set readable permission");
        }

        Log.i(TAG, "Binary copied from " + assetPath + " to: " + destFile.getAbsolutePath());
    }

    private void parseServerInfo(String line) {
        try {
            if (line.contains("Server starting at")) {
                String[] parts = line.split(":");
                if (parts.length >= 3) {
                    port = Integer.parseInt(parts[parts.length - 1].trim());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse server info", e);
        }
    }

    private String getLocalIP() {
        try {
            java.util.Enumeration<java.net.NetworkInterface> interfaces =
                java.net.NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                java.net.NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;

                java.util.Enumeration<java.net.InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    java.net.InetAddress addr = addresses.nextElement();
                    if (addr instanceof java.net.Inet4Address) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get local IP", e);
        }
        return "unknown";
    }

    public void restartServer(ServerCallback callback) {
        stopServer();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        startServer(callback);
    }
}
