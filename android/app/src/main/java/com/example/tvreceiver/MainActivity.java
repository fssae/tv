package com.example.tvreceiver;

import android.graphics.Typeface;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.net.InetAddress;

public class MainActivity extends AppCompatActivity implements
        GoServerManager.ServerCallback,
        VideoFileObserver.VideoEventListener,
        VideoPlayerManager.PlayerEventListener {

    private static final String TAG = "MainActivity";

    private GoServerManager serverManager;
    private VideoFileObserver fileObserver;
    private VideoPlayerManager playerManager;

    private LinearLayout infoContainer;
    private FrameLayout videoContainer;
    private TextView tvIP;
    private TextView tvPort;
    private TextView tvStatus;
    private TextView tvHint;
    private ProgressBar progressBar;
    private ImageView qrCodeView;

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initManagers();
        startServer();
    }

    private void initViews() {
        infoContainer = findViewById(R.id.infoContainer);
        videoContainer = findViewById(R.id.videoContainer);
        tvIP = findViewById(R.id.tvIP);
        tvPort = findViewById(R.id.tvPort);
        tvStatus = findViewById(R.id.tvStatus);
        tvHint = findViewById(R.id.tvHint);
        progressBar = findViewById(R.id.progressBar);
        qrCodeView = findViewById(R.id.qrCodeView);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void initManagers() {
        serverManager = new GoServerManager(this);
        playerManager = new VideoPlayerManager(this, videoContainer);
        playerManager.setListener(this);
    }

    private void startServer() {
        showStatus("正在启动服务器...");
        serverManager.startServer(this);
    }

    private void setupFileObserver() {
        String videoPath = serverManager.getVideoPath();
        fileObserver = new VideoFileObserver(videoPath, this);
        fileObserver.startWatching();
        Log.i(TAG, "FileObserver started for: " + videoPath);
    }

    @Override
    public void onServerStarted(String ip, int port) {
        runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            tvIP.setText(ip);
            tvIP.setTypeface(null, Typeface.BOLD);
            tvPort.setText(String.valueOf(port));
            tvStatus.setText("服务器已启动");
            tvStatus.setTextColor(getColor(android.R.color.holo_green_dark));
            tvHint.setText("使用手机浏览器访问以上地址上传视频");
            infoContainer.setVisibility(View.VISIBLE);
        });

        setupFileObserver();
    }

    @Override
    public void onServerFailed(String error) {
        runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            tvStatus.setText("服务器启动失败: " + error);
            tvStatus.setTextColor(getColor(android.R.color.holo_red_dark));

            handler.postDelayed(() -> {
                progressBar.setVisibility(View.VISIBLE);
                tvStatus.setText("正在重试...");
                serverManager.restartServer(MainActivity.this);
            }, 3000);
        });
    }

    @Override
    public void onVideoReady(String videoPath) {
        runOnUiThread(() -> {
            Toast.makeText(this, "收到视频，准备播放...", Toast.LENGTH_SHORT).show();
            playVideo(videoPath);
        });
    }

    private void playVideo(String videoPath) {
        File videoFile = new File(videoPath);
        if (videoFile.exists() && videoFile.length() > 0) {
            infoContainer.setVisibility(View.GONE);
            playerManager.playVideo(videoPath);
        }
    }

    @Override
    public void onPlaybackCompleted() {
        runOnUiThread(() -> {
            playerManager.releasePlayer();
            infoContainer.setVisibility(View.VISIBLE);
            Toast.makeText(this, "播放完成", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onPlaybackError(String error) {
        runOnUiThread(() -> {
            Toast.makeText(this, "播放错误: " + error, Toast.LENGTH_LONG).show();
            playerManager.releasePlayer();
            infoContainer.setVisibility(View.VISIBLE);
        });
    }

    @Override
    public void onPlayerReady() {
        Log.i(TAG, "Player ready");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (serverManager != null && !serverManager.isRunning()) {
            startServer();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (playerManager != null) {
            playerManager.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fileObserver != null) {
            fileObserver.stopWatching();
        }
        if (playerManager != null) {
            playerManager.releasePlayer();
        }
        if (serverManager != null) {
            serverManager.stopServer();
        }
    }
}
