package com.toasttab.consumer.service

import com.toasttab.food.service.AvailableFood
import com.toasttab.food.service.FoodService
import com.toasttab.food.service.DelayedFoodRepository
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.StatusPages
import io.ktor.html.respondHtml
import io.ktor.http.HttpStatusCode
import io.ktor.locations.Location
import io.ktor.locations.Locations
import io.ktor.locations.locations
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
import java.time.Duration
import java.util.NoSuchElementException


val Number.ms get() = Duration.ofMillis(this.toLong())


fun main(args: Array<String>) {
    val server = embeddedServer(Netty, port = 8080) {
//        DummyFoodRepository.getFoodNames()
//        FoodRepository
//        AvailableFood()
        DelayedFoodRepository
        val foodService = DelayedFoodRepository(DummyFoodRepository, delay = 2000.ms)
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

@Location("/")
object RootLocation

@Location("/food/{name}")
data class Food(val name: String)

fun Routing.homeRoute(foodService: FoodRepository) {
    get<RootLocation> {
        val movies = foodService.getAvailableFood()

        call.respondHtml {
            body {
                ul {
                    for (movie in movies) {
                        li {
                            a(href = call.locations.href(Food(name = movie.name))) {
                                +movie
                            }
                        }
                    }
                }
            }
        }
    }
}

fun Routing.movieRoute(foodService: FoodRepository) {
    get<Food> { location ->
        val summary = foodService.getFoodDetails(location.name)
                ?: throw NoSuchElementException("Can't find movie with name ${location.name}")
        call.respondHtml {
            body {
                h1 { +location.name }
                p {
                    +summary
                }
            }
        }
    }
}
