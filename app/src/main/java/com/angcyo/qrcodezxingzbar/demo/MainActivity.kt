package com.angcyo.qrcodezxingzbar.demo

import android.content.Intent
import android.graphics.Bitmap
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.angcyo.rcode.RCode
import com.angcyo.rcode.ScanActivity
import kotlinx.android.synthetic.main.activity_main.*

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

        scan_button.setOnClickListener {
            //扫一扫
            ScanActivity.start(this, AppScanFragment::class.java)
        }
        create_qrcode.setOnClickListener {
            //创建二维码
            AsyncTask.execute {
                val time = System.currentTimeMillis()
                bitmap = RCode.syncEncodeQRCode(edit_text.text?.toString(), 500)
                runOnUiThread {
                    image_view.setImageBitmap(bitmap)
                    text_view.text = "创建二维码耗时:${System.currentTimeMillis() - time}ms"
                }
            }
        }
    }

    override fun onActivityReenter(resultCode: Int, data: Intent?) {
        super.onActivityReenter(resultCode, data)
        Log.i("angcyo", "onActivityReenter:" + data)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.i("angcyo", "onActivityResult:" + data)

        val result = ScanActivity.onResult(requestCode, resultCode, data)
        text_view.text = result ?: "已取消"
    }
}
