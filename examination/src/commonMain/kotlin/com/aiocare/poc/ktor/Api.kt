package com.aiocare.poc.ktor

import com.aiocare.cortex.cat.hans.Command
import com.aiocare.cortex.cat.hans.CommandLoader
import com.aiocare.model.SteadyFlowData
import com.aiocare.model.WaveformData
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class Api {

    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json()
        }
        install(Logging) {
            logger = object : io.ktor.client.plugins.logging.Logger {
                override fun log(message: String) {
                    println(message)
                }
            }
            level = LogLevel.ALL
        }
    }

    private val jsonSerializer = Json {
        ignoreUnknownKeys = true
        isLenient = false
    }

    suspend fun getList(): List<Item> {
        val json =
            httpClient.get("https://gist.githubusercontent.com/skaminskiProexe/d0cd81711e5481022e561ee7a0a06c3a/raw/ec7dd5623671eda000101258904c5e3ba20062ae/gistfile1.json")
                .bodyAsText()
        return jsonSerializer.decodeFromString(json)
    }

    suspend fun getHansSequence(item: Item): List<Command> {
        val dataResponse = httpClient.get(item.url).bodyAsText().split("\n")
        return CommandLoader().load(dataResponse)
    }

    suspend fun postNewRawData(postData: PostData): String {
        return httpClient.post("https://api.dev.aiocare.com/v2/raw-data/recording") {
            header("x-api-key", "9ceafbd6-9f56-4223-9c0c-db64cb473c8b")
            contentType(ContentType.Application.Json)
            setBody(postData)
        }.let {
            "${it.status} --> ${it.responseTime} --> ${it.bodyAsText()}"

        }
    }

    @Serializable
    data class Item(val name: String, val url: String)


    @Serializable
    data class Env(
        val temperature: Float,
        val pressure: Float,
        val humidity: Float,
        val timestamp: Long
    )

    @Serializable
    data class PostData(
        val environment: Environment,
        val environmentalParamBefore: Env,
        val environmentalParamAfter: Env,
        val zeroFlowData: List<Int>,
        val steadyFlowRawData: List<SteadyFlowData>?,
        val waveformRawData: List<WaveformData>?,
        val type: String,
        val rawDataType: String,
        val notes: String
    )

    @Serializable
    data class Environment(
        val recordingDevice: String,
        val hansIpAddress: String,
        val hansSerialNumber: String,
        val hansCalibrationId: String,
        val date: String,
        val appVersion: String,
        val spirometerDeviceSerial: String,
        val operator: String
    )
}