import com.eaglesakura.armyknife.runtime.extensions.encodeMD5
import java.nio.charset.Charset

buildscript {
    repositories {
        maven(url = "https://dl.bintray.com/eaglesakura/maven/")
    }
    dependencies {
        classpath("com.eaglesakura.armyknife.armyknife-runtime:armyknife-runtime:1.3.7")
    }
}

extra["artifact_name"] = project.name
extra["artifact_group"] = "com.eaglesakura.firearm.${extra["artifact_name"]}"
extra["bintray_user"] = "eaglesakura"
extra["bintray_labels"] = arrayOf("android", "kotlin")
extra["bintray_vcs_url"] = "https://github.com/eaglesakura/${project.name}"

/**
 * Auto configure.
 */
extra["artifact_version"] = System.getenv("CIRCLE_TAG").let { CIRCLE_TAG ->
    val majorMinor = if (CIRCLE_TAG.isNullOrEmpty()) {
        rootProject.extra["base_version"] as String
    } else {
        return@let CIRCLE_TAG.substring(CIRCLE_TAG.indexOf('v') + 1)
    }

    val buildNumberFile = rootProject.file(".configs/secrets/build-number.env")
    if (buildNumberFile.isFile) {
        return@let "$majorMinor.build-${buildNumberFile.readText(Charset.forName("UTF-8"))}"
    }

    return@let when {
        hasProperty("install_snapshot") -> "$majorMinor.99999"
        System.getenv("CIRCLE_BUILD_NUM") != null -> "$majorMinor.build-${System.getenv("CIRCLE_BUILD_NUM")}"
        else -> "$majorMinor.snapshot"
    }
}.trim()


val gradleHashFileText = listOf(
    rootProject.projectDir
        .listFiles()!!
        .filter { it.isFile }
        .filter { it.name.contains(".gradle") },
    rootProject.projectDir
        .listFiles()!!
        .filter { it.isDirectory }
        .map { it.listFiles()!!.toList() }
        .flatten()
        .filter { it.name.contains(".gradle") }
).asSequence().flatten()
    .filter {
        it.isFile && it.name.contains(".gradle")
    }
    .toSet()
    .toList()
    .sortedWith { a, b -> a.absolutePath.compareTo(b.absolutePath) }.toList()
    .let { fileList ->
        val root = rootProject.rootDir
        buildString {
            fileList
                .map { it to it.readText() }
                .map { it.first to it.second.replace("\n", "").replace("\r", "") }
                .forEach { (file, text) ->
                    append(text.toByteArray().encodeMD5())
                    append(" : ")
                    append(
                        file.canonicalPath.substring(root.canonicalPath.length)
                            .replace("\\", "/")
                    )
                    append("\n")
                }
        }
    }
    .also { hashText ->
        val dst = rootProject.file(".circleci/hash-gradle")
        dst.delete()
        dst.writeText(hashText)
    }

println("====================================== Project Configuration ==============================================")
println("project          : ${project.name}")
println("artifact         : ${project.extra["artifact_version"]}")
println("===========================================================================================================")
