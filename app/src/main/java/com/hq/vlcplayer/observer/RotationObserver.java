package com.hq.vlcplayer.observer;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;

import com.hq.vlcplayer.MainActivity;
import com.hq.vlcplayer.view.utils.LogUtil;

/**
 * 观察屏幕旋转设置变化
 */
public class RotationObserver extends ContentObserver {
    ContentResolver mResolver;

    public RotationObserver(Context context, Handler handler) {
        super(handler);
        mResolver = context.getContentResolver();
    }

    //屏幕旋转设置改变时调用  
    @Override
    public void onChange(boolean selfChange) {
        // TODO Auto-generated method stub  
        super.onChange(selfChange);
        MainActivity.rotationSettingState = selfChange;
        LogUtil.i("RotationState", "onChange: " + MainActivity.rotationSettingState);
    }

    public void startObserver() {
        mResolver.registerContentObserver(Settings.System
                        .getUriFor(Settings.System.ACCELEROMETER_ROTATION), false,
                this);
    }

    public void stopObserver() {
        mResolver.unregisterContentObserver(this);
    }
} 