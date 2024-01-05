package com.aiocare.util

data class ButtonVM(
    val visible: Boolean,
    val text: String = "",
    val onClickAction: ClickAction = {}
)