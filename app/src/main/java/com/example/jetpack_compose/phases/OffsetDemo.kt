package com.example.jetpack_compose.phases

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

// Question 6: the read is inside the offset lambda -> placement step of the layout phase.
@Composable
fun OffsetDemo() {
    var offsetX by remember { mutableStateOf<Dp>(0.dp) }
    SideEffect { Log.d("Phases", "OffsetDemo: composition") }

    Column {
        Button(onClick = { offsetX = if (offsetX == 0.dp) 80.dp else 0.dp }) {
            Text("Move")
        }
        Text(
            text = "Offset demo",
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToPx(), 0) }   // where is `offsetX` read?
                .logPhases("offset demo"),
        )
    }
}

// Question 6, experiment: the read is moved above the Text -> composition phase.
@Composable
fun OffsetDemoHoisted() {
    var offsetX by remember { mutableStateOf<Dp>(0.dp) }
    SideEffect { Log.d("Phases", "OffsetDemoHoisted: composition") }

    Column {
        Button(onClick = { offsetX = if (offsetX == 0.dp) 80.dp else 0.dp }) {
            Text("Move")
        }

        val offsetPx = with(LocalDensity.current) { offsetX.roundToPx() }   // read in composition

        Text(
            text = "Offset demo",
            modifier = Modifier
                .offset { IntOffset(offsetPx, 0) }               // the lambda now uses the Int
                .logPhases("offset demo"),
        )
    }
}
