package com.aiocare.supercat

interface IPhoneInfo {

    fun getPhoneModel(): String
}

expect class PhoneInfo() : IPhoneInfo
