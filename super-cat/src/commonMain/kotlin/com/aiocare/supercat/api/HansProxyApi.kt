package com.aiocare.supercat.api

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

enum class TimeoutTypes(val value: Long){
    NORMAL(25000),
    LONG(120000),
}

class HansProxyApi(private val hostAddress: String, timeoutTypes: TimeoutTypes) {

    private val httpClient = HttpClient {
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    println(message)
                }
            }
            level = LogLevel.ALL
        }
        install(HttpTimeout) {
            requestTimeoutMillis = timeoutTypes.value
            connectTimeoutMillis = timeoutTypes.value
        }
    }

    suspend fun isConnected(): Boolean {
        val response = httpClient.get("$hostAddress/serial/status").status
        return response.isSuccess()
    }

    suspend fun getAvailablePorts(): List<String> {
        val response = httpClient.get("$hostAddress/serial/find").bodyAsText()
        return response
            .substring(1, response.length - 1)
            .split(",")
            .map { it.trim() }
    }

    suspend fun connect(comName: String): Boolean {
        val response = httpClient.get("$hostAddress/serial/connect/$comName").status
        return response.isSuccess()
    }

    suspend fun command(hansCommand: HansCommand): Response {
        val responseText =
            httpClient.get("$hostAddress/command/${hansCommand.command}").bodyAsText()
                .removeSuffix("\r")
        return when (responseText) {
            Response.NoInteractive.OK.key -> Response.NoInteractive.OK
            Response.NoInteractive.NOT_RECOGNIZED.key -> Response.NoInteractive.NOT_RECOGNIZED
            else -> Response.TEXT(responseText)
        }
    }

    suspend fun waveformLoadRun(hansCommand: HansCommand): Response {
        val responseText =
            httpClient.get("$hostAddress/waveform/loadRun/${hansCommand.command}").bodyAsText()
                .removeSuffix("\r")
        return when (responseText) {
            Response.NoInteractive.OK.key -> Response.NoInteractive.OK
            Response.NoInteractive.NOT_RECOGNIZED.key -> Response.NoInteractive.NOT_RECOGNIZED
            else -> Response.TEXT(responseText)
        }
    }

    suspend fun waveformLoad(hansCommand: HansCommand): Response {
        val responseText =
            httpClient.get("$hostAddress/waveform/load/${hansCommand.command}").bodyAsText()
                .removeSuffix("\r")
        return when (responseText) {
            Response.NoInteractive.OK.key -> Response.NoInteractive.OK
            Response.NoInteractive.NOT_RECOGNIZED.key -> Response.NoInteractive.NOT_RECOGNIZED
            else -> Response.TEXT(responseText)
        }
    }

    suspend fun customWaveform(hansCommand: HansCommand): Response {
        val responseText =
            httpClient.get("$hostAddress/waveform/custom/${hansCommand.command}").bodyAsText()
                .removeSuffix("\r")
        return when (responseText) {
            Response.NoInteractive.OK.key -> Response.NoInteractive.OK
            Response.NoInteractive.NOT_RECOGNIZED.key -> Response.NoInteractive.NOT_RECOGNIZED
            else -> Response.TEXT(responseText)
        }
    }

    suspend fun getAvailableSequences(): Dir {
        val jsonSerializer = Json {
            ignoreUnknownKeys = true
            isLenient = false
        }
        val json = httpClient.get("$hostAddress/files/tree").bodyAsText()
        return jsonSerializer.decodeFromString(json)
    }
}
