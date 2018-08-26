package com.toasttab.test.activity

import android.app.ProgressDialog
import android.content.DialogInterface
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MotionEvent
import android.widget.Toast
import com.parrot.arsdk.arcommands.ARCOMMANDS_MINIDRONE_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR_ENUM
import com.parrot.arsdk.arcommands.ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DEVICE_STATE_ENUM
import com.parrot.arsdk.arcontroller.ARControllerCodec
import com.parrot.arsdk.arcontroller.ARFrame
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService
import com.parrot.sdksample.drone.MiniDrone
import com.toasttab.test.R
import kotlinx.android.synthetic.main.activity_minidrone.*
import timber.log.Timber


class KMiniDroneActivity : AppCompatActivity() {
    val TAG = javaClass.name

    var mMiniDrone: MiniDrone? = null
    lateinit var mConnectionProgressDialog: ProgressDialog
    lateinit var mDownloadProgressDialog: ProgressDialog

    var mNbMaxDownload = 0
    var mCurrentDownloadIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("Started activity: $TAG")

        setContentView(R.layout.activity_minidrone)

        initIHM()

        val intent = intent
        val service = intent.getParcelableExtra<ARDiscoveryDeviceService>(DeviceListActivity.EXTRA_DEVICE_SERVICE)

        // TODO Conor: passing in new service just to prevent NPE, use intent-provided service when ready.
        // mMiniDrone = MiniDrone(this, service)
        mMiniDrone = MiniDrone(this, ARDiscoveryDeviceService())
        mMiniDrone?.addListener(mMiniDroneListener) ?: Timber.d("Drone not initialised")
    }

    override fun onStart() {
        super.onStart()

        if (mMiniDrone == null) {
            return
        }

        // show a loading view while the minidrone is connecting
        if (ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING != mMiniDrone?.connectionState) {
            mConnectionProgressDialog = ProgressDialog(this, R.style.AppCompatAlertDialogStyle)
            with(mConnectionProgressDialog) {
                isIndeterminate = true
                setMessage("Connecting...")
                setCancelable(false)
                // TODO Conor: dialog temporarily not shown as it causes View to leak.
                // WindowManager: android.view.WindowLeaked
//                 show()
            }

            // if the connection to the MiniDrone fails, finish the activity
            if (!mMiniDrone!!.connect()) {
                Toast.makeText(this, "Connection to drone failed", Toast.LENGTH_LONG).show()
                // finish() todo uncomment when working with real drone.
            }
        }
    }

    override fun onBackPressed() {
        if (mMiniDrone == null || !mMiniDrone!!.disconnect())
            finish()

        mConnectionProgressDialog = ProgressDialog(this, R.style.AppCompatAlertDialogStyle)
        with(mConnectionProgressDialog) {
            isIndeterminate = true
            setMessage("Disconnecting ...")
            setCancelable(false)
            show()
        }
    }

    public override fun onDestroy() {
        mMiniDrone?.dispose()
        super.onDestroy()
    }


    private fun initIHM() {
        Timber.d("initHM...")

        emergencyBt.setOnClickListener { mMiniDrone?.emergency() }

        takeOffOrLandBt.setOnClickListener {
            when (mMiniDrone?.flyingState) {
                ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_LANDED -> mMiniDrone?.takeOff()
                ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING, ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING -> mMiniDrone?.land()
                else -> Timber.d("MiniDrone flying state not handled: ${mMiniDrone?.flyingState}")
            }
        }

        takePictureBt.setOnClickListener { mMiniDrone?.takePicture() }

        with(downloadBt) {
            isEnabled = true
            setOnClickListener {
                mMiniDrone?.getLastFlightMedias()

                mDownloadProgressDialog = ProgressDialog(this@KMiniDroneActivity, R.style.AppCompatAlertDialogStyle)
                with(mDownloadProgressDialog) {
                    isIndeterminate = true
                    setMessage("Fetching medias")
                    setCancelable(false)
                    setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel") { dialog, which -> mMiniDrone?.cancelGetLastFlightMedias() }
                    show()
                }
            }
        }


        // Up action
        gazUpBt.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                    mMiniDrone?.setGaz(50.toByte())
                }

                MotionEvent.ACTION_UP -> {
                    v.isPressed = false
                    mMiniDrone?.setGaz(0.toByte())
                }
            }
            true
        }

        // Down action
        gazDownBt.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                    mMiniDrone?.setGaz((-50).toByte())
                }

                MotionEvent.ACTION_UP -> {
                    v.isPressed = false
                    mMiniDrone?.setGaz(0.toByte())
                }
            }
            true
        }

        // Yaw left action
        yawLeftBt.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                    mMiniDrone?.setYaw((-50).toByte())
                }

                MotionEvent.ACTION_UP -> {
                    v.isPressed = false
                    mMiniDrone?.setYaw(0.toByte())
                }
            }
            true
        }

        // Yaw right action
        yawRightBt.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                    mMiniDrone?.setYaw(50.toByte())
                }

                MotionEvent.ACTION_UP -> {
                    v.isPressed = false
                    mMiniDrone?.setYaw(0.toByte())
                }
            }
            true
        }

        // Forward action
        forwardBt.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                    mMiniDrone?.setPitch(50.toByte())
                    mMiniDrone?.setFlag(1.toByte())
                }

                MotionEvent.ACTION_UP -> {
                    v.isPressed = false
                    mMiniDrone?.setPitch(0.toByte())
                    mMiniDrone?.setFlag(0.toByte())
                }
            }
            true
        }

        backBt.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                    mMiniDrone?.setPitch((-50).toByte())
                    mMiniDrone?.setFlag(1.toByte())
                }

                MotionEvent.ACTION_UP -> {
                    v.isPressed = false
                    mMiniDrone?.setPitch(0.toByte())
                    mMiniDrone?.setFlag(0.toByte())
                }
            }
            true
        }

        rollLeftBt.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                    mMiniDrone?.setRoll((-50).toByte())
                    mMiniDrone?.setFlag(1.toByte())
                }

                MotionEvent.ACTION_UP -> {
                    v.isPressed = false
                    mMiniDrone?.setRoll(0.toByte())
                    mMiniDrone?.setFlag(0.toByte())
                }
            }
            true
        }

        rollRightBt.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                    mMiniDrone?.setRoll(50.toByte())
                    mMiniDrone?.setFlag(1.toByte())
                }

                MotionEvent.ACTION_UP -> {
                    v.isPressed = false
                    mMiniDrone?.setRoll(0.toByte())
                    mMiniDrone?.setFlag(0.toByte())
                }
            }
            true
        }
    }


    private val mMiniDroneListener = object : MiniDrone.Listener {
        override fun onDroneConnectionChanged(state: ARCONTROLLER_DEVICE_STATE_ENUM?) {
            when (state) {
                ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING -> mConnectionProgressDialog.dismiss()

                ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_STOPPED -> {
                    // if the deviceController is stopped, go back to the previous activity
                    mConnectionProgressDialog.dismiss()
                    finish()
                }
                else -> {
                }
            }
        }

        override fun onBatteryChargeChanged(batteryPercentage: Int) {
            batteryLabel.text = String.format("%d%%", batteryPercentage)
        }

        override fun onPilotingStateChanged(state: ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM) {
            when (state) {
                ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_LANDED -> {
                    takeOffOrLandBt.text = "Take off"
                    takeOffOrLandBt.isEnabled = true
                    downloadBt.isEnabled = true
                }
                ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING, ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING -> {
                    takeOffOrLandBt.text = "Land"
                    takeOffOrLandBt.isEnabled = true
                    downloadBt.isEnabled = false
                }
                else -> {
                    takeOffOrLandBt.isEnabled = false
                    downloadBt.isEnabled = false
                }
            }
        }

        override fun onPictureTaken(error: ARCOMMANDS_MINIDRONE_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR_ENUM) {
            Timber.tag(TAG).i("Picture has been taken")
        }

        override fun configureDecoder(codec: ARControllerCodec) {
            videoView.configureDecoder(codec)
        }

        override fun onFrameReceived(frame: ARFrame) {
            videoView.displayFrame(frame)
        }

        override fun onMatchingMediasFound(nbMedias: Int) {
            mDownloadProgressDialog.dismiss()

            mNbMaxDownload = nbMedias
            mCurrentDownloadIndex = 1

            if (nbMedias > 0) {
                mDownloadProgressDialog = ProgressDialog(this@KMiniDroneActivity, R.style.AppCompatAlertDialogStyle)
                with(mDownloadProgressDialog) {
                    isIndeterminate = false
                    setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
                    setMessage("Downloading medias")
                    max = mNbMaxDownload * 100
                    secondaryProgress = mCurrentDownloadIndex * 100
                    progress = 0
                    setCancelable(false)
                    setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel") { dialog, which -> mMiniDrone?.cancelGetLastFlightMedias() }
                    show()
                }

            }
        }

        override fun onDownloadProgressed(mediaName: String, progress: Int) {
            mDownloadProgressDialog.progress = (mCurrentDownloadIndex - 1) * 100 + progress
        }

        override fun onDownloadComplete(mediaName: String) {
            mCurrentDownloadIndex++
            mDownloadProgressDialog.secondaryProgress = mCurrentDownloadIndex * 100

            if (mCurrentDownloadIndex > mNbMaxDownload) {
                mDownloadProgressDialog.dismiss()
//                mDownloadProgressDialog = null
            }
        }
    }
}