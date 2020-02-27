/*
 * Copyright (C) 2010 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.angcyo.rcode.core;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.angcyo.rcode.BuildConfig;
import com.angcyo.rcode.FrameDataDecode;
import com.angcyo.rcode.R;
import com.angcyo.rcode.RCode;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.client.android.DecodeHandlerJni;
import com.google.zxing.common.HybridBinarizer;

import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;

import java.io.ByteArrayOutputStream;
import java.util.Map;

/**
 * https://github.com/XieZhiFa/ZxingZbar
 */
final class DecodeHandler extends Handler {
    private static final String TAG = DecodeHandler.class.getSimpleName();
    static boolean debug = BuildConfig.DEBUG;

    private final IActivity activity;
    private final MultiFormatReader multiFormatReader;
    //默认扫码类型, 自动切换
    DecoderMode mDecoderMode = DecoderMode.Zbar;
    long lastDecoderTime = 0L;
    private boolean running = true;

    DecodeHandler(IActivity activity, Map<DecodeHintType, Object> hints) {
        multiFormatReader = new MultiFormatReader();
        multiFormatReader.setHints(hints);
        this.activity = activity;
    }

    private static void bundleThumbnail(PlanarYUVLuminanceSource source, Bundle bundle) {
        int[] pixels = source.renderThumbnail();
        int width = source.getThumbnailWidth();
        int height = source.getThumbnailHeight();
        Bitmap bitmap = Bitmap.createBitmap(pixels, 0, width, width, height, Bitmap.Config.ARGB_8888);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out);
        bundle.putByteArray(DecodeThread.BARCODE_BITMAP, out.toByteArray());
        bundle.putFloat(DecodeThread.BARCODE_SCALED_FACTOR, (float) width / source.getWidth());
    }

    @Override
    public void handleMessage(Message message) {
        if (message == null || !running) {
            return;
        }
        if (message.what == R.id.decode) {
            decode((byte[]) message.obj, message.arg1, message.arg2);

        } else if (message.what == R.id.quit) {
            running = false;
            Looper.myLooper().quit();
        }
    }

    private void changeDecodeMode() {
        if (mDecoderMode == DecoderMode.Zxing) {
            mDecoderMode = DecoderMode.Zbar;
        } else {
            mDecoderMode = DecoderMode.Zxing;
        }
    }

    /**
     * Decode the data within the viewfinder rectangle, and time how long it took. For efficiency,
     * reuse the same reader objects from one decode to the next.
     *
     * @param data   The YUV preview frame.
     * @param width  The width of the preview frame.
     * @param height The height of the preview frame.
     */
    private void decode(byte[] data, int width, int height) {
        long start = System.currentTimeMillis();

        if (lastDecoderTime == 0) {
            lastDecoderTime = start;
        }

        String resultQRcode = null;

        if (mDecoderMode == DecoderMode.Zxing) { //如果扫描模式是Zxing
//            // --- add java进行数组的转换 速度很慢
//            byte[] rotatedData = new byte[data.length];
//            for (int y = 0; y < height; y++) {
//                for (int x = 0; x < width; x++)
//                    rotatedData[x * height + height - y - 1] = data[x + y * width];
//            }
//            int tmp = width;
//            width = height;
//            height = tmp;
//            data = rotatedData;
//            L.d(TAG, "数组转换用时: " + (System.currentTimeMillis() - start));
//            //--- end


            /*
              因为相机传感器捕获的数据是横向的, 所以需要将数据进行90度的旋转, 用java进行转换在红米三手机测试大概需要 600ms左右
              因此换了C语言, 只需要 35ms左右 速度快了接近 20倍
             */
            data = DecodeHandlerJni.dataHandler(data, data.length, width, height);
            //L.d(TAG, "数组转换用时: " + (System.currentTimeMillis() - start));
            int tmp = width;
            width = height;
            height = tmp;

            Result rawResult = null;
            PlanarYUVLuminanceSource source = activity.getCameraManager().buildLuminanceSource(data, width, height);
            if (source != null) {
                BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                try {
                    rawResult = multiFormatReader.decodeWithState(bitmap);
                } catch (ReaderException re) {
                    // continue
                } finally {
                    multiFormatReader.reset();
                }
            }
            if (rawResult != null) {
                resultQRcode = rawResult.getText();
            }

        } else {
            Image barcode = new Image(width, height, "Y800");
            barcode.setData(data);
            Rect rect = activity.getCameraManager().getFramingRectInPreview();
            if (rect != null) {
                /*
                    zbar 解码库,不需要将数据进行旋转,因此设置裁剪区域是的x为 top, y为left
                    设置了裁剪区域,解码速度快了近5倍左右
                 */
                // 设置截取区域，也就是你的扫描框在图片上的区域.
                barcode.setCrop(rect.top, rect.left, rect.width(), rect.height());
            }
            ImageScanner mImageScanner = new ImageScanner();
            int result = mImageScanner.scanImage(barcode);
            if (result != 0) {
                SymbolSet symSet = mImageScanner.getResults();
                for (Symbol sym : symSet)
                    resultQRcode = sym.getData();
            }
        }

        long end = System.currentTimeMillis();
        if (debug) {
            Log.d(TAG, "处理帧 " + (end - start) + " ms " + mDecoderMode);
        }

        if ((end - start) < RCode.DECODER_TIME && end - lastDecoderTime > RCode.DECODER_TIME) {
            //切换模式
            changeDecodeMode();
            lastDecoderTime = end;
        }

        Handler handler = activity.getHandler();
        if (!TextUtils.isEmpty(resultQRcode)) {
            // 非空表示识别出结果了。
            if (handler != null) {
                if (debug) {
                    Log.d(TAG, "解码成功: " + resultQRcode);
                }
                Message message = Message.obtain(handler, R.id.decode_succeeded, resultQRcode);
                message.sendToTarget();

                //将识别到的这一帧, 丢给外部额外处理
                FrameDataDecode.decode(data, width, height);
            }
        } else {
            if (handler != null) {
                Message message = Message.obtain(handler, R.id.decode_failed);
                message.sendToTarget();
            }
        }
    }
}
