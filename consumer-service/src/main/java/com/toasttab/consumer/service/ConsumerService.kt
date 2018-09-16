package com.toasttab.consumer.service

import com.toasttab.food.service.AvailableFood
import com.toasttab.food.service.DummyFoodService
import com.toasttab.food.service.FoodService
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.StatusPages
import io.ktor.html.respondHtml
import io.ktor.http.HttpStatusCode
import io.ktor.locations.Location
import io.ktor.locations.Locations
import io.ktor.locations.get
import io.ktor.locations.locations
import io.ktor.response.respondText
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.h1
import kotlinx.html.li
import kotlinx.html.p
import kotlinx.html.ul
import java.util.NoSuchElementException

fun main(args: Array<String>) {
    val server = embeddedServer(Netty, port = 8080) {

    }
    server.start(wait = true)
}
