package com.example.tvreceiver;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class ServerService extends Service implements GoServerManager.ServerCallback {
    private static final String TAG = "ServerService";
    private static final String CHANNEL_ID = "tv_receiver_channel";
    private static final int NOTIFICATION_ID = 1001;

    private GoServerManager serverManager;
    private VideoFileObserver fileObserver;

    public static final String ACTION_VIDEO_RECEIVED = "com.example.tvreceiver.VIDEO_RECEIVED";
    public static final String EXTRA_VIDEO_PATH = "video_path";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());

        serverManager = new GoServerManager(this);
        serverManager.startServer(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fileObserver != null) {
            fileObserver.stopWatching();
        }
        if (serverManager != null) {
            serverManager.stopServer();
        }
    }

    @Override
    public void onServerStarted(String ip, int port) {
        Log.i(TAG, "Server started: " + ip + ":" + port);
        setupFileObserver();
        updateNotification("服务器运行中: " + ip + ":" + port);
    }

    @Override
    public void onServerFailed(String error) {
        Log.e(TAG, "Server failed: " + error);
        updateNotification("服务器错误: " + error);
    }

    private void setupFileObserver() {
        String videoPath = serverManager.getVideoPath();
        fileObserver = new VideoFileObserver(videoPath, path -> {
            Log.i(TAG, "Video received: " + path);
            notifyVideoReceived(path);
        });
        fileObserver.startWatching();
    }

    private void notifyVideoReceived(String videoPath) {
        Intent intent = new Intent(ACTION_VIDEO_RECEIVED);
        intent.putExtra(EXTRA_VIDEO_PATH, videoPath);
        intent.setPackage(getPackageName());
        sendBroadcast(intent);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "视频投屏服务",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("保持后台服务运行以接收视频");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("视频投屏接收器")
            .setContentText("正在等待连接...")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build();
    }

    private void updateNotification(String text) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("视频投屏接收器")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build();

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, notification);
        }
    }
}
