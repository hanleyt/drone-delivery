package com.parrot.sdksample.drone

import android.content.Context
import android.os.Handler
import android.util.Log
import com.parrot.arsdk.arcommands.ARCOMMANDS_MINIDRONE_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR_ENUM
import com.parrot.arsdk.arcommands.ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DEVICE_STATE_ENUM
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DICTIONARY_KEY_ENUM
import com.parrot.arsdk.arcontroller.ARCONTROLLER_ERROR_ENUM
import com.parrot.arsdk.arcontroller.ARControllerCodec
import com.parrot.arsdk.arcontroller.ARControllerDictionary
import com.parrot.arsdk.arcontroller.ARControllerException
import com.parrot.arsdk.arcontroller.ARDeviceController
import com.parrot.arsdk.arcontroller.ARDeviceControllerListener
import com.parrot.arsdk.arcontroller.ARDeviceControllerStreamListener
import com.parrot.arsdk.arcontroller.ARFeatureCommon
import com.parrot.arsdk.arcontroller.ARFeatureMiniDrone
import com.parrot.arsdk.arcontroller.ARFrame
import com.parrot.arsdk.ardiscovery.ARDISCOVERY_PRODUCT_ENUM
import com.parrot.arsdk.ardiscovery.ARDISCOVERY_PRODUCT_FAMILY_ENUM
import com.parrot.arsdk.ardiscovery.ARDiscoveryDevice
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService
import com.parrot.arsdk.ardiscovery.ARDiscoveryException
import com.parrot.arsdk.ardiscovery.ARDiscoveryService
import com.parrot.arsdk.arutils.ARUTILS_DESTINATION_ENUM
import com.parrot.arsdk.arutils.ARUTILS_FTP_TYPE_ENUM
import com.parrot.arsdk.arutils.ARUtilsException
import com.parrot.arsdk.arutils.ARUtilsManager
import com.toasttab.drone.Drone
import timber.log.Timber
import java.util.ArrayList

class MiniDrone(private val mContext: Context, private val mDeviceService: ARDiscoveryDeviceService) : Drone {

    private val mListeners = mutableListOf<Listener>()

    private lateinit var mHandler: Handler

    private var mDeviceController: ARDeviceController? = null
    private var mSDCardModule: SDCardModule? = null
    /**
     * Get the current connection state
     * @return the connection state of the drone
     */
    var connectionState: ARCONTROLLER_DEVICE_STATE_ENUM = ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_STOPPED
        private set
    /**
     * Get the current flying state
     * @return the flying state
     */
    var flyingState: ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM? = null
        private set
    private var mCurrentRunId: String? = null
    private val mProductType: ARDISCOVERY_PRODUCT_ENUM

    private var mFtpListManager: ARUtilsManager? = null
    private var mFtpQueueManager: ARUtilsManager? = null
    //endregion notify listener block

    private val mSDCardModuleListener = object : SDCardModule.Listener {
        override fun onMatchingMediasFound(nbMedias: Int) {
            mHandler.post { notifyMatchingMediasFound(nbMedias) }
        }

        override fun onDownloadProgressed(mediaName: String, progress: Int) {
            mHandler.post { notifyDownloadProgressed(mediaName, progress) }
        }

        override fun onDownloadComplete(mediaName: String) {
            mHandler.post { notifyDownloadComplete(mediaName) }
        }
    }

    init {
        // needed because some callbacks will be called on the main thread
        mHandler = Handler(mContext.mainLooper)


        // if the product type of the deviceService match with the types supported
        mProductType = ARDiscoveryService.getProductFromProductID(mDeviceService.productID)
        val family = ARDiscoveryService.getProductFamily(mProductType)
        if (ARDISCOVERY_PRODUCT_FAMILY_ENUM.ARDISCOVERY_PRODUCT_FAMILY_MINIDRONE == family) {

            val discoveryDevice = createDiscoveryDevice(mDeviceService)
            if (discoveryDevice != null) {
                mDeviceController = createDeviceController(discoveryDevice)
                discoveryDevice.dispose()
            }

        } else {
            Timber.tag(TAG).e("DeviceService type is not supported by MiniDrone")
        }
    }

    private fun createDiscoveryDevice(service: ARDiscoveryDeviceService): ARDiscoveryDevice? {
        var device: ARDiscoveryDevice? = null
        try {
            device = ARDiscoveryDevice(mContext, service)
        } catch (e: ARDiscoveryException) {
            Timber.tag(TAG).e(e, "Exception")
            Timber.tag(TAG).e("Error: %s", e.error)
        }

        return device
    }

    private val mDeviceControllerListener = object : ARDeviceControllerListener {
        override fun onStateChanged(deviceController: ARDeviceController, newState: ARCONTROLLER_DEVICE_STATE_ENUM, error: ARCONTROLLER_ERROR_ENUM) {
            connectionState = newState
            if (ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING == connectionState) {
                mDeviceController!!.startVideoStream()
            } else if (ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_STOPPED == connectionState) {
                if (mSDCardModule != null) {
                    mSDCardModule!!.cancelGetFlightMedias()
                }
                if (mFtpListManager != null) {
                    mFtpListManager!!.closeFtp(mContext, mDeviceService)
                    mFtpListManager = null
                }
                if (mFtpQueueManager != null) {
                    mFtpQueueManager!!.closeFtp(mContext, mDeviceService)
                    mFtpQueueManager = null
                }
            }
            mHandler.post { notifyConnectionChanged(connectionState) }
        }

        override fun onExtensionStateChanged(deviceController: ARDeviceController, newState: ARCONTROLLER_DEVICE_STATE_ENUM, product: ARDISCOVERY_PRODUCT_ENUM, name: String, error: ARCONTROLLER_ERROR_ENUM) {}

        override fun onCommandReceived(deviceController: ARDeviceController, commandKey: ARCONTROLLER_DICTIONARY_KEY_ENUM, elementDictionary: ARControllerDictionary?) {
            // if event received is the battery update
            if (commandKey == ARCONTROLLER_DICTIONARY_KEY_ENUM.ARCONTROLLER_DICTIONARY_KEY_COMMON_COMMONSTATE_BATTERYSTATECHANGED && elementDictionary != null) {
                val args = elementDictionary[ARControllerDictionary.ARCONTROLLER_DICTIONARY_SINGLE_KEY]
                if (args != null) {
                    val battery = args[ARFeatureCommon.ARCONTROLLER_DICTIONARY_KEY_COMMON_COMMONSTATE_BATTERYSTATECHANGED_PERCENT] as Int
                    mHandler.post { notifyBatteryChanged(battery) }
                }
            } else if (commandKey == ARCONTROLLER_DICTIONARY_KEY_ENUM.ARCONTROLLER_DICTIONARY_KEY_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED && elementDictionary != null) {
                val args = elementDictionary[ARControllerDictionary.ARCONTROLLER_DICTIONARY_SINGLE_KEY]
                if (args != null) {
                    val state = ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.getFromValue(args[ARFeatureMiniDrone.ARCONTROLLER_DICTIONARY_KEY_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE] as Int)

                    mHandler.post {
                        flyingState = state
                        notifyPilotingStateChanged(state)
                    }
                }
            } else if (commandKey == ARCONTROLLER_DICTIONARY_KEY_ENUM.ARCONTROLLER_DICTIONARY_KEY_MINIDRONE_MEDIARECORDEVENT_PICTUREEVENTCHANGED && elementDictionary != null) {
                val args = elementDictionary[ARControllerDictionary.ARCONTROLLER_DICTIONARY_SINGLE_KEY]
                if (args != null) {
                    val error = ARCOMMANDS_MINIDRONE_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR_ENUM.getFromValue(args[ARFeatureMiniDrone.ARCONTROLLER_DICTIONARY_KEY_MINIDRONE_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR] as Int)
                    mHandler.post { notifyPictureTaken(error) }
                }
            } else if (commandKey == ARCONTROLLER_DICTIONARY_KEY_ENUM.ARCONTROLLER_DICTIONARY_KEY_COMMON_RUNSTATE_RUNIDCHANGED && elementDictionary != null) {
                val args = elementDictionary[ARControllerDictionary.ARCONTROLLER_DICTIONARY_SINGLE_KEY]
                if (args != null) {
                    val runID = args[ARFeatureCommon.ARCONTROLLER_DICTIONARY_KEY_COMMON_RUNSTATE_RUNIDCHANGED_RUNID] as String
                    mHandler.post { mCurrentRunId = runID }
                }
            }// if event received is the run id
            // if event received is the picture notification
            // if event received is the flying state update
        }
    }

    private val mStreamListener = object : ARDeviceControllerStreamListener {
        override fun configureDecoder(deviceController: ARDeviceController, codec: ARControllerCodec): ARCONTROLLER_ERROR_ENUM {
            notifyConfigureDecoder(codec)
            return ARCONTROLLER_ERROR_ENUM.ARCONTROLLER_OK
        }

        override fun onFrameReceived(deviceController: ARDeviceController, frame: ARFrame): ARCONTROLLER_ERROR_ENUM {
            notifyFrameReceived(frame)
            return ARCONTROLLER_ERROR_ENUM.ARCONTROLLER_OK
        }

        override fun onFrameTimeout(deviceController: ARDeviceController) {}
    }

    interface Listener {
        /**
         * Called when the connection to the drone changes
         * Called in the main thread
         * @param state the state of the drone
         */
        fun onDroneConnectionChanged(state: ARCONTROLLER_DEVICE_STATE_ENUM?)

        /**
         * Called when the battery charge changes
         * Called in the main thread
         * @param batteryPercentage the battery remaining (in percent)
         */
        fun onBatteryChargeChanged(batteryPercentage: Int)

        /**
         * Called when the piloting state changes
         * Called in the main thread
         * @param state the piloting state of the drone
         */
        fun onPilotingStateChanged(state: ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM)

        /**
         * Called when a picture is taken
         * Called on a separate thread
         * @param error ERROR_OK if picture has been taken, otherwise describe the error
         */
        fun onPictureTaken(error: ARCOMMANDS_MINIDRONE_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR_ENUM)

        /**
         * Called when the video decoder should be configured
         * Called on a separate thread & only for Mambo FPV
         * @param codec the codec to configure the decoder with
         */
        fun configureDecoder(codec: ARControllerCodec)

        /**
         * Called when a video frame has been received
         * Called on a separate thread & only for Mambo FPV
         * @param frame the video frame
         */
        fun onFrameReceived(frame: ARFrame)

        /**
         * Called before medias will be downloaded
         * Called in the main thread
         * @param nbMedias the number of medias that will be downloaded
         */
        fun onMatchingMediasFound(nbMedias: Int)

        /**
         * Called each time the progress of a download changes
         * Called in the main thread
         * @param mediaName the name of the media
         * @param progress the progress of its download (from 0 to 100)
         */
        fun onDownloadProgressed(mediaName: String, progress: Int)

        /**
         * Called when a media download has ended
         * Called in the main thread
         * @param mediaName the name of the media
         */
        fun onDownloadComplete(mediaName: String)
    }

    fun dispose() {
        if (mDeviceController != null)
            mDeviceController!!.dispose()
    }

    //region DroneDiscoveryListener functions
    fun addListener(listener: Listener) {
        mListeners.add(listener)
    }

    fun removeListener(listener: Listener) {
        mListeners.remove(listener)
    }
    //endregion DroneDiscoveryListener

    /**
     * Connect to the drone
     * @return true if operation was successful.
     * Returning true doesn't mean that device is connected.
     * You can be informed of the actual connection through [Listener.onDroneConnectionChanged]
     */
    fun connect(): Boolean {
        var success = false
        if (mDeviceController != null && ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_STOPPED == connectionState) {
            val error = mDeviceController!!.start()
            if (error == ARCONTROLLER_ERROR_ENUM.ARCONTROLLER_OK) {
                success = true
            }
        }
        return success
    }

    /**
     * Disconnect from the drone
     * @return true if operation was successful.
     * Returning true doesn't mean that device is disconnected.
     * You can be informed of the actual disconnection through [Listener.onDroneConnectionChanged]
     */
    fun disconnect(): Boolean {
        var success = false
        if (mDeviceController != null && ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING == connectionState) {
            val error = mDeviceController!!.stop()
            if (error == ARCONTROLLER_ERROR_ENUM.ARCONTROLLER_OK) {
                success = true
            }
        }
        return success
    }

    override fun takeOff() {
        if (mDeviceController != null && connectionState == ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING) {
            mDeviceController!!.featureMiniDrone.sendPilotingTakeOff()
        }
    }

    override fun land() {
        if (mDeviceController != null && connectionState == ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING) {
            mDeviceController!!.featureMiniDrone.sendPilotingLanding()
        }
    }

    fun emergency() {
        if (mDeviceController != null && connectionState == ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING) {
            mDeviceController!!.featureMiniDrone.sendPilotingEmergency()
        }
    }

    fun takePicture() {
        if (mDeviceController != null && connectionState == ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING) {
            // RollingSpider (not evo) are still using old deprecated command
            if (ARDISCOVERY_PRODUCT_ENUM.ARDISCOVERY_PRODUCT_MINIDRONE == mProductType) {
                mDeviceController!!.featureMiniDrone.sendMediaRecordPicture(0.toByte())
            } else {
                mDeviceController!!.featureMiniDrone.sendMediaRecordPictureV2()
            }
        }
    }

    /**
     * Set the forward/backward angle of the drone
     * Note that [MiniDrone.setFlag] should be set to 1 in order to take in account the pitch value
     * @param pitch value in percentage from -100 to 100
     */
    override fun setPitch(pitch: Byte) {
        if (mDeviceController != null && connectionState == ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING) {
            mDeviceController!!.featureMiniDrone.setPilotingPCMDPitch(pitch)
        }
    }

    /**
     * Set the side angle of the drone
     * Note that [MiniDrone.setFlag] should be set to 1 in order to take in account the roll value
     * @param roll value in percentage from -100 to 100
     */
    override fun setRoll(roll: Byte) {
        if (mDeviceController != null && connectionState == ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING) {
            mDeviceController!!.featureMiniDrone.setPilotingPCMDRoll(roll)
        }
    }

    override fun setYaw(yaw: Byte) {
        if (mDeviceController != null && connectionState == ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING) {
            mDeviceController!!.featureMiniDrone.setPilotingPCMDYaw(yaw)
        }
    }

    override fun setGaz(gaz: Byte) {
        if (mDeviceController != null && connectionState == ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING) {
            mDeviceController!!.featureMiniDrone.setPilotingPCMDGaz(gaz)
        }
    }

    /**
     * Take in account or not the pitch and roll values
     * @param flag 1 if the pitch and roll values should be used, 0 otherwise
     */
    override fun setFlag(flag: Byte) {
        if (mDeviceController != null && connectionState == ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING) {
            mDeviceController!!.featureMiniDrone.setPilotingPCMDFlag(flag)
        }
    }

    /**
     * Download the last flight medias
     * Uses the run id to download all medias related to the last flight
     * If no run id is available, download all medias of the day
     */
    fun getLastFlightMedias() {
        try {
            if (mFtpListManager == null) {
                mFtpListManager = ARUtilsManager()
                mFtpListManager!!.initFtp(mContext, mDeviceService, ARUTILS_DESTINATION_ENUM.ARUTILS_DESTINATION_DRONE, ARUTILS_FTP_TYPE_ENUM.ARUTILS_FTP_TYPE_GENERIC)
            }
            if (mFtpQueueManager == null) {
                mFtpQueueManager = ARUtilsManager()
                mFtpQueueManager!!.initFtp(mContext, mDeviceService, ARUTILS_DESTINATION_ENUM.ARUTILS_DESTINATION_DRONE, ARUTILS_FTP_TYPE_ENUM.ARUTILS_FTP_TYPE_GENERIC)
            }
            if (mSDCardModule == null) {
                mSDCardModule = SDCardModule(mFtpListManager!!, mFtpQueueManager!!)
                mSDCardModule!!.addListener(mSDCardModuleListener)
            }
        } catch (e: ARUtilsException) {
            Log.e(TAG, "Exception", e)
        }

        val runId = mCurrentRunId
        if (runId != null && !runId.isEmpty()) {
            mSDCardModule!!.getFlightMedias(runId)
        } else {
            Log.e(TAG, "RunID not available, fallback to the day's medias")
            mSDCardModule!!.getTodaysFlightMedias()
        }
    }

    fun cancelGetLastFlightMedias() {
        if (mSDCardModule != null) {
            mSDCardModule!!.cancelGetFlightMedias()
        }
    }

    private fun createDeviceController(discoveryDevice: ARDiscoveryDevice): ARDeviceController? {
        var deviceController: ARDeviceController? = null
        try {
            deviceController = ARDeviceController(discoveryDevice)

            deviceController.addListener(mDeviceControllerListener)
            deviceController.addStreamListener(mStreamListener)
        } catch (e: ARControllerException) {
            Log.e(TAG, "Exception", e)
        }

        return deviceController
    }

    //region notify listener block
    private fun notifyConnectionChanged(state: ARCONTROLLER_DEVICE_STATE_ENUM?) {
        val listenersCpy = ArrayList(mListeners)
        for (listener in listenersCpy) {
            listener.onDroneConnectionChanged(state)
        }
    }

    private fun notifyBatteryChanged(battery: Int) {
        val listenersCpy = ArrayList(mListeners)
        for (listener in listenersCpy) {
            listener.onBatteryChargeChanged(battery)
        }
    }

    private fun notifyPilotingStateChanged(state: ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM) {
        val listenersCpy = ArrayList(mListeners)
        for (listener in listenersCpy) {
            listener.onPilotingStateChanged(state)
        }
    }

    private fun notifyPictureTaken(error: ARCOMMANDS_MINIDRONE_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR_ENUM) {
        val listenersCpy = ArrayList(mListeners)
        for (listener in listenersCpy) {
            listener.onPictureTaken(error)
        }
    }

    private fun notifyConfigureDecoder(codec: ARControllerCodec) {
        val listenersCpy = ArrayList(mListeners)
        for (listener in listenersCpy) {
            listener.configureDecoder(codec)
        }
    }

    private fun notifyFrameReceived(frame: ARFrame) {
        val listenersCpy = ArrayList(mListeners)
        for (listener in listenersCpy) {
            listener.onFrameReceived(frame)
        }
    }

    private fun notifyMatchingMediasFound(nbMedias: Int) {
        val listenersCpy = ArrayList(mListeners)
        for (listener in listenersCpy) {
            listener.onMatchingMediasFound(nbMedias)
        }
    }

    private fun notifyDownloadProgressed(mediaName: String, progress: Int) {
        val listenersCpy = ArrayList(mListeners)
        for (listener in listenersCpy) {
            listener.onDownloadProgressed(mediaName, progress)
        }
    }

    private fun notifyDownloadComplete(mediaName: String) {
        val listenersCpy = ArrayList(mListeners)
        for (listener in listenersCpy) {
            listener.onDownloadComplete(mediaName)
        }
    }

    companion object {
        private val TAG = "MiniDrone"
    }
}
