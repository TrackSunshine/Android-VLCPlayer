package com.hq.vlcplayer.view.widget;


import android.content.Context;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.TextureView;


import com.hq.vlcplayer.application.App;
import com.hq.vlcplayer.view.player.VLCPlayer;

import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.vlc.listener.MediaListenerEvent;
import org.videolan.vlc.listener.MediaPlayerControl;
import org.videolan.vlc.listener.VideoSizeChange;
import org.videolan.vlc.util.LogUtils;

/**
 * TextureView实现的VLC Video
 * 修正home键切换前后台黑屏的问题
 * 参考自https://github.com/mengzhidaren
 */
public class VLCVideoView extends TextureView implements MediaPlayerControl, TextureView.SurfaceTextureListener, VideoSizeChange {
    private final String tag = "VideoView";

    private SurfaceTextTextureObserver observer;

    public void setObserver(SurfaceTextTextureObserver observer) {
        this.observer = observer;
    }

    public interface SurfaceTextTextureObserver {
        void onSurfaceTextureAvailable();
        void onSurfaceTextureDestroyed();
    }

    private SurfaceTexture mSurfaceTexture;

    private VLCPlayer videoMediaLogic;

    public VLCVideoView(Context context) {
        this(context, null);
    }

    public VLCVideoView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VLCVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (isInEditMode()) {
            return;
        }
        init(context);
    }

    public void setMediaListenerEvent(MediaListenerEvent mediaListenerEvent) {
        videoMediaLogic.setMediaListenerEvent(mediaListenerEvent);
    }

    @Override
    public boolean canControl() {
        return videoMediaLogic.canControl();
    }

    /**
     * 关闭avtivity时 停止时用
     */
    public void onStop() {
        videoMediaLogic.onStop();
    }

    /**
     * 退出应用进程时回收
     */
    public void onDestory() {
        if (videoMediaLogic != null)
            videoMediaLogic.onDestory();
    }

    private void init(Context context) {
        if (App.getInstance().currentPlayProgress > 0) {
            videoMediaLogic = new VLCPlayer(context, App.getInstance().currentPlayProgress);
        } else {
            videoMediaLogic = new VLCPlayer(context);
        }
        videoMediaLogic.setVideoSizeChange(this);
        setSurfaceTextureListener(this);
    }


    @Override
    public boolean isPrepare() {
        return videoMediaLogic.isPrepare();
    }


    @Override
    public void startPlay(String path) {
        videoMediaLogic.startPlay(path);
    }

    @Override
    public void start() {
        videoMediaLogic.start();
    }

    @Override
    public void pause() {
        videoMediaLogic.pause();
    }

    @Override
    public int getDuration() {
        return videoMediaLogic.getDuration();
    }

    @Override
    public int getCurrentPosition() {
        return videoMediaLogic.getCurrentPosition();
    }

    @Override
    public void seekTo(int pos) {
        videoMediaLogic.seekTo(pos);
    }

    @Override
    public boolean isPlaying() {
        return videoMediaLogic.isPlaying();
    }


    @Override
    public void setMirror(boolean mirror) {
        this.mirror = mirror;
        if (mirror) {
            setScaleX(-1f);
        } else {
            setScaleX(1f);
        }
    }

    private boolean mirror = false;

    @Override
    public boolean getMirror() {
        return mirror;
    }


    @Override
    public int getBufferPercentage() {
        return videoMediaLogic.getBufferPercentage();
    }


    @Override
    public boolean setPlaybackSpeedMedia(float speed) {
        return videoMediaLogic.setPlaybackSpeedMedia(speed);
    }

    @Override
    public float getPlaybackSpeed() {
        return videoMediaLogic.getPlaybackSpeed();
    }


    @Override
    public void setLoop(boolean isLoop) {
        videoMediaLogic.setLoop(isLoop);
    }

    @Override
    public boolean isLoop() {
        return videoMediaLogic.isLoop();
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (mSurfaceTexture != null) {
            setSurfaceTexture(mSurfaceTexture);
            videoMediaLogic.setSurface(mSurfaceTexture);
        } else {
            videoMediaLogic.setSurface(surface);
        }
        if (observer != null) {
            observer.onSurfaceTextureAvailable();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        // todo 注意内存泄漏的问题，可以在关闭页面的时候，调用videoMediaLogic.onSurfaceTextureDestroyedUI();
        LogUtils.i("onSurfaceTextureDestroyed", "onSurfaceTextureDestroyed");
        mSurfaceTexture = surface;
//        videoMediaLogic.onSurfaceTextureDestroyedUI();
        if (observer != null) {
            observer.onSurfaceTextureDestroyed();
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    //根据播放状态 打开关闭旋转动画
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (isInEditMode()) {
            return;
        }
        setKeepScreenOn(true);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (isInEditMode()) {
            return;
        }
        setKeepScreenOn(false);
    }


    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
//        if (changed) {
//            adjustAspectRatio(mVideoWidth, mVideoHeight, rotation);
//        }
    }

    private int mVideoWidth;
    private int mVideoHeight;
    private int rotation = 0;

    public int getVideoRotation() {
        return rotation;
    }


    @Override
    public void onVideoSizeChanged(int width, int height, int visibleWidth, int visibleHeight, int orientation) {
        LogUtils.i(tag, "onVideoSizeChanged   video=" + width + "x" + height + " visible="
                + visibleWidth + "x" + visibleHeight + "   orientation=" + orientation);
        if (width * height == 0) return;
        this.mVideoWidth = visibleWidth;
        this.mVideoHeight = visibleHeight;
        this.rotation = orientation;
    }

    public void setMedia(Media media) {
        videoMediaLogic.setMedia(media);
    }

    /**
     * 字幕画布
     *
     * @param surfaceSubtitlesView
     */
    public void setSurfaceSubtitlesView(TextureView surfaceSubtitlesView) {
        videoMediaLogic.setSurfaceSubtitlesView(surfaceSubtitlesView);
    }

    public void setAddSlave(String addSlave) {
        videoMediaLogic.setAddSlave(addSlave);
    }

    public void setMediaPlayer(MediaPlayer mediaPlayer) {
        videoMediaLogic.setMediaPlayer(mediaPlayer);
    }

    public MediaPlayer getMediaPlayer() {
        return videoMediaLogic.getMediaPlayer();
    }

    @Override
    public boolean canPause() {
        return videoMediaLogic.canPause();
    }

    @Override
    public boolean canSeekBackward() {
        return videoMediaLogic.canSeekBackward();
    }

    @Override
    public boolean canSeekForward() {
        return videoMediaLogic.canSeekForward();
    }

    @Override
    public int getAudioSessionId() {
        return videoMediaLogic.getAudioSessionId();
    }
}