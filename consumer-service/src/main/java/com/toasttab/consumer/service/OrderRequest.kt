package com.toasttab.consumer.service

import java.util.UUID


interface OrderRequest {

    val food: UUID
    val location: Pair<Int, Int>
}
