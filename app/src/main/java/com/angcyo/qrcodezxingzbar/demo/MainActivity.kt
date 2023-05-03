package com.angcyo.qrcodezxingzbar.demo

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.angcyo.rcode.RCode
import com.angcyo.rcode.ScanActivity
import com.google.zxing.BarcodeFormat

/**
 * Email:angcyo@126.com
 *
 * @author angcyo
 * @date 2020-02-27
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
class MainActivity : AppCompatActivity() {

    companion object {
        var bitmap: Bitmap? = null
    }

    val REQUEST_CODE = 0x808

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<View>(R.id.scan_button).setOnClickListener {
            //扫一扫
            ScanActivity.start(this, AppScanFragment::class.java)
        }
        findViewById<View>(R.id.create_qrcode).setOnClickListener {
            //创建二维码
            createCode(BarcodeFormat.QR_CODE)
        }
        findViewById<View>(R.id.create_barcode).setOnClickListener {
            //创建条形码
            createCode(BarcodeFormat.CODE_128)
        }
        initBarcodeLayout()
    }

    override fun onActivityReenter(resultCode: Int, data: Intent?) {
        super.onActivityReenter(resultCode, data)
        Log.i("angcyo", "onActivityReenter:" + data)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.i("angcyo", "onActivityResult:" + data)

        val result = ScanActivity.onResult(requestCode, resultCode, data)
        findViewById<TextView>(R.id.text_view).text = result ?: "已取消"
    }

    fun createCode(format: BarcodeFormat) {
        AsyncTask.execute {
            val time = System.currentTimeMillis()
            val width =
                if (format == BarcodeFormat.QR_CODE || format == BarcodeFormat.DATA_MATRIX) 500 else 600
            val height =
                if (format == BarcodeFormat.QR_CODE || format == BarcodeFormat.DATA_MATRIX) width else 300
            bitmap = RCode.syncEncodeCode(
                findViewById<TextView>(R.id.edit_text).text?.toString(),
                width,
                height,
                Color.BLACK,
                Color.LTGRAY,
                null,
                format
            )
            runOnUiThread {
                findViewById<ImageView>(R.id.image_view).setImageBitmap(bitmap)
                findViewById<TextView>(R.id.text_view).text =
                    "创建Code耗时:${System.currentTimeMillis() - time}ms"
            }
        }
    }

    fun initBarcodeLayout() {
        val codeList = mutableListOf(
            BarcodeFormat.AZTEC,
            BarcodeFormat.CODABAR,
            BarcodeFormat.CODE_39,
            BarcodeFormat.CODE_93,
            BarcodeFormat.CODE_128,
            BarcodeFormat.DATA_MATRIX,
            BarcodeFormat.EAN_8,
            BarcodeFormat.EAN_13,
            BarcodeFormat.ITF,
            BarcodeFormat.MAXICODE,
            BarcodeFormat.PDF_417,
            BarcodeFormat.QR_CODE,
            BarcodeFormat.RSS_14,
            BarcodeFormat.RSS_EXPANDED,
            BarcodeFormat.UPC_A,
            BarcodeFormat.UPC_E,
            BarcodeFormat.UPC_EAN_EXTENSION
        )

        codeList.forEach { format ->
            findViewById<ViewGroup>(R.id.layout).addView(Button(this).apply {
                text = format.toString()
                setOnClickListener {
                    createCode(format)
                }
            })
        }
    }
}
