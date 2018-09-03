package com.toasttab.drone

import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch

class DroneControllerImpl(
        val drone: Drone,
        override var currentLocation: Location = Location(0.0, 0.0)
) : DroneController {

    private val pitchFactor = 500.0
    private val rollFactor = 500.0

    private var arrivalCallback: (() -> Unit)? = null
    private var errorHandler: ((Exception) -> Unit)? = null

    override fun sendToLocation(location: Location) {
        launch(onCompletion = {arrivalCallback?.invoke()}) {
            try {
                drone.takeOff()

                drone.setPitch(50)
                drone.setFlag(1)
                delay((location.x * pitchFactor).toInt())
                drone.setPitch(0)
                drone.setFlag(0)

                currentLocation = currentLocation.copy(x = location.x)

                drone.setRoll(50)
                drone.setFlag(1)
                delay((location.y * rollFactor).toInt())
                drone.setRoll(0)
                drone.setFlag(0)

                currentLocation = currentLocation.copy(y = location.y)

                drone.land()
            } catch (e: Exception) {
                errorHandler?.invoke(e)
            }
        }
    }

    override fun onArrival(arrivalCallback: () -> Unit) {
        this.arrivalCallback = arrivalCallback
    }

    override fun onError(errorHandler: (Exception) -> Unit) {
        this.errorHandler = errorHandler
    }

}
