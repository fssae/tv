package com.example.tvreceiver;

import android.content.Context;
import android.net.Uri;
import android.os.FileObserver;
import android.util.Log;

import java.io.File;

public class VideoFileObserver extends FileObserver {
    private static final String TAG = "VideoFileObserver";

    private final String videoPath;
    private final VideoEventListener listener;
    private boolean isWatching = false;

    public interface VideoEventListener {
        void onVideoReady(String videoPath);
    }

    public VideoFileObserver(String videoPath, VideoEventListener listener) {
        super(videoPath, FileObserver.CLOSE_WRITE | FileObserver.CREATE);
        this.videoPath = videoPath;
        this.listener = listener;
    }

    @Override
    public void onEvent(int event, String path) {
        if (path == null) return;

        String affectedFile = new File(new File(videoPath).getParent(), path).getAbsolutePath();
        Log.d(TAG, "Event: " + event + " on file: " + affectedFile);

        if ((event & FileObserver.CLOSE_WRITE) != 0) {
            if (affectedFile.equals(videoPath)) {
                Log.i(TAG, "Video file write completed: " + videoPath);
                notifyVideoReady();
            }
        }
    }

    private void notifyVideoReady() {
        File videoFile = new File(videoPath);
        if (videoFile.exists() && videoFile.length() > 0) {
            if (listener != null) {
                listener.onVideoReady(videoPath);
            }
        } else {
            Log.w(TAG, "Video file is empty or does not exist");
        }
    }

    public void startWatching() {
        if (!isWatching) {
            super.startWatching();
            isWatching = true;
            Log.i(TAG, "Started watching: " + videoPath);
        }
    }

    public void stopWatching() {
        if (isWatching) {
            super.stopWatching();
            isWatching = false;
            Log.i(TAG, "Stopped watching: " + videoPath);
        }
    }
}
