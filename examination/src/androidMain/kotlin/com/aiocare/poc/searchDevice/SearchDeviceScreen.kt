package com.aiocare.poc.searchDevice

import android.graphics.Color
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
//import com.aiocare.CppLogic
import com.aiocare.SimpleButton
import com.aiocare.util.ButtonVM
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter


@Composable
fun SearchDeviceScreen(viewModel: SearchDeviceViewModel) {

    SideEffect {
        viewModel.onStart()
    }

    var initialized by  remember { mutableStateOf(false) }
    if (!initialized) {
//        viewModel.initCortex(CppLogic(LocalContext.current))
        initialized = true
    }

    Column {
        if (viewModel.uiState.loading) {
            CircularProgressIndicator()
        }
        Text(text = viewModel.uiState.battery)
        if(viewModel.uiState.graphItems.isEmpty())
        Column {
            viewModel.uiState.devices.forEach {
                SimpleButton(buttonVM = ButtonVM(
                    visible = true,
                    onClickAction = it.onDeviceClicked,
                    text = it.text
                ))
            }
        }
        if(viewModel.uiState.x != 0.0)
        Row {
            listOf(Pair(viewModel.uiState.x.toFloat(), "x"),
                Pair(viewModel.uiState.y.toFloat(), "y"),
                Pair(viewModel.uiState.z.toFloat(), "z"),
                ).forEach {
                Column {
                    CircularProgressIndicator(progress = it.first)
                    Text(text = it.second)
                }
            }
        }
        if(viewModel.uiState.graphItems.isNotEmpty())
        AndroidView(
            modifier = Modifier.fillMaxSize(), // Occupy the max size in the Compose UI tree
            factory = { context ->
                LineChart(context).apply {
                    description.text = ""
                    description.textColor = Color.RED
                    val xAxisValues = ArrayList<String>()
                    xAxis.valueFormatter = IndexAxisValueFormatter(xAxisValues)
                    axisLeft.axisMinimum = -6f
                    axisLeft.axisMaximum = 6f
                }
            },
            update = { view ->
                val entries = viewModel.uiState.graphItems.mapIndexed { index, fl ->
                    Entry(index.toFloat(), fl)
                }
                val lineDataSet = LineDataSet(entries, "");
                lineDataSet.setDrawFilled(false)
                lineDataSet.setDrawCircles(false)
                lineDataSet.setDrawValues(false)
                lineDataSet.fillColor = Color.WHITE
                lineDataSet.color = Color.RED
                lineDataSet.setCircleColor(Color.DKGRAY)
                if(view.data!=null)
                    view.data.clearValues()
                view.data = LineData(lineDataSet)
                view.invalidate()
            }
        )
    }
}