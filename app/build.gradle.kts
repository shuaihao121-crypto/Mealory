import java.util.Properties

plugins {
    id("com.android.application")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose")
}

fun loadProperties(file: File): Properties = Properties().apply {
    if (file.isFile) file.inputStream().use(::load)
}

fun String.asBuildConfigString(): String =
    "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""

val releaseMetadata = loadProperties(rootProject.file("release.properties"))
val signingPropertiesFile = rootProject.file("keystore.properties")
val signingProperties = loadProperties(signingPropertiesFile)
val releasePackageRequested = gradle.startParameter.taskNames.any { taskName ->
    taskName.contains("Release", ignoreCase = true) &&
        listOf("assemble", "bundle", "package").any { taskName.contains(it, ignoreCase = true) }
}

android {
    namespace = "com.mealory.app"
    compileSdk = 37
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = "com.mealory.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        create("release") {
            if (signingPropertiesFile.isFile) {
                storeFile = file(signingProperties.getProperty("storeFile"))
                storePassword = signingProperties.getProperty("storePassword")
                keyAlias = signingProperties.getProperty("keyAlias")
                keyPassword = signingProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "DEVELOPER_NAME", "Mealory 开发者".asBuildConfigString())
            buildConfigField("String", "SUPPORT_EMAIL", "support@example.invalid".asBuildConfigString())
            buildConfigField(
                "String",
                "PRIVACY_POLICY_URL",
                "https://example.invalid/mealory/privacy".asBuildConfigString(),
            )
        }
        release {
            val developerName = releaseMetadata.getProperty("developerName", "").trim()
            val supportEmail = releaseMetadata.getProperty("supportEmail", "").trim()
            val privacyPolicyUrl = releaseMetadata.getProperty("privacyPolicyUrl", "").trim()
            if (releasePackageRequested) {
                require(developerName.isNotBlank()) {
                    "release.properties must contain developerName"
                }
                require(supportEmail.matches(Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))) {
                    "release.properties must contain a valid supportEmail"
                }
                require(privacyPolicyUrl.startsWith("https://")) {
                    "release.properties must contain an HTTPS privacyPolicyUrl"
                }
                require(signingPropertiesFile.isFile) {
                    "keystore.properties is required for a Release package"
                }
                listOf("storeFile", "storePassword", "keyAlias", "keyPassword").forEach { key ->
                    require(!signingProperties.getProperty(key).isNullOrBlank()) {
                        "keystore.properties is missing $key"
                    }
                }
            }
            buildConfigField("String", "DEVELOPER_NAME", developerName.asBuildConfigString())
            buildConfigField("String", "SUPPORT_EMAIL", supportEmail.asBuildConfigString())
            buildConfigField("String", "PRIVACY_POLICY_URL", privacyPolicyUrl.asBuildConfigString())
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    val roomVersion = "2.8.5"

    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.datastore:datastore-preferences:1.3.0")
    implementation("androidx.exifinterface:exifinterface:1.4.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.navigation:navigation-compose:2.10.2")
    implementation("androidx.paging:paging-compose:3.5.3")
    implementation("androidx.paging:paging-runtime-ktx:3.5.3")
    implementation("androidx.room:room-ktx:$roomVersion")
    implementation("androidx.room:room-paging:$roomVersion")
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.work:work-runtime-ktx:2.12.0")
    // Coil 3.4+ is compiled against Kotlin newer than AGP 9.4's built-in Kotlin 2.2.10.
    //noinspection NewerVersionAvailable
    implementation("io.coil-kt.coil3:coil-compose:3.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")

    ksp("androidx.room:room-compiler:$roomVersion")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.room:room-testing:$roomVersion")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
}

