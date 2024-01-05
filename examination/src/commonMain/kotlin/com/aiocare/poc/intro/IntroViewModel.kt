package com.aiocare.poc.intro

import com.aiocare.Screens
import com.aiocare.mvvm.Config
import com.aiocare.mvvm.StatefulViewModel
import com.aiocare.sdk.AioCareSdk
import com.aiocare.sdk.permission.Permission
import com.aiocare.util.ButtonVM

data class IntroUiState(
    val navigateNextButtonVM: ButtonVM = ButtonVM(visible = false),
    val checkAgainButtonVM: ButtonVM = ButtonVM(visible = false),
    val warningsPermission: String = "",
    val warningsBluetooth: String = "",
)

class IntroViewModel constructor(
    config: Config
) : StatefulViewModel<IntroUiState>(IntroUiState(), config) {

    private lateinit var cachedPermission: Permission
    private lateinit var navigateCallback: (String) -> Unit
    fun init(permission: Permission, navigate: (String) -> Unit) {
        cachedPermission = permission
        AioCareSdk.init(permission = cachedPermission)
        navigateCallback = navigate
        checkPermission(cachedPermission)
    }

    private fun checkPermission(permission: Permission) {
        updateUiState {
            it.copy(
                warningsBluetooth = when (permission.isBluetoothEnabled()) {
                    true -> ""
                    false -> "bluetooth is disabled"
                },
                warningsPermission = when (permission.arePermissionObtained()) {
                    true -> ""
                    false -> "missing permissions = ${
                        permission.getMissingPermissions().joinToString(separator = ",")
                    }"
                },
                navigateNextButtonVM = ButtonVM(
                    visible = permission.isBluetoothEnabled() && permission.arePermissionObtained(),
                    text = "search and connect device",
                    onClickAction = { navigateCallback.invoke(Screens.SearchDevice.route) }
                ),
                checkAgainButtonVM = ButtonVM(
                    visible = !(permission.isBluetoothEnabled() && permission.arePermissionObtained()),
                    text = "try again",
                    onClickAction = { checkPermission(cachedPermission) }
                )
            )
        }
        if (permission.isBluetoothEnabled() && permission.arePermissionObtained()) {
            navigateCallback.invoke(Screens.SuperCat.route)
        }
    }
}