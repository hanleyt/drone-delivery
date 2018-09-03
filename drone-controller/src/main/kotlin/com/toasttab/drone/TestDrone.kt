package com.toasttab.drone

import java.util.Date
import kotlin.concurrent.timer


class TestDrone : Drone {

    var flyingState = Drone.FlyingState.STOPPED

    override fun takeOff() {
        flyingState = Drone.FlyingState.FLYING
    }

    override fun land() {
        flyingState = Drone.FlyingState.STOPPED
    }

    private var xLocation : Double = 0.0

    private val pitchFactor: Double = 1.0

    override fun setPitch(pitch: Byte) {
        if(pitch == 0.toByte()){
            val timer = timer("pitch tracker", true, Date(), 100) { xLocation += pitch * pitchFactor }
            timer.cancel()
        }
    }

    override fun setRoll(roll: Byte) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setYaw(yaw: Byte) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setGaz(gaz: Byte) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private var flag: Byte = 0

    override fun setFlag(flag: Byte) {
     this.flag = flag
    }

}
