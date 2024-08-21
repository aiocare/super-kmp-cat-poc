package com.aiocare.cat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.aiocare.poc.superCat.InputData

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Cat() {
    val pagerState = rememberPagerState()

    var catData: InputData? = remember {
        null
    }

    if (catData == null) {
//        catData = getRawCat(
//            LocalContext.current,
//            eu.proexe.android.myspirooalgorithms.R.raw.ms25112021128815_201123_1_0_2_16ls_increment0_1_duration2s
//        ).let {
//            InputData(it.skipFirst(30)).movmean(9)
//        }
    }

//    Graph(
//        graphItems = catData.calcIndexesToIndexedInputData(),
////        blueList = HansParser().findPeaksXPoints(catData, FlowType.Inhale),
////        greenList = HansParser().findZeroesIndexes(catData),
//        greenList = HansParser().modernExhales(catData),
////        greenList = HansParser().findSections(catData, flowType = FlowType.Exhale).toList().map { listOf(it.first().x, it.last().x) }.flatten(),//.skipFirst(116).take(2),
////        blackList = HansParser().modernInhales(catData),//.skipFirst(90),
//        points = HansParser().findPeaksPoints(catData, FlowType.Exhale),
//        points2 = HansParser().findPeaksPoints(catData, FlowType.Inhale),
//    )
}

//private fun getRawCat(context: Context, resId: Int): InputData {
//    return context.resources.openRawResource(
//        resId
//    ).bufferedReader().use { it.readText() }.split("\n").map { it.toInt() }.toInputData()
//}


//@Composable
//fun Graph(
//    graphItems: IndexedInputData?,
//    greenList: List<Int> = listOf(),
//    blueList: List<Int> = listOf(),
//    blackList: List<Int> = listOf(),
//    points: List<Point> = listOf(),
//    points2: List<Point> = listOf(),
//) {
//    AndroidView(
//        modifier = Modifier.fillMaxSize(), // Occupy the max size in the Compose UI tree
//        factory = { context ->
//            LineChart(context).apply {
//                description.text = ""
//                description.textColor = Color.RED
//                val xAxisValues = ArrayList<String>()
//                xAxis.valueFormatter = IndexAxisValueFormatter(xAxisValues)
////                    axisLeft.axisMinimum = -6f
////                    axisLeft.axisMaximum = 6f
//            }
//        },
//        update = { view ->
//            val entries = graphItems?.mapIndexed { index, fl ->
//                Entry(fl.index.toFloat(), fl.value.toFloat())
//            }
//            val lineDataSet = LineDataSet(entries, "");
//            lineDataSet.setDrawFilled(false)
//            lineDataSet.setDrawCircles(false)
//            lineDataSet.setDrawValues(false)
//            lineDataSet.fillColor = Color.WHITE
//            lineDataSet.color = Color.RED
//            lineDataSet.setCircleColor(Color.DKGRAY)
//            if (view.data != null)
//                view.data.clearValues()
//
//            view.data = LineData(
//                lineDataSet,
//                greenList.toGraphSet(graphItems, Color.GREEN),
//                blueList.toGraphSet(graphItems, Color.BLUE),
//                blackList.toGraphSet(graphItems, Color.BLACK),
//                points.toGraphSet(),
//                points2.toGraphSet()
//            )
//
//            view.invalidate()
//        }
//    )
//}

//fun List<Point>.toGraphSet(): LineDataSet {
//    return LineDataSet(
//        map {
//            Entry(
//                it.x.toFloat(),
//                it.y.toFloat()
//            )
//        }, ""
//    ).apply {
//        this.setDrawCircles(true)
//        this.color = Color.MAGENTA
//        this.lineWidth = 5f
//        this.setDrawCircleHole(true)
//        this.setDrawValues(true)
//    }
//}

//fun List<Int>.toGraphSet(graphItems: IndexedInputData?, color: Int) =
//    LineDataSet(
//        sorted().map {
//            Entry(
//                it.toFloat(),
//                graphItems?.find { f -> it == f.index }?.value?.toFloat() ?: 0f
//            )
//        }, ""
//    ).apply {
//        this.setDrawCircles(true)
//        this.color = color
//        this.lineWidth = 5f
//        this.setDrawCircleHole(true)
//        this.setDrawValues(true)
//    }