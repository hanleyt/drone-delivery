package com.toasttab.consumer.service

import java.util.Date


interface OrderResponse {

    val successful: Boolean
    val deliveryTime: Date?
}
