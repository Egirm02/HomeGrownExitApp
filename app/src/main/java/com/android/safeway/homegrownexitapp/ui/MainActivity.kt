package com.android.safeway.homegrownexitapp.ui


import android.Manifest
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.usb.UsbDevice
import android.os.*
import android.text.TextUtils
import android.util.Log
import android.view.KeyEvent
import android.view.Surface
import android.view.View
import android.view.WindowManager
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModelProvider
import com.android.safeway.homegrownexitapp.R
import com.android.safeway.homegrownexitapp.databinding.ActivityMainBinding
import com.android.safeway.homegrownexitapp.util.BluetoothLeService
import com.android.safeway.homegrownexitapp.util.BluetoothLeService.LocalBinder
import com.android.safeway.homegrownexitapp.util.SampleGattAttributes
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
import java.io.File
import java.util.*

const val LIGHT_RED = "2"
const val LIGHT_GREEN = "1"
const val LIGHT_OFF = "0"

class MainActivity : AppCompatActivity(), LifecycleObserver, CameraDialogParent,
    CameraViewInterface.Callback {


    private var picPath: String = ""
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

    //ble
    private var mBluetoothLeService: BluetoothLeService? = null
    private var mConnected = false
    private var characteristicTX: BluetoothGattCharacteristic? = null
    private var characteristicRX: BluetoothGattCharacteristic? = null

    val HM_RX_TX = UUID.fromString(SampleGattAttributes.HM_RX_TX)

    private val LIST_NAME = "NAME"
    private val LIST_UUID = "UUID"
    private val mConnectionState: TextView? = null

    private val mDeviceName: String? = null
    private val mDeviceAddress = "10:CE:A9:EA:95:45"

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
        try {
            mCameraHelper.setDefaultFrameFormat(UVCCameraHelper.FRAME_FORMAT_MJPEG)
            mCameraHelper.initUSBMonitor(this, mUVCCameraView, listener)
            mCameraHelper.setOnPreviewFrameListener { nv21Yuv ->
                Log.d(
                    this.TAG,
                    "onPreviewResult: " + nv21Yuv.size
                )
            }
        } catch (e: IllegalStateException) {
            //ignore for now
        }

        if (isVersionM()) {
            checkAndRequestPermissions()
        }

        try {
            binding.btnCaptureImage.setOnClickListener { changeLightState("1") }
            binding.btnCaptureVideo.setOnClickListener { captureVideo(3) }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        //ble


//        getActionBar().setTitle(mDeviceName);
        //      getActionBar().setDisplayHomeAsUpEnabled(true);
        val gattServiceIntent = Intent(this, BluetoothLeService::class.java)
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE)


    }

    private fun changeLightState(state: String) {
        characteristicTX?.setValue(state)
        mBluetoothLeService?.writeCharacteristic(characteristicTX)
        mBluetoothLeService?.setCharacteristicNotification(characteristicRX, true)
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
        if (mMissPermissions.isEmpty().not()) {
            ActivityCompat.requestPermissions(
                this,
                mMissPermissions.toTypedArray<String>(),
                REQUEST_CODE
            )
        }
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
        captureImage()
        isAuditInProgress = true
        viewModel.initLookup(barcode)
        //observes for any event which coming from view model
        viewModel.event.observe(this, { type ->

            when (type) {
                HomeViewModel.CALLBACK.SHOW_GREEN -> {
                    binding.imgBasket.setBackgroundColor(getColor(android.R.color.holo_green_dark))
                    changeLightState(LIGHT_GREEN)
                    //wait for 5 seconds then switch to welcome view
                    Handler(Looper.getMainLooper()).postDelayed({
                        resetView()
                    }, 5000)
                }
                HomeViewModel.CALLBACK.SHOW_RED -> {
                    binding.imgBasket.setBackgroundColor(getColor(android.R.color.holo_red_dark))
                    changeLightState(LIGHT_RED)
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
        viewModel.showSurfaceView.set(true)
        viewModel.showImage.set(false)
        binding.imgBasket.setBackgroundColor(getColor(R.color.white))
        changeLightState(LIGHT_OFF)


    }

    private fun captureImage() {
        if (mCameraHelper == null || !mCameraHelper.isCameraOpened) {
            val msg = "sorry,camera open failed"
            showShortMsg(msg)
            return
        }
        picPath = (UVCCameraHelper.ROOT_PATH + "USBCamera" + "/images/"
                + System.currentTimeMillis() + UVCCameraHelper.SUFFIX_PNG)
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
                    loadImage()
                }
            })
    }

    /**
     * Method to load the image to the view to show preview.
     */
    private fun loadImage() {
        try {
            val imgFile = File(picPath)
            if (imgFile.exists()) {
                val myBitmap: Bitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                binding.imgBasket.setImageBitmap(myBitmap)
                viewModel.showSurfaceView.set(false)
                viewModel.showImage.set(true)

            }
        } catch (e: Exception) {
            //there are chances of getting Nullpointer, IllegalArgument, SecurityExceptions. In all the cases we need to do the same. Hence not catching it separately.
            viewModel.showSurfaceView.set(true)
            viewModel.showImage.set(false)
        }


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

    //ble
                //-1
    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            mBluetoothLeService = (service as LocalBinder).service
            mBluetoothLeService?.let{
                if (!it.initialize()) {
                    //  Log.e(DeviceControlActivity.TAG, "Unable to initialize Bluetooth")
                    finish()
                }
                // Automatically connects to the device upon successful start-up initialization.
                it.connect(mDeviceAddress)
            }

        }

        override fun onServiceDisconnected(componentName: ComponentName) {
          //  mBluetoothLeService = null
        }
    }
                //-2
    private val mGattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothLeService.ACTION_GATT_CONNECTED == action) {
                mConnected = true
                updateConnectionState(R.string.connected)
                invalidateOptionsMenu()
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED == action) {
                mConnected = false
                updateConnectionState(R.string.disconnected)
                invalidateOptionsMenu()
               // clearUI()
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED == action) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService?.supportedGattServices)
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE == action) {
              //  displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA))
            }
        }
    }

                //-3
    override fun onResume() {
        super.onResume()
        checkAndRequestPermissions()
                    registerReceiver(
                        mGattUpdateReceiver, makeGattUpdateIntentFilter()
                    )
                    if (mBluetoothLeService != null) {
                        val result = mBluetoothLeService?.connect(mDeviceAddress)
                     //   Log.d(DeviceControlActivity.TAG, "Connect request result=$result")
                    }
    }

    //-4
    private fun updateConnectionState(resourceId: Int) {
        runOnUiThread { mConnectionState?.setText(resourceId) }
    }


    //-5
    private fun displayGattServices(gattServices: List<BluetoothGattService>?) {
        if (gattServices == null) return
        var uuid: String? = null
        val unknownServiceString = resources.getString(R.string.unknown_service)
        val gattServiceData = ArrayList<HashMap<String, String>>()


        // Loops through available GATT Services.
        for (gattService in gattServices) {
            val currentServiceData = HashMap<String, String>()
            uuid = gattService.uuid.toString()
            currentServiceData[LIST_NAME] = SampleGattAttributes.lookup(uuid, unknownServiceString)

            // If the service exists for HM 10 Serial, say so.
            if (SampleGattAttributes.lookup(uuid, unknownServiceString) === "HM 10 Serial") {
              //  isSerial.setText("Yes, serial :-)")
            } else {
              //  isSerial.setText("No, serial :-(")
            }
            currentServiceData[LIST_UUID] = uuid
            gattServiceData.add(currentServiceData)

            // get characteristic when UUID matches RX/TX UUID
            characteristicTX = gattService.getCharacteristic(BluetoothLeService.UUID_HM_RX_TX)
            characteristicRX = gattService.getCharacteristic(BluetoothLeService.UUID_HM_RX_TX)
        }
    }

    //-6
    private fun makeGattUpdateIntentFilter(): IntentFilter? {
        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE)
        return intentFilter
    }


}