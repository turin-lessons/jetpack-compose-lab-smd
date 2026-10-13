package com.example.jetpack_compose.phases

import android.util.Log
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.layout

/**
 * Logs the layout phase (measure and placement) and the drawing phase
 * of the element it is applied to.
 */
fun Modifier.logPhases(name: String): Modifier = this
    .layout { measurable, constraints ->
        Log.d("Phases", "$name: layout/measure")
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            Log.d("Phases", "$name: layout/place")
            placeable.place(0, 0)
        }
    }
    .drawBehind { Log.d("Phases", "$name: draw") }
