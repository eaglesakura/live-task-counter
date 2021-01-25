# What is this repository?

Library for android applications with Kotlin.

armyknife is small library, but it can be more small.
If you have to shrink to application, then proguard-options set to enable.

# how to implementation into your project

```groovy
// /build.gradle
allprojects {
    repositories {
        // add the below line into build.gradle.
        maven { url 'https://dl.bintray.com/eaglesakura/maven/' }
    }
}

// /app/build.gradle
dependencies {
    // check versions
    // https://github.com/eaglesakura/armyknife-jetpack/releases
    implementation 'com.eaglesakura.armyknife:armyknife-jetpack:${replace version}'

    // with jetpack libraries...
    implementation 'androidx.annotation:annotation:1.1.0'
    implementation 'androidx.appcompat:appcompat:1.0.0'
}
```

## Dev / LocalInstall

```sh
./gradlew -Pinstall_snapshot build uploadArchives
```

```groovy
repositories {
    mavenLocal()
}

// replace version("major.minor.99999")
implementation 'com.eaglesakura.armyknife:armyknife-runtime:${replace version}'
```
