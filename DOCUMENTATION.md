# Jetpack Compose Phases — explained for a Python web developer

You know Python and the web (Django / Flask / FastAPI, HTML, CSS, a bit of JavaScript).
You have not written Android UI before. This document explains what this lab is about
using things you already know. The numbers in it are real: they were measured by running
the demos in this project on an Android emulator.

> Exam-style answers to the 14 questions of the practice class are in
> [PRACTICE_ANSWERS.md](PRACTICE_ANSWERS.md). This file is the "explain it to me simply" version.

---

## 1. The 60-second version

- **Jetpack Compose** is how you build Android screens in Kotlin. Instead of an HTML
  template you write **functions** that describe the screen.
- A screen is redrawn many times per second. Every redraw goes through **three phases**:
  **Composition → Layout → Drawing**. It is the same idea as the browser's
  **build the DOM → reflow → repaint**.
- When a piece of **state** changes, Compose re-runs only the parts that **read** that state.
- The lab teaches one skill: **read state as late as possible**, so a change triggers the
  cheapest phase. You prove it by printing to **Logcat** (Android's `print()` / server log).

---

## 2. Translate the vocabulary

| In Compose / Android | What you already know | Note |
|---|---|---|
| `@Composable fun Foo()` | A template macro, a React component, or `with div(): p("hi")` in Python's `dominate` | It does not *return* HTML. It **emits** UI nodes while it runs. |
| `Column { … }` / `Row { … }` / `Box { … }` | `display:flex; flex-direction:column` / `row` / children stacked on top of each other | |
| `Text("hi")` | `<p>hi</p>` | |
| `Modifier.padding(8.dp).background(Color.Red)` | A chain of CSS properties (or stacked decorators) | **Order matters**, like decorators. |
| `dp` | A CSS `px` (density independent) | Compose's `px` means real device pixels. |
| `var x by remember { mutableStateOf(0) }` | Vue `ref(0)` / React `useState(0)` / Streamlit `st.session_state` | An **observable** value that survives re-runs. |
| Recomposition | Re-rendering the template with new data | Only for the parts that read the changed value. |
| `Log.d("Tag", "msg")` | `logging.debug("msg")` | Goes to **Logcat**. |
| Logcat | `tail -f server.log` / the `runserver` console | Search `tag:Phases` ≈ `grep Phases`. |
| Gradle (`./gradlew`) | pip + a build backend | `app/build.gradle.kts` ≈ `pyproject.toml` |
| `gradle/libs.versions.toml` | `requirements.txt` with pinned versions | |
| Emulator | A VM / Docker container that plays the phone | |
| `adb` | `ssh` / `docker exec` into that phone | |
| `LazyColumn` | A virtualised list (infinite scroll, like `react-window`) | Only the visible rows exist. |
| `derivedStateOf { … }` | Vue `computed` | Notifies only when the **result** changes. |
| `Activity` | One "page" / entry point of the app | `MainActivity` is `index`. |

### Kotlin in 2 minutes

| Python | Kotlin |
|---|---|
| `x = 5` | `var x = 5` (changeable) or `val x = 5` (read-only) |
| `def f(a: int) -> str:` | `fun f(a: Int): String { … }` |
| `lambda a: a + 1` | `{ a -> a + 1 }` |
| `f"Item {i}"` | `"Item $i"` |
| `None` | `null` |
| `[x * 2 for x in xs]` | `xs.map { x -> x * 2 }` |
| `a if cond else b` | `if (cond) a else b` (an `if` is an expression) |
| `print("hi")` | `Log.d("Tag", "hi")` |
| passing a callback as the last argument | **Trailing lambda**: `Column { … }` is `Column(content = { … })` |
| property access that calls code | **Delegate**: with `var x by remember {…}` you write `x`, not `x.value` |

---

## 3. A frame = three phases

```
 state changed
      |
      v
 +---------------+     +---------------+     +---------------+
 | 1 COMPOSITION | --> |   2 LAYOUT    | --> |  3 DRAWING    |
 | WHAT to show  |     | WHERE to put  |     | HOW it looks  |
 | output: tree  |     | output: size  |     | output: pixels|
 |               |     | and (x, y)    |     |               |
 +---------------+     +---------------+     +---------------+
   browser:               browser:              browser:
   build the DOM          reflow                repaint
```

1. **Composition** runs your `@Composable` functions and builds a **UI tree** (like the DOM).
2. **Layout** walks the tree and gives every node a **size and a position**. It has two steps:
   **measure** (how big am I?) and **place** (where do my children go?).
3. **Drawing** paints the sized and positioned tree.

The phases only go **forward** inside one frame. Layout never creates UI, and drawing never
changes the layout. (The one exception is `LazyColumn`: it composes its rows *during* layout,
because only layout knows how many rows fit on the screen.)

### How layout works (and why it is fast)

Each node does three things: **measure children → decide own size → place children**, and each
node is measured **only once**. Example: `Row[ Image 48x48, Column[ Text 100x20, Text 80x20 ] ]`.

```
measure Row
  measure Image          -> 48 x 48
  measure Column
    measure Text A       -> 100 x 20
    measure Text B       -> 80 x 20
  Column size            -> 100 x 40     (widest child, heights added)
Row size                 -> 148 x 48     (widths added, tallest child)
place Image  at (0, 0)
place Column at (48, 0)   -> Text A at (48, 0), Text B at (48, 20)
```

If every node were measured twice at each level, the cost would double with every level
(2 to the power of the depth), and deep screens would crawl. One measurement per node keeps
it linear, like one pass over a list.

### Why is it not slow if all three phases run every frame?

They do not. Compose **remembers who read which state** and re-runs only those readers.
A function whose inputs did not change is **skipped** (the same idea as `functools.lru_cache`
or `React.memo`).

---

## 4. The one rule: *where* you read a state decides the cost

A toy model in Python (not real Compose code) of how Compose knows what to re-run:

```python
current_scope = None            # "which block of code is running right now?"

class State:
    def __init__(self, value):
        self._value = value
        self._readers = set()

    @property
    def value(self):
        if current_scope is not None:
            self._readers.add(current_scope)   # remember who read me
        return self._value

    @value.setter
    def value(self, new):
        self._value = new
        for scope in self._readers:            # re-run ONLY the readers
            scope.invalidate()
```

The "scope" is a **restart scope**: a block Compose can re-run on its own. There are three kinds,
one per phase:

| Where the state is read | Example | What re-runs | Closest web analogy |
|---|---|---|---|
| In the **body of a composable** (building a modifier) | `Modifier.padding(x)` | composition, then layout, then drawing | Changing the template / DOM: everything after it |
| In a **layout lambda** | `Modifier.offset { IntOffset(x, 0) }` | layout (placement) only | `transform: translate()`: moves it without redoing the page |
| In a **draw lambda** | `Canvas { drawRect(color) }` | drawing only | Changing `background-color`: repaint only |

Two more things to know:

- **Where a state is created does not matter, only where it is read.** The same
  `remember { mutableStateOf(...) }` can trigger one phase or all three.
- Composition **may skip** layout and drawing if nothing's size or position changed. The table
  shows the worst case for each read.

---

## 5. How the lab project is organised

```
app/src/main/java/com/example/jetpack_compose/
├── MainActivity.kt            entry point: shows DemoHost()
└── phases/
    ├── DemoHost.kt            chip row at the top: pick which demo to show
    ├── PhaseLog.kt            Modifier.logPhases("name"): prints measure / place / draw
    ├── LectureTree.kt         Q4   the Row / Image / Column / Text tree
    ├── PaddingDemo.kt         Q5   state read in composition
    ├── OffsetDemo.kt          Q6   state read in layout (+ the "hoisted" experiment)
    ├── ColorDemo.kt           Q7   state read in drawing (+ the Box version)
    ├── Parallax.kt            Q9   slow (composition read) vs fast (layout read)
    ├── ScrollToTop.kt         Q10  naive vs derivedStateOf, working Top button
    ├── Skip.kt                Q11  value vs lambda: who recomposes?
    ├── Cyclic.kt              Q12-13 the cyclic dependency and its fix
    └── StackBelow.kt          Q14  a custom Layout written by hand
```

### The two logging tricks

```kotlin
// Prints once per composition (runs after each successful composition):
SideEffect { Log.d("Phases", "PaddingDemo: composition") }

// Prints for layout and drawing of whatever it is attached to:
fun Modifier.logPhases(name: String): Modifier = this
    .layout { measurable, constraints ->
        Log.d("Phases", "$name: layout/measure")            // measure step
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            Log.d("Phases", "$name: layout/place")          // place step
            placeable.place(0, 0)
        }
    }
    .drawBehind { Log.d("Phases", "$name: draw") }          // drawing phase
```

In Python terms: it is a decorator that wraps a function with three `print()` calls.
Logging inside a composable body is only for experiments. A real app keeps side effects out
of it (like not doing I/O inside a Jinja template).

### How to run it

**Android Studio:** open this folder, wait for the Gradle sync, start the emulator, press
Run. In the **Logcat** panel type `tag:Phases` in the search box. Tap a chip at the top to pick
a demo.

**Command line** (`adb` lives in `~/Library/Android/sdk/platform-tools/`):

```bash
./gradlew :app:installDebug
adb shell am start -n com.example.jetpack_compose/.MainActivity
adb logcat -s Phases Parallax Derived Skip Cycle StackBelow
```

---

## 6. The experiments, one by one

All results are from a real run (Pixel 8 Pro emulator, API 37).

### Q4: See the first frame (chip "Q4 tree")

What you see in Logcat:

```
LectureTree: composition
Row, Image, Column, Text A, Text B : layout/measure     (in that order)
Row, Image, Column, Text A, Text B : layout/place       (same order)
Row, Image, Column, Text A, Text B : draw
```

Same order as the diagram in section 3: composition, then measure, then place, then draw.
**Rotating the phone** runs everything again from scratch, because Android destroys and
recreates the Activity (like a full page reload). A plain `remember` value is lost;
`rememberSaveable` survives.

> Side note: on a cold start I saw the two `Text` nodes measure a second time. Only they did,
> not their parents. It is most likely the text finishing its font loading, not a bug.

### Q5: Read in composition (chip "Q5 padding")

```kotlin
Modifier.padding(padding)        // `padding` is read here, while building the modifier
```

Every tap prints all four lines: `composition`, `layout/measure`, `layout/place`, `draw`.
Web analogy: you changed a margin in the template. The page is re-rendered, re-flowed and repainted.

### Q6: Read in layout (chips "Q6 offset" and "Q6 hoisted")

```kotlin
Modifier.offset { IntOffset(offsetX.roundToPx(), 0) }   // read inside the lambda -> layout
```

Every tap prints only `offset demo: layout/place`. No composition, no measure.

The experiment moves the read **above** the `Text` (`val offsetPx = …`). Same state, same result
on screen, but now every tap prints all four lines. Moving one line changed the cost.

Web analogy: `transform: translate()` versus changing `left` through a re-render.

### Q7: Read in drawing (chips "Q7 canvas" and "Q7 box")

- `Canvas { drawRect(color) }` prints only `draw`.
- `Box(...background(color))` prints `composition` and then the later phases.

The state is stored in the same place in both. Only the **read** is different. In a stricter
test (with a modifier that Compose can compare), `background(color)` printed only
`composition` and `draw`: Compose really does skip layout when the size did not change.

### Q9: Parallax (chips "Q9 slow" and "Q9 fast")

The purple box moves at half the scroll speed. The only difference is where the scroll
offset is read:

```kotlin
// slow: read while building the modifier -> composition on every scroll event
.offset(y = with(LocalDensity.current) { (listState.firstVisibleItemScrollOffset / 2).toDp() })

// fast: read inside the lambda -> only placement re-runs
.offset { IntOffset(x = 0, y = listState.firstVisibleItemScrollOffset / 2) }
```

About 3 seconds of scrolling: **slow = 82 to 99 compositions, fast = 1** (just the first one).

### Q10: `derivedStateOf` (chips "Q10 naive" and "Q10 derived")

The "Top" button should appear once you scroll past item 0.

```kotlin
// naive: recomposes every time the first visible index changes
val showButton = listState.firstVisibleItemIndex > 0

// derived: recomposes only when the Boolean itself flips
val showButton by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
```

Scrolling to about item 38: **naive = 39 compositions, derived = 2**.

Python analogy: the naive version is "re-render on every change of `scroll_index`". The
derived version is "re-render when `scroll_index > 0` changes from `False` to `True`".
The Top button works in both: it calls `listState.animateScrollToItem(0)` from a coroutine
(`rememberCoroutineScope`), like `await scroll_to(0)`.

### Q11: Skipping and "pass a getter, not a value" (chips "Q11 value" and "Q11 lambda")

```kotlin
Child(count)             // reads `count` in Parent  -> Parent re-runs
Child(count = { count }) // Child reads it later      -> only Child re-runs
```

Three taps on the "Add" button:

| | Parent | Child | Sibling |
|---|---|---|---|
| `Child(count)` | 3 | 3 | **0** (inputs did not change, so skipped) |
| `Child(count = { count })` | **0** | 3 | **0** |

Python analogy: `child(count=count)` versus `child(count=lambda: state.count)`. Passing the
getter postpones the read to the code that really needs the value, so only that code is
invalidated.

### Q12 and Q13: The cyclic dependency (chips "Q13 cyclic" and "Q13 fixed")

The bad code measures the image in layout, stores the height in state, and uses it as padding
in composition:

```kotlin
.onSizeChanged { size -> imageHeightPx = size.height }       // layout WRITES the state
...
Modifier.padding(top = with(LocalDensity.current) { imageHeightPx.toDp() })  // composition READS it
```

What happens (the image is 360 px tall):

| | composition reads | text padding | what the user sees |
|---|---|---|---|
| Frame 1 | `imageHeightPx = 0` | 0 | text drawn **on top of** the image (wrong) |
| Frame 2 | `imageHeightPx = 360` | 360 px | text below the image (correct) |

Logcat: `composition #1: 0`, `onSizeChanged: 360`, `composition #2: 360`.

Web analogy: JavaScript calls `getBoundingClientRect()`, writes the result into state, and
re-renders: two renders and a flicker. The fix is to let the **layout** do it. Here that is a
`Column`, like using flexbox instead of measuring in JS. `CyclicFixed` produces only
`composition #1`.

### Q14: Write your own layout (chips "Q14 stack" and "Q14 stack x3")

`StackBelow` is a tiny custom flexbox: measure each child **once**, take the widest width and
the summed heights plus gaps, then place the children one below another.

```
2 children:  measure child, measure child, place children
3 children:  measure child, measure child, measure child, place children
```

One measure per child and one placement: the "one pass" rule from section 3, in code.

---

## 7. Cheat sheet

1. **Read state as late as possible.** Composition is the most expensive place, drawing the cheapest.
2. A value that changes **every frame** (animation, scroll, drag) belongs in a **draw lambda**
   (looks only) or a **layout lambda** such as `offset { }` (movement). Never in the composable body.
3. Need the value in composition anyway? Use **`derivedStateOf`** to turn a fast-changing number
   into a slow-changing Boolean or category.
4. Pass **`() -> T`** instead of **`T`** to push the read down to the child that uses it.
5. Never store a layout result in state and feed it back to composition. Use a layout (`Column`,
   `Row`, `Box`, or a custom `Layout`) so the work happens in one pass.
6. Do not trust a guess: **log it** (`SideEffect`, `logPhases`) or use Android Studio's
   **Layout Inspector** (it shows recomposition counts).

## 8. Glossary

| Term | Meaning |
|---|---|
| Composable | A function marked `@Composable` that emits UI |
| Composition / recomposition | Running composables to build the tree / re-running some of them after a state change |
| Restart scope | The block of code Compose can re-run alone when a state it read changes |
| Skipping | Not re-running a composable whose inputs are unchanged |
| Measure / place | Layout steps: find each node's size, then give it a position |
| Constraints | The min/max width and height a parent allows a child (like CSS width/height limits) |
| Modifier | A chain of properties applied to a composable (size, padding, offset, draw behaviour…) |
| Snapshot state | The observable value type behind `mutableStateOf` |
| Logcat | The Android log viewer |
