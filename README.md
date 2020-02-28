# QrCodeZxingZbar

使用`Zxing` `Zbar` 结合 扫描, 默认`1s`切换一次`Zxing`或`Zbar`.

- 具有`Zxing`所有特性
- 具有`Zbar`所有特性
- 支持`生成二维码`
- 支持`从图片扫码`, 三层方法识别.
- api 16+
- zxing 3.3.3, 3.4需要api 24+

# `zxing` 和 `zbar` 的区别 总结一下:

- zbar 快, 支持的格式少.
- zxing 慢, 支持的格式多.

---

![](https://raw.githubusercontent.com/angcyo/QrCodeZxingZbar/master/png/png1.png)

![](https://raw.githubusercontent.com/angcyo/QrCodeZxingZbar/master/png/png2.png)

# 使用方法

## 启动扫码

```java
ScanActivity.start(this)
```

## 获取扫码结果

```java
ScanActivity.onResult(requestCode, resultCode, data)
```

## 创建二维码

```java
RCode.syncEncodeQRCode("内容", 500)
```

## 扫描图片

```java
RCode.scanPicture(bitmap)
```

# 定制界面

## 继承`ScanFragment`, 重写`getLayoutId`方法

```kotlin
class AppScanFragment : ScanFragment() {
    override fun getLayoutId(): Int {
        return R.layout.fragment_app_scan
    }
}

```

`xml`文件中,包含`qr_code_scan_layout`即可,其他元素可自行添加.

```xml
<include layout="@layout/qr_code_scan_layout" />

```

## 启动自定义界面

```java
ScanActivity.start(this, AppScanFragment::class.java)
```


# 依赖

```groovy

allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
    
dependencies {
    implementation 'com.github.angcyo:QrCodeZxingZbar:1.0.2'
}
```

# 感谢

https://github.com/XieZhiFa/ZxingZbar

# 混淆

库中已经自带了混淆规则, 手动添加如下:

```
-keep class net.sourceforge.zbar.**{*;}
-keep class com.google.zxing.client.android.**{*;}
```

# 下载体验

扫码安装

![](https://raw.githubusercontent.com/angcyo/QrCodeZxingZbar/master/png/png3.png)

---
**群内有`各(pian)种(ni)各(jin)样(qun)`的大佬,等你来撩.**

# 联系作者

[点此QQ对话](http://wpa.qq.com/msgrd?v=3&uin=664738095&site=qq&menu=yes)  `该死的空格`    [点此快速加群](https://shang.qq.com/wpa/qunwpa?idkey=cbcf9a42faf2fe730b51004d33ac70863617e6999fce7daf43231f3cf2997460)

![](https://gitee.com/angcyo/res/raw/master/code/all_in1.jpg)

![](https://gitee.com/angcyo/res/raw/master/code/all_in2.jpg)
