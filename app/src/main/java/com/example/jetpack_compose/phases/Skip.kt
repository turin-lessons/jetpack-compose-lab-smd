package com.example.jetpack_compose.phases

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

// Question 11, part 1: Parent reads `count` and passes the value -> Parent restarts.
@Composable
fun Parent() {
    var count by remember { mutableIntStateOf(0) }
    SideEffect { Log.d("Skip", "Parent: composition") }

    Column {
        Button(onClick = { count++ }) { Text("Add") }
        Child(count)
        Sibling()
    }
}

@Composable
fun Child(count: Int) {
    SideEffect { Log.d("Skip", "Child: composition") }
    Text("Count: $count")
}

@Composable
fun Sibling() {
    SideEffect { Log.d("Skip", "Sibling: composition") }
    Text("I never change")
}

// Question 11, part 2: Parent passes a lambda; the state is read inside Child.
@Composable
fun ParentLambda() {
    var count by remember { mutableIntStateOf(0) }
    SideEffect { Log.d("Skip", "ParentLambda: composition") }

    Column {
        Button(onClick = { count++ }) { Text("Add") }
        ChildLambda(count = { count })               // pass a lambda that returns the value
        SiblingLambda()
    }
}

@Composable
fun ChildLambda(count: () -> Int) {
    SideEffect { Log.d("Skip", "ChildLambda: composition") }
    Text("Count: ${count()}")                        // the state is read here, not in ParentLambda
}

@Composable
fun SiblingLambda() {
    SideEffect { Log.d("Skip", "SiblingLambda: composition") }
    Text("I never change")
}
