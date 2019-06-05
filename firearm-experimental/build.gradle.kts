apply(from = "../dsl/android-library.gradle")
apply(from = "../dsl/ktlint.gradle")
apply(from = "../dsl/bintray.gradle")

dependencies {
    /**
     * Kotlin support
     */
    "api"("com.eaglesakura.armyknife.armyknife-jetpack:armyknife-jetpack:1.3.0")
}