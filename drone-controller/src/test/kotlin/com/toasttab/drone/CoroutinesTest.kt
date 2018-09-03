package com.toasttab.drone

import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Test
import kotlin.concurrent.thread


class CoroutinesTest {
    @Test
    fun `how many threads can we start`() {
        val jobs = List(100_000) {
            thread {
                Thread.sleep(1000L)
                print(".")
            }
        }
        jobs.forEach { it.join() }

    }

    @Test
    fun `how many co-routines can we start`() {
        runBlocking {
            val jobs = List(100_000) {
                launch {
                    delay(1000L)
                    print(".")
                }
            }
            jobs.forEach { it.join() }
        }
    }
}
