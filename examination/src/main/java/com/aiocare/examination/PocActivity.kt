package com.aiocare.examination

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.SideEffect
import androidx.navigation.compose.rememberNavController
import com.aiocare.sdk.AioCareSdk
import com.aiocare.sdk.permission.Permission

class PocActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SideEffect {
                AioCareSdk.init(Permission(this))
            }
            val navController = rememberNavController()
            com.aiocare.poc.NavGraph(navController = navController)
        }
    }

}