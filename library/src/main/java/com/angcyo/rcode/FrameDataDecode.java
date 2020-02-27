package com.angcyo.rcode;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;

import java.io.ByteArrayOutputStream;

/**
 * Email:angcyo@126.com
 *
 * @author angcyo
 * @date 2019/09/07
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
public class FrameDataDecode {
    public static int previewFormat = ImageFormat.NV21;
    public static IFrameDataDecode iFrameDataDecode;

    /**
     * 将camera 的 帧数据, 转换成图片
     */
    public static void decode(byte[] data, int width, int height) {
        if (iFrameDataDecode != null) {
            //有时候会绿屏
            YuvImage yuv = new YuvImage(data, previewFormat, width, height, null);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuv.compressToJpeg(new Rect(0, 0, width, height), 50, out);

            iFrameDataDecode.onFrameDataDecode(out.toByteArray());
        }
    }

    public static void release() {
        iFrameDataDecode = null;
    }

    public interface IFrameDataDecode {
        void onFrameDataDecode(byte[] data);
    }
}