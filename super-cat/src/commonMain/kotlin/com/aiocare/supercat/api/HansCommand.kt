package com.aiocare.supercat.api

import com.aiocare.model.Units

class HansCommand private constructor(private val rawCommand: String) {
    val command: String
        get() {
            return rawCommand.replace(" ", "_")
        }

    companion object {

        enum class Type {
            Exhale, Inhale
        }

        fun reset() = HansCommand("Reset")

        fun run() = HansCommand("Run")

        fun readTemperature() = HansCommand("SendData Temperature")

        fun readHumidity() = HansCommand("SendData RH")

        fun volume(volume: Units.VolumeUnit.LITER) = HansCommand("Volume ${volume.value}")

        fun flow(
            flow: Units.FlowUnit.L_S,
            volume: Units.VolumeUnit.LITER,
            type: Type
        ): HansCommand {
            return HansCommand("Flow_${flow.value},_${volume.value},_${type.name}")
        }

        fun waveform(
            waveFormName: String
        ): HansCommand {
            return HansCommand(waveFormName.replace("/", "@").removeSuffix(".fvw"))
        }

        fun waveformData() = rawCommand("SendSpirometry")

        fun rawCommand(command: String) = HansCommand(command)
    }
}
