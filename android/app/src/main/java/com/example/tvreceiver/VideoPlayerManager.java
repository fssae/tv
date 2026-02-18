package com.example.tvreceiver;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.StyledPlayerView;

import java.io.File;

public class VideoPlayerManager implements Player.Listener {
    private static final String TAG = "VideoPlayerManager";

    private final Context context;
    private final FrameLayout container;
    private ExoPlayer player;
    private StyledPlayerView playerView;
    private PlayerEventListener listener;

    public interface PlayerEventListener {
        void onPlaybackCompleted();
        void onPlaybackError(String error);
        void onPlayerReady();
    }

    public VideoPlayerManager(Context context, FrameLayout container) {
        this.context = context;
        this.container = container;
    }

    public void setListener(PlayerEventListener listener) {
        this.listener = listener;
    }

    public void playVideo(String videoPath) {
        File videoFile = new File(videoPath);
        if (!videoFile.exists()) {
            Log.e(TAG, "Video file does not exist: " + videoPath);
            if (listener != null) {
                listener.onPlaybackError("Video file does not exist");
            }
            return;
        }

        releasePlayer();

        player = new ExoPlayer.Builder(context).build();
        player.addListener(this);

        playerView = new StyledPlayerView(context);
        playerView.setPlayer(player);
        playerView.setUseController(true);
        playerView.setShowBuffering(StyledPlayerView.SHOW_BUFFERING_WHEN_PLAYING);

        container.removeAllViews();
        container.addView(playerView, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ));
        container.setVisibility(View.VISIBLE);

        MediaItem mediaItem = MediaItem.fromUri(Uri.fromFile(videoFile));
        player.setMediaItem(mediaItem);
        player.prepare();
        player.setPlayWhenReady(true);

        Log.i(TAG, "Started playing: " + videoPath);
    }

    public void pause() {
        if (player != null) {
            player.pause();
        }
    }

    public void resume() {
        if (player != null) {
            player.play();
        }
    }

    public void stop() {
        if (player != null) {
            player.stop();
        }
    }

    public void releasePlayer() {
        if (player != null) {
            player.removeListener(this);
            player.release();
            player = null;
        }
        if (playerView != null) {
            playerView.setPlayer(null);
            playerView = null;
        }
        container.removeAllViews();
        container.setVisibility(View.GONE);
    }

    public boolean isPlaying() {
        return player != null && player.isPlaying();
    }

    public long getCurrentPosition() {
        return player != null ? player.getCurrentPosition() : 0;
    }

    public long getDuration() {
        return player != null ? player.getDuration() : 0;
    }

    @Override
    public void onPlaybackStateChanged(int playbackState) {
        switch (playbackState) {
            case Player.STATE_READY:
                Log.d(TAG, "Player ready");
                if (listener != null) {
                    listener.onPlayerReady();
                }
                break;
            case Player.STATE_ENDED:
                Log.i(TAG, "Playback ended");
                if (listener != null) {
                    listener.onPlaybackCompleted();
                }
                break;
            case Player.STATE_BUFFERING:
                Log.d(TAG, "Player buffering");
                break;
            case Player.STATE_IDLE:
                Log.d(TAG, "Player idle");
                break;
        }
    }

    @Override
    public void onPlayerError(PlaybackException error) {
        Log.e(TAG, "Playback error: " + error.getMessage(), error);
        if (listener != null) {
            listener.onPlaybackError(error.getMessage());
        }
    }
}
