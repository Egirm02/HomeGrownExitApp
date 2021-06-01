package com.android.safeway.homegrownexitapp.ui

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.usb.UsbDevice
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.KeyEvent
import android.view.Surface
import android.view.View
import android.view.WindowManager
import android.widget.SeekBar
import android.widget.Toast
//import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModelProvider
import com.android.safeway.homegrownexitapp.R
import com.android.safeway.homegrownexitapp.databinding.ActivityMainBinding
import com.android.safeway.homegrownexitapp.util.Utils
import com.android.safeway.homegrownexitapp.viewmodel.HomeViewModel
import com.jiangdg.usbcamera.UVCCameraHelper
import com.jiangdg.usbcamera.UVCCameraHelper.OnMyDevConnectListener
import com.jiangdg.usbcamera.utils.FileUtils
import com.serenegiant.usb.CameraDialog.CameraDialogParent
import com.serenegiant.usb.USBMonitor
import com.serenegiant.usb.common.AbstractUVCCameraHandler.OnCaptureListener
import com.serenegiant.usb.common.AbstractUVCCameraHandler.OnEncodeResultListener
import com.serenegiant.usb.encoder.RecordParams
import com.serenegiant.usb.widget.CameraViewInterface
import java.util.*


class MainActivity : AppCompatActivity(), LifecycleObserver, CameraDialogParent,
    CameraViewInterface.Callback {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: HomeViewModel
    private var barcode = ""
    private var isWaiting = false
    private var isAuditInProgress = false
    private val REQUEST_CODE = 1
    private val mMissPermissions: MutableList<String> = ArrayList()

    private lateinit var mTextureView: View
    private lateinit var mCameraHelper: UVCCameraHelper
    private lateinit var mUVCCameraView: CameraViewInterface
   // private val mDialog: AlertDialog? = null
    private val defaultRecordDuration = 0
    private val TAG = "Debug"

    private var isRequest = false
    private var isPreview: Boolean = false

    private val REQUIRED_PERMISSION_LIST = arrayOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.RECORD_AUDIO
    )

    private val listener: OnMyDevConnectListener = object : OnMyDevConnectListener {
        override fun onAttachDev(device: UsbDevice) {
            // request open permission
            showShortMsg("lisnter is called1")
            if (!isRequest) {
                isRequest = true
                showShortMsg("lisnter is called2")
                if (mCameraHelper != null) {
                    showShortMsg("mCameraHelper is not Null")
                    mCameraHelper.requestPermission(0)
                }
            }
        }

        override fun onDettachDev(device: UsbDevice) {
            // close camera
            if (isRequest) {
                isRequest = false
                mCameraHelper.closeCamera()
                showShortMsg(device.deviceName + " is out")
            }
        }

        override fun onConnectDev(device: UsbDevice, isConnected: Boolean) {
            if (!isConnected) {
                showShortMsg("fail to connect,please check resolution params")
                isPreview = false
            } else {
                isPreview = true
                showShortMsg("connecting")
                // initialize seekbar
                // need to wait UVCCamera initialize over
                Thread {
                    try {
                        Thread.sleep(2500)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                    Looper.prepare()
                    if (mCameraHelper != null && mCameraHelper.isCameraOpened) {
                       binding.seekbarBrightness.setProgress(mCameraHelper.getModelValue(UVCCameraHelper.MODE_BRIGHTNESS))
                        binding.seekbarContrast.setProgress(mCameraHelper.getModelValue(UVCCameraHelper.MODE_CONTRAST))
                    }
                    Looper.loop()
                }.start()
            }
        }

        override fun onDisConnectDev(device: UsbDevice) {
            showShortMsg("disconnecting")
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        supportActionBar?.hide()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        viewModel = ViewModelProvider(this, HomeViewModel.Factory(application))
            .get(HomeViewModel::class.java)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        mTextureView = binding.cameraView


        initView();
        showShortMsg("onCreate")


        // step.1 initialize UVCCameraHelper
        mUVCCameraView = mTextureView as CameraViewInterface
        mUVCCameraView.setCallback(this)
        mCameraHelper = UVCCameraHelper.getInstance()
        mCameraHelper.setDefaultFrameFormat(UVCCameraHelper.FRAME_FORMAT_MJPEG)
        mCameraHelper.initUSBMonitor(this, mUVCCameraView, listener)

        mCameraHelper.setOnPreviewFrameListener { nv21Yuv ->
            Log.d(
                this.TAG,
                "onPreviewResult: " + nv21Yuv.size
            )
        }

        if (isVersionM()) {
            checkAndRequestPermissions()
        } else {
            startMainActivity()
        }

        try {
          binding.btnCaptureImage.setOnClickListener { captureImage() }
            binding.btnCaptureVideo.setOnClickListener { captureVideo(3) }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun showShortMsg(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onStart() {
        super.onStart()
        // step.2 register USB event broadcast
        if (mCameraHelper != null) {
            mCameraHelper.registerUSB()
        }
    }

    override fun onStop() {
        super.onStop()
        // step.3 unregister USB event broadcast
        if (mCameraHelper != null) {
            mCameraHelper.unregisterUSB()
        }
    }

    private fun initView() {
//        setSupportActionBar(mToolbar);
        binding.seekbarBrightness.setMax(100)
        binding.seekbarBrightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (mCameraHelper != null && mCameraHelper.isCameraOpened) {
                    mCameraHelper.setModelValue(UVCCameraHelper.MODE_BRIGHTNESS, progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        binding.seekbarContrast.setMax(100)
        binding.seekbarContrast.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (mCameraHelper != null && mCameraHelper.isCameraOpened) {
                    mCameraHelper.setModelValue(UVCCameraHelper.MODE_CONTRAST, progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }

    private fun isVersionM(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    }

    private fun checkAndRequestPermissions() {
        mMissPermissions. clear()
        for (permission in  REQUIRED_PERMISSION_LIST) {
            val result = ContextCompat.checkSelfPermission(this, permission)
            if (result != PackageManager.PERMISSION_GRANTED) {
                mMissPermissions.add(permission)
            }
        }
        // check permissions has granted
        if (mMissPermissions.isEmpty()) {
            startMainActivity()
        } else {
            ActivityCompat.requestPermissions(
                this,
                mMissPermissions.toTypedArray<String>(),
                REQUEST_CODE
            )
        }
    }

    private fun startMainActivity() {
        Handler().postDelayed({
            barcode = "347557"
            handleView()
//            startActivity(Intent(this@MainActivity, USBCameraActivity::class.java))
//            finish()
        }, 3000)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val pressedKey = event.unicodeChar.toChar()
        if (pressedKey.isDigit() || pressedKey.isLetter()  && !isAuditInProgress) {
            barcode += pressedKey
            Log.d("***** keyEvent - ",barcode)
            initHandle()
        }
        return true
    }

    override fun onResume() {
        super.onResume()
        checkAndRequestPermissions()
    }

    private fun initHandle() {
        if (!isWaiting) {
            isWaiting = true
            Handler(Looper.getMainLooper()).postDelayed({
                isWaiting = false
                handleView()
            }, 2000)
        }

    }

    private fun handleView() {
        isAuditInProgress = true
        viewModel.initLookup(barcode)
        //observes for any event which coming from view model
        viewModel.event.observe(this, { type ->

            when (type) {
                HomeViewModel.CALLBACK.SHOW_GREEN -> {
                    //wait for 5 seconds then switch to welcome view
                    Handler(Looper.getMainLooper()).postDelayed({
                        resetView()
                    }, 5000)
                }
                HomeViewModel.CALLBACK.SHOW_RED -> {
                    //wait for 10 seconds then switch to welcome view
                    Handler(Looper.getMainLooper()).postDelayed({
                        resetView()
                    }, 10000)
                }
                HomeViewModel.CALLBACK.SHOW_PROGRESS -> {
                    Utils.showProgressDialog(this, true)
                }
                else -> {
                    //nothing to do as of now, added this block to avoid lint warnings.
                }
            }
        })
    }

    private fun resetView() {
        isAuditInProgress = false
        barcode = ""
        isWaiting = false
        viewModel.showGreen.set(false)
        viewModel.showRed.set(false)
        viewModel.showWelcome.set(true)
    }

    private fun captureImage() {
        if (mCameraHelper == null || !mCameraHelper.isCameraOpened) {
            val msg = "sorry,camera open failed"
            showShortMsg(msg)
            return
        }
        val picPath = (UVCCameraHelper.ROOT_PATH + "USBCamera" + "/images/"
                + System.currentTimeMillis() + UVCCameraHelper.SUFFIX_JPEG)
        mCameraHelper.capturePicture(picPath,
            OnCaptureListener { path ->
                if (TextUtils.isEmpty(path)) {
                    return@OnCaptureListener
                }
                Handler(mainLooper).post {
                    Toast.makeText(
                        this@MainActivity,
                        "save path:$path", Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }


    private fun captureVideo(recordInSecs: Int?) {
        var recordInSecs = recordInSecs
        if (mCameraHelper == null || !mCameraHelper.isCameraOpened) {
            showShortMsg("sorry,camera open failed")
            return
        }
        if (recordInSecs == null) {
            recordInSecs = defaultRecordDuration
        }
        if (!mCameraHelper.isPushing) {
            val videoPath =
                (UVCCameraHelper.ROOT_PATH + "USBCamera" + "/videos/" + System.currentTimeMillis()
                        + UVCCameraHelper.SUFFIX_MP4)

//                    FileUtils.createfile(FileUtils.ROOT_PATH + "test666.h264");
            // if you want to record,please create RecordParams like this
            val params = RecordParams()
            params.recordPath = videoPath
            params.recordDuration = recordInSecs // auto divide saved,default 0 means not divided
            params.isVoiceClose = binding.switchRecVoice.isChecked() // is close voice
            params.isSupportOverlay = true // overlay only support armeabi-v7a & arm64-v8a
            mCameraHelper.startPusher(params, object : OnEncodeResultListener {
                override fun onEncodeResult(
                    data: ByteArray,
                    offset: Int,
                    length: Int,
                    timestamp: Long,
                    type: Int
                ) {
                    // type = 1,h264 video stream
                    if (type == 1) {
                        FileUtils.putFileStream(data, offset, length)
                    }
                    // type = 0,aac audio stream
                    if (type == 0) {
                        //add condtion
                    }
                }

                override fun onRecordResult(videoPath: String) {
                    if (TextUtils.isEmpty(videoPath)) {
                        return
                    }
                    Handler(mainLooper).post {
                        Toast.makeText(
                            this@MainActivity,
                            "save videoPath:$videoPath",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            })
            // if you only want to push stream,please call like this
            // mCameraHelper.startPusher(listener);
            showShortMsg("start record...")
            binding.switchRecVoice.setEnabled(false)
        } else {
            FileUtils.releaseFile()
            mCameraHelper.stopPusher()
            showShortMsg("stop record...")
            binding.switchRecVoice.setEnabled(true)
        }
    }


    override fun getUSBMonitor(): USBMonitor {
        return mCameraHelper.usbMonitor
    }

    override fun onDialogResult(canceled: Boolean) {
        if (canceled) {
            showShortMsg("Canceled")
        }
    }

    override fun onSurfaceCreated(view: CameraViewInterface?, surface: Surface?) {
        if (!isPreview && mCameraHelper.isCameraOpened) {
            mCameraHelper.startPreview(mUVCCameraView)
            isPreview = true
        }
    }

    override fun onSurfaceChanged(
        view: CameraViewInterface?,
        surface: Surface?,
        width: Int,
        height: Int
    ) {

    }

    override fun onSurfaceDestroy(view: CameraViewInterface?, surface: Surface?) {
        if (isPreview && mCameraHelper.isCameraOpened) {
            mCameraHelper.stopPreview()
            isPreview = false
        }
    }
}