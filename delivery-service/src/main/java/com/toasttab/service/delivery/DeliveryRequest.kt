package com.toasttab.service.delivery

import com.toasttab.food.service.AvailableFood

data class DeliveryRequest(
        val consumerLocation: Pair<Int, Int>,
        val requestedFood: AvailableFood
)
