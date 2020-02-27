package com.angcyo.rcode;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.DisplayMetrics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;

import java.util.EnumMap;
import java.util.Map;

import static com.angcyo.rcode.encode.QRCodeEncoder.HINTS_DECODE;

/**
 * https://github.com/angcyo/QrCodeZxingZbar
 * Email:angcyo@126.com
 *
 * @author angcyo
 * @date 2017/04/18 16:26
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
public class RCode {
    /**
     * 每种模式, 解码时长, 超过时长切换模式解码
     */
    public static long DECODER_TIME = 1_000L;

    public static final Map<EncodeHintType, Object> HINTS = new EnumMap<>(EncodeHintType.class);

    static {
        //编码设置
        HINTS.put(EncodeHintType.CHARACTER_SET, "utf-8");
        //二维码容错率
        HINTS.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        //边距
        HINTS.put(EncodeHintType.MARGIN, 0);
    }

    private RCode() {
    }

    //<editor-fold desc="二维码创建">

    /**
     * 同步创建黑色前景色、白色背景色的二维码图片。该方法是耗时操作，请在子线程中调用。
     *
     * @param content 要生成的二维码图片内容
     * @param size    图片宽高，单位为px
     */
    @Nullable
    public static Bitmap syncEncodeQRCode(String content, int size) {
        return syncEncodeQRCode(content, size, Color.BLACK, Color.WHITE, null);
    }

    /**
     * 同步创建指定前景色、白色背景色的二维码图片。该方法是耗时操作，请在子线程中调用。
     *
     * @param content         要生成的二维码图片内容
     * @param size            图片宽高，单位为px
     * @param foregroundColor 二维码图片的前景色
     */
    @Nullable
    public static Bitmap syncEncodeQRCode(String content, int size, int foregroundColor) {
        return syncEncodeQRCode(content, size, foregroundColor, Color.WHITE, null);
    }

    /**
     * 同步创建指定前景色、白色背景色、带logo的二维码图片。该方法是耗时操作，请在子线程中调用。
     *
     * @param content         要生成的二维码图片内容
     * @param size            图片宽高，单位为px
     * @param foregroundColor 二维码图片的前景色
     * @param logo            二维码图片的logo
     */
    @Nullable
    public static Bitmap syncEncodeQRCode(String content, int size, int foregroundColor, Bitmap logo) {
        return syncEncodeQRCode(content, size, foregroundColor, Color.WHITE, logo);
    }

    /**
     * 同步创建指定前景色、指定背景色、带logo的二维码图片。该方法是耗时操作，请在子线程中调用。
     *
     * @param content         要生成的二维码图片内容
     * @param size            图片宽高，单位为px
     * @param foregroundColor 二维码图片的前景色
     * @param backgroundColor 二维码图片的背景色
     * @param logo            二维码图片的logo
     */
    @Nullable
    public static Bitmap syncEncodeQRCode(String content, int size, int foregroundColor, int backgroundColor, Bitmap logo) {
        try {
            BitMatrix matrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size, HINTS);
            int[] pixels = new int[size * size];
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    if (matrix.get(x, y)) {
                        pixels[y * size + x] = foregroundColor;
                    } else {
                        pixels[y * size + x] = backgroundColor;
                    }
                }
            }
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, size, 0, 0, size, size);
            return addLogoToQRCode(bitmap, logo);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 添加logo到二维码图片上
     *
     * @param src
     * @param logo
     * @return
     */
    private static Bitmap addLogoToQRCode(Bitmap src, Bitmap logo) {
        if (src == null || logo == null) {
            return src;
        }

        int srcWidth = src.getWidth();
        int srcHeight = src.getHeight();
        int logoWidth = logo.getWidth();
        int logoHeight = logo.getHeight();

        float scaleFactor = srcWidth * 1.0f / 5 / logoWidth;
        Bitmap bitmap = Bitmap.createBitmap(srcWidth, srcHeight, Bitmap.Config.ARGB_8888);
        try {
            Canvas canvas = new Canvas(bitmap);
            canvas.drawBitmap(src, 0, 0, null);
            canvas.scale(scaleFactor, scaleFactor, srcWidth / 2, srcHeight / 2);
            canvas.drawBitmap(logo, (srcWidth - logoWidth) / 2, (srcHeight - logoHeight) / 2, null);
            canvas.save();
            //canvas.saveLayer(0f, 0f, measuredWidth.toFloat(), measuredHeight.toFloat(), null, Canvas.ALL_SAVE_FLAG)
            canvas.restore();
        } catch (Exception e) {
            bitmap = null;
        }
        return bitmap;
    }

    //</editor-fold desc="二维码创建">

    //<editor-fold desc="二维码扫描">

    /**
     * 第二层 扫描
     */
    private static String scanPictureFun2(Bitmap scanBitmap) {
        PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(
                rgb2YUV(scanBitmap),
                scanBitmap.getWidth(),
                scanBitmap.getHeight(),
                0, 0,
                scanBitmap.getWidth(),
                scanBitmap.getHeight(),
                false
        );

        BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));
        MultiFormatReader reader = new MultiFormatReader();
        Result result;
        try {
            result = reader.decode(binaryBitmap, HINTS_DECODE);
        } catch (Exception e) {
            e.printStackTrace();
            try {
                result = reader.decode(new BinaryBitmap(new GlobalHistogramBinarizer(source)), HINTS_DECODE);
                return result.getText();
            } catch (Exception e2) {
                return scanPictureFun3(scanBitmap);
            }
        }
        return result.getText();
    }

    /**
     * 第三层 扫描方法, 使用 ZBar
     */
    private static String scanPictureFun3(Bitmap scanBitmap) {
        Image barcode = new Image(scanBitmap.getWidth(), scanBitmap.getHeight(), "Y800");
//            Debug.logTimeStart("rgb2YUV")
//            barcode.data = rgb2YUV(scanBitmap) //BmpUtil.generateBitstream(scanBitmap, Bitmap.CompressFormat.JPEG, 100)
//            Debug.logTimeEnd("rgb2YUV")

//        int width = scanBitmap.getWidth();
//        int height = scanBitmap.getHeight();
//        int[] pixels = new int[(width * height)];
//        scanBitmap.getPixels(pixels, 0, width, 0, 0, width, height);

//            Debug.logTimeStart("rgb2YUV_2")
        barcode.setData(rgb2YUV(scanBitmap));
//            Debug.logTimeEnd("rgb2YUV_2")

        ImageScanner mImageScanner = new ImageScanner();
        int result = mImageScanner.scanImage(barcode);
        String resultQRcode = null;
        if (result != 0) {
            SymbolSet symSet = mImageScanner.getResults();
            for (Symbol sym : symSet) {
                resultQRcode = sym.getData();
            }
        }
        return resultQRcode;
    }

    /**
     * 第一层 扫描
     */
    @Nullable
    private static String scanPictureFun1(@NonNull Bitmap bitmap) {
        Result result;
        RGBLuminanceSource source = null;
        try {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int[] pixels = new int[(width * height)];
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
            source = new RGBLuminanceSource(width, height, pixels);
            result = new MultiFormatReader().decode(new BinaryBitmap(new HybridBinarizer(source)), HINTS_DECODE);
            return result.getText();
        } catch (Exception e) {
            e.printStackTrace();
            if (source != null) {
                try {
                    result = new MultiFormatReader().decode(new BinaryBitmap(new GlobalHistogramBinarizer(source)), HINTS_DECODE);
                    return result.getText();
                } catch (Throwable e2) {
                    e2.printStackTrace();
                    return scanPictureFun2(bitmap);
                }
            }
            return "";
        }
    }

    @Nullable
    public static String scanPicture(Context context, String bitmapPath) {
        try {
            Bitmap bitmap = createBitmap(context, bitmapPath);
            return scanPictureFun1(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 调用三层扫码方法, 耗时操作.
     */
    @Nullable
    public static String scanPicture(@Nullable Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        return scanPictureFun1(bitmap);
    }

    /**
     * 根据图片路径创建一个合适大小的位图
     *
     * @param filePath
     * @return
     */
    public static Bitmap createBitmap(Context context, String filePath) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        int displayWidth = dm.widthPixels;

        //实例位图设置
        BitmapFactory.Options opts = new BitmapFactory.Options();
        //表示不全将图片加载到内存,而是读取图片的信息2
        opts.inJustDecodeBounds = true;
        //首先读取图片信息,并不加载图片到内存,防止内存泄露
        BitmapFactory.decodeFile(filePath, opts);
        //获取图片的宽
        int imageWidth = opts.outWidth;
        //获取图片的高
        int imageHeight = opts.outHeight;

        float scale = 1f;
        if (imageWidth > displayWidth || imageHeight > displayWidth) {
            //计算图片,宽度的缩放率
            float scaleX = imageWidth * 1f / displayWidth;
            //计算图片,高度的缩放率
            float scaleY = imageHeight * 1f / displayWidth;

            //如果图片的宽比高大,则采用宽的比率
            if ((scaleX > scaleY) && (scaleY >= 1)) {
                scale = scaleX;
            }

            //如果图片的高比宽大,则采用高的比率
            if ((scaleY > scaleX) && (scaleX >= 1)) {
                scale = scaleY;
            }
        }

        //表示加载图片到内存
        opts.inJustDecodeBounds = false;
        //设置Bitmap的采样率
        opts.inSampleSize = (int) scale;

        return BitmapFactory.decodeFile(filePath, opts);
    }

    /**
     * 将RGB转成yuv
     */
    public static byte[] rgb2YUV(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[(width * height)];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        int len = width * height;
        byte[] yuv = new byte[(len * 3 / 2)];
        int y;
        int u;
        int v;

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < height; j++) {
                int rgb = pixels[i * width + j] & 0x00FFFFFF;

                int r = rgb & 0xFF;
                int g = rgb >> 8 & 0xFF;
                int b = rgb >> 16 & 0xFF;

                y = (66 * r + 129 * g + 25 * b + 128 >> 8) + 16;
                u = (-38 * r - 74 * g + 112 * b + 128 >> 8) + 128;
                v = (112 * r - 94 * g - 18 * b + 128 >> 8) + 128;

                if (y < 16) {
                    y = 16;
                } else if (y > 255) {
                    y = 255;
                }

                if (u < 0) {
                    u = 0;
                } else if (u > 255) {
                    u = 255;
                }

                if (v < 0) {
                    v = 0;
                } else if (v > 255) {
                    v = 255;
                }

                yuv[i * width + j] = (byte) y;
                yuv[len + (i >> 1) * width + (j & ~1)] = (byte) u;
                yuv[len + (i >> 1) * width + (j & ~1) + 1] = (byte) v;
            }
        }
        return yuv;
    }

    //</editor-fold desc="二维码扫描">
}
