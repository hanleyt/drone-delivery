package com.toasttab.drone

import kotlinx.coroutines.experimental.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch

class DroneControllerImplTest {

    private lateinit var droneController: DroneController

    @Before
    fun setUp() {
        droneController = DroneControllerImpl(TestDrone())
    }

    @Test
    fun `drone starts out at location (0, 0)`() {
        assertThat(droneController.currentLocation).isEqualTo(Location(0.0,0.0))
    }

    @Test
    fun `drone can be sent to a location`() {
        val countDownLatch = CountDownLatch(1)
        droneController.onArrival { countDownLatch.countDown() }


        runBlocking {
            val newLocation = droneController.sendToLocation(Location(5.0, 1.0))
            newLocation.await()
        }

        countDownLatch.await()

        assertThat(droneController.currentLocation).isEqualTo(Location(5.0,1.0))
    }
}
