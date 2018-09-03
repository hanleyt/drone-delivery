package com.toasttab.drone

class DroneControllerImpl(val drone: Drone, override val currentLocation: Location) : DroneController {

    override fun sendToLocation(location: Location) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onArrival(arrivalCallback: () -> Unit) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onError(errorHandler: (Exception) -> Unit) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}
