package com.angcyo.qrcodezxingzbar.demo

import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import android.widget.Toast
import com.angcyo.rcode.RCode
import com.angcyo.rcode.ScanFragment
import kotlinx.android.synthetic.main.fragment_app_scan.*

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/02/27
 */
class AppScanFragment : ScanFragment() {
    override fun getLayoutId(): Int {
        return R.layout.fragment_app_scan
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        light_switch_view.setOnClickListener {
            openFlashlight((it as CompoundButton).isChecked)
        }

        photo_selector_view.setOnClickListener {
            if (MainActivity.bitmap == null) {
                Toast.makeText(context, "请先创建二维码", Toast.LENGTH_LONG).show()
            } else {
                handleDecode(RCode.scanPicture(MainActivity.bitmap))
            }
        }
    }
}