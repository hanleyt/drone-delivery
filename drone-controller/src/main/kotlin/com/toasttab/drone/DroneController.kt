package com.toasttab.drone


interface DroneController {

    fun getCurrentLocation(): Location

    fun sendToLocation(location: Location)

    fun onArrival(arrivalCallback: (Location) -> Unit)

    fun onError()
}


