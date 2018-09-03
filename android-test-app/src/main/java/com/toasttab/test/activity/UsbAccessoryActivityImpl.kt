package com.toasttab.test.activity

import com.parrot.arsdk.ardiscovery.UsbAccessoryActivity

class UsbAccessoryActivityImpl : UsbAccessoryActivity() {
    override fun getBaseActivity(): Class<*> {
        return DeviceListActivity::class.java
    }
}
