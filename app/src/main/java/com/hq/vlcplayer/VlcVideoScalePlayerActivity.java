package com.hq.vlcplayer;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.hq.vlcplayer.application.App;
import com.hq.vlcplayer.view.utils.LogUtil;
import com.hq.vlcplayer.view.widget.VLCControlView;
import com.hq.vlcplayer.view.widget.VLCScaleVideoView;
import com.hq.vlcplayer.view.widget.VLCVideoView;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.vlc.util.VLCInstance;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class VlcVideoScalePlayerActivity extends FragmentActivity {

    private VLCControlView mParentContainer;
    private VLCScaleVideoView vlcVideoView;
    private TextureView surface;//字幕画布
    //全屏按键
    protected ImageView mFullscreenButton;

    //本地字幕文件
    private File alaveFile = new File(Environment.getExternalStorageDirectory(), "text2.srt");

    private Handler mHandler = new Handler();

    //当前是否全屏
    protected boolean mIfCurrentIsFullscreen = false;

    /**
     * 是否全屏
     */
    public boolean isIfCurrentIsFullscreen() {
        return mIfCurrentIsFullscreen;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.framelayout_scale_video);
        bindViews();
        bindEvent();
//        vlcVideoView.startPlay(MainActivity.getUrl(this));
        startPlay();
    }

    private void bindViews() {
        mParentContainer = findViewById(R.id.frame_layout_scale);
        mFullscreenButton = findViewById(R.id.fullscreen);
        vlcVideoView = findViewById(R.id.player);
        surface = findViewById(R.id.surface);
    }

    private void bindEvent() {
        mFullscreenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fullScreenClick();
            }
        });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.i("onMeasureVideo", "onConfigurationChanged: -----> screen orientation changed");
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mParentContainer.getLayoutParams();
        if (newConfig.orientation == ActivityInfo.SCREEN_ORIENTATION_USER) { // 横屏的时候的标识
            if (!isIfCurrentIsFullscreen()) {
                layoutParams.width = App.getInstance().screenHeight + getBottomKeyboardHeight();
                // 这里需要注意nexus系列的平板的布局，特别是navigation bar(虚拟按键栏)的影响
                layoutParams.height = App.getInstance().screenWidth - App.getInstance().stateBarHeight - getBottomKeyboardHeight();
                mParentContainer.setLayoutParams(layoutParams);
                mIfCurrentIsFullscreen = true;
                mParentContainer.getFullscreenButton().setImageResource(mParentContainer.getShrinkImageRes());
            }
        } else {
            //新版本isIfCurrentIsFullscreen的标志位内部提前设置了，所以不会和手动点击冲突
            if (isIfCurrentIsFullscreen()) {
                layoutParams.width = App.getInstance().screenWidth;
                layoutParams.height = 200 * App.getInstance().screenDensity;
                mParentContainer.setLayoutParams(layoutParams);
                mIfCurrentIsFullscreen = false;
                mParentContainer.getFullscreenButton().setImageResource(mParentContainer.getEnlargeImageRes());
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        vlcVideoView.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        vlcVideoView.pause();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        vlcVideoView.setAddSlave(null);
        vlcVideoView.onStop();
    }

    //打开硬件加速
    private void mediaDecodePlay() {
        Media media = new Media(VLCInstance.get(this), Uri.parse(MainActivity.getUrl(this)));
        media.setHWDecoderEnabled(true, false);
        vlcVideoView.setMedia(media);
        vlcVideoView.startPlay(null);
    }

    //自定义 源文件
    private void startPlay() {
        Log.i("ceshiing", "startPlay: ");
        ArrayList<String> options = new ArrayList<>();
        options.add("-vvv");//显示全部调试日志
        LibVLC libVLC = new LibVLC(this, options);
        Media media = new Media(libVLC, Uri.parse(MainActivity.getUrl(this)));
        MediaPlayer m = new MediaPlayer(libVLC);
        m.setTime(5000);
        vlcVideoView.setMediaPlayer(m);
        vlcVideoView.setMedia(media);
        surface.setVisibility(View.VISIBLE);
        vlcVideoView.setSurfaceSubtitlesView(surface);
        //字幕
        vlcVideoView.setAddSlave(alaveFile.getAbsolutePath());
        vlcVideoView.startPlay(null);
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
            mFullscreenButton.setImageResource(mParentContainer.getShrinkImageRes());
        } else {
            // 即将切换成竖屏
            layoutParams.width = App.getInstance().screenWidth;
            layoutParams.height = 200 * App.getInstance().screenDensity;
            mParentContainer.setLayoutParams(layoutParams);
            mIfCurrentIsFullscreen = false;
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            mFullscreenButton.setImageResource(mParentContainer.getEnlargeImageRes());
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
