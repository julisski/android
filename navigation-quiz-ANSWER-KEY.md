# Navigation Quiz — Answer Key (Detailed)

Covers the six teaching projects: **NavDetailList, NavThreeScreen, NavFourScreen, NavViewModelState, NavDataLayer**.
All use **Navigation 3 (Nav3)** — a single Activity holds a back stack of `NavKey` objects, and `NavDisplay` swaps the screen whenever the top key changes. There is **no** `NavController`, `NavHost`, `navigate("route")`, or XML nav graph in any of these projects.

> Note: Blackboard randomizes option order on screen, so the A–E letters below refer to the order in the source `.txt` file. Match by answer text, not by letter.

---

## Question 1 — What renders the top of the back stack?

**Correct: NavDisplay**

`NavDisplay` is the Nav3 composable that reads the back stack, draws whichever key is currently on top, and animates the transition when the top changes. In every project it appears as `NavDisplay(backStack = backStack, onBack = ..., entryProvider = ...)`.

**Why the others are wrong:**
- **NavHost** — This is the *Navigation Compose (Nav2)* host composable, not Nav3. These projects deliberately do not use it; it would require a `NavController` and string routes.
- **Scaffold** — A Material 3 layout frame that supplies app-bar/inset structure and `innerPadding`. It wraps the UI but knows nothing about the back stack. In the code it merely hosts `AppNavigation`.
- **NavController** — The Nav2 object that *drives* navigation; it does not exist in Nav3. In Nav3 the back stack itself (a list of keys) plays that role.
- **LazyColumn** — A scrolling list container used *inside* a screen to render rows. It has nothing to do with switching between screens.

---

## Question 2 — Creating and seeding the back stack

**Correct: `rememberNavBackStack(CategoriesKey)`**

This Nav3 function builds the back stack, seeds it with the start key, and **remembers** it across recomposition and configuration changes (rotation). Pushing a key navigates forward; popping navigates back.

**Why the others are wrong:**
- **`remember { mutableStateOf(CategoriesKey) }`** — Holds a *single* key, not a stack, and survives recomposition but **not** rotation. You could never represent a multi-level back stack (`[Categories, Items, Detail]`) with one value.
- **`rememberNavController()`** — A Nav2 API. There is no `NavController` in Nav3, so this would not compile against these projects.
- **`rememberSaveable { CategoriesKey }`** — `rememberSaveable` saves a small Bundle-able value; it is not a back stack and offers no push/pop operations.
- **`NavBackStack(CategoriesKey)`** — Even if such a constructor existed, calling it raw (without `remember…`) would rebuild a fresh stack on every recomposition, losing all navigation history.

---

## Question 3 — `data object` key vs `data class` key

**Correct: DetailKey needs an itemId argument; the list screen needs none.**

A key both *names* a destination and *carries its arguments*. `CategoriesKey`/`ListKey` is a `data object` (a singleton) because there is only one such screen and it carries no data. `DetailKey(val itemId: Int)` and `ItemsKey(val categoryId: Int)` are `data class`es because each instance must carry a different argument — `DetailKey(1)` vs `DetailKey(5)` are distinct keys landing on the same entry block with different data.

**Why the others are wrong:**
- **"A data object cannot implement NavKey"** — False. `data object CategoriesKey : NavKey` does exactly that in the code. Both `data object` and `data class` can implement `NavKey`.
- **"A data class loads measurably faster"** — There is no performance difference; the choice is about whether the key needs to carry per-instance arguments, not speed.
- **"The first screen must always be a data object"** — Not a rule. NavDataLayer's first screen is `ListKey` (a data object) by choice, but a start destination could carry arguments and be a data class.
- **"Only a data class can be @Serializable"** — False. Both are marked `@Serializable` in the projects; `@Serializable data object CategoriesKey` is valid and required.

---

## Question 4 — What `backStack.add(DetailKey(id))` actually does

**Correct: It adds a key to the back stack for NavDisplay to match.**

This is "the jump." Tapping a row does **not** name or call a screen directly — it just appends a key. `NavDisplay` then matches that key's **type** to the matching `entry<…> { }` block and runs it. The `id` inside the key only chooses *which data* the screen shows, not *which* screen.

**Why the others are wrong:**
- **"It calls `navController.navigate(\"detail\")`"** — That is the Nav2 string-route API, which these projects do not use. There is no `NavController` here.
- **"It fires an Intent that starts a separate Activity"** — Nav3 is single-Activity. No Intents, no new Activities; the back stack is in-process Compose state.
- **"It directly invokes the DetailScreen composable by name"** — Screens are only ever invoked by `NavDisplay` via the matched `entry` block. The tap handler never calls `DetailScreen(...)` itself.
- **"It tears down the current NavDisplay and builds a replacement"** — `NavDisplay` is not recreated; it stays mounted and simply re-renders the new top key. The previous key remains in the stack (state preserved) underneath.

---

## Question 5 — NavFourScreen "Start over" (`while (backStack.size > 1) backStack.removeLastOrNull()`)

**Correct: All keys but the first pop, returning to the root.**

The loop pops keys until only the bottom one (`CategoriesKey`) remains, jumping the user all the way back to the first screen in one action — regardless of how deep they had drilled (Categories → Items → Detail → Fact).

**Why the others are wrong:**
- **"The app exits because the back stack is emptied"** — The condition is `size > 1`, so it stops at one key. The stack is never emptied, and the app does not exit.
- **"Only the single top key pops"** — That describes a plain `removeLastOrNull()` (one back step). The `while` loop pops *repeatedly*, so it does far more than one step.
- **"The back stack is duplicated, doubling every screen"** — `removeLastOrNull()` only removes; nothing here adds or copies keys.
- **"It navigates forward and opens a fifth screen"** — Removing keys is the opposite of navigating forward; no key is ever added.

---

## Question 6 — What `entry<DetailKey> { key -> DetailScreen(...) }` does

**Correct: It registers the screen for that key and passes the key in.**

Inside `entryProvider`, each `entry<KeyType> { }` is like a `case` in a switch: it **registers** which composable to show when that key type is on top. The body does **not** run at registration time — `NavDisplay` runs it only when a matching key reaches the top. The lambda receives the actual key instance so a data-class key can read its arguments (`key.itemId`).

**Why the others are wrong:**
- **"It renders DetailScreen immediately when the app launches"** — Registration is not execution. The block runs only when a `DetailKey` is on top — never at launch, where `CategoriesKey`/`ListKey` is on top.
- **"It permanently removes every DetailKey from the stack"** — `entry` registers a destination; it never mutates the back stack.
- **"It converts the DetailKey data class into a data object"** — `entry` does no type conversion; the key's declaration is fixed at compile time.
- **"It sets the system back-button behavior for every screen"** — Back behavior is defined separately by `NavDisplay`'s `onBack = { backStack.removeLastOrNull() }`, not by an `entry` block.

---

## Question 7 — Why keys carry only an id, not the whole object

**Correct: The id stays small and serializable; the screen looks it up.**

Keys must be `@Serializable` (so Nav3 can save/restore them across process death). Carrying just an `Int` id keeps the key tiny and trivially serializable. The screen then resolves the full object via a lookup — e.g. `itemById(key.itemId)` — treating the data layer/list as the source of truth.

**Why the others are wrong:**
- **"Nav3 keys are forbidden from holding properties"** — False; `DetailKey(val itemId: Int)` holds a property. The point is to keep that property *small and serializable*, not to ban properties.
- **"Passing the whole object is faster but drains battery"** — Not a battery issue; the real concerns are serialization size and keeping a single source of truth.
- **"The Item class cannot be referenced inside a composable"** — It is referenced constantly (e.g. `DetailScreen(item: Item)`). The screen just receives the resolved object, not the raw key.
- **"The id is the only field Item contains"** — Item has several fields (`title`, `blurb`, `categoryId`, and in some projects `fact`). The id is chosen *because* it is a compact handle, not because it is the only field.

---

## Question 8 — Why ViewModel state survives rotation but `remember {}` does not

**Correct: It is scoped to the Activity and re-attached after recreation.**

A `ViewModel` is stored in a `ViewModelStore` owned by a `ViewModelStoreOwner` (here the Activity). During a configuration change Android hands the *same* store to the recreated Activity, so `viewModel()` returns the **same** `FavoritesViewModel` instance with its favorites set intact. A `remember {}` value lives only in the composition, which rotation throws away — so it resets.

**Why the others are wrong:**
- **"`remember` clears its value every few seconds"** — `remember` has no timer; it persists across recomposition and is lost only when the composition leaves (e.g. rotation), not on a clock.
- **"The ViewModel writes each value to a remote server"** — No network is involved. The plain in-memory ViewModel survives rotation purely because of scoping. (Process death would additionally need `SavedStateHandle`, as the code comments note.)
- **"Rotation never recreates the Activity"** — It does by default; recreation is exactly the problem the ViewModel solves.
- **"A ViewModel keeps its data inside the serialized nav key"** — It does not. Favorites live in the ViewModel; nav keys carry only ids. These are separate state owners (the comments explicitly list what lives where).

---

## Question 9 — How the Detail screen changes a favorite (unidirectional data flow)

**Correct: It calls `viewModel.toggleFavorite(id)`; state flows back down.**

UDF: **events flow up, state flows down.** The button's `onToggleFavorite` calls `toggleFavorite(item.id)` on the shared ViewModel. The ViewModel atomically updates its private `MutableStateFlow`; the new value emits through the read-only `StateFlow`, which the screens observe via `collectAsStateWithLifecycle()`, triggering recomposition. The composable never owns or mutates the data itself.

**Why the others are wrong:**
- **"The Detail composable mutates the favorites Set directly"** — This breaks UDF and is exactly what the project forbids. `_favorites` is private to the ViewModel; the UI only gets a read-only `StateFlow` and must go through `toggleFavorite`.
- **"It writes the change into a `rememberSaveable` variable"** — `rememberSaveable` is used in this screen only for a *local contrast counter*. It is per-composable and could not be shared with the Items list, which also shows the star.
- **"It edits the static `sampleItems` list"** — `sampleItems` is fixed sample data and holds no favorite flag. Favorites are kept as a separate `Set<Int>` of ids in the ViewModel.
- **"It pushes a new key onto the back stack"** — Toggling a favorite is a *state change*, not navigation. No key is added; you stay on the Detail screen.

---

## Question 10 — NavDataLayer's sealed `PlanetsUiState`

**Correct: Loading, Empty, Error, and Success.**

`PlanetsUiState` is a `sealed interface` with exactly these four subtypes. Because the set is closed, the screen's `when (uiState)` is **exhaustive** — the compiler forces handling of every case, so there is no forgotten spinner or blank screen. `Loading` shows a spinner, `Empty` a friendly note, `Error(message)` a message + Retry button, `Success(planets)` the list.

**Why the others are wrong:**
- **"Start, Running, Paused, Stopped"** — Resembles a generic state machine / thread states, not this UI model.
- **"Foreground, Background, Created, Destroyed"** — These are Android *lifecycle*-flavored terms, a different concept from screen UI state.
- **"Push, Pop, Peek, Clear"** — These are *stack operations* (relevant to the back stack), not UI states.
- **"Idle, Tap, Drag, Release"** — These are *gesture/input* phases, unrelated to data-loading state.

---

## Quick answer summary

| # | Correct answer | Project(s) emphasized |
|---|----------------|-----------------------|
| 1 | NavDisplay | all |
| 2 | `rememberNavBackStack(CategoriesKey)` | all |
| 3 | DetailKey needs an itemId argument; the list screen needs none | all |
| 4 | It adds a key to the back stack for NavDisplay to match | NavDetailList |
| 5 | All keys but the first pop, returning to the root | NavFourScreen |
| 6 | It registers the screen for that key and passes the key in | all |
| 7 | The id stays small and serializable; the screen looks it up | all |
| 8 | It is scoped to the Activity and re-attached after recreation | NavViewModelState |
| 9 | It calls `viewModel.toggleFavorite(id)`; state flows back down | NavViewModelState |
| 10 | Loading, Empty, Error, and Success | NavDataLayer |
