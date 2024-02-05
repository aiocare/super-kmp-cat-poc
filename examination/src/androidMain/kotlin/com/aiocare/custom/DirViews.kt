package com.aiocare.custom

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.aiocare.supercat.api.Dir
import com.aiocare.supercat.api.WaveFormFile

@Composable
fun FileView(waveFormFile: WaveFormFile, path: String= "", onClicked: (String) -> Unit) {
    Btn(modifier = Modifier.padding(start = 8.dp), Color.Blue, waveFormFile.name){
        onClicked("${path}/${waveFormFile.name}")
    }
}

@Composable
fun DirView(modifier: Modifier = Modifier, dir: Dir, path: String, onClicked: (String) -> Unit) {
    var shown by remember { mutableStateOf(false) }
    Column(modifier = modifier.padding(start = 16.dp)) {
        Btn(modifier = Modifier.clickable { shown = !shown }, Color.DarkGray, dir.name){ shown = !shown }
        if(shown)
            dir.files.forEach { FileView(waveFormFile = it, "${path}/${dir.name}", onClicked) }
        if(shown)
            dir.dirs.forEach {
                DirView(dir = it, path = "${path}/${dir.name}", onClicked = onClicked)
            }
    }
}

@Composable
fun Btn(modifier: Modifier = Modifier, color: Color, text: String, onClick: () -> Unit) {
    Button(
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(color),
        onClick = { onClick.invoke() },
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(text = text)
    }
}