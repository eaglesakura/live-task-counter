apply(from = "../dsl/android-library.gradle")
apply(from = "../dsl/ktlint.gradle")
apply(from = "../dsl/bintray.gradle")

dependencies {
    /**
     * Kotlin support
     */
    "api"("com.eaglesakura.armyknife.armyknife-runtime:armyknife-runtime:1.3.3")
    "api"("com.eaglesakura.armyknife.armyknife-jetpack:armyknife-jetpack:1.3.7")
    "compileOnly"("com.eaglesakura.armyknife.armyknife-jetpack-dependencies:armyknife-jetpack-dependencies:1.3.7")
    "testImplementation"("com.eaglesakura.armyknife.armyknife-jetpack-dependencies:armyknife-jetpack-dependencies:1.3.7")
    "androidTestImplementation"("com.eaglesakura.armyknife.armyknife-jetpack-dependencies:armyknife-jetpack-dependencies:1.3.7")
}