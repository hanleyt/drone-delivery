package com.toasttab.consumer.service

import com.toasttab.food.service.AvailableFood


interface ConsumerAPI {

    fun getAvailableFood(): List<AvailableFood>

    fun orderFood(orderRequest: OrderRequest)

}

class ConsumerAPIImpl: ConsumerAPI {

    override fun getAvailableFood(): List<AvailableFood> {
        DelayedFoodRepository
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun orderFood(orderRequest: OrderRequest) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}
