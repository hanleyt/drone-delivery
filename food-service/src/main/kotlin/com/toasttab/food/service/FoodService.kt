package com.toasttab.food.service

interface FoodService {
    fun getAvailableFood(): List<AvailableFood>
    fun advertiseFood(availableFood: AvailableFood)
    fun withdrawFood(availableFood: AvailableFood)
}

internal class FoodServiceImpl : FoodService {

    override fun getAvailableFood(): List<AvailableFood> {
        TODO("return currently available food")
    }

    override fun withdrawFood(availableFood: AvailableFood) {
        TODO("not implemented")
    }

    override fun advertiseFood(availableFood: AvailableFood) {
        TODO("not implemented")
    }

}
