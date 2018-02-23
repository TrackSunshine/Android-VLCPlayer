package com.hq.vlcplayer;

import android.app.Dialog;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.hq.vlcplayer.application.App;
import com.hq.vlcplayer.view.utils.CommonUtil;
import com.hq.vlcplayer.view.utils.LogUtil;
import com.hq.vlcplayer.view.widget.ENDownloadView;
import com.hq.vlcplayer.view.widget.ENPlayView;
import com.hq.vlcplayer.view.widget.SmileyLoadingView;
import com.hq.vlcplayer.view.widget.VLCVideoView;

import org.videolan.vlc.listener.MediaListenerEvent;

import java.lang.reflect.Method;
import java.util.Timer;
import java.util.TimerTask;


public class VlcVideoPlayerActivity extends FragmentActivity implements View.OnClickListener, View.OnTouchListener, MediaListenerEvent, SeekBar.OnSeekBarChangeListener {

    // 正常
    public static final int CURRENT_STATE_NORMAL = 0;
    // 准备中
    public static final int CURRENT_STATE_PREPAREING = 1;
    // 播放中
    public static final int CURRENT_STATE_PLAYING = 2;
    // 开始缓冲
    public static final int CURRENT_STATE_PLAYING_BUFFERING_START = 3;

    public static final int CURRENT_STATE_PLAYING_BUFFERING_VALID = 4;
    // 暂停
    public static final int CURRENT_STATE_PAUSE = 5;
    // 自动播放结束
    public static final int CURRENT_STATE_AUTO_COMPLETE = 6;
    // 错误状态
    public static final int CURRENT_STATE_STOP = 7;
    // 停止状态
    public static final int CURRENT_STATE_ERROR = 8;

    //音频焦点的监听
    protected AudioManager mAudioManager;

    //渲染控件父类
    protected FrameLayout mParentContainer;

    protected VLCVideoView vlcVideoView;

    private RelativeLayout videoControlLayout;

    //播放按键
    protected View mStartButton;

    //封面
    protected View mThumbImageView;

    //loading view
    protected View mLoadingProgressBar;

    //进度条
    protected SeekBar mProgressBar;

    //全屏按键
    protected ImageView volumeState;

    //全屏按键
    protected ImageView mFullscreenButton;

    //返回按键
    protected ImageView mBackButton;

    //锁定图标
    protected ImageView mLockScreen;

    //时间显示
    protected TextView mCurrentTimeTextView, mTotalTimeTextView;

    //title
    protected TextView mTitleTextView;

    //顶部和底部区域
    protected ViewGroup mTopContainer, mBottomContainer;

    //封面父布局
    protected RelativeLayout mThumbImageViewLayout;

    private boolean isFristPlay = true;

    // 退出全屏显示的案件图片
    protected int mShrinkImageRes = -1;

    //全屏显示的案件图片
    protected int mEnlargeImageRes = -1;

    private Bitmap pauseCover;

    //当前是否全屏
    protected boolean mIfCurrentIsFullscreen = false;

    //当前的播放状态
    protected int mCurrentState = -1;

    // 是否是触摸滑动事件
    protected boolean mTouchMovingEvent = false;

    // 是否是缓存的文件
    protected boolean mCacheFile = false;

    // 触摸滑动的X,Y坐标
    private float touchX, touchY;

    // 是否改变音量
    protected boolean needMute = true;

    // 是否改变音量
    protected boolean mChangeVolume = false;

    // 触摸的是否进度条
    protected boolean mTouchingProgressBar = false;

    // 进度定时器
    protected Timer updateProcessTimer;
    // 定时器任务
    protected ProgressTimerTask mProgressTimerTask;

    // 触摸显示后隐藏的时间
    protected int mDismissControlTime = 3 * 1000;
    // 触摸显示消失定时
    protected Timer mDismissControlViewTimer;
    // 触摸显示消失定时任务
    protected DismissControlViewTimerTask mDismissControlViewTimerTask;

    // 播放进度手势偏差值
    protected int mProgressThreshold = 80;

    // 播放声音手势偏差值
    protected int mVolumThreshold = 10;

    //手势调节音量的大小
    protected int mGestureDownVolume;

    //音量dialog
    protected Dialog mVolumeDialog;
    //音量进度条的progress
    protected ProgressBar mDialogVolumeProgressBar;

    private boolean surfaceTextureAvailable = false;

    private Handler mHandler = new Handler();

    /**
     * 双击
     */
    protected GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.framelayout_video);
        mAudioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        bindViews();
        bindEvent();
        vlcVideoView.startPlay(MainActivity.getUrl(this));
    }

    protected void bindViews() {
        mParentContainer = findViewById(R.id.frame_layout_scale);
        vlcVideoView = findViewById(R.id.player);
        mStartButton = findViewById(R.id.start);
        videoControlLayout = findViewById(R.id.parent_layout);
        mTitleTextView = findViewById(R.id.title);
        mBackButton = findViewById(R.id.back);
        volumeState = findViewById(R.id.volume_state);
        mFullscreenButton = findViewById(R.id.fullscreen);
        mProgressBar = findViewById(R.id.progress);
        mCurrentTimeTextView = findViewById(R.id.current);
        mTotalTimeTextView = findViewById(R.id.total);
        mBottomContainer = findViewById(R.id.layout_bottom);
        mTopContainer = findViewById(R.id.layout_top);
        mThumbImageViewLayout = findViewById(R.id.thumb);
        mLockScreen = findViewById(R.id.lock_screen);
    }

    private void bindEvent() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                touchDoubleUp();
                return true;
            }

            // 单击事件处理逻辑
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                startDismissControlViewTimer();
                onClickUiToggle();
                return false;
            }
        });

        vlcVideoView.setMediaListenerEvent(this);

        vlcVideoView.setObserver(new VLCVideoView.SurfaceTextTextureObserver() {
            @Override
            public void onSurfaceTextureAvailable() {
                surfaceTextureAvailable = true;
            }

            @Override
            public void onSurfaceTextureDestroyed() {
                surfaceTextureAvailable = false;
            }
        });
        mLoadingProgressBar = findViewById(R.id.loading);
        if (mStartButton != null) {
            mStartButton.setOnClickListener(this);
        }

        if (mFullscreenButton != null) {
            mFullscreenButton.setOnClickListener(this);
        }

        if (volumeState != null) {
            volumeState.setOnClickListener(this);
        }

        if (mProgressBar != null) {
            mProgressBar.setOnSeekBarChangeListener(this);
        }

        if (videoControlLayout != null) {
            videoControlLayout.setOnTouchListener(this);
        }

        if (mBottomContainer != null) {
            mBottomContainer.setOnClickListener(this);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mParentContainer.getLayoutParams();
        if (newConfig.orientation == ActivityInfo.SCREEN_ORIENTATION_USER) { // 横屏的时候的标识
            if (!isIfCurrentIsFullscreen()) {
                layoutParams.width = App.getInstance().screenHeight + getBottomKeyboardHeight();
                // 这里需要注意nexus系列的平板的布局，特别是navigation bar(虚拟按键栏)的影响
                layoutParams.height = App.getInstance().screenWidth - App.getInstance().stateBarHeight - getBottomKeyboardHeight();
                mParentContainer.setLayoutParams(layoutParams);
                mIfCurrentIsFullscreen = true;
                mFullscreenButton.setImageResource(getShrinkImageRes());
            }
        } else {
            //新版本isIfCurrentIsFullscreen的标志位内部提前设置了，所以不会和手动点击冲突
            if (isIfCurrentIsFullscreen()) {
                layoutParams.width = App.getInstance().screenWidth;
                layoutParams.height = 200 * App.getInstance().screenDensity;
                mParentContainer.setLayoutParams(layoutParams);
                mIfCurrentIsFullscreen = false;
                mFullscreenButton.setImageResource(getEnlargeImageRes());
            }
        }
    }

    /**
     * 亮度、进度、音频
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {

        int id = v.getId();
        float x = event.getX();
        float y = event.getY();

        if (id == R.id.start || id == R.id.volume_state || id == R.id.fullscreen) {
            return false;
        }

        if (id == R.id.parent_layout) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touchSurfaceDown(x, y);
                    if (needMute) {
                        mGestureDownVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    float deltaX = x - touchX;
                    float deltaY = y - touchY;
                    float absDeltaX = Math.abs(deltaX);
                    float absDeltaY = Math.abs(deltaY);
                    int curWidth = CommonUtil.getCurrentScreenLand(this) ? App.getInstance().screenHeight : App.getInstance().screenWidth;
                    // 触摸修改播放音量大小
                    if (x > curWidth * 4 / 5) {
                        if (absDeltaY > mVolumThreshold) {
                            needMute = true;
                            if (volumeState.getTag() != null && volumeState.getTag().equals("volume_mute")) {
                                volumeState.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.volume_valid));
                                volumeState.setTag("volume_valid");
                            }
                            mTouchMovingEvent = true;
                            mChangeVolume = true;
                            deltaY = -deltaY;
                            int max = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                            int deltaV = (int) deltaY * max * 1 / mParentContainer.getHeight();
                            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mGestureDownVolume + deltaV, 0);
                            showVolumeDialog(max, mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
                        }
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    // 重启在滑动进度条的时候关闭的定时器
                    mTouchMovingEvent = false;
                    dismissVolumeDialog();
                    break;
                default:
                    break;
            }
            if (!mTouchMovingEvent) {
                gestureDetector.onTouchEvent(event);
            }
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.start:
                clickStartIcon();
                break;
            case R.id.volume_state:
                // 需要静音
                if (needMute) {
                    mGestureDownVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
                    volumeState.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.volume_mute));
                    volumeState.setTag("volume_mute");
                } else {
                    mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mGestureDownVolume, 0);
                    volumeState.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.volume_valid));
                    volumeState.setTag("volume_valid");
                }
                needMute = !needMute;
                break;
            case R.id.fullscreen:
                fullScreenClick();
                break;
            default:
                break;
        }
    }

    private void fullScreenClick() {
        int screenOrientation = getResources().getConfiguration().orientation;
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mParentContainer.getLayoutParams();
        if (screenOrientation != 2) {
            // 即将切换成横屏
            layoutParams.width = App.getInstance().screenHeight + getBottomKeyboardHeight();
            layoutParams.height = App.getInstance().screenWidth - App.getInstance().stateBarHeight - getBottomKeyboardHeight();
            mParentContainer.setLayoutParams(layoutParams);
            mIfCurrentIsFullscreen = true;
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            mFullscreenButton.setImageResource(getShrinkImageRes());
        } else {
            // 即将切换成竖屏
            layoutParams.width = App.getInstance().screenWidth;
            layoutParams.height = 200 * App.getInstance().screenDensity;
            mParentContainer.setLayoutParams(layoutParams);
            mIfCurrentIsFullscreen = false;
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            mFullscreenButton.setImageResource(getEnlargeImageRes());
        }

        // 重置重力感应
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                LogUtil.i("RotationState", "run: " + MainActivity.rotationSettingState);
                if (!MainActivity.rotationSettingState)
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
            }
        }, 1000);
    }

    @Override
    public void eventBuffing(int event, float buffing) {
        if (buffing > 50.0) {
            // 视频的播放区间缓冲完成
            changeUiToPrepareingClear();
            mCurrentState = CURRENT_STATE_PLAYING;
            if (mBottomContainer.getVisibility() == View.VISIBLE) {
                startDismissControlViewTimer();
                startProgressTimer();
            }
        } else {
            mCurrentState = CURRENT_STATE_PLAYING_BUFFERING_START;
        }
    }

    @Override
    public void eventPlayInit(boolean openClose) {
        // 视频处于初始化中(准备中)
        mCurrentState = CURRENT_STATE_PREPAREING;
        if (openClose) {
            changeUiToPreparingShow();
        }
    }

    @Override
    public void eventStop(boolean isPlayError) {
        mCurrentState = CURRENT_STATE_STOP;
    }

    @Override
    public void eventError(int event, boolean show) {
        mCurrentState = CURRENT_STATE_ERROR;
    }

    @Override
    public void eventPlay(boolean isPlaying) {
        if (isPlaying) {
            //  todo 这里黑屏的处理，可能需要将上次播放的哪一帧保存截取成图片，
//            if (App.getInstance().currentPlayProgress > 0) {
//                vlcVideoView.getMediaPlayer().setTime(App.getInstance().currentPlayProgress);
//            }
            mCurrentState = CURRENT_STATE_PLAYING;
            startProgressTimer();
            if (mBottomContainer.getVisibility() == View.VISIBLE) {
                changeUiToPlayingShow();
            }
        } else {
            mCurrentState = CURRENT_STATE_PAUSE;
            cancelProgressTimer();
            changeUiToPauseShow();
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        int time = seekBar.getProgress() * vlcVideoView.getDuration() / 100;
        mCurrentTimeTextView.setText(CommonUtil.stringForTime(time));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        mTouchingProgressBar = true;
        cancelProgressTimer();
        cancelDismissControlViewTimer();
        setViewShowState(mStartButton, View.GONE);
        changeUiToPreparingShow();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        mTouchingProgressBar = false;
        try {
            final int time = seekBar.getProgress() * vlcVideoView.getDuration() / 100;
            vlcVideoView.seekTo(time);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        vlcVideoView.pause();
    }

    @Override
    protected void onStart() {
        super.onStart();
        vlcVideoView.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        vlcVideoView.onStop();
    }


    protected void touchSurfaceDown(float x, float y) {
        touchX = x;
        touchY = y;
        mChangeVolume = false;
    }

    /**
     * 点击触摸显示和隐藏逻辑
     */
    protected void onClickUiToggle() {
        // 加载中
        if (!vlcVideoView.isPlaying() && mCurrentState == CURRENT_STATE_PREPAREING) {
            if (mBottomContainer != null) {
                if (mBottomContainer.getVisibility() == View.VISIBLE) {
                    changeUiToPrepareingClear();
                } else {
                    changeUiToPreparingShow();
                }
            }
        } else if (vlcVideoView.isPlaying() && mCurrentState == CURRENT_STATE_PLAYING) { // 播放当中
            if (mBottomContainer != null) {
                if (mBottomContainer.getVisibility() == View.VISIBLE) {
                    changeUiToPlayingClear();
                } else {
                    changeUiToPlayingShow();
                }
            }
        } else if (!vlcVideoView.isPlaying() && mCurrentState == CURRENT_STATE_PAUSE) {
            if (mBottomContainer != null) {
                if (mBottomContainer.getVisibility() == View.VISIBLE) {
                    changeUiToPauseClear();
                } else {
                    changeUiToPauseShow();
                }
            }
        }
    }

    /**
     * 双击暂停/播放
     * 如果不需要，重载为空方法即可
     */
    protected void touchDoubleUp() {
        if (vlcVideoView.isPlaying() && mCurrentState == CURRENT_STATE_PLAYING) {
            vlcVideoView.pause();
        } else if (mCurrentState == CURRENT_STATE_PAUSE) {
            vlcVideoView.start();
        }
    }

    /**
     * 播放按键点击
     */
    protected void clickStartIcon() {
        if (mCurrentState == CURRENT_STATE_NORMAL || mCurrentState == CURRENT_STATE_ERROR) {
        } else if (vlcVideoView.isPlaying() && mCurrentState == CURRENT_STATE_PLAYING) {
            vlcVideoView.pause();
            setStateAndUi(CURRENT_STATE_PAUSE);
        } else if (mCurrentState == CURRENT_STATE_PAUSE) {
            vlcVideoView.start();
            setStateAndUi(CURRENT_STATE_PLAYING);
        } else if (mCurrentState == CURRENT_STATE_AUTO_COMPLETE) {
        } else if (!vlcVideoView.isPlaying() && mCurrentState == CURRENT_STATE_PLAYING_BUFFERING_VALID) {
            vlcVideoView.start();
            setStateAndUi(CURRENT_STATE_PLAYING);
        }
    }

    /**
     * 设置播放显示状态
     *
     * @param state
     */
    protected void setStateAndUi(int state) {
        mCurrentState = state;
        switch (mCurrentState) {
            case CURRENT_STATE_NORMAL:
                break;
            case CURRENT_STATE_PREPAREING:
                resetProgressAndTime();
                break;
            case CURRENT_STATE_ERROR:
                break;
            case CURRENT_STATE_AUTO_COMPLETE:
                cancelProgressTimer();
                if (mProgressBar != null) {
                    mProgressBar.setProgress(100);
                }
                if (mCurrentTimeTextView != null && mTotalTimeTextView != null) {
                    mCurrentTimeTextView.setText(mTotalTimeTextView.getText());
                }
                break;
        }
        resolveUIState(state);
    }

    /**
     * 处理控制显示
     *
     * @param state
     */
    protected void resolveUIState(int state) {
        switch (state) {
            case CURRENT_STATE_NORMAL:
                break;
            case CURRENT_STATE_PREPAREING:
                changeUiToPreparingShow();
                startDismissControlViewTimer();
                break;
            case CURRENT_STATE_PLAYING:
                changeUiToPlayingShow();
                startDismissControlViewTimer();
                break;
            case CURRENT_STATE_PAUSE:
                changeUiToPauseShow();
                cancelDismissControlViewTimer();
                break;
            case CURRENT_STATE_ERROR:
                changeUiToError();
                break;
            case CURRENT_STATE_AUTO_COMPLETE:
                changeUiToCompleteShow();
                cancelDismissControlViewTimer();
                break;
            case CURRENT_STATE_PLAYING_BUFFERING_START:
                changeUiToPlayingBufferingShow();
                break;
        }
    }

    /**
     * 刷新UI之播放状态展示
     */
    protected void changeUiToPreparingShow() {
        setViewShowState(mLoadingProgressBar, View.VISIBLE);
        if (mLoadingProgressBar instanceof SmileyLoadingView) {
            SmileyLoadingView enDownloadView = (SmileyLoadingView) mLoadingProgressBar;
            enDownloadView.start();
        }
    }

    /**
     * 刷新UI之装冲状态清除
     */
    protected void changeUiToPrepareingClear() {
        setViewShowState(mLoadingProgressBar, View.INVISIBLE);
        if (mLoadingProgressBar instanceof SmileyLoadingView) {
            ((SmileyLoadingView) mLoadingProgressBar).stop();
        }
    }

    /**
     * 刷新UI之播放状态展示
     */
    protected void changeUiToPlayingShow() {
        setViewShowState(mTopContainer, View.VISIBLE);
        setViewShowState(mBottomContainer, View.VISIBLE);
        setViewShowState(mStartButton, View.VISIBLE);
        setViewShowState(mLoadingProgressBar, View.INVISIBLE);
        setViewShowState(mThumbImageViewLayout, View.INVISIBLE);
        setViewShowState(mLockScreen, View.GONE);
        if (mLoadingProgressBar instanceof ENDownloadView) {
            ((ENDownloadView) mLoadingProgressBar).reset();
        }
        updateStartImage();
    }

    /**
     * 刷新UI之播放状态清除
     */
    protected void changeUiToPlayingClear() {
        setViewShowState(mTopContainer, View.INVISIBLE);
        setViewShowState(mBottomContainer, View.INVISIBLE);
        setViewShowState(mStartButton, View.INVISIBLE);
        setViewShowState(mLoadingProgressBar, View.INVISIBLE);
        setViewShowState(mThumbImageViewLayout, View.INVISIBLE);
        setViewShowState(mLockScreen, View.GONE);

        if (mLoadingProgressBar instanceof ENDownloadView) {
            ((ENDownloadView) mLoadingProgressBar).reset();
        }
    }

    /**
     * 刷新UI之暂停状态显示
     */
    protected void changeUiToPauseShow() {
        setViewShowState(mTopContainer, View.VISIBLE);
        setViewShowState(mBottomContainer, View.VISIBLE);
        setViewShowState(mStartButton, View.VISIBLE);
        setViewShowState(mLoadingProgressBar, View.INVISIBLE);
        setViewShowState(mThumbImageViewLayout, View.INVISIBLE);
        setViewShowState(mLockScreen, View.GONE);

        if (mLoadingProgressBar instanceof ENDownloadView) {
            ((ENDownloadView) mLoadingProgressBar).reset();
        }
        updateStartImage();
        updatePauseCover();
    }

    /**
     * 刷新UI之Pause状态清除Ui的显示
     */
    protected void changeUiToPauseClear() {
        changeUiToClear();
        if (!pauseCover.isRecycled())
            pauseCover.recycle();
    }

    /**
     * 更新暂停的封面
     */
    protected void updatePauseCover() {
        try {
            // 可以获取当前帧的画面
            pauseCover = vlcVideoView.getBitmap();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 清除控制菜单的UI
     */
    protected void changeUiToClear() {
        setViewShowState(mTopContainer, View.INVISIBLE);
        setViewShowState(mBottomContainer, View.INVISIBLE);
        setViewShowState(mStartButton, View.INVISIBLE);
        setViewShowState(mLoadingProgressBar, View.INVISIBLE);
        setViewShowState(mThumbImageViewLayout, View.INVISIBLE);
        setViewShowState(mLockScreen, View.GONE);

        if (mLoadingProgressBar instanceof ENDownloadView) {
            ((ENDownloadView) mLoadingProgressBar).reset();
        }
    }

    /**
     * 刷新UI之播放缓冲展示
     */
    protected void changeUiToPlayingBufferingShow() {
        setViewShowState(mLoadingProgressBar, View.VISIBLE);
        if (mLoadingProgressBar instanceof ENDownloadView) {
            ENDownloadView enDownloadView = (ENDownloadView) mLoadingProgressBar;
            if (enDownloadView.getCurrentState() == ENDownloadView.STATE_PRE) {
                ((ENDownloadView) mLoadingProgressBar).start();
            }
        }
    }

    /**
     * 刷新UI之播放缓冲清除
     */
    protected void changeUiToPlayingBufferingClear() {

        setViewShowState(mLoadingProgressBar, View.GONE);
        if (mLoadingProgressBar instanceof ENDownloadView) {
            ENDownloadView enDownloadView = (ENDownloadView) mLoadingProgressBar;
            if (enDownloadView.getCurrentState() == ENDownloadView.STATE_PRE) {
                ((ENDownloadView) mLoadingProgressBar).start();
            }
        }
        updateStartImage();
    }

    /**
     * 更新开始按键显示
     */
    protected void updateStartImage() {
        if (mStartButton instanceof ENPlayView) {
            ENPlayView enPlayView = (ENPlayView) mStartButton;
            enPlayView.setDuration(500);
            if (vlcVideoView.isPlaying()) {
                enPlayView.play();
            } else {
                enPlayView.pause();
            }
        } else if (mStartButton instanceof ImageView) {
            ImageView imageView = (ImageView) mStartButton;
            if (vlcVideoView.isPlaying()) {
                imageView.setImageResource(R.drawable.video_click_pause_selector);
            } else {
                imageView.setImageResource(R.drawable.video_click_play_selector);
            }
        }
    }

    /**
     * 刷新UI之视频播放完成
     */
    protected void changeUiToCompleteShow() {
        setViewShowState(mTopContainer, View.VISIBLE);
        setViewShowState(mBottomContainer, View.VISIBLE);
        setViewShowState(mStartButton, View.VISIBLE);
        setViewShowState(mLoadingProgressBar, View.INVISIBLE);
        setViewShowState(mThumbImageViewLayout, View.VISIBLE);
        setViewShowState(mLockScreen, View.GONE);

        if (mLoadingProgressBar instanceof ENDownloadView) {
            ((ENDownloadView) mLoadingProgressBar).reset();
        }
        updateStartImage();
    }

    /**
     * 刷新UI之视频播放出错
     */
    protected void changeUiToError() {
        setViewShowState(mTopContainer, View.INVISIBLE);
        setViewShowState(mBottomContainer, View.INVISIBLE);
        setViewShowState(mStartButton, View.VISIBLE);
        setViewShowState(mLoadingProgressBar, View.INVISIBLE);
        setViewShowState(mThumbImageViewLayout, View.INVISIBLE);
        setViewShowState(mLockScreen, View.GONE);
        if (mLoadingProgressBar instanceof ENDownloadView) {
            ((ENDownloadView) mLoadingProgressBar).reset();
        }
        updateStartImage();
    }

    protected void setViewShowState(View view, int visibility) {
        if (view != null) {
            view.setVisibility(visibility);
        }
    }

    protected void startDismissControlViewTimer() {
        cancelDismissControlViewTimer();
        mDismissControlViewTimer = new Timer();
        mDismissControlViewTimerTask = new DismissControlViewTimerTask();
        mDismissControlViewTimer.schedule(mDismissControlViewTimerTask, mDismissControlTime);
    }

    protected void cancelDismissControlViewTimer() {
        if (mDismissControlViewTimer != null) {
            mDismissControlViewTimer.cancel();
            mDismissControlViewTimer = null;
        }
        if (mDismissControlViewTimerTask != null) {
            mDismissControlViewTimerTask.cancel();
            mDismissControlViewTimerTask = null;
        }

    }

    protected void startProgressTimer() {
        cancelProgressTimer();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
            }
        }, 500);
        updateProcessTimer = new Timer();
        mProgressTimerTask = new ProgressTimerTask();
        updateProcessTimer.schedule(mProgressTimerTask, 0, 500);
    }

    protected void cancelProgressTimer() {
        if (updateProcessTimer != null) {
            updateProcessTimer.cancel();
            updateProcessTimer = null;
        }
        if (mProgressTimerTask != null) {
            mProgressTimerTask.cancel();
            mProgressTimerTask = null;
        }

    }

    private class ProgressTimerTask extends TimerTask {
        @Override
        public void run() {
            if (vlcVideoView.isPlaying()) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setTextAndProgress(0);
                    }
                });
            }
        }
    }

    protected void setTextAndProgress(int secProgress) {
        int position = vlcVideoView.getCurrentPosition();
        App.getInstance().currentPlayProgress = position;
        int duration = vlcVideoView.getDuration();
        int progress = position * 100 / (duration == 0 ? 1 : duration);
        setProgressAndTime(progress, secProgress, position, duration);
    }

    protected void setProgressAndTime(int progress, int secProgress, int currentTime, int totalTime) {
        if (mProgressBar == null || mTotalTimeTextView == null || mCurrentTimeTextView == null) {
            return;
        }

        if (!mTouchingProgressBar) {
            if (progress != 0) mProgressBar.setProgress(progress);
        }
        if (secProgress != 0 && !mCacheFile) {
            mProgressBar.setSecondaryProgress(secProgress);
        }
        mTotalTimeTextView.setText(CommonUtil.stringForTime(totalTime));
        if (currentTime > 0)
            mCurrentTimeTextView.setText(CommonUtil.stringForTime(currentTime));
    }

    /**
     * 重置进度条的时间
     */
    protected void resetProgressAndTime() {
        if (mProgressBar == null || mTotalTimeTextView == null || mCurrentTimeTextView == null) {
            return;
        }
        mProgressBar.setProgress(0);
        mProgressBar.setSecondaryProgress(0);
        mCurrentTimeTextView.setText(CommonUtil.stringForTime(0));
        mTotalTimeTextView.setText(CommonUtil.stringForTime(0));
    }

    private class DismissControlViewTimerTask extends TimerTask {

        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    hideAllWidget();
                    setViewShowState(mLockScreen, View.GONE);
                }
            });
        }
    }

    /**
     * 隐藏Control Menu所有的View
     */
    protected void hideAllWidget() {
        setViewShowState(mBottomContainer, View.INVISIBLE);
        setViewShowState(mTopContainer, View.INVISIBLE);
        setViewShowState(mStartButton, View.INVISIBLE);
    }

    /**
     * 触摸音量dialog
     */
    protected void showVolumeDialog(int max, int volumePercent) {
        if (mVolumeDialog == null) {
            View localView = LayoutInflater.from(this).inflate(R.layout.video_volume_dialog, null);
            if (localView.findViewById(R.id.volume_progressbar) instanceof ProgressBar) {
                mDialogVolumeProgressBar = ((ProgressBar) localView.findViewById(R.id.volume_progressbar));
                if (mDialogVolumeProgressBar != null) {
                    mDialogVolumeProgressBar.setMax(max);
                }
            }
            mVolumeDialog = new Dialog(this, R.style.video_style_dialog_progress);
            mVolumeDialog.setContentView(localView);
            mVolumeDialog.getWindow().addFlags(8);
            mVolumeDialog.getWindow().addFlags(32);
            mVolumeDialog.getWindow().addFlags(16);
            mVolumeDialog.getWindow().setLayout(-2, -2);
            WindowManager.LayoutParams localLayoutParams = mVolumeDialog.getWindow().getAttributes();
            localLayoutParams.gravity = Gravity.TOP | Gravity.LEFT;
            localLayoutParams.width = mParentContainer.getWidth();
            localLayoutParams.height = mParentContainer.getHeight();
            int location[] = new int[2];
            mParentContainer.getLocationOnScreen(location);

            localLayoutParams.x = location[0];
            localLayoutParams.y = location[1] - App.getInstance().stateBarHeight;
            Log.i("wandh", "showVolumeDialog: 的宽=" + mParentContainer.getWidth() + "-->高=" + mParentContainer.getHeight() + "--->location[0]=" + location[0] + "location[1]=" + location[1] + "location[1] - App.getInstance().stateBarHeight = " + (location[1] - App.getInstance().stateBarHeight));
            mVolumeDialog.getWindow().setAttributes(localLayoutParams);
        }
        if (!mVolumeDialog.isShowing()) {
            mVolumeDialog.show();
        }
        if (mDialogVolumeProgressBar != null) {
            mDialogVolumeProgressBar.setProgress(volumePercent);
        }
    }

    protected void dismissVolumeDialog() {
        if (mVolumeDialog != null) {
            mVolumeDialog.dismiss();
            mVolumeDialog = null;
        }
    }


    /**
     * 是否全屏
     */
    public boolean isIfCurrentIsFullscreen() {
        return mIfCurrentIsFullscreen;
    }

    /**
     * 获取右下角退出全屏(显示收缩)到原尺寸按键资源id
     * 必须在setUp之前设置
     * 不设置使用默认
     */
    public int getShrinkImageRes() {
        if (mShrinkImageRes == -1) {
            return R.drawable.video_shrink;
        }
        return mShrinkImageRes;
    }


    public int getEnlargeImageRes() {
        if (mEnlargeImageRes == -1) {
            return R.drawable.video_enlarge;
        }
        return mEnlargeImageRes;
    }

    /**
     * 获取底部虚拟键盘的高度
     */
    public int getBottomKeyboardHeight() {
        int screenHeight = getAccurateScreenDpi()[1];
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int heightDifference = screenHeight - dm.heightPixels;
        return heightDifference;
    }

    /**
     * 获取精确的屏幕大小
     */
    public int[] getAccurateScreenDpi() {
        int[] screenWH = new int[2];
        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics dm = new DisplayMetrics();
        try {
            Class<?> c = Class.forName("android.view.Display");
            Method method = c.getMethod("getRealMetrics", DisplayMetrics.class);
            method.invoke(display, dm);
            screenWH[0] = dm.widthPixels;
            screenWH[1] = dm.heightPixels;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return screenWH;
    }
}
