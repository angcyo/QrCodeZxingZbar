package com.angcyo.rcode.core;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;

import com.angcyo.rcode.camera.CameraManager;

/**
 * Email:angcyo@126.com
 *
 * @author angcyo
 * @date 2018/02/27 14:00
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
public interface IActivity {
    CameraManager getCameraManager();

    Handler getHandler();

    Activity getHandleActivity();

    IViewfinderView getViewfinderView();

    /**
     * 扫描返回后的结果
     */
    void handleDecode(String data);

    void setResult(int resultCode, Intent data);

    void finish();

    PackageManager getPackageManager();

    void startActivity(Intent intent);

    void drawViewfinder();

}
