package com.toasttab.service.delivery

import com.toasttab.drone.DroneController

interface DeliveryService {

    fun requestDelivery(deliveryRequest: DeliveryRequest): DeliveryResponse

}

class DeliveryServiceImpl(val droneController: DroneController) : DeliveryService {

    override fun requestDelivery(deliveryRequest: DeliveryRequest): DeliveryResponse {
        TODO("not implemented")
    }
}
