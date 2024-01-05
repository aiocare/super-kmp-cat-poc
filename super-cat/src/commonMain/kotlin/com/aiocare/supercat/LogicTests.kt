package com.aiocare.supercat

import com.aiocare.model.Units
import com.aiocare.supercat.api.HansCommand
import com.aiocare.supercat.api.HansProxyApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.time.measureTimedValue

class LogicTests(private val hostAddress: String) {

    private val api by lazy { HansProxyApi(hostAddress) }

    fun test() {
        GlobalScope.launch {
            val single = listOf(
                HansCommand.volume(Units.VolumeUnit.LITER(8.0)),
                HansCommand.reset()
            )
            val multiple = listOf(
                HansCommand.volume(Units.VolumeUnit.LITER(1.0)),
                HansCommand.reset(),
                HansCommand.volume(Units.VolumeUnit.LITER(2.0)),
                HansCommand.reset(),
                HansCommand.volume(Units.VolumeUnit.LITER(3.0)),
                HansCommand.reset(),
                HansCommand.volume(Units.VolumeUnit.LITER(4.0)),
                HansCommand.reset(),
                HansCommand.volume(Units.VolumeUnit.LITER(5.0)),
                HansCommand.reset(),
                HansCommand.volume(Units.VolumeUnit.LITER(6.0)),
                HansCommand.reset(),
                HansCommand.volume(Units.VolumeUnit.LITER(7.0)),
                HansCommand.reset(),
                HansCommand.volume(Units.VolumeUnit.LITER(8.0)),
                HansCommand.reset()
            )

            api.command(HansCommand.volume(Units.VolumeUnit.LITER(0.0)))
            api.command(HansCommand.reset())

            val singleTime = measureTimedValue {
                single.forEach {
                    api.command(it)
                }
            }

            api.command(HansCommand.volume(Units.VolumeUnit.LITER(0.0)))
            api.command(HansCommand.reset())

            val multipleTime = measureTimedValue {
                multiple.forEach {
                    api.command(it)
                }
            }

            println("${singleTime.duration.inWholeMilliseconds}")
            println("${multipleTime.duration.inWholeMilliseconds}")
        }
    }
}
