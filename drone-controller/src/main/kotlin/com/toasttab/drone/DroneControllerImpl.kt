package com.toasttab.drone

class DroneControllerImpl(val drone: Drone) : DroneController {

    override fun getCurrentLocation(): Location {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun sendToLocation(location: Location) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onArrival(arrivalCallback: (Location) -> Unit) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onError() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
