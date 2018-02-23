package com.hq.vlcplayer.application;

import android.app.Application;
import android.content.Context;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import com.danikula.videocache.HttpProxyCacheServer;
import com.hq.vlcplayer.view.utils.CommonUtil;

/**
 * Created by yyl on 2017/11/30.
 */

public class App extends Application {

    public static App mDemoApplication = null;

    private Context context;

    public static App getInstance() {
        return mDemoApplication;
    }

    public int screenDensity; // 屏幕的密度
    public int screenWidth; // 屏幕的宽
    public int screenHeight; // 屏幕的高
    public int stateBarHeight; // 手机状态栏的高度

    public int currentPlayProgress = 0;

    /**
     * 获取Content
     *
     * @return
     */
    public Context getContext() {
        return context;
    }

    private HttpProxyCacheServer proxy;

    public static HttpProxyCacheServer getProxy(Context context) {
        App app = (App) context.getApplicationContext();
        return app.proxy == null ? (app.proxy = app.newProxy()) : app.proxy;
    }

    /**
     * 注意被app 安全软件禁网后 127.0.0.1 的getLocalPort 会被限制权限 而闪退
     *
     */
    private HttpProxyCacheServer newProxy() {
        return new HttpProxyCacheServer(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        mDemoApplication = this;

        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        screenDensity = (int) displayMetrics.density;
        screenWidth = displayMetrics.widthPixels;
        screenHeight = displayMetrics.heightPixels;
        stateBarHeight = CommonUtil.getStatusBarHeight(this);

        // copy 字库到SD中
        CommonUtil.deepFile(getContext(), "test2.srt", Environment.getExternalStorageDirectory().getAbsolutePath());
    }

}
