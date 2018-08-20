package com.toasttab.service.delivery

import com.toasttab.drone.Drone
import com.toasttab.drone.DroneController
import com.toasttab.drone.DroneControllerImpl
import com.toasttab.drone.TestDrone
import com.toasttab.food.service.AvailableFood
import org.junit.Test
import java.util.UUID

internal class DeliveryServiceImplTest {

    @Test
    fun `simple delivery service request`() {


        val droneController = DroneControllerImpl(TestDrone())

        val deliveryService = DeliveryServiceImpl(droneController)

        deliveryService.requestDelivery(DeliveryRequest(Pair(0, 0), AvailableFood(UUID.randomUUID(), "Cheese", Pair(1, 0))))
    }


}
