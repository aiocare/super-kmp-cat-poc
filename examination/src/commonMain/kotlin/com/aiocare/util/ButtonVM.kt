package com.aiocare.util

data class ButtonVM(
    val visible: Boolean,
    val text: String = "",
    val enabled: Boolean = true,
    val onClickAction: ClickAction = {}
)