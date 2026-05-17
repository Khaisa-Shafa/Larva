/*
 * Copyright 2017-2022 Jiangdg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jiangdg.ausbc.camera

import android.content.ContentValues
import android.content.Context
import android.hardware.usb.UsbDevice
import android.media.ImageReader
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.jiangdg.ausbc.R
import com.jiangdg.ausbc.callback.IDeviceConnectCallBack
import com.jiangdg.ausbc.callback.IPreviewDataCallBack
import com.jiangdg.ausbc.camera.bean.CameraStatus
import com.jiangdg.ausbc.camera.bean.CameraUvcInfo
import com.jiangdg.ausbc.camera.bean.PreviewSize
import com.jiangdg.ausbc.utils.*
import com.jiangdg.ausbc.utils.CameraUtils.isFilterDevice
import com.jiangdg.ausbc.utils.CameraUtils.isUsbCamera
import com.jiangdg.usb.DeviceFilter
import com.jiangdg.usb.USBMonitor
import com.jiangdg.uvc.IFrameCallback
import com.jiangdg.uvc.UVCCamera
import java.io.File
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.Exception

/** UVC Camera usage
 *
 * @author Created by jiangdg on 2021/12/20
 *
 * Deprecated since version 3.3.0, and it will be deleted in the future.
 * I recommend using the [CameraUVC] API for your application.
 */
@kotlin.Deprecated("Deprecated since version 3.3.0")
class CameraUvcStrategy(ctx: Context) : ICameraStrategy(ctx) {
    private var mDevSettableFuture: SettableFuture<UsbDevice?>? = null
    private var mCtrlBlockSettableFuture: SettableFuture<USBMonitor.UsbControlBlock?>? = null
    private val mConnectSettableFuture: SettableFuture<Boolean> = SettableFuture()
    private val mNV21DataQueue: LinkedBlockingDeque<ByteArray> by lazy {
        LinkedBlockingDeque(MAX_NV21_DATA)
    }
    private val mRequestPermission: AtomicBoolean by lazy {
        AtomicBoolean(false)
    }
    private var mUsbMonitor: USBMonitor? = null
    private var mUVCCamera: UVCCamera? = null
    private var mDevConnectCallBack: IDeviceConnectCallBack? = null
    private var mCacheDeviceList: MutableList<UsbDevice> = arrayListOf()

    init {
        register()
    }

    override fun loadCameraInfo() {
        try {
            val devList = getUsbDeviceListInternal()
            if (devList.isNullOrEmpty()) {
                val emptyTip = "Find no uvc devices, " +
                        "if you want some special device please use getUsbDeviceList() " +
                        "or add device info into default_device_filter.xml"
                postCameraStatus(
                    CameraStatus(
                        CameraStatus.ERROR,
                        emptyTip
                    )
                )
                Logger.e(TAG, emptyTip)
                return
            }
            devList.forEach { dev ->
                loadCameraInfoInternal(dev)
            }
        } catch (e: Exception) {
            Logger.e(TAG, " Find no uvc devices, err = ${e.localizedMessage}", e)
        }
    }

    private fun loadCameraInfoInternal(dev: UsbDevice) {
        if (mCameraInfoMap.containsKey(dev.deviceId)) {
            return
        }
        val cameraInfo = CameraUvcInfo(dev.deviceId.toString()).apply {
            cameraVid = dev.vendorId
            cameraPid = dev.productId
            cameraName = dev.deviceName
            cameraProtocol = dev.deviceProtocol
            cameraClass = dev.deviceClass
            cameraSubClass = dev.deviceSubclass
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                cameraProductName = dev.productName
                cameraManufacturerName = dev.manufacturerName
            }
        }
        mCameraInfoMap[dev.deviceId] = cameraInfo
    }

    override fun startPreviewInternal() {
        val device = mDevSettableFuture?.get()
        val ctrlBlock = mCtrlBlockSettableFuture?.get()
        if (device == null || ctrlBlock == null) {
            Logger.w(TAG, "startPreviewInternal: device/ctrlBlock null, skip")
            return
        }
        try {
            createCamera()
            realStartPreview()
        } catch (e: Exception) {
            stopPreview()
            Logger.e(TAG, " preview failed, err = ${e.localizedMessage}", e)
            postCameraStatus(CameraStatus(CameraStatus.ERROR, e.localizedMessage))
        }
    }

    private fun createCamera(): Boolean? {
        val ctrlBlock = mCtrlBlockSettableFuture?.get()
        val device = mDevSettableFuture?.get()
        Logger.e(TAG, "createCamera: device=$device, ctrlBlock=$ctrlBlock")

        device ?: run { Logger.e(TAG, "createCamera: device is NULL!"); return null }
        ctrlBlock ?: run { Logger.e(TAG, "createCamera: ctrlBlock is NULL!"); return null }

        getRequest()?.let { request ->
            request.cameraId = device.deviceId.toString()
            mUVCCamera = UVCCamera().apply {
                try {
                    open(ctrlBlock)
                    Logger.e(TAG, "✅ open berhasil")
                } catch (e: Exception) {
                    Logger.e(TAG, "open failed: ${e.message}, retrying...")
                    Thread.sleep(500)
                    try {
                        open(ctrlBlock)
                    } catch (e2: Exception) {
                        Logger.e(TAG, "open failed second: ${e2.message}")
                        throw e2
                    }
                }
            }

            Thread.sleep(200)

            val yuyvSizes = mUVCCamera?.getSupportedSizeList(UVCCamera.FRAME_FORMAT_YUYV)
            val mjpegSizes = mUVCCamera?.getSupportedSizeList(UVCCamera.FRAME_FORMAT_MJPEG)
            Logger.e(TAG, "YUYV sizes: $yuyvSizes")
            Logger.e(TAG, "MJPEG sizes: $mjpegSizes")
            Logger.e(TAG, "Default sizes: ${mUVCCamera?.supportedSizeList}")

//            Logger.e(TAG, "Supported sizes: ${mUVCCamera?.supportedSizeList}")

            val combinations = listOf(
                Triple(640, 480, 0.25f),
                Triple(320, 240, 0.25f),
                Triple(640, 480, 0.5f),
                Triple(320, 240, 0.5f),
            )

            var previewSet = false
            mUVCCamera?.setPreviewSize(640, 480, MIN_FS, MAX_FS, UVCCamera.FRAME_FORMAT_YUYV, 1.0f)
            Logger.e(TAG, "✅ setPreviewSize YUYV 640x480 bw=1.0f berhasil")
            previewSet = true

            if (!previewSet) {
                Logger.e(TAG, "❌ Semua kombinasi YUYV gagal, coba MJPEG...")
                // Fallback ke MJPEG jika YUYV tidak support
                for ((w, h, bw) in combinations) {
                    try {
                        mUVCCamera?.setPreviewSize(w, h, MIN_FS, MAX_FS, UVCCamera.FRAME_FORMAT_MJPEG, bw)
                        Logger.e(TAG, "✅ setPreviewSize MJPEG ${w}x${h} bw=$bw berhasil")
                        previewSet = true
                        break
                    } catch (e: Exception) {
                        Logger.e(TAG, "❌ setPreviewSize MJPEG ${w}x${h} bw=$bw gagal: ${e.message}")
                    }
                }
            }

            if (!previewSet) Logger.e(TAG, "❌ Semua kombinasi gagal!")

            // FIX: Gunakan PIXEL_FORMAT_YUV agar data yang diterima frameCallBack
            // adalah YUYV yang valid, konsisten dengan konversi yuyvToBitmap di Activity
            // Di createCamera(), ganti:
//            mUVCCamera?.setFrameCallback(frameCallBack, UVCCamera.PIXEL_FORMAT_RAW)
//            Logger.e(TAG, "✅ setFrameCallback PIXEL_FORMAT_RAW")
        }
        return true
    }

    private var imageReader: ImageReader? = null

    private fun realStartPreview(): Boolean? {
        try {
            val st = getSurfaceTexture()
            val holder = getSurfaceHolder()
            Logger.e(TAG, "realStartPreview: st=$st, holder=$holder")

            mUVCCamera?.autoFocus = true
            mUVCCamera?.autoWhiteBlance = true
            Logger.e(TAG, "startPreview called, isPreviewing=${mIsPreviewing.get()}")
            mUVCCamera?.startPreview()
            Logger.e(TAG, "startPreview done")

            Thread.sleep(200)

            if (st != null) {
                mUVCCamera?.setPreviewTexture(st)
                Logger.e(TAG, "✅ setPreviewTexture dipanggil")
            }
            Thread.sleep(100)

            mUVCCamera?.setFrameCallback(frameCallBack, UVCCamera.PIXEL_FORMAT_RAW)
            Logger.e(TAG, "✅ setFrameCallback dipasang setelah setPreviewTexture")

            mUVCCamera?.updateCameraParams()
            mIsPreviewing.set(true)

            Logger.e(TAG, "mPreviewDataCbList size = ${mPreviewDataCbList.size}")

            getRequest()?.apply {
                postCameraStatus(CameraStatus(CameraStatus.START, Pair(previewWidth, previewHeight).toString()))
            }
            val dev = mDevSettableFuture?.get().apply {
                mDevConnectCallBack?.onConnectDev(this)
            }
            if (Utils.debugCamera) {
                Logger.i(TAG, " start preview success!!!, id(${dev?.deviceName}")
            }
        } catch (e: Exception) {
            postCameraStatus(CameraStatus(CameraStatus.ERROR, e.localizedMessage))
            Logger.e(TAG, " startPreview failed. err = ${e.localizedMessage}", e)
            return null
        }
        return true
    }

    override fun stopPreviewInternal() {
        mRequestPermission.set(false)
        mIsPreviewing.set(false)
        val camera = mUVCCamera
        mUVCCamera = null
        Thread {
            try {
                camera?.destroy()
            } catch (e: Exception) {
                Logger.e(TAG, "destroy error: ${e.message}")
            }
        }.start()
        postCameraStatus(CameraStatus(CameraStatus.STOP))
    }

    override fun captureImageInternal(savePath: String?) {
        if (!hasCameraPermission() || !hasStoragePermission()) {
            mMainHandler.post {
                mCaptureDataCb?.onError("Have no storage or camera permission.")
            }
            Logger.i(TAG, "captureImageInternal failed, has no storage/camera permission.")
            return
        }
        if (mIsCapturing.get()) {
            return
        }
        mSaveImageExecutor.submit {
            val data = mNV21DataQueue.pollFirst(CAPTURE_TIMES_OUT_SEC, TimeUnit.SECONDS)
            if (data == null || getRequest() == null) {
                mMainHandler.post {
                    mCaptureDataCb?.onError("Times out or camera request is null")
                }
                Logger.i(TAG, "captureImageInternal failed, times out.")
                return@submit
            }
            mIsCapturing.set(true)
            mMainHandler.post {
                mCaptureDataCb?.onBegin()
            }
            val date = mDateFormat.format(System.currentTimeMillis())
            val title = savePath ?: "IMG_JJCamera_$date"
            val displayName = savePath ?: "$title.jpg"
            val path = savePath ?: "$mCameraDir/$displayName"
            val orientation = 0
            val location = Utils.getGpsLocation(getContext())
            val width = getRequest()!!.previewWidth
            val height = getRequest()!!.previewHeight
            val ret = MediaUtils.saveYuv2Jpeg(path, data, width, height)
            if (!ret) {
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                }
                mMainHandler.post {
                    mCaptureDataCb?.onError("save yuv to jpeg failed.")
                }
                Logger.w(TAG, "save yuv to jpeg failed.")
                return@submit
            }
            val values = ContentValues()
            values.put(MediaStore.Images.ImageColumns.TITLE, title)
            values.put(MediaStore.Images.ImageColumns.DISPLAY_NAME, displayName)
            values.put(MediaStore.Images.ImageColumns.DATA, path)
            values.put(MediaStore.Images.ImageColumns.DATE_TAKEN, date)
            values.put(MediaStore.Images.ImageColumns.ORIENTATION, orientation)
            values.put(MediaStore.Images.ImageColumns.LONGITUDE, location?.longitude)
            values.put(MediaStore.Images.ImageColumns.LATITUDE, location?.latitude)
            getContext()?.contentResolver?.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            )
            mMainHandler.post {
                mCaptureDataCb?.onComplete(path)
            }
            mIsCapturing.set(false)
            if (Utils.debugCamera) {
                Logger.i(TAG, "captureImageInternal save path = $path")
            }
        }
    }

    override fun switchCameraInternal(cameraId: String?) {
        getRequest()?.let {
            if (Utils.debugCamera) {
                Logger.i(TAG, "switchCameraInternal, camera id = $cameraId")
            }
            if (cameraId.isNullOrEmpty()) {
                Logger.e(TAG, "camera id invalid.")
                return@let
            }
            if (getCurrentDevice()?.deviceId?.toString() == cameraId) {
                Logger.e(TAG, "camera was already opened.")
                return@let
            }
            getUsbDeviceList()?.find {
                cameraId == it.deviceId.toString()
            }.also { dev ->
                if (dev == null) {
                    Logger.e(TAG, "switch camera(: $cameraId) failed, not found.")
                    return@also
                }
                if (!mCacheDeviceList.contains(dev)) {
                    mCacheDeviceList.add(dev)
                }
                stopPreviewInternal()
                requestCameraPermission(dev)
            }
        }
    }

    override fun updateResolutionInternal(width: Int, height: Int) {
        getRequest()?.let { request ->
            request.previewWidth = width
            request.previewHeight = height
            stopPreviewInternal()
            startPreviewInternal()
        }
    }

    override fun getAllPreviewSizes(aspectRatio: Double?): MutableList<PreviewSize>? {
        getRequest()?.let { request ->
            val cameraInfo = mCameraInfoMap.values.find {
                request.cameraId == it.cameraId
            }
            val previewSizeList = cameraInfo?.cameraPreviewSizes ?: mutableListOf()
            if (previewSizeList.isEmpty()) {
                Logger.i(TAG, "getAllPreviewSizes = ${mUVCCamera?.supportedSizeList}")
                if (mUVCCamera?.supportedSizeList?.isNotEmpty() == true) {
                    mUVCCamera?.supportedSizeList
                } else {
                    mUVCCamera?.getSupportedSizeList(UVCCamera.FRAME_FORMAT_YUYV)
                }.also { sizeList ->
                    sizeList?.forEach { size ->
                        previewSizeList.find {
                            it.width == size.width && it.height == size.height
                        }.also {
                            if (it == null) {
                                previewSizeList.add(PreviewSize(size.width, size.height))
                            }
                        }
                    }
                    cameraInfo?.cameraPreviewSizes = previewSizeList
                }
            }
            aspectRatio ?: return previewSizeList
            val aspectList = mutableListOf<PreviewSize>()
            aspectList.clear()
            cameraInfo?.cameraPreviewSizes?.forEach { size ->
                val width = size.width
                val height = size.height
                val ratio = width.toDouble() / height
                if (ratio == aspectRatio) {
                    aspectList.add(size)
                }
            }
            Logger.i(TAG, "getAllPreviewSizes aspectRatio = $aspectRatio, size = $aspectList")
            return aspectList
        }
        return null
    }

    override fun register() {
        if (mUsbMonitor?.isRegistered == true) {
            return
        }
        mUsbMonitor = USBMonitor(getContext(), object : USBMonitor.OnDeviceConnectListener {
            override fun onAttach(device: UsbDevice?) {
                if (Utils.debugCamera) {
                    Logger.i(TAG, "attach device = ${device?.toString()}")
                }
                device ?: return
                if (!isUsbCamera(device) && !isFilterDevice(getContext(), device)) {
                    return
                }
                if (!mCacheDeviceList.contains(device)) {
                    device.let {
                        mCacheDeviceList.add(it)
                    }
                    mDevConnectCallBack?.onAttachDev(device)
                }
                loadCameraInfoInternal(device)
                requestCameraPermission(device)
            }

            override fun onDetach(device: UsbDevice?) {
                if (Utils.debugCamera) {
                    Logger.i(TAG, "onDetach device = ${device?.deviceName}")
                }
                if (!isUsbCamera(device) && !isFilterDevice(getContext(), device) && !mCacheDeviceList.contains(device)) {
                    return
                }
                mCameraInfoMap.remove(device?.deviceId)
                mDevConnectCallBack?.onDetachDec(device)
                if (mCacheDeviceList.contains(device)) {
                    mCacheDeviceList.remove(device)
                }
                val dev = mDevSettableFuture?.get()
                if (dev?.deviceId == device?.deviceId) {
                    mRequestPermission.set(false)
                }
            }

            override fun onConnect(
                device: UsbDevice?,
                ctrlBlock: USBMonitor.UsbControlBlock?,
                createNew: Boolean
            ) {
                if (!isUsbCamera(device) && !isFilterDevice(getContext(), device) && !mCacheDeviceList.contains(device)) {
                    return
                }

                mDevSettableFuture = SettableFuture()
                mCtrlBlockSettableFuture = SettableFuture()
                mDevSettableFuture?.set(device)
                mCtrlBlockSettableFuture?.set(ctrlBlock)
                mConnectSettableFuture.set(true)

                // FIX: Selalu startPreview ulang saat device connect
                // stopPreview dulu kalau sedang preview tanpa device (device=null)
                if (mIsPreviewing.get()) {
                    Logger.w(TAG, "onConnect: stop preview lama lalu restart dengan device baru")
                    stopPreview()
                    // Delay singkat biarkan stopPreview selesai
                    Thread.sleep(200)
                }

                getRequest()?.apply {
                    if (getSurfaceTexture() != null) {
                        startPreview(this, getSurfaceTexture())
                    } else {
                        startPreview(this, getSurfaceHolder())
                    }
                }
            }

            override fun onDisconnect(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?) {
                if (Utils.debugCamera) {
                    Logger.i(TAG, "onDisconnect device = ${device?.deviceName}")
                }
                if (!isUsbCamera(device) && !isFilterDevice(getContext(), device) && !mCacheDeviceList.contains(device)) {
                    return
                }
                val curDevice = mDevSettableFuture?.get()
                if (curDevice?.deviceId != device?.deviceId) {
                    return
                }
                stopPreview()
                mDevConnectCallBack?.onDisConnectDec(device, ctrlBlock)
                mConnectSettableFuture.set(false)
            }

            override fun onCancel(device: UsbDevice?) {
                if (Utils.debugCamera) {
                    Logger.i(TAG, "onCancel device = ${device?.deviceName}")
                }
                if (!isUsbCamera(device) && !isFilterDevice(getContext(), device) && !mCacheDeviceList.contains(device)) {
                    return
                }
                val curDevice = mDevSettableFuture?.get()
                if (curDevice?.deviceId != device?.deviceId) {
                    return
                }
                stopPreview()
                mDevConnectCallBack?.onDisConnectDec(device)
            }
        })
        mUsbMonitor?.register()
        if (Utils.debugCamera) {
            Logger.i(TAG, "register uvc device monitor")
        }
    }

    override fun unRegister() {
        if (mUsbMonitor?.isRegistered == false) {
            return
        }
        mUsbMonitor?.unregister()
        mUsbMonitor?.destroy()
        mUsbMonitor = null
        if (Utils.debugCamera) {
            Logger.i(TAG, "unRegister uvc device monitor")
        }
    }

    fun setDeviceConnectStatusListener(cb: IDeviceConnectCallBack) {
        this.mDevConnectCallBack = cb
    }

    fun getUsbDeviceList(resId: Int? = null): MutableList<UsbDevice>? {
        return mUsbMonitor?.deviceList?.let { usbDevList ->
            val list = arrayListOf<UsbDevice>()
            if (resId == null) {
                null
            } else {
                DeviceFilter.getDeviceFilters(getContext(), resId)
            }.also { filterList ->
                if (filterList == null) {
                    list.addAll(usbDevList)
                    return@also
                }
                usbDevList.forEach { dev ->
                    val filterDev = filterList.find {
                        it.mProductId == dev?.productId && it.mVendorId == dev.vendorId
                    }
                    if (filterDev != null) {
                        list.add(dev)
                    }
                }
            }
            list
        }
    }

    fun getCurrentDevice(): UsbDevice? {
        return try {
            val isConnected = mConnectSettableFuture.get(3, TimeUnit.SECONDS)
            if (isConnected != true) {
                return null
            }
            mDevSettableFuture?.get(1, TimeUnit.SECONDS)
        } catch (e: Exception) {
            null
        }
    }

    fun sendCameraCommand(command: Int): Int? {
        return mUVCCamera?.sendCommand(command).apply {
            Logger.i(TAG, "send command ret = $this")
        }
    }

    fun setAutoFocus(enable: Boolean) { mUVCCamera?.autoFocus = enable }
    fun setAutoWhiteBalance(autoWhiteBalance: Boolean) { mUVCCamera?.autoWhiteBlance = autoWhiteBalance }
    fun setZoom(zoom: Int) { mUVCCamera?.zoom = zoom }
    fun getZoom() = mUVCCamera?.zoom
    fun setGain(gain: Int) { mUVCCamera?.gain = gain }
    fun getGain() = mUVCCamera?.gain
    fun setGamma(gamma: Int) { mUVCCamera?.gamma = gamma }
    fun getGamma() = mUVCCamera?.gamma
    fun setBrightness(brightness: Int) { mUVCCamera?.brightness = brightness }
    fun getBrightness() = mUVCCamera?.brightness
    fun setContrast(contrast: Int) { mUVCCamera?.contrast = contrast }
    fun getContrast() = mUVCCamera?.contrast
    fun setSharpness(sharpness: Int) { mUVCCamera?.sharpness = sharpness }
    fun getSharpness() = mUVCCamera?.sharpness
    fun setSaturation(saturation: Int) { mUVCCamera?.saturation = saturation }
    fun getSaturation() = mUVCCamera?.saturation
    fun setHue(hue: Int) { mUVCCamera?.hue = hue }
    fun getHue() = mUVCCamera?.hue

    private fun getUsbDeviceListInternal(): MutableList<UsbDevice>? {
        return mUsbMonitor?.getDeviceList(arrayListOf<DeviceFilter>())?.let { devList ->
            mCacheDeviceList.clear()
            val devInfoList = ArrayList<String>()
            devList.forEach {
                devInfoList.add(it.deviceName)
                if (isUsbCamera(it) || isFilterDevice(getContext(), it)) {
                    mCacheDeviceList.add(it)
                }
            }
            Logger.i(TAG, " find some device list, = $devInfoList")
            mCacheDeviceList
        }
    }

    private fun requestCameraPermission(device: UsbDevice?) {
        if (mRequestPermission.get()) {
            return
        }
        mCacheDeviceList.find {
            device?.deviceId == it.deviceId
        }.also { dev ->
            if (dev == null) {
                Logger.e(TAG, "open camera failed, not found.")
                return@also
            }
            mRequestPermission.set(true)
            mUsbMonitor?.requestPermission(dev)
        }
    }

    private val frameCallBack = IFrameCallback { frame ->
        android.util.Log.e("FRAME_DEBUG", "🔥 FRAME! size=${frame?.capacity()}")
        mPreviewDataCbList.forEach { cb ->
            frame?.apply {
                position(0)
                val data = ByteArray(capacity())
                get(data)
                cb.onPreviewData(
                    data,
                    getRequest()!!.previewWidth,
                    getRequest()!!.previewHeight,
                    IPreviewDataCallBack.DataFormat.YUYV  // ganti ke MJPEG
                )
                if (mNV21DataQueue.size >= MAX_NV21_DATA) mNV21DataQueue.removeLast()
                mNV21DataQueue.offerFirst(data)
            }
        }
    }

    companion object {
        private const val TAG = "CameraUvc"
        private const val MIN_FS = 10
        private const val MAX_FS = 60
        private const val MAX_NV21_DATA = 5
        private const val CAPTURE_TIMES_OUT_SEC = 1L
    }
}