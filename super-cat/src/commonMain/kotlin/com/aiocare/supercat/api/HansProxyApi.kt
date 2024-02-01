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

class HansProxyApi(private val hostAddress: String) {

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
            requestTimeoutMillis = 25000
            connectTimeoutMillis = 25000
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
//        val json = httpClient.get("$hostAddress/files/tree").bodyAsText()
        val json = "{\"name\":\"waveforms\",\"files\":[],\"dirs\":[{\"name\":\"aaa\",\"files\":[{\"name\":\"Bat2-5Pk.fvw\"}],\"dirs\":[]},{\"name\":\"C1-C13 (ISO26782)\",\"files\":[{\"name\":\"C1.fvw\"},{\"name\":\"C10.fvw\"},{\"name\":\"C11.fvw\"},{\"name\":\"C12.fvw\"},{\"name\":\"C13.fvw\"},{\"name\":\"C2.fvw\"},{\"name\":\"C3.fvw\"},{\"name\":\"C4.fvw\"},{\"name\":\"C5.fvw\"},{\"name\":\"C6.fvw\"},{\"name\":\"C7.fvw\"},{\"name\":\"C8.fvw\"},{\"name\":\"C9.fvw\"}],\"dirs\":[]},{\"name\":\"Custom\",\"files\":[{\"name\":\".DS_Store\"},{\"name\":\"Bat1-7Pk-Adjusted.fvw\"},{\"name\":\"Bat12-0Pk-Adjusted.fvw\"}],\"dirs\":[]},{\"name\":\"PEF (ISO23747)\",\"files\":[{\"name\":\".DS_Store\"}],\"dirs\":[{\"name\":\"Profile A\",\"files\":[{\"name\":\"Aat1-7Pk.fvw\"},{\"name\":\"Aat10-0Pk.fvw\"},{\"name\":\"Aat12-0Pk.fvw\"},{\"name\":\"Aat14-5Pk.fvw\"},{\"name\":\"Aat17-0Pk.fvw\"},{\"name\":\"Aat2-5Pk.fvw\"},{\"name\":\"Aat3-0Pk.fvw\"},{\"name\":\"Aat3-3Pk.fvw\"},{\"name\":\"Aat5-0Pk.fvw\"},{\"name\":\"Aat6-0Pk.fvw\"},{\"name\":\"Aat7-5Pk.fvw\"},{\"name\":\"Aat9-0Pk.fvw\"}],\"dirs\":[]},{\"name\":\"Profile B\",\"files\":[{\"name\":\".DS_Store\"},{\"name\":\"Bat1-7Pk.fvw\"},{\"name\":\"Bat10-0Pk.fvw\"},{\"name\":\"Bat12-0Pk.fvw\"},{\"name\":\"Bat14-5Pk.fvw\"},{\"name\":\"Bat17-0Pk.fvw\"},{\"name\":\"Bat2-5Pk.fvw\"},{\"name\":\"Bat3-0Pk.fvw\"},{\"name\":\"Bat3-3Pk.fvw\"},{\"name\":\"Bat5-0Pk.fvw\"},{\"name\":\"Bat6-0Pk.fvw\"},{\"name\":\"Bat7-5Pk.fvw\"},{\"name\":\"Bat9-0Pk.fvw\"}],\"dirs\":[]}]}]}"
        return jsonSerializer.decodeFromString(json)
    }
}
