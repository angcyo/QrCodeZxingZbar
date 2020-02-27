package com.angcyo.rcode.core;

import com.angcyo.rcode.camera.CameraManager;
import com.google.zxing.ResultPoint;

/**
 * Email:angcyo@126.com
 *
 * @author angcyo
 * @date 2020/02/27
 */
public interface IViewfinderView {
    void addPossibleResultPoint(ResultPoint point);

    void drawViewfinder();

    void setCameraManager(CameraManager cameraManager);
}
