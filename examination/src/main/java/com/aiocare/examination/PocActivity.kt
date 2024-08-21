package com.aiocare.examination

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.SideEffect
import androidx.navigation.compose.rememberNavController
import com.aiocare.bluetooth.PlatformPermission
import com.aiocare.bluetooth.di.KoinContext
import com.aiocare.sdk.AioCareSdk

class PocActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SideEffect {
                AioCareSdk.init(PlatformPermission(this), KoinContext.DeviceFactoryType.Real)
            }
            val navController = rememberNavController()
            com.aiocare.poc.NavGraph(navController = navController)
        }
    }

}