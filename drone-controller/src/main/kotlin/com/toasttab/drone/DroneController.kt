package com.toasttab.drone


interface DroneController {

    val currentLocation: Location

    fun sendToLocation(location: Location)

    fun onArrival(arrivalCallback: () -> Unit)

    fun onError(errorHandler: (Exception) -> Unit)
}
