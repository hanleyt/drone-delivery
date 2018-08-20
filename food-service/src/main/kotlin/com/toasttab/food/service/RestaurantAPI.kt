package com.toasttab.food.service


interface RestaurantAPI {

    fun advertiseFood(availableFood: AvailableFood)

    fun withdrawFood(availableFood: AvailableFood)
}
