# What is this repository?

`LiveTaskCounter` is Execute(async Task) count Library.

# Example.

```kotlin

private val counter = LiveTaskCounter()

// ProgressUI VISIBLE(or INVISIBLE)
val progressVisibility: LiveData<Int> = 
        Transformations.map(counter) { snapshot ->
            if(snapshot.empty) {
                // Not Progress.
                View.INVISIBLE
            } else {
                // Progress now.
                View.VISIBLE
            }
        }


suspend fun foo() {
    counter.withCount {
        // do heavy something
    }
}
suspend fun bar() {
    counter.withCount {
        // do heavy something
    }
}
```

# How to Install.

```groovy
// /app/build.gradle
dependencies {
    // check versions
    implementation 'com.eaglesakura.live-task-counter:live-task-counter:${replace version}'
}
```
