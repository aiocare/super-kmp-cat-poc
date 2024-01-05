package com.aiocare.poc.intro

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.aiocare.SimpleButton
import com.aiocare.sdk.permission.Permission


@Composable
fun IntroScreen(
    viewModel: IntroViewModel,
    navController: NavController
) {
    var inited by remember { mutableStateOf(false) }

    if (!inited) {
        viewModel.init(Permission(LocalContext.current)) {
            navController.navigate(it)
        }
        inited = true
    }

    val state = viewModel.uiState
    Column {
        Text(state.warningsBluetooth)
        Text(state.warningsPermission)
        SimpleButton(buttonVM = state.navigateNextButtonVM)
        SimpleButton(buttonVM = state.checkAgainButtonVM)
    }

    if (state.warningsPermission.isNotEmpty()) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", LocalContext.current.packageName, null)
        intent.setData(uri)
        LocalContext.current.startActivity(intent)
    }
}
