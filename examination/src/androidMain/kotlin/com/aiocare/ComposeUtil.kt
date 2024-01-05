package com.aiocare

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aiocare.util.ButtonVM

@Composable
fun SimpleButton(modifier: Modifier = Modifier, buttonVM: ButtonVM) {
    if (buttonVM.visible)
        Button(
            onClick = { buttonVM.onClickAction.invoke() },
            shape = RoundedCornerShape(8.dp),
            modifier = modifier
                .padding(16.dp)
        ) {
            Text(text = buttonVM.text)
        }
}