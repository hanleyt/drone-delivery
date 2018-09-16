package com.toasttab.food.service

interface FoodService {
    suspend fun getFoodNames(): List<String>
    suspend fun getFoodDetails(name:String): AvailableFood?
}

object DummyFoodService : FoodService {
}
