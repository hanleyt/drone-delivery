package com.toasttab.food.service

interface FoodService {
    suspend fun getAvailableFood(): List<AvailableFood>
    suspend fun getFoodDetails(name:String): AvailableFood

    fun advertiseFood(availableFood: AvailableFood)
    fun withdrawFood(availableFood: AvailableFood)
}

//internal class FoodServiceImpl : FoodService {
//    override suspend fun getFoodDetails(): AvailableFood {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    override suspend fun getAvailableFood(): List<AvailableFood> {
//        TODO("return currently available food")
//    }
//
//    override fun withdrawFood(availableFood: AvailableFood) {
//        TODO("not implemented")
//    }
//
//    override fun advertiseFood(availableFood: AvailableFood) {
//        TODO("not implemented")
//    }
//
//}
