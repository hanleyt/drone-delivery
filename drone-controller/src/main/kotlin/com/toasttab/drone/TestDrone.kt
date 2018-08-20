package com.toasttab.drone


class TestDrone : Drone {

    var flyingState = Drone.FlyingState.STOPPED
    var currentLocation = Pair(0, 0)

    override fun takeOff() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun land() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setPitch(pitch: Byte) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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

    override fun setFlag(flag: Byte) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}
