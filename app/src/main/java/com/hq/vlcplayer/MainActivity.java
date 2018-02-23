package com.hq.vlcplayer;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.hq.vlcplayer.observer.RotationObserver;
import com.hq.vlcplayer.view.utils.LogUtil;

import org.videolan.vlc.util.VLCInstance;

public class MainActivity extends AppCompatActivity {

    public static String TAG = MainActivity.class.getSimpleName();

    private Button btn, btn1;

    public static boolean rotationSettingState = false; // 屏幕方向锁定为 true

//    private static final String path = "https://s3-ap-northeast-1.amazonaws.com/dev-contents.actibookone.com/fortest/94M.mp4";
    private static final String path = "https://res.exexm.com/cw_145225549855002";

    private RotationObserver mRotationObserver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initLibs();
        bindViews();
        bindEvent();
        initDatas();
    }

    private void initDatas() {
        mRotationObserver = new RotationObserver(this, new Handler());
        rotationSettingState = getRotationStatus(this) == 0 ? true : false;
        LogUtil.i("RotationState", "initDatas: " + MainActivity.rotationSettingState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mRotationObserver.startObserver();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRotationObserver.stopObserver();
    }

    //得到屏幕旋转的状态
    private int getRotationStatus(Context context) {
        int status = 0;
        try {
            status = android.provider.Settings.System.getInt(context.getContentResolver(),
                    android.provider.Settings.System.ACCELEROMETER_ROTATION);
        } catch (Settings.SettingNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return status;
    }

    private void bindEvent() {
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, VlcVideoPlayerActivity.class));
            }
        });

        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, VlcVideoScalePlayerActivity.class));
            }
        });
    }

    private void bindViews() {
        btn = (Button) findViewById(R.id.btn);
        btn1 = (Button) findViewById(R.id.btn1);
    }

    // 加载VLC的库
    private void initLibs() {
        if (VLCInstance.testCompatibleCPU(this)) {
            LogUtil.i(TAG, "support   cpu");
        } else {
            Log.i(TAG, "not support  cpu");
        }
    }

    public static String getUrl(Context context) {
        return path;
    }

}
