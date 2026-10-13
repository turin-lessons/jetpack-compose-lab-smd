package com.example.jetpack_compose.phases

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** One entry per demo of the practice class; the chip row below picks which one is shown. */
enum class Demo(val label: String) {
    Q4("Q4 tree"),
    Q5("Q5 padding"),
    Q6("Q6 offset"),
    Q6X("Q6 hoisted"),
    Q7("Q7 canvas"),
    Q7X("Q7 box"),
    Q9S("Q9 slow"),
    Q9F("Q9 fast"),
    Q10N("Q10 naive"),
    Q10D("Q10 derived"),
    Q11A("Q11 value"),
    Q11B("Q11 lambda"),
    Q13A("Q13 cyclic"),
    Q13B("Q13 fixed"),
    Q14A("Q14 stack"),
    Q14B("Q14 stack x3"),
}

@Composable
fun DemoHost(modifier: Modifier = Modifier) {
    var selected by rememberSaveable { mutableStateOf(Demo.Q4.name) }

    Column(modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Demo.entries.forEach { demo ->
                FilterChip(
                    selected = demo.name == selected,
                    onClick = { selected = demo.name },
                    label = { Text(demo.label) },
                )
            }
        }
        HorizontalDivider()
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (Demo.valueOf(selected)) {
                Demo.Q4 -> LectureTree()
                Demo.Q5 -> PaddingDemo()
                Demo.Q6 -> OffsetDemo()
                Demo.Q6X -> OffsetDemoHoisted()
                Demo.Q7 -> ColorDemo()
                Demo.Q7X -> ColorDemoBox()
                Demo.Q9S -> ParallaxSlow()
                Demo.Q9F -> ParallaxFast()
                Demo.Q10N -> ScrollToTopNaive()
                Demo.Q10D -> ScrollToTopDerived()
                Demo.Q11A -> Parent()
                Demo.Q11B -> ParentLambda()
                Demo.Q13A -> CyclicDemo()
                Demo.Q13B -> CyclicFixed()
                Demo.Q14A -> StackBelowDemo()
                Demo.Q14B -> StackBelowThree()
            }
        }
    }
}
