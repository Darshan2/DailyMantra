---
name: unit-testing
description: Write or review JVM/Android unit tests in this repo (DailyMantra). Always use this skill whenever the user asks to "write a test", "add unit tests", "test this class/interceptor/repository/viewmodel/datastore", "cover this with tests", or is about to create or edit a file under a `src/test/` source set — even when the user doesn't say "test" explicitly, e.g. "make sure X is covered" or "can you verify this behaves correctly before I ship it". Enforces this project's conventions — JUnit4 + Truth assertions, fakes over mocks (MockK only as a last resort), and correct coroutine/Flow testing patterns.
---

# Unit Testing Conventions

This repo already has an established test style — see `common/core/src/test/java/com/get/dailymantra/common/core/data/network/interceptors/IdempotencyInterceptorTest.kt`. Match it rather than inventing a new one.

## Why these conventions

- **JUnit4 + Truth**: the project standardized on these (`libs.junit`, `libs.truth` in `gradle/libs.versions.toml`). Truth's fluent chains (`assertThat(x).isEqualTo(y)`) read closer to plain English and give better failure messages than raw JUnit asserts.
- **Fakes over mocks**: the project has no Mockito/MockK dependency today. A handwritten fake actually executes code paths (a fake repository really stores and returns what you put in it), so it catches bugs a mock configured with `when(...).thenReturn(...)` would hide by construction. Reach for a mocking library only when a fake is genuinely impractical (see below) — don't add one as a default habit.

## Test structure & style

- Class name: `<ClassUnderTest>Test`, package mirrors the source file's package, lives under the same module's `src/test/java/...`.
- Test method names are backtick-quoted, descriptive sentences of the behavior being verified — not camelCase method names. Example: `` `POST without existing header gets a Request-Id assigned` ``. The name should make the assertion below it almost redundant to explain.
- Body follows arrange → act → assert, separated by blank lines. No `// Arrange` / `// Act` comments — the blank lines and descriptive test name already carry that structure.
- Use `@Before fun setUp()` / `@After fun tearDown()` only when there's real setup/teardown to do (starting a `MockWebServer`, resetting shared state) — not as boilerplate on every test class.
- Extract repeated fixture construction into small private helper functions in the test class (see `jsonBody()` and `execute()` in the reference file) instead of repeating builder chains in every test.
- One behavior per test. Prefer several small, named tests over one large test asserting many unrelated things — a failure should point at exactly what broke.

## Assertions: Truth, always

- `import com.google.common.truth.Truth.assertThat`
- Use fluent chains: `.isEqualTo(...)`, `.isNotNull()`, `.isNull()`, `.isTrue()`/`.isFalse()`, `.contains(...)`, `.hasSize(...)`, `.isNotEqualTo(...)`.
- Never mix in `org.junit.Assert.assertEquals`/`assertTrue`/etc. in the same codebase — pick Truth every time so failure output stays consistent.
- If the module's `build.gradle.kts` doesn't yet have `testImplementation(libs.truth)`, add it — `common/core/build.gradle.kts` is the reference wiring.

## Fakes over mocks

For a dependency that's an interface (repository, data source, token provider, dispatcher provider, etc.), write a small `Fake<Interface>` class implementing it with controllable in-memory state — e.g. a `var itemsToReturn`, a `var shouldThrow: Throwable?`, or a `val recordedCalls = mutableListOf<...>()` if you need to assert on invocations. This is usually *less* code than configuring a mock, and it reads as plain Kotlin instead of mock-framework DSL.

- **A fake's package must mirror the package of the class/interface it fakes, exactly** — same relative path, just swapped from the `src/main/java/...` source set to `src/test/java/...` (or `src/androidTest/java/...` if the original is only reachable from an instrumented test). Find the original file first, then place the fake at the identical package path.
  - Example: `TokenProvider` lives at `common/core/src/main/java/com/get/dailymantra/common/core/data/network/TokenProvider.kt`, so `FakeTokenProvider` belongs at `common/core/src/test/java/com/get/dailymantra/common/core/data/network/FakeTokenProvider.kt`.
  - Do **not** drop it into whichever test class's folder happens to consume it first (e.g. `.../data/network/interceptors/FakeTokenProvider.kt` just because an interceptor test living there needed it first) — that's the exact drift this rule prevents.
  - Why: mirroring makes location predictable from the main-source tree alone, with no need to grep for where a fake ended up, and it stops fakes piling up in whatever folder wrote the first test that needed them. It also means a fake is exactly as reusable across the module as the class it fakes is.
- Keep fakes minimal: implement only what the interface requires and what the scenarios actually exercise. Don't build a fully general reusable test double up front — add capabilities when a new test needs them.
- The reference test's use of a real `MockWebServer` instead of mocking `OkHttpClient`/`Call` is the same instinct applied to a library boundary: exercise a real (or realistic lightweight) collaborator rather than scripting mock expectations.

### When a fake genuinely isn't practical

Try a fake first, including for call-count/argument assertions — a fake can record calls into a list just as well as a mock can verify them. Only fall back to a mocking library when the dependency is a large third-party interface where a fake would mean stubbing out many unrelated methods just to satisfy the compiler.

In that case, use **MockK** (idiomatic Kotlin DSL, avoids Mockito's Java-shaped `when(...).thenReturn(...)` syntax). It isn't a dependency yet — add it to `gradle/libs.versions.toml` under `[versions]`/`[libraries]` following the existing formatting, then `testImplementation(libs.mockk)` in the module needing it. Keep it scoped to the test that actually needs it rather than pulling it into every module preemptively.

## Coroutines and Flow

- Suspend functions: wrap the test body in `kotlinx.coroutines.test.runTest { ... }`. `kotlinx-coroutines-test` is already a test dependency in `common/core`.
- Never use `runBlocking` where `runTest` applies — `runTest` skips real delays and gives virtual-time control, so tests stay fast and deterministic.
- If the class under test takes a dispatcher, inject a `TestDispatcher` (`StandardTestDispatcher()` / `UnconfinedTestDispatcher()`) or a fake dispatcher-provider rather than letting it default to `Dispatchers.IO`/`Dispatchers.Main`.
- For `Flow`-returning APIs (repository observers, DataStore-backed flows, DAOs), assert emissions with **Turbine** rather than manually collecting into a list:
  ```kotlin
  flow.test {
      assertThat(awaitItem()).isEqualTo(expected)
      awaitComplete()
  }
  ```
  Turbine isn't a dependency yet — add `app.cash.turbine:turbine` to the catalog and `testImplementation(libs.turbine)` when a test first needs it.

## Before writing the test: dependency checklist

1. Confirm the module's `build.gradle.kts` has `testImplementation(libs.junit)` and `testImplementation(libs.truth)` — check `common/core/build.gradle.kts` if unsure what wiring looks like.
2. If the test needs coroutines, confirm `testImplementation(libs.kotlinx.coroutines.test)`.
3. Only add `mockk` or `turbine` to `gradle/libs.versions.toml` and the module's dependencies at the point a test actually needs them — don't add test dependencies speculatively.
4. Before creating a new `Fake<X>`, locate `X`'s source file and note its package. Create the fake at that same package path under `src/test/java/...` (or `src/androidTest/java/...`) — never under the folder of whatever test happens to need it first.

## Reference example

`common/core/src/test/java/com/get/dailymantra/common/core/data/network/interceptors/IdempotencyInterceptorTest.kt` is the canonical example: backtick test names, Truth assertions throughout, a real `MockWebServer` collaborator instead of a mocked `OkHttpClient`, and small private helpers (`jsonBody()`, `execute()`) for fixture setup. When in doubt about formatting or structure, match that file.
