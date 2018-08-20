package com.toasttab.test

import android.app.Application
import timber.log.Timber

class App : Application() {

    companion object {
        lateinit var sInstance: App
    }

    fun get(): App {
        return sInstance
    }

    override fun onCreate() {
        super.onCreate()
        sInstance = this

        // Init Timber logging.
        if (BuildConfig.DEBUG) {
            Timber.plant(object : Timber.DebugTree() {
                // Shows line number for all logs, not just Exceptions.
                override fun createStackElementTag(element: StackTraceElement): String? {
                    return super.createStackElementTag(element) + ':'.toString() + element.lineNumber
                }
            })
        }
        Timber.d("App initialised: %s", javaClass.canonicalName)
    }
}