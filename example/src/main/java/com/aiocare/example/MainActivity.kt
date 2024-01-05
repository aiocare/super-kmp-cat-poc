package com.aiocare.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.aiocare.examination.PocActivity

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, PocActivity::class.java))
    }
}