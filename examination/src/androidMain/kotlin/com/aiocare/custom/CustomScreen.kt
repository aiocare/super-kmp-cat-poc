package com.aiocare.custom

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.aiocare.poc.superCat.custom.CustomViewModel
import com.aiocare.poc.superCat.custom.Dir
import com.aiocare.poc.superCat.custom.WaveFormFile
import com.google.gson.Gson

@Composable
fun CustomScreen(
    viewModel: CustomViewModel,
    navController: NavController
) {
    DirView(modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState()),
        dir = getDataFromJson(), path = ""){
        Log.d("XDDD", "${it}")
    }
}

private fun getDataFromJson(): Dir {
    val json = "{\"name\":\"waveforms\",\"files\":[],\"dirs\":[{\"name\":\"aaa\",\"files\":[{\"name\":\"Bat2-5Pk.fvw\"}],\"dirs\":[]},{\"name\":\"C1-C13 (ISO26782)\",\"files\":[{\"name\":\"C1.fvw\"},{\"name\":\"C10.fvw\"},{\"name\":\"C11.fvw\"},{\"name\":\"C12.fvw\"},{\"name\":\"C13.fvw\"},{\"name\":\"C2.fvw\"},{\"name\":\"C3.fvw\"},{\"name\":\"C4.fvw\"},{\"name\":\"C5.fvw\"},{\"name\":\"C6.fvw\"},{\"name\":\"C7.fvw\"},{\"name\":\"C8.fvw\"},{\"name\":\"C9.fvw\"}],\"dirs\":[]},{\"name\":\"Custom\",\"files\":[{\"name\":\".DS_Store\"},{\"name\":\"Bat1-7Pk-Adjusted.fvw\"},{\"name\":\"Bat12-0Pk-Adjusted.fvw\"}],\"dirs\":[]},{\"name\":\"PEF (ISO23747)\",\"files\":[{\"name\":\".DS_Store\"}],\"dirs\":[{\"name\":\"Profile A\",\"files\":[{\"name\":\"Aat1-7Pk.fvw\"},{\"name\":\"Aat10-0Pk.fvw\"},{\"name\":\"Aat12-0Pk.fvw\"},{\"name\":\"Aat14-5Pk.fvw\"},{\"name\":\"Aat17-0Pk.fvw\"},{\"name\":\"Aat2-5Pk.fvw\"},{\"name\":\"Aat3-0Pk.fvw\"},{\"name\":\"Aat3-3Pk.fvw\"},{\"name\":\"Aat5-0Pk.fvw\"},{\"name\":\"Aat6-0Pk.fvw\"},{\"name\":\"Aat7-5Pk.fvw\"},{\"name\":\"Aat9-0Pk.fvw\"}],\"dirs\":[]},{\"name\":\"Profile B\",\"files\":[{\"name\":\".DS_Store\"},{\"name\":\"Bat1-7Pk.fvw\"},{\"name\":\"Bat10-0Pk.fvw\"},{\"name\":\"Bat12-0Pk.fvw\"},{\"name\":\"Bat14-5Pk.fvw\"},{\"name\":\"Bat17-0Pk.fvw\"},{\"name\":\"Bat2-5Pk.fvw\"},{\"name\":\"Bat3-0Pk.fvw\"},{\"name\":\"Bat3-3Pk.fvw\"},{\"name\":\"Bat5-0Pk.fvw\"},{\"name\":\"Bat6-0Pk.fvw\"},{\"name\":\"Bat7-5Pk.fvw\"},{\"name\":\"Bat9-0Pk.fvw\"}],\"dirs\":[]}]}]}"
    return Gson().fromJson(json, Dir::class.java)
}