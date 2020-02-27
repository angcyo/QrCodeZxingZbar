package com.google.zxing.client.android;

/**
 * Created by Android on 2017/8/17.
 * https://github.com/XieZhiFa/ZxingZbar
 */

public class DecodeHandlerJni {

    static {
        System.loadLibrary("decodeHandler");
    }

    public static native byte[] dataHandler(byte[] by, int length, int width, int height);

/*    public static native byte[] rgb2yuv(int[] by, int width, int height);

    public static native byte[] rgb2yuv(byte[] by, int width, int height);*/
}
