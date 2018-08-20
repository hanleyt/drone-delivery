package com.toasttab.food.service

import java.util.UUID


data class AvailableFood(

        val guid: UUID,

        val name: String,

        val location: Pair<Int, Int>
)
