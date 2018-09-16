package com.toasttab.food.service

data class AvailableFood(

        val name: String,

        val description: String,

        val location: Pair<Int, Int>? = null
)
