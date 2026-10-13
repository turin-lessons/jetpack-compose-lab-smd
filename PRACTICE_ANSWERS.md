# Practice 3 — Jetpack Compose Phases: answers

All hands-on numbers below were measured on the Pixel 8 Pro emulator (API 37, density 3.0)
with the code in `app/src/main/java/com/example/jetpack_compose/phases/`.
Run the app and pick a demo with the chip row at the top; filter Logcat with `tag:Phases`,
`tag:Parallax`, `tag:Derived`, `tag:Skip`, `tag:Cycle`, `tag:StackBelow`.

| Question | Demo chip | File |
|---|---|---|
| Q4 | Q4 tree | `LectureTree.kt`, `PhaseLog.kt` |
| Q5 | Q5 padding | `PaddingDemo.kt` |
| Q6 | Q6 offset / Q6 hoisted | `OffsetDemo.kt` |
| Q7 | Q7 canvas / Q7 box | `ColorDemo.kt` |
| Q9 | Q9 slow / Q9 fast | `Parallax.kt` |
| Q10 | Q10 naive / Q10 derived | `ScrollToTop.kt` |
| Q11 | Q11 value / Q11 lambda | `Skip.kt` |
| Q13 | Q13 cyclic / Q13 fixed | `Cyclic.kt` |
| Q14 | Q14 stack / Q14 stack x3 | `StackBelow.kt` |

---

## Part 1 — The frame

### Question 1 — The three phases of a frame

**(a)** Order: **Composition → Layout → Drawing**.
- *Composition* decides **what** to show. It runs the composable functions and outputs the **UI tree** (a tree of layout nodes with their modifiers and layout logic).
- *Layout* decides **where** to put it. It takes the UI tree and outputs a **size and (x, y) position for every node**.
- *Drawing* decides **how** it renders. It takes the sized and positioned tree and outputs the **drawing commands / pixels** on the canvas.

**(b)** The layout phase has two steps: **measurement** (each node learns its size, given constraints from its parent) and **placement** (each node places its children at positions).

**(c)** Normally composition finishes before layout starts, so layout never creates UI. `LazyColumn` (and `LazyRow`, `BoxWithConstraints`) are the exception: they only compose the items that are visible, and which items are visible depends on the size the parent gives them, which is known only during **layout**. So their children are composed *inside* the layout phase (subcomposition).
Real screen: a chat list with 10 000 messages. The `LazyColumn` gets, say, 2400 px of height from its parent in layout, and only then composes the ~10 message rows that fit; when you scroll, new rows are composed during layout. `BoxWithConstraints` is the same idea: a screen that shows a one-pane or two-pane UI depending on `maxWidth`.

**(d)** Because Compose does not really redo everything. It **tracks which state each piece of code reads** and re-runs only the scopes that read a changed value (and a composable whose inputs did not change is **skipped**). If nothing relevant changed, a whole phase is skipped for that element. The mechanism is *snapshot state read tracking + restart scopes + skipping*.

### Question 2 — Walk the layout algorithm

Tree: `Row[ Image 48×48, Column[ Text A 100×20, Text B 80×20 ] ]`.

**(a)** Order (depth-first; measure everything, then place):

```
measure Row
  measure Image            -> 48×48
  measure Column
    measure Text A         -> 100×20
    measure Text B         -> 80×20
  Column decides its size  -> 100×40
Row decides its size       -> 148×48
place Row
  place Image    at (0, 0)
  place Column   at (48, 0)
    place Text A at (0, 0)  inside the Column
    place Text B at (0, 20) inside the Column
```

(My Logcat from Question 4 shows exactly this order: Row, Image, Column, Text A, Text B for measure, and the same for place.)

**(b)**
- Column: width = max(100, 80) = **100**, height = 20 + 20 = **40** → **100 × 40**.
- Row: width = 48 + 100 = **148**, height = max(48, 40) = **48** → **148 × 48**.
- Positions relative to the Row's top-left corner (Row aligns children to the top by default):

| Node | (x, y) |
|---|---|
| Row | (0, 0) |
| Image | (0, 0) |
| Column | (48, 0) |
| Text A | (48, 0) |
| Text B | (48, 20) |

**(c)** Every node is measured **once** and placed **once** per pass, so the work is proportional to the number of nodes (linear). The lecture stresses it because if each parent measured each child twice, the cost would double at every level of the tree: a tree of depth *d* would cost about **2^d** measurements, i.e. exponential in depth. Deep nested UIs would become unusably slow. That is why Compose forbids measuring a child twice (single-pass measurement).

**(d)** The **composition phase** produced it. Each node of the UI tree holds: its **modifier chain**, its **layout/measure policy** (the rule for measuring and placing its children), its **children**, and for leaves the content to show (text, painter, …). The layout phase then adds the measured size and position to each node.

### Question 3 — State reads and restart scopes

**(a)** Equivalent. Snippet 2's `by` delegate is only syntax sugar for `.value`. In both, the read happens **when the state is used**: at the moment `Modifier.padding(...)` evaluates its argument while the composable body runs (composition phase). Creating it with `remember { mutableStateOf(8.dp) }` reads nothing. What matters is where the state is *read*, not where it is created.

**(b)**
- `Text("A")`: `offsetX` is read **inside the `offset { }` lambda**, so in the **layout phase** (placement step). When it changes: no recomposition, no re-measure; only the placement re-runs (and the node's position changes).
- `Text("B")`: `offsetX` is read in the **composable body** (`offsetPx = ... offsetX.roundToPx()`), so in the **composition phase**. When it changes: the enclosing composable **recomposes**, builds a new modifier, and then layout and drawing follow.

**(c)** A restart scope is a block of code that Compose can **re-execute on its own** when a snapshot state read inside it changes. For a read made directly in the body of a composable function, the restart scope is **that composable function** (the nearest enclosing non-inline composable; the content lambdas of `Column`/`Row`/`Box` are inline, so they belong to the function around them). In layout there are separate scopes for the measure step and the placement step.

---

## Part 2 — Seeing the phases

### Question 4 — The first frame (measured)

Logcat, `tag:Phases`, cold start:

```
LectureTree: composition
Row: layout/measure      Image: layout/measure     Column: layout/measure
Text A: layout/measure   Text B: layout/measure
Row: layout/place        Image: layout/place       Column: layout/place
Text A: layout/place     Text B: layout/place
Row: draw  Image: draw  Column: draw  Text A: draw  Text B: draw
```

- The order matches Question 2: measure Row → Image → Column → Text A → Text B, then place in the same order, then draw. Composition comes first of all.
- On this cold start I also saw a **second, short pass**: `Text B` and `Text A` measured/placed again and everything drew again. Only the two `Text` nodes re-measured (not Row/Column/Image). It did not happen after rotation. This is most likely the text nodes re-measuring themselves once their font had been resolved. It is not a composition or a bug in the tree.
- **Rotate:** the whole thing runs again from scratch: `LectureTree: composition`, all measure, all place, all draw. A rotation is a configuration change; the Activity is **destroyed and recreated**, `setContent` runs again, so there is a brand-new composition (and a `remember`ed value would be lost; `rememberSaveable` survives it).

### Question 5 — Reading state in the composition phase (measured)

Prediction: all four lines on every tap. Log, per tap, three taps in a row:

```
PaddingDemo: composition
padding demo: layout/measure
padding demo: layout/place
padding demo: draw
```

No difference from the prediction. `padding` is read while the modifier chain is built, which is inside `PaddingDemo`'s body, so `PaddingDemo` is the restart scope: it recomposes, the padding changes the size, so layout and drawing follow.

### Question 6 — Reading state in the layout phase (measured)

Prediction and log for `Modifier.offset { IntOffset(offsetX.roundToPx(), 0) }`, per tap:

```
offset demo: layout/place        <- the only line
```

No `OffsetDemo: composition`, no `layout/measure`, not even `draw`. The read is inside the placement block, which has its own restart scope, so only placement re-runs.

Experiment (read moved above the Text, "Q6 hoisted"), per tap:

```
OffsetDemoHoisted: composition
offset demo: layout/measure
offset demo: layout/place
offset demo: draw
```

The `offsetX` read now happens in the body of the composable, so it is a **composition-phase** read: the function recomposes and the whole chain follows. Same state, same result on screen, but the work moved from "placement only" to "everything". (My logging helper can add `layout` lines of its own on every recomposition; see the note under Question 7.)

### Question 7 — Reading state in the drawing phase (measured)

`Canvas { drawRect(color) }`, per tap:

```
color demo: draw                 <- the only line
```

(The `draw` line can occasionally repeat; there was never a composition or layout line.)

`Box(...background(color).logPhases(...))`, per tap:

```
ColorDemoBox: composition
color demo: layout/measure
color demo: layout/place
color demo: draw
```

Both demos store `color` in the same place (a `remember`ed state in the same composable). The only difference is **where it is read**: inside the draw lambda → only drawing; while the modifier is built → composition, and then layout and draw. So where a state is *created* has little bearing; where it is *read* decides which phases run.

> **Extra finding.** The `layout/measure` + `layout/place` lines in the "box" run are partly an artefact of my `logPhases` helper: it builds a new lambda on each recomposition, so Compose treats the modifier as changed and re-measures. I checked this with a throwaway helper whose modifier compares equal: with it, `background(color)` read in composition gave only `composition` + `draw` (no layout). So composition **can** skip layout when the size and position do not change, exactly as the docs say. The lecture's table ("composition → layout → draw") is the worst case.

### Question 8 — Which phase re-runs?

| | Where it is read | Composition | Layout | Drawing |
|---|---|---|---|---|
| (a) `padding(p)` | composition (modifier built) | **runs** | **runs** (size changes) | **runs** |
| (b) `offset { IntOffset(p, 0) }` | layout, placement step | skipped | **runs** (placement only) | only if size or position changed |
| (c) `offset(p)` (Dp overload) | composition (modifier built) | **runs** | **runs** | only if size or position changed |
| (d) `Canvas { drawRect(c) }` | drawing | skipped | skipped | **runs** |
| (e) `Layout { … uses s … }` | layout, measure step | skipped | **runs** (measure, then place) | only if size or position changed |
| (f) `drawWithContent { … c … }` | drawing | skipped | skipped | **runs** |

For a value that changes **on every frame** I would use **(d)/(f)**, a read in the drawing phase, when it only changes how something looks (color, alpha, a progress arc): then no recomposition and no layout happens, only drawing. If the per-frame value moves things (a parallax, a drag), use **(b)**: it skips composition and re-runs only placement. Never (a)/(c) for per-frame values, because they recompose 60 times per second.

(In my runs, (b) showed only `layout/place` and no `draw` line for a pure position change: a position-only change did not need the node's own draw block to run again.)

---

## Part 3 — Optimizing state reads

### Question 9 — Parallax (measured, ~3 s of scrolling, same gesture for both)

| Version | Composition lines | Last number |
|---|---|---|
| `ParallaxSlow` | 82 (and 99 in another run) | `slow: composition #82` |
| `ParallaxFast` | 1 (only the initial one) | `fast: composition #1` |

- **Slow** triggers **composition, layout and drawing** on every scroll event (it recomposes with a new offset, then lays out and draws).
- **Fast** triggers only **layout (placement) and drawing**: no recomposition at all. The state is read inside `offset { }`, so only the placement scope of that box re-runs.

(The Layout Inspector cross-check is optional; it would show the same: a huge recomposition count for the slow version and none for the fast one.)

### Question 10 — `derivedStateOf` (measured, scrolled to item ≈ 38)

| Version | Composition lines | Last number |
|---|---|---|
| `ScrollToTopNaive` | 39 | `naive: composition #39` |
| `ScrollToTopDerived` | 2 | `derived: composition #2` |

Why so different if the button changes visibility only twice? In the naive version `listState.firstVisibleItemIndex` is read in composition, so the composable is subscribed to the **index** and recomposes every time a new item becomes the first visible one (~38 times), although the Boolean `index > 0` stayed `true`. With `derivedStateOf` the composition is subscribed only to the **derived Boolean**: Compose re-evaluates the calculation when the index changes, but only notifies the composition when the *result* changes (false → true once, and again if you scroll back). That is 1 initial composition + 1 change = 2.

The button now works: `val scope = rememberCoroutineScope()` and `onClick = { scope.launch { listState.animateScrollToItem(0) } }` (done in both demos). I tested it: from item 39 the list animated back to item 0 and the button disappeared.

### Question 11 — Skipping and deferred reads (measured, three taps on Add)

| Version | Parent | Child | Sibling |
|---|---|---|---|
| value: `Child(count)` | 3 | 3 | **0** |
| lambda: `Child(count = { count })` | **0** | 3 | **0** |

(Each function also had its 1 initial composition.)

Explanation: in the first version `count` is read in the **body of `Parent`**, so `Parent`'s restart scope is subscribed to it and re-runs, passing a new `Int` to `Child`, which therefore recomposes too; `Sibling` has no changed inputs, so it is skipped. In the lambda version `Parent` only *creates* a lambda and never reads the value itself; the read `count()` happens in `Child`'s body, so **`Child`'s restart scope is the only one subscribed**, and only `Child` re-runs.

---

## Part 4 — Cyclic phase dependency

### Question 12 — Two frames for one layout

Image is 360 px tall.

**Frame 1.**
- Composition reads `imageHeightPx = 0` → top padding of the text = `0.toDp()` = 0.
- Layout: the image is measured at 360 px; `onSizeChanged` runs during the layout phase and **writes** `imageHeightPx = 360`. That state was already read in this frame's composition, so a **recomposition is scheduled**.
- Drawing: the text is drawn with 0 padding, so it sits **on top of the image, not below it**. The user sees this wrong intermediate picture.

**Frame 2.**
- Recomposition: `imageHeightPx = 360` → top padding = 360 px converted to dp.
- Layout: text is now placed below the image; `onSizeChanged` fires again with 360 (same value, so no further write and no third frame).
- Drawing: the text is correctly below the image.

**(a)** `toDp()` converts a **pixel** value, which only exists after measurement (layout phase), into a **Dp** that composition uses to build modifiers. That shows we are feeding a layout result back into composition: information flowing backwards.
**(b)** The three-step pattern: **(1)** a state is read in composition, **(2)** a later phase (layout, via `onSizeChanged` / `onGloballyPositioned`) **writes** that same state from a measured result, **(3)** that triggers another composition and layout for the next frame. Composition → layout → write to state → composition again.
**(c)** It breaks the **unidirectional flow of the phases** (Composition → Layout → Drawing, never backwards in one frame; "unidirectional data flow"). It is a *backwards write*, and costs an extra frame and a flicker.

### Question 13 — Reproduce the loop and fix it (measured)

`CyclicDemo` (`tag:Cycle`):

```
composition #1: imageHeightPx=0
onSizeChanged: height=360
composition #2: imageHeightPx=360
```

Two compositions, with values 0 and then 360 (120 dp × 3.0 = 360 px). `CyclicFixed`:

```
fixed: composition #1
```

One composition, one frame. The **`Column`** is the single source of truth: it measures the purple box, learns its height inside its own layout pass, and places the text right below it in that same pass. The height is never copied into a state, so nothing has to flow back into composition.

### Question 14 — A custom layout (measured)

`StackBelow` (three TODOs done): measure each child once with loose constraints, width = widest child, height = sum of heights + gaps, place each child below the previous one.

```
StackBelow with 2 children:    StackBelow with 3 children:
measure child                  measure child
measure child                  measure child
place children                 measure child
                               place children
```

So there are **n "measure child" lines (one per child) before one "place children"**: every child is measured exactly once, the cost grows by one measure per added child, and nothing is measured twice. That is the "one pass" rule from Question 2 in code. The screen looks like `CyclicFixed`, with an 8 dp gap, and with no state at all.
