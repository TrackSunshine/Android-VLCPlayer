package com.hq.vlcplayer.view.widget;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.hq.vlcplayer.R;
import com.hq.vlcplayer.application.App;
import com.hq.vlcplayer.view.utils.CommonUtil;

import org.videolan.vlc.listener.MediaListenerEvent;

import java.util.Timer;
import java.util.TimerTask;


/**
 * Created by hq on 2018/2/13
 *
 * 布局内实现控制菜单的功能
 *
 */

public class VLCControlView extends FrameLayout implements View.OnClickListener, View.OnTouchListener, MediaListenerEvent, SeekBar.OnSeekBarChangeListener {

    // 正常
    public static final int CURRENT_STATE_NORMAL = 0;
    // 准备中
    public static final int CURRENT_STATE_PREPAREING = 1;
    // 播放中
    public static final int CURRENT_STATE_PLAYING = 2;
    // 开始缓冲
    public static final int CURRENT_STATE_PLAYING_BUFFERING_START = 3;
    // 暂停
    public static final int CURRENT_STATE_PAUSE = 4;
    // 自动播放结束
    public static final int CURRENT_STATE_AUTO_COMPLETE = 5;
    // 错误状态
    public static final int CURRENT_STATE_STOP = 6;
    // 停止状态
    public static final int CURRENT_STATE_ERROR = 7;

    //上下文
    protected Context mContext;

    private float videoScale = 9f / 16f;

    private boolean isFullVideoState;

    //音频焦点的监听
    protected AudioManager mAudioManager;

    protected VLCScaleVideoView vlcVideoView;

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

    // 退出全屏显示的案件图片
    protected int mShrinkImageRes = -1;

    //全屏显示的案件图片
    protected int mEnlargeImageRes = -1;

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

    //当前的播放状态
    protected int mCurrentState = -1;

    // 是否是触摸滑动事件
    protected boolean mTouchMovingEvent = false;

    // 是否是缓存的文件
    protected boolean mCacheFile = false;

    // 触摸滑动的X,Y坐标
    private float touchX , touchY;

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
    protected int mDismissControlTime = 3000;
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

    /**
     * 双击
     */
    protected GestureDetector gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
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

    /**
     * 获取全屏按键，全屏播放的时候在外面设置其事件处理
     */
    public ImageView getFullscreenButton() {
        return mFullscreenButton;
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

    public VLCControlView(Context context) {
        this(context, null);
    }

    public VLCControlView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VLCControlView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        mAudioManager = (AudioManager) getActivityContext().getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
    }

    protected Context getActivityContext() {
        return CommonUtil.getActivityContext(getContext());
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        isFullVideoState = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        onMeasureVideo(widthMeasureSpec, heightMeasureSpec, getLayoutParams().height);
    }

    private void onMeasureVideo(int widthMeasureSpec, int heightMeasureSpec, int heightParams) {
        int measureWidth = MeasureSpec.getSize(widthMeasureSpec);
        int measureHeight = MeasureSpec.getSize(heightMeasureSpec);
        measureHeight = isFullVideoState ? measureHeight : (int) (measureWidth * videoScale);
        setMeasuredDimension(measureWidth, measureHeight);
        if (isFullVideoState && heightParams != LayoutParams.MATCH_PARENT) {
            getLayoutParams().height = LayoutParams.MATCH_PARENT;
            if (isInEditMode()) {
                setMeasuredDimension(measureWidth, measureHeight);
            }
            return;
        } else if (!isFullVideoState && heightParams == LayoutParams.MATCH_PARENT) {
            getLayoutParams().height = measureHeight;
            if (isInEditMode()) {
                setMeasuredDimension(measureWidth, measureHeight);
            }
            return;
        }

        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                final int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(measureWidth, MeasureSpec.EXACTLY);
                final int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(measureHeight, MeasureSpec.EXACTLY);
                child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
            }
        }
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                child.layout(0, 0, getMeasuredWidth(), getMeasuredHeight());
            }
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        bindViews(getContext());
        bindEvents();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        vlcVideoView.onStop();
        vlcVideoView.onDestory();
    }

    protected void bindViews(Context context) {
        vlcVideoView = (VLCScaleVideoView) findViewById(R.id.player);
        mStartButton = findViewById(R.id.start);
        videoControlLayout = (RelativeLayout) findViewById(R.id.parent_layout);
        mTitleTextView = (TextView) findViewById(R.id.title);
        mBackButton = (ImageView) findViewById(R.id.back);
        volumeState = findViewById(R.id.volume_state);
        mFullscreenButton = findViewById(R.id.fullscreen);
        mProgressBar = (SeekBar) findViewById(R.id.progress);
        mCurrentTimeTextView = (TextView) findViewById(R.id.current);
        mTotalTimeTextView = (TextView) findViewById(R.id.total);
        mBottomContainer = (ViewGroup) findViewById(R.id.layout_bottom);
        mTopContainer = (ViewGroup) findViewById(R.id.layout_top);
        mThumbImageViewLayout = (RelativeLayout) findViewById(R.id.thumb);
        mLockScreen = (ImageView) findViewById(R.id.lock_screen);
        mLoadingProgressBar = findViewById(R.id.loading);
    }

    public void bindEvents() {
        vlcVideoView.setMediaListenerEvent(this);
        if (mStartButton != null) {
            mStartButton.setOnClickListener(this);
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

    /**
     * 音频的控制实现(亮度、进度实现方式相同)
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {

        int id = v.getId();
        float x = event.getX();
        float y = event.getY();

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
                    int curWidth = CommonUtil.getCurrentScreenLand((Activity) getActivityContext()) ? App.getInstance().screenHeight : App.getInstance().screenWidth;
                    int curHeight = CommonUtil.getCurrentScreenLand((Activity) getActivityContext()) ? App.getInstance().screenWidth : App.getInstance().screenHeight;
                    // 触摸修改播放音量大小
                    if (x > curWidth * 4 / 5) {
                        if (absDeltaY > mVolumThreshold) {
                            needMute = true;
                            if (volumeState.getTag() != null && volumeState.getTag().equals("volume_mute")) {
                                volumeState.setImageBitmap(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.volume_valid));
                                volumeState.setTag("volume_valid");
                            }
                            mTouchMovingEvent = true;
                            mChangeVolume = true;
                            deltaY = -deltaY;
                            int max = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                            int deltaV = (int) deltaY * max * 2 / getHeight();
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
            if (!mTouchMovingEvent){
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
                    volumeState.setImageBitmap(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.volume_mute));
                    volumeState.setTag("volume_mute");
                } else {
                    mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mGestureDownVolume, 0);
                    volumeState.setImageBitmap(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.volume_valid));
                    volumeState.setTag("volume_valid");
                }
                needMute = !needMute;
                break;
            default:
                break;
        }
    }

    @Override
    public void eventBuffing(int event, float buffing) {
        if (buffing > 0.0) {
            // 视频的播放区间缓冲完成
            changeUiToPrepareingClear();
            mCurrentState = CURRENT_STATE_PLAYING;
            if (mBottomContainer.getVisibility() == VISIBLE) {
                setViewShowState(mStartButton, VISIBLE);
                startDismissControlViewTimer();
                startProgressTimer();
            }
        } else{
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
            mCurrentState = CURRENT_STATE_PLAYING;
            startProgressTimer();
            if (mBottomContainer.getVisibility() == View.VISIBLE) {
                changeUiToPlayingShow();
            }
        } else {
            mCurrentState = CURRENT_STATE_PAUSE;
            cancelProgressTimer();
            if (mBottomContainer.getVisibility() == View.VISIBLE) {
                changeUiToPauseShow();
            }
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
        setViewShowState(mStartButton, GONE);
        changeUiToPreparingShow();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        mTouchingProgressBar = false;
        try {
            int time = seekBar.getProgress() * vlcVideoView.getDuration() / 100;
            vlcVideoView.seekTo(time);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        setViewShowState(mLoadingProgressBar, VISIBLE);
        if (mLoadingProgressBar instanceof SmileyLoadingView) {
            SmileyLoadingView enDownloadView = (SmileyLoadingView) mLoadingProgressBar;
            enDownloadView.start();
        }
    }

    /**
     * 刷新UI之装冲状态清除
     */
    protected void changeUiToPrepareingClear() {
        setViewShowState(mLoadingProgressBar, INVISIBLE);
        if (mLoadingProgressBar instanceof SmileyLoadingView) {
            ((SmileyLoadingView) mLoadingProgressBar).stop();
        }
    }

    /**
     * 刷新UI之播放状态展示
     */
    protected void changeUiToPlayingShow() {
        setViewShowState(mTopContainer, VISIBLE);
        setViewShowState(mBottomContainer, VISIBLE);
        setViewShowState(mStartButton, VISIBLE);
        setViewShowState(mLoadingProgressBar, INVISIBLE);
        setViewShowState(mThumbImageViewLayout, INVISIBLE);
        setViewShowState(mLockScreen, GONE);
        if (mLoadingProgressBar instanceof ENDownloadView) {
            ((ENDownloadView) mLoadingProgressBar).reset();
        }
        updateStartImage();
    }

    /**
     * 刷新UI之播放状态清除
     */
    protected void changeUiToPlayingClear() {
        setViewShowState(mTopContainer, INVISIBLE);
        setViewShowState(mBottomContainer, INVISIBLE);
        setViewShowState(mStartButton, INVISIBLE);
        setViewShowState(mLoadingProgressBar, INVISIBLE);
        setViewShowState(mThumbImageViewLayout, INVISIBLE);
        setViewShowState(mLockScreen, GONE);

        if (mLoadingProgressBar instanceof ENDownloadView) {
            ((ENDownloadView) mLoadingProgressBar).reset();
        }
    }

    /**
     * 刷新UI之暂停状态显示
     */
    protected void changeUiToPauseShow() {
        setViewShowState(mTopContainer, VISIBLE);
        setViewShowState(mBottomContainer, VISIBLE);
        setViewShowState(mStartButton, VISIBLE);
        setViewShowState(mLoadingProgressBar, INVISIBLE);
        setViewShowState(mThumbImageViewLayout, INVISIBLE);
        setViewShowState(mLockScreen, GONE);

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
        updatePauseCover();
    }

    /**
     * 更新暂停的封面，暂未使用
     */
    protected void updatePauseCover() {
    }

    /**
     * 清除控制菜单的UI
     */
    protected void changeUiToClear() {
        setViewShowState(mTopContainer, INVISIBLE);
        setViewShowState(mBottomContainer, INVISIBLE);
        setViewShowState(mStartButton, INVISIBLE);
        setViewShowState(mLoadingProgressBar, INVISIBLE);
        setViewShowState(mThumbImageViewLayout, INVISIBLE);
        setViewShowState(mLockScreen, GONE);

        if (mLoadingProgressBar instanceof ENDownloadView) {
            ((ENDownloadView) mLoadingProgressBar).reset();
        }
    }

    /**
     * 刷新UI之播放缓冲展示
     */
    protected void changeUiToPlayingBufferingShow() {
        setViewShowState(mLoadingProgressBar, VISIBLE);
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

        setViewShowState(mLoadingProgressBar, GONE);
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
        setViewShowState(mTopContainer, VISIBLE);
        setViewShowState(mBottomContainer, VISIBLE);
        setViewShowState(mStartButton, VISIBLE);
        setViewShowState(mLoadingProgressBar, INVISIBLE);
        setViewShowState(mThumbImageViewLayout, VISIBLE);
        setViewShowState(mLockScreen, GONE);

        if (mLoadingProgressBar instanceof ENDownloadView) {
            ((ENDownloadView) mLoadingProgressBar).reset();
        }
        updateStartImage();
    }

    /**
     * 刷新UI之视频播放出错
     */
    protected void changeUiToError() {
        setViewShowState(mTopContainer, INVISIBLE);
        setViewShowState(mBottomContainer, INVISIBLE);
        setViewShowState(mStartButton, VISIBLE);
        setViewShowState(mLoadingProgressBar, INVISIBLE);
        setViewShowState(mThumbImageViewLayout, INVISIBLE);
        setViewShowState(mLockScreen, GONE);
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
        updateProcessTimer = new Timer();
        mProgressTimerTask = new ProgressTimerTask();
        updateProcessTimer.schedule(mProgressTimerTask, 0, 300);
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
                post(new Runnable() {
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
            if (getActivityContext() != null) {
                ((Activity) getActivityContext()).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hideAllWidget();
                        setViewShowState(mLockScreen, GONE);
                    }
                });
            }
        }
    }

    /**
     * 隐藏Control Menu所有的View
     */
    protected void hideAllWidget() {
        setViewShowState(mBottomContainer, INVISIBLE);
        setViewShowState(mTopContainer, INVISIBLE);
        setViewShowState(mStartButton, INVISIBLE);
    }

    /**
     * 触摸音量dialog
     */
    protected void showVolumeDialog(int max, int volumePercent) {
        if (mVolumeDialog == null) {
            View localView = LayoutInflater.from(getActivityContext()).inflate(R.layout.video_volume_dialog, null);
            if (localView.findViewById(R.id.volume_progressbar) instanceof ProgressBar) {
                mDialogVolumeProgressBar = ((ProgressBar) localView.findViewById(R.id.volume_progressbar));
                if (mDialogVolumeProgressBar != null) {
                    mDialogVolumeProgressBar.setMax(max);
                }
            }
            mVolumeDialog = new Dialog(getActivityContext(), R.style.video_style_dialog_progress);
            mVolumeDialog.setContentView(localView);
            mVolumeDialog.getWindow().addFlags(8);
            mVolumeDialog.getWindow().addFlags(32);
            mVolumeDialog.getWindow().addFlags(16);
            mVolumeDialog.getWindow().setLayout(-2, -2);
            WindowManager.LayoutParams localLayoutParams = mVolumeDialog.getWindow().getAttributes();
            localLayoutParams.gravity = Gravity.TOP | Gravity.LEFT;
            localLayoutParams.width = getWidth();
            localLayoutParams.height = getHeight();
            int location[] = new int[2];
            getLocationOnScreen(location);
            localLayoutParams.x = location[0];
            localLayoutParams.y = location[1];
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

}