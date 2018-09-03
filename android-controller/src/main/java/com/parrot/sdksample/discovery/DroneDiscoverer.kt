package com.parrot.sdksample.discovery

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.IBinder
import android.support.v4.content.LocalBroadcastManager
import android.util.Log

import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService
import com.parrot.arsdk.ardiscovery.ARDiscoveryService
import com.parrot.arsdk.ardiscovery.receivers.ARDiscoveryServicesDevicesListUpdatedReceiver
import com.parrot.arsdk.ardiscovery.receivers.ARDiscoveryServicesDevicesListUpdatedReceiverDelegate

import java.util.ArrayList

interface DroneDiscoveryListener {
    /**
     * Called when the list of seen drones is updated
     * Called in the main thread
     * @param dronesList list of ARDiscoveryDeviceService which represents all available drones
     * Content of this list respect the drone types given in startDiscovery
     */
    fun onDronesListUpdated(dronesList: List<ARDiscoveryDeviceService>)
}

class DroneDiscoverer(private val mCtx: Context) {

    private val mListeners = mutableListOf<DroneDiscoveryListener>()

    private var mArdiscoveryService: ARDiscoveryService? = null
    private var mArdiscoveryServiceConnection: ServiceConnection? = null
    private val mArdiscoveryServicesDevicesListUpdatedReceiver: ARDiscoveryServicesDevicesListUpdatedReceiver

    private val mMatchingDrones = mutableListOf<ARDiscoveryDeviceService>()

    private var mStartDiscoveryAfterConnection: Boolean = false

    private val mDiscoveryListener = ARDiscoveryServicesDevicesListUpdatedReceiverDelegate {
        if (mArdiscoveryService != null) {
            // clear current list
            mMatchingDrones.clear()
            val deviceList = mArdiscoveryService!!.deviceServicesArray

            if (deviceList != null) {
                for (service in deviceList) {
                    mMatchingDrones.add(service)
                }
            }
            notifyServiceDiscovered(mMatchingDrones)
        }
    }



    init {
        mArdiscoveryServicesDevicesListUpdatedReceiver = ARDiscoveryServicesDevicesListUpdatedReceiver(mDiscoveryListener)
    }

    /**
     * Add a droneDiscoveryListener
     * All callbacks of the interface DroneDiscoveryListener will be called within this function
     * Should be called in the main thread
     * @param droneDiscoveryListener an object that implements the [DroneDiscoveryListener] interface
     */
    fun addListener(droneDiscoveryListener: DroneDiscoveryListener) {
        mListeners.add(droneDiscoveryListener)

        notifyServiceDiscovered(mMatchingDrones)
    }

    /**
     * remove a droneDiscoveryListener from the droneDiscoveryListener list
     * @param droneDiscoveryListener an object that implements the [DroneDiscoveryListener] interface
     */
    fun removeListener(droneDiscoveryListener: DroneDiscoveryListener) {
        mListeners.remove(droneDiscoveryListener)
    }

    /**
     * Setup the drone discoverer
     * Should be called before starting discovering
     */
    fun setup() {
        // registerReceivers
        val localBroadcastMgr = LocalBroadcastManager.getInstance(mCtx)
        localBroadcastMgr.registerReceiver(mArdiscoveryServicesDevicesListUpdatedReceiver,
                IntentFilter(ARDiscoveryService.kARDiscoveryServiceNotificationServicesDevicesListUpdated))

        // create the service connection
        if (mArdiscoveryServiceConnection == null) {
            mArdiscoveryServiceConnection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName, service: IBinder) {
                    mArdiscoveryService = (service as ARDiscoveryService.LocalBinder).service

                    if (mStartDiscoveryAfterConnection) {
                        startDiscovering()
                        mStartDiscoveryAfterConnection = false
                    }
                }

                override fun onServiceDisconnected(name: ComponentName) {
                    mArdiscoveryService = null
                }
            }
        }

        if (mArdiscoveryService == null) {
            // if the discovery service doesn't exists, bind to it
            val i = Intent(mCtx, ARDiscoveryService::class.java)
            mCtx.bindService(i, mArdiscoveryServiceConnection!!, Context.BIND_AUTO_CREATE)
        }
    }

    /**
     * Cleanup the object
     * Should be called when the object is not used anymore
     */
    fun cleanup() {
        stopDiscovering()
        //close discovery service
        Log.i(TAG, "closeServices ...")

        if (mArdiscoveryService != null) {
            Thread(Runnable {
                mArdiscoveryService!!.stop()

                mCtx.unbindService(mArdiscoveryServiceConnection!!)
                mArdiscoveryService = null
            }).start()
        }

        // unregister receivers
        val localBroadcastMgr = LocalBroadcastManager.getInstance(mCtx)
        localBroadcastMgr.unregisterReceiver(mArdiscoveryServicesDevicesListUpdatedReceiver)
    }

    /**
     * Start discovering Parrot drones
     * For Wifi drones, the device should be on the drone's network
     * When drones will be discovered, you will be notified through [DroneDiscoveryListener.onDronesListUpdated]
     */
    fun startDiscovering() {
        if (mArdiscoveryService != null) {
            Log.i(TAG, "Start discovering")
            mDiscoveryListener.onServicesDevicesListUpdated()
            mArdiscoveryService!!.start()
            mStartDiscoveryAfterConnection = false
        } else {
            mStartDiscoveryAfterConnection = true
        }
    }

    /**
     * Stop discovering Parrot drones
     */
    fun stopDiscovering() {
        if (mArdiscoveryService != null) {
            Log.i(TAG, "Stop discovering")
            mArdiscoveryService!!.stop()
        }
        mStartDiscoveryAfterConnection = false
    }

    private fun notifyServiceDiscovered(dronesList: List<ARDiscoveryDeviceService>) {
        val listenersCpy = ArrayList(mListeners)
        for (listener in listenersCpy) {
            listener.onDronesListUpdated(dronesList)
        }
    }

    companion object {
        private const val TAG = "DroneDiscoverer"
    }
}
