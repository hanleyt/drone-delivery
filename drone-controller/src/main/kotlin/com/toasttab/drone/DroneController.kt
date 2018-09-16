package com.toasttab.drone

import kotlinx.coroutines.experimental.Deferred


interface DroneController {

    val currentLocation: Location

    fun sendToLocation(location: Location): Deferred<Location>

    fun onArrival(arrivalCallback: () -> Unit)

    fun onError(errorHandler: (Exception) -> Unit)
}
