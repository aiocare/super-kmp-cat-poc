//package com.aiocare
//
//import android.content.Context
//import com.aiocare.examination.R
//import com.google.gson.Gson
////import eu.proexe.android.myspirooalgorithms.MySpirooAlgorithms
////import eu.proexe.android.myspirooalgorithms.SpirometryEx
//
//actual class CppLogic(private val context: Context): ICppLogic {
//
////    private var algorithm: SpirometryEx? = null
//
//    private fun getCalibration() = Gson().fromJson(
//        context.resources.openRawResource(
//            R.raw.calibration
//        ).bufferedReader().use { it.readText() }, Calibration::class.java)
//
//    override fun init(){
////        MySpirooAlgorithms.init()
////        algorithm = SpirometryEx(
////////            getCalibration().self_adjusting.x.toDoubleArray(),
////            getCalibration().parameters.toDoubleArray(),
////            doubleArrayOf(0.0),
////            doubleArrayOf(30.0),
////            doubleArrayOf(
////                119.83,
////                -2.5712,
////                21.0,
////                991.0,
////                36.0,
////                0.0,
////                0.0,
////                0.0,
////                0.0,
////                0.0,
////                0.0,
////                0.0,
////                0.0,
////                0.0,
////                0.0,
////                0.0,
////                0.0,
////                0.0,
////                0.0,
////                0.0
////            )
////        )
////        algorithm?.setBtps(true)
////        algorithm?.setFrcForPlot(20, 180)
//    }
//
//    override fun calculate(input: Collection<Double>): List<Double> {
//        TODO()
////        return input.map {
////            algorithm?.calculate(it,
////                30.01677703857422,
////                999.47998046875,
////                50.63299560546875,
////                0f,
////                0f,
////                0f)?.flow()?:it
////        }
//    }
//}