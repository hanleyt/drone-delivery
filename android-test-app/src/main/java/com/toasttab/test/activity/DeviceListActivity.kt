package com.toasttab.test.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import com.parrot.arsdk.ARSDK
import com.parrot.arsdk.ardiscovery.ARDISCOVERY_PRODUCT_ENUM
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService
import com.parrot.arsdk.ardiscovery.ARDiscoveryService
import com.parrot.sdksample.discovery.DroneDiscoverer
import com.parrot.sdksample.discovery.DroneDiscoveryListener
import com.toasttab.test.R
import java.util.ArrayList
import java.util.HashSet

class DeviceListActivity : AppCompatActivity() {

    private lateinit var mDroneDiscoverer: DroneDiscoverer

    private val mDronesList = ArrayList<ARDiscoveryDeviceService>()

    private val mDiscovererDroneDiscoveryListener = object : DroneDiscoveryListener {

        override fun onDronesListUpdated(dronesList: List<ARDiscoveryDeviceService>) {
            mDronesList.clear()
            mDronesList.addAll(dronesList)

            mAdapter.notifyDataSetChanged()
        }
    }

    private val mAdapter = object : BaseAdapter() {
        override fun getCount(): Int {
            return mDronesList.size
        }

        override fun getItem(position: Int): Any {
            return mDronesList[position]
        }

        override fun getItemId(position: Int): Long {
            return 0
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var rowView: View? = convertView
            // reuse views
            if (rowView == null) {
                val inflater = layoutInflater
                rowView = inflater.inflate(android.R.layout.simple_list_item_1, null)
                // configure view holder
                val viewHolder = ViewHolder()
                viewHolder.text = rowView!!.findViewById(android.R.id.text1) as TextView
                rowView.tag = viewHolder
            }

            // fill data
            val holder = rowView.tag as ViewHolder
            val service = getItem(position) as ARDiscoveryDeviceService
            holder.text!!.text = service.name + " on " + service.networkType

            return rowView
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO Conor: For automatically launching KMiniDroneActivity for test purposes, remove when ready.
//        Handler().postDelayed({ startActivity(Intent(this@DeviceListActivity, MiniDroneActivity::class.java)) }, 500)

        setContentView(R.layout.activity_device_list)
        val listView = findViewById(R.id.list) as ListView

        // Assign adapter to ListView
        listView.adapter = mAdapter
        listView.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            // launch the activity related to the type of discovery device service
            var intent: Intent? = null

            val service = mAdapter.getItem(position) as ARDiscoveryDeviceService
            val product = ARDiscoveryService.getProductFromProductID(service.productID)
            when (product) {
                ARDISCOVERY_PRODUCT_ENUM.ARDISCOVERY_PRODUCT_MINIDRONE, ARDISCOVERY_PRODUCT_ENUM.ARDISCOVERY_PRODUCT_MINIDRONE_EVO_BRICK, ARDISCOVERY_PRODUCT_ENUM.ARDISCOVERY_PRODUCT_MINIDRONE_EVO_LIGHT, ARDISCOVERY_PRODUCT_ENUM.ARDISCOVERY_PRODUCT_MINIDRONE_DELOS3 -> intent = Intent(this@DeviceListActivity, MiniDroneActivity::class.java)
                else -> Log.e(TAG, "The type $product is not supported by this sample")
            }

            if (intent != null) {
                intent.putExtra(EXTRA_DEVICE_SERVICE, service)
                startActivity(intent)
            }
        }

        mDroneDiscoverer = DroneDiscoverer(this)

        val permissionsToRequest = HashSet<String>()
        for (permission in PERMISSIONS_NEEDED) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                    Toast.makeText(this, "Please allow permission $permission", Toast.LENGTH_LONG).show()
                    finish()
                    return
                } else {
                    permissionsToRequest.add(permission)
                }
            }
        }
        if (permissionsToRequest.size > 0) {
            ActivityCompat.requestPermissions(this,
                    permissionsToRequest.toTypedArray(),
                    REQUEST_CODE_PERMISSIONS_REQUEST)
        }
    }

    override fun onResume() {
        super.onResume()

        // setup the drone discoverer and register as listener
        mDroneDiscoverer.setup()
        mDroneDiscoverer.addListener(mDiscovererDroneDiscoveryListener)

        // start discovering
        mDroneDiscoverer.startDiscovering()
    }

    override fun onPause() {
        super.onPause()

        // clean the drone discoverer object
        mDroneDiscoverer.stopDiscovering()
        mDroneDiscoverer.cleanup()
        mDroneDiscoverer.removeListener(mDiscovererDroneDiscoveryListener)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        var denied = false
        if (permissions.size == 0) {
            // canceled, finish
            denied = true
        } else {
            for (i in permissions.indices) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    denied = true
                }
            }
        }

        if (denied) {
            Toast.makeText(this, "At least one permission is missing.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    internal class ViewHolder {
        var text: TextView? = null
    }

    companion object {
        val EXTRA_DEVICE_SERVICE = "EXTRA_DEVICE_SERVICE"

        private val TAG = "DeviceListActivity"

        /**
         * List of runtime permission we need.
         */
        private val PERMISSIONS_NEEDED = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_COARSE_LOCATION)

        /**
         * Code for permission request result handling.
         */
        private val REQUEST_CODE_PERMISSIONS_REQUEST = 1

        // this block loads the native libraries
        // it is mandatory
        init {
            ARSDK.loadSDKLibs()
        }
    }

}
