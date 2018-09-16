package com.toasttab.food.service

import kotlinx.coroutines.experimental.delay

interface FoodService {
    suspend fun getFoodNames(): List<String>
    suspend fun getFoodDetails(name:String): AvailableFood?
}

object DummyFoodService : FoodService {
    override suspend fun getFoodNames(): List<String> {
        delay(5000)
        return listOf(
                "Food1",
                "Food2",
                "Food3",
                "Food4"
        )
    }

    override suspend fun getFoodDetails(name: String): AvailableFood? {
        delay(5000)
        return when (name) {
            "Food1" -> AvailableFood("Food1", "Summary1")
            "Food2" -> AvailableFood("Food2", "Summary2")
            "Food3" -> AvailableFood("Food3", "Summary3")
            "Food4" -> AvailableFood("Food4", "Summary4")
            else -> null
        }
    }
}
