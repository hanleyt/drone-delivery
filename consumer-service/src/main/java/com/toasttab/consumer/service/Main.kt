package com.toasttab.consumer.service

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
import io.ktor.routing.Routing
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
        val foodService = DummyFoodService
        install(Locations)
        install(StatusPages) {
            this.exception<NoSuchElementException> {
                call.respondHtml(HttpStatusCode.NotFound) {
                    body {
                        h1 { +"404 Not Found" }
                        p { +"${it.message}" }
                    }
                }
            }
        }
        routing {
            homeRoute(foodService)
            movieRoute(foodService)
        }
    }
    server.start(wait = true)
}

@Location("/") object RootLocation

@Location("/food/{name}") data class Food(val name: String)

fun Routing.homeRoute(foodService: FoodService) {
    get<RootLocation> {
        val foodNames = foodService.getFoodNames()

        call.respondHtml {
            body {
                ul {
                    for (food in foodNames) {
                        li {
                            a(href = call.locations.href(Food(name = food))) {
                                +food
                            }
                        }
                    }
                }
            }
        }
    }
}

fun Routing.movieRoute(foodService: FoodService) {
    get<Food> { location ->
        val details = foodService.getFoodDetails(location.name)
                ?: throw NoSuchElementException("Can't find movie with name ${location.name}")
        call.respondHtml {
            body {
                h1 { +location.name }
                p {
                    +"What?"
                    +details.description

                }
            }
        }
    }
}
