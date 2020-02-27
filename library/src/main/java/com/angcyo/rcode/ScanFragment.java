package com.angcyo.rcode;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.angcyo.rcode.camera.CameraManager;
import com.angcyo.rcode.core.AmbientLightManager;
import com.angcyo.rcode.core.BeepManager;
import com.angcyo.rcode.core.CaptureActivityHandler;
import com.angcyo.rcode.core.IActivity;
import com.angcyo.rcode.core.IViewfinderView;
import com.angcyo.rcode.core.InactivityTimer;
import com.angcyo.rcode.core.ViewfinderView;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.Result;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * 二维码扫描
 * <p>
 * https://github.com/angcyo/QrCodeZxingZbar
 * <p>
 * Email:angcyo@126.com
 *
 * @author angcyo
 * @date 2019/05/23
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
public class ScanFragment extends Fragment implements IActivity, SurfaceHolder.Callback {

    //<editor-fold desc="接口回调">

    @Override
    public void surfaceCreated(final SurfaceHolder holder) {
        if (holder == null) {
            Log.e("ScanFragment", "*** WARNING *** surfaceCreated() gave us a null surface!");
        }
        if (!hasSurface) {
            hasSurface = true;

            View view = getView();
            if (view != null) {
                view.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        initCamera(holder);
                    }
                }, 16);
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
    }

    @Override
    public CameraManager getCameraManager() {
        return cameraManager;
    }

    @Override
    public Handler getHandler() {
        return handler;
    }

    @Override
    public Activity getHandleActivity() {
        return requireActivity();
    }

    @Override
    public IViewfinderView getViewfinderView() {
        return viewfinderView;
    }

    /**
     * 处理扫码返回的结果
     */
    @Override
    public void handleDecode(String data) {
        if (TextUtils.isEmpty(data)) {
        } else {
            Activity handleActivity = getHandleActivity();
            if (handleActivity instanceof IHandleDecode) {
                if (((IHandleDecode) handleActivity).handleDecode(data)) {
                    return;
                }
            }
            playVibrate();
            beepManager.playBeepSoundAndVibrate();
        }
    }

    @Override
    public void setResult(int resultCode, Intent data) {

    }

    @Override
    public void finish() {

    }

    @Override
    public PackageManager getPackageManager() {
        return getHandleActivity().getPackageManager();
    }

    @Override
    public void drawViewfinder() {
        viewfinderView.drawViewfinder();
    }

    //</editor-fold desc="接口回调">

    //<editor-fold desc="成员变量">

    protected boolean hasSurface = false;
    protected InactivityTimer inactivityTimer;
    protected BeepManager beepManager;
    protected AmbientLightManager ambientLightManager;
    protected CameraManager cameraManager;
    protected CaptureActivityHandler handler;
    protected Result lastResult;
    protected SurfaceView surfaceView;
    protected ViewfinderView viewfinderView;
    protected Long VIBRATE_DURATION = 50L;

    protected Collection<BarcodeFormat> decodeFormats;
    protected Map<DecodeHintType, Object> decodeHints;
    protected String characterSet;

    protected Result savedResultToShow;

    //</editor-fold desc="成员变量">

    //<editor-fold desc="初始化方法">

    protected int getLayoutId() {
        return R.layout.qr_code_scan_layout;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(getLayoutId(), container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initFragment(view, savedInstanceState);
    }

    protected void initFragment(@NonNull View view, @Nullable Bundle savedInstanceState) {
        hasSurface = false;
        inactivityTimer = new InactivityTimer(getHandleActivity());
        beepManager = new BeepManager(getHandleActivity());
        ambientLightManager = new AmbientLightManager(getHandleActivity().getApplicationContext());
        cameraManager = new CameraManager(getHandleActivity().getApplicationContext());

        surfaceView = view.findViewById(R.id.qr_code_preview_view);
        viewfinderView = view.findViewById(R.id.qr_code_viewfinder_view);
        viewfinderView.laserColor = ContextCompat.getColor(getHandleActivity(), R.color.colorAccent);

        viewfinderView.setCameraManager(cameraManager);

        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        if (hasSurface) {
            // The activity was paused but not stopped, so the surface still exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
            initCamera(surfaceHolder);
        } else {
            // Install the callback and wait for surfaceCreated() to init the camera.
            surfaceHolder.addCallback(this);
        }
    }

    protected void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }

        if (cameraManager.isOpen()) {
            Log.w("ScanFragment", "initCamera() while already open -- late SurfaceView callback?");
            return;
        }

        if (ActivityCompat.checkSelfPermission(getHandleActivity(), Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED) {

            try {
                cameraManager.openDriver(surfaceHolder);
                // Creating the handler starts the preview, which can also throw a RuntimeException.
                if (handler == null) {
                    handler = new CaptureActivityHandler(this,
                            decodeFormats, decodeHints, characterSet, cameraManager);
                }
                decodeOrStoreSavedBitmap(null, null);
            } catch (IOException ioe) {
                Log.w("ScanFragment", "$ioe");
                displayFrameworkBugMessageAndExit();
            } catch (RuntimeException e) {
                // Barcode Scanner has seen crashes in the wild of this variety:
                // java.?lang.?RuntimeException: Fail to connect to camera service
                Log.w("ScanFragment", "Unexpected error initializing camera :$e");
                displayFrameworkBugMessageAndExit();
            }
        } else {
            Log.w("ScanFragment", "请检查权限.");
            displayFrameworkBugMessageAndExit();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (getView() != null) {
            getView().setKeepScreenOn(true);
        }

        handler = null;
        lastResult = null;

        beepManager.updatePrefs();

        ambientLightManager.start(cameraManager);

        inactivityTimer.onResume();

        if (hasSurface) {
            // The activity was paused but not stopped, so the surface still exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
            //initCamera(surfaceView.holder)
            initCamera(surfaceView.getHolder());
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (getView() != null) {
            getView().setKeepScreenOn(false);
        }

        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        inactivityTimer.onPause();
        ambientLightManager.stop();
        beepManager.close();
        cameraManager.closeDriver();
        //historyManager = null; // Keep for onActivityResult
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        surfaceView.getHolder().removeCallback(this);
        inactivityTimer.shutdown();
    }

    //</editor-fold desc="初始化方法">

    //<editor-fold desc="处理方法">

    /**
     * 打开摄像头出错
     */
    protected void displayFrameworkBugMessageAndExit() {

    }

    protected void decodeOrStoreSavedBitmap(Bitmap bitmap, Result result) {
        // Bitmap isn't used yet -- will be used soon
        if (handler == null) {
            savedResultToShow = result;
        } else {
            if (result != null) {
                savedResultToShow = result;
            }
            if (savedResultToShow != null) {
                Message message = Message.obtain(handler, R.id.decode_succeeded, savedResultToShow);
                if (handler != null) {
                    handler.sendMessage(message);
                }
            }
            savedResultToShow = null;
        }
    }

    /**
     * 震动一下, 需要权限VIBRATE
     */
    protected void playVibrate() {
        Vibrator vibrator = (Vibrator) getHandleActivity().getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(VIBRATE_DURATION);
    }

    //</editor-fold desc="处理方法">

    //<editor-fold desc="操作方法">

    /**
     * 打开闪光灯
     */
    public void openFlashlight(boolean open) {
        cameraManager.setTorch(open);
    }

    /**
     * 重新扫描
     */
    public void scanAgain(long delay) {
        if (handler != null) {
            handler.sendEmptyMessageDelayed(R.id.restart_preview, delay);
        }
    }

    //</editor-fold desc="操作方法">

}
