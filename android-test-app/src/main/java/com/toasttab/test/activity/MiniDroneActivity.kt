package com.toasttab.test.activity

import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.TextView

import com.parrot.arsdk.arcommands.ARCOMMANDS_MINIDRONE_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR_ENUM
import com.parrot.arsdk.arcommands.ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DEVICE_STATE_ENUM
import com.parrot.arsdk.arcontroller.ARControllerCodec
import com.parrot.arsdk.arcontroller.ARFrame
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService
import com.parrot.sdksample.drone.MiniDrone
import com.toasttab.drone.Drone
import com.toasttab.drone.DroneControllerImpl
import com.toasttab.drone.Location
import com.toasttab.test.R
import com.toasttab.test.view.H264VideoView
import kotlinx.android.synthetic.main.activity_minidrone.*

class MiniDroneActivity : AppCompatActivity() {
    private var mMiniDrone: MiniDrone? = null

    private var mConnectionProgressDialog: ProgressDialog? = null
    private var mDownloadProgressDialog: ProgressDialog? = null

    private var mVideoView: H264VideoView? = null

    private var mBatteryLabel: TextView? = null
    private var mTakeOffLandBt: Button? = null
    private var mDownloadBt: Button? = null

    private var mNbMaxDownload: Int = 0
    private var mCurrentDownloadIndex: Int = 0

    private val mMiniDroneListener = object : MiniDrone.Listener {
        override fun onDroneConnectionChanged(state: ARCONTROLLER_DEVICE_STATE_ENUM) {
            when (state) {
                ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING -> mConnectionProgressDialog!!.dismiss()

                ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_STOPPED -> {
                    // if the deviceController is stopped, go back to the previous activity
                    mConnectionProgressDialog!!.dismiss()
                    finish()
                }

                else -> {
                }
            }
        }

        override fun onBatteryChargeChanged(batteryPercentage: Int) {
            mBatteryLabel!!.text = String.format("%d%%", batteryPercentage)
        }

        override fun onPilotingStateChanged(state: ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM) {
            when (state) {
                ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_LANDED -> {
                    mTakeOffLandBt!!.text = "Take off"
                    mTakeOffLandBt!!.isEnabled = true
                    mDownloadBt!!.isEnabled = true
                }
                ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING, ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING -> {
                    mTakeOffLandBt!!.text = "Land"
                    mTakeOffLandBt!!.isEnabled = true
                    mDownloadBt!!.isEnabled = false
                }
                else -> {
                    mTakeOffLandBt!!.isEnabled = false
                    mDownloadBt!!.isEnabled = false
                }
            }
        }

        override fun onPictureTaken(error: ARCOMMANDS_MINIDRONE_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR_ENUM) {
            Log.i(TAG, "Picture has been taken")
        }

        override fun configureDecoder(codec: ARControllerCodec) {
            mVideoView!!.configureDecoder(codec)
        }

        override fun onFrameReceived(frame: ARFrame) {
            mVideoView!!.displayFrame(frame)
        }

        override fun onMatchingMediasFound(nbMedias: Int) {
            mDownloadProgressDialog!!.dismiss()

            mNbMaxDownload = nbMedias
            mCurrentDownloadIndex = 1

            if (nbMedias > 0) {
                mDownloadProgressDialog = ProgressDialog(this@MiniDroneActivity, R.style.AppCompatAlertDialogStyle)
                mDownloadProgressDialog!!.isIndeterminate = false
                mDownloadProgressDialog!!.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
                mDownloadProgressDialog!!.setMessage("Downloading medias")
                mDownloadProgressDialog!!.max = mNbMaxDownload * 100
                mDownloadProgressDialog!!.secondaryProgress = mCurrentDownloadIndex * 100
                mDownloadProgressDialog!!.progress = 0
                mDownloadProgressDialog!!.setCancelable(false)
                mDownloadProgressDialog!!.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel") { dialog, which -> mMiniDrone!!.cancelGetLastFlightMedias() }
                mDownloadProgressDialog!!.show()
            }
        }

        override fun onDownloadProgressed(mediaName: String, progress: Int) {
            mDownloadProgressDialog!!.progress = (mCurrentDownloadIndex - 1) * 100 + progress
        }

        override fun onDownloadComplete(mediaName: String) {
            mCurrentDownloadIndex++
            mDownloadProgressDialog!!.secondaryProgress = mCurrentDownloadIndex * 100

            if (mCurrentDownloadIndex > mNbMaxDownload) {
                mDownloadProgressDialog!!.dismiss()
                mDownloadProgressDialog = null
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_minidrone)

        initIHM()

        val intent = intent
        val service = intent.getParcelableExtra<ARDiscoveryDeviceService>(DeviceListActivity.EXTRA_DEVICE_SERVICE)
        mMiniDrone = MiniDrone(this, service)
        mMiniDrone!!.addListener(mMiniDroneListener)

    }

    override fun onStart() {
        super.onStart()

        // show a loading view while the minidrone is connecting
        if (mMiniDrone != null && ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING != mMiniDrone!!.connectionState) {
            mConnectionProgressDialog = ProgressDialog(this, R.style.AppCompatAlertDialogStyle)
            mConnectionProgressDialog!!.isIndeterminate = true
            mConnectionProgressDialog!!.setMessage("Connecting ...")
            mConnectionProgressDialog!!.setCancelable(false)
            mConnectionProgressDialog!!.show()

            // if the connection to the MiniDrone fails, finish the activity
            if (!mMiniDrone!!.connect()) {
                finish()
            }
        }
    }

    override fun onBackPressed() {
        if (mMiniDrone != null) {
            mConnectionProgressDialog = ProgressDialog(this, R.style.AppCompatAlertDialogStyle)
            mConnectionProgressDialog!!.isIndeterminate = true
            mConnectionProgressDialog!!.setMessage("Disconnecting ...")
            mConnectionProgressDialog!!.setCancelable(false)
            mConnectionProgressDialog!!.show()

            if (!mMiniDrone!!.disconnect()) {
                finish()
            }
        } else {
            finish()
        }
    }

    public override fun onDestroy() {
        mMiniDrone!!.dispose()
        super.onDestroy()
    }

    private fun initIHM() {
        mVideoView = findViewById(R.id.videoView) as H264VideoView

        findViewById(R.id.emergencyBt).setOnClickListener { mMiniDrone!!.emergency() }

        mTakeOffLandBt = findViewById(R.id.takeOffOrLandBt) as Button
        mTakeOffLandBt!!.setOnClickListener {
            when (mMiniDrone!!.flyingState) {
                ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_LANDED -> mMiniDrone!!.takeOff()
                ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING, ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING -> mMiniDrone!!.land()
            }
        }

        testCommand.setOnClickListener {
            val droneController = DroneControllerImpl(mMiniDrone as Drone, Location(0.0, 0.0))
            droneController.sendToLocation(Location(5.0, 1.0))
        }

        findViewById(R.id.takePictureBt).setOnClickListener { mMiniDrone!!.takePicture() }

        mDownloadBt = findViewById(R.id.downloadBt) as Button
        mDownloadBt!!.isEnabled = false
        mDownloadBt!!.setOnClickListener {
            mMiniDrone!!.getLastFlightMedias()

            mDownloadProgressDialog = ProgressDialog(this@MiniDroneActivity, R.style.AppCompatAlertDialogStyle)
            mDownloadProgressDialog!!.isIndeterminate = true
            mDownloadProgressDialog!!.setMessage("Fetching medias")
            mDownloadProgressDialog!!.setCancelable(false)
            mDownloadProgressDialog!!.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel") { dialog, which -> mMiniDrone!!.cancelGetLastFlightMedias() }
            mDownloadProgressDialog!!.show()
        }

        findViewById(R.id.gazUpBt).setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                    mMiniDrone!!.setGaz(50.toByte())
                }

                MotionEvent.ACTION_UP -> {
                    v.isPressed = false
                    mMiniDrone!!.setGaz(0.toByte())
                }

                else -> {
                }
            }

            true
        }

        findViewById(R.id.gazDownBt).setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                    mMiniDrone!!.setGaz((-50).toByte())
                }

                MotionEvent.ACTION_UP -> {
                    v.isPressed = false
                    mMiniDrone!!.setGaz(0.toByte())
                }

                else -> {
                }
            }

            true
        }

        findViewById(R.id.yawLeftBt).setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                    mMiniDrone!!.setYaw((-50).toByte())
                }

                MotionEvent.ACTION_UP -> {
                    v.isPressed = false
                    mMiniDrone!!.setYaw(0.toByte())
                }

                else -> {
                }
            }

            true
        }

        findViewById(R.id.yawRightBt).setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                    mMiniDrone!!.setYaw(50.toByte())
                }

                MotionEvent.ACTION_UP -> {
                    v.isPressed = false
                    mMiniDrone!!.setYaw(0.toByte())
                }

                else -> {
                }
            }

            true
        }

        findViewById(R.id.forwardBt).setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                    mMiniDrone!!.setPitch(50.toByte())
                    mMiniDrone!!.setFlag(1.toByte())
                }

                MotionEvent.ACTION_UP -> {
                    v.isPressed = false
                    mMiniDrone!!.setPitch(0.toByte())
                    mMiniDrone!!.setFlag(0.toByte())
                }

                else -> {
                }
            }

            true
        }

        findViewById(R.id.backBt).setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                    mMiniDrone!!.setPitch((-50).toByte())
                    mMiniDrone!!.setFlag(1.toByte())
                }

                MotionEvent.ACTION_UP -> {
                    v.isPressed = false
                    mMiniDrone!!.setPitch(0.toByte())
                    mMiniDrone!!.setFlag(0.toByte())
                }

                else -> {
                }
            }

            true
        }

        findViewById(R.id.rollLeftBt).setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                    mMiniDrone!!.setRoll((-50).toByte())
                    mMiniDrone!!.setFlag(1.toByte())
                }

                MotionEvent.ACTION_UP -> {
                    v.isPressed = false
                    mMiniDrone!!.setRoll(0.toByte())
                    mMiniDrone!!.setFlag(0.toByte())
                }

                else -> {
                }
            }

            true
        }

        findViewById(R.id.rollRightBt).setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                    mMiniDrone!!.setRoll(50.toByte())
                    mMiniDrone!!.setFlag(1.toByte())
                }

                MotionEvent.ACTION_UP -> {
                    v.isPressed = false
                    mMiniDrone!!.setRoll(0.toByte())
                    mMiniDrone!!.setFlag(0.toByte())
                }

                else -> {
                }
            }

            true
        }

        mBatteryLabel = findViewById(R.id.batteryLabel) as TextView
    }

    companion object {
        private val TAG = "MiniDroneActivity"
    }
}
