package com.toasttab.drone

interface Drone {

    fun takeOff()

    fun land()

    /**
     * Set the forward/backward angle of the drone
     * Note that {@link MiniDrone#setFlag(byte)} should be set to 1 in order to take in account the pitch value
     * @param pitch value in percentage from -100 to 100
     */
    fun setPitch(pitch: Byte)

    /**
     * Set the side angle of the drone
     * Note that {@link MiniDrone#setFlag(byte)} should be set to 1 in order to take in account the roll value
     * @param roll value in percentage from -100 to 100
     */
    fun setRoll(roll:Byte)

    fun setYaw(yaw: Byte)


    fun setGaz(gaz: Byte)

    /**
     * Take in account or not the pitch and roll values
     * @param flag 1 if the pitch and roll values should be used, 0 otherwise
     */
    fun setFlag(flag: Byte)

    enum class FlyingState{
        STOPPED,
        FLYING,
    }

}
