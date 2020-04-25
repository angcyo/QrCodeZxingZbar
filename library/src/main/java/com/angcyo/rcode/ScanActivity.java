package com.angcyo.rcode;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentFactory;
import androidx.fragment.app.FragmentTransaction;

/**
 * https://github.com/angcyo/QrCodeZxingZbar
 * <p>
 * Email:angcyo@126.com
 *
 * @author angcyo
 * @date 2019/06/03
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */

public class ScanActivity extends AppCompatActivity implements IHandleDecode {

    /**
     * 存放返回数据的key
     */
    public static final String KEY_DATA = "key_data";
    public static final String KEY_TARGET = "key_target";
    /**
     * 请求code
     */
    public static final int REQUEST_CODE = 0x909;

    public static void start(@NonNull Activity activity) {
        start(activity, null);
    }

    public static void start(@NonNull Activity activity, @Nullable Class<? extends ScanFragment> target) {
        start(activity, ScanActivity.class, target);
    }

    public static void start(@NonNull Activity activity, @NonNull Class<? extends ScanActivity> targetActivity, @Nullable Class<? extends ScanFragment> target) {
        Intent intent = new Intent(activity, targetActivity);
        intent.putExtra(KEY_TARGET, target);
        activity.startActivityForResult(intent, REQUEST_CODE);
    }

    /**
     * 返回值获取
     */
    @Nullable
    public static String onResult(int requestCode, int resultCode, @Nullable Intent data) {
        String result = null;
        if (resultCode == Activity.RESULT_OK) {
            if (data != null) {
                result = data.getStringExtra(KEY_DATA);
            }
        }
        return result;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initScanLayout(savedInstanceState);
    }

    protected void initScanLayout(@Nullable Bundle savedInstanceState) {
        FrameLayout rootLayout = new FrameLayout(this);
        rootLayout.setId(R.id.qr_code_frame_layout);
        rootLayout.setBackgroundColor(Color.BLACK);
        setContentView(rootLayout, new ViewGroup.LayoutParams(-1, -1));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        rootLayout.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            onPermissionsGranted();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onPermissionsGranted();
            }
        }
    }

    protected void onPermissionsGranted() {
        Class target = ScanFragment.class;

        Intent intent = getIntent();
        if (intent != null) {
            Class targetClass = (Class) intent.getSerializableExtra(KEY_TARGET);
            if (targetClass != null) {
                target = targetClass;
            }
        }
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        Fragment targetFragment = new FragmentFactory().instantiate(getClassLoader(), target.getName());
        if (intent != null) {
            targetFragment.setArguments(intent.getExtras());
        }
        fragmentTransaction.add(R.id.qr_code_frame_layout, targetFragment);
        fragmentTransaction.commit();
    }

    @Override
    public boolean handleDecode(@NonNull String data) {
        Intent intent = new Intent();
        intent.putExtra(KEY_DATA, data);
        setResult(Activity.RESULT_OK, intent);
        finish();
        return false;
    }
}
