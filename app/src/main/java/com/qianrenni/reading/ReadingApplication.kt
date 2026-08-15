package com.qianrenni.reading

import android.app.Application
import com.qianrenni.reading.di.AppContainer

/**
 * Application 入口：持有全局手动 DI 容器。
 */
class ReadingApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
