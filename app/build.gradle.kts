plugins { id("com.android.application") }

android {
    namespace = "dev.enginehost.plugin.rpgmaker.web"
    compileSdk = 36
    androidResources {
        // Enginehost attaches this APK's resources to the host's own
        // Resources object and refuses a bundle compiled at 0x7f, the
        // host's own id, because the host's table would win every lookup.
        additionalParameters += listOf("--package-id", "0x80", "--allow-reserved-package-id")
    }

    defaultConfig {
        applicationId = "dev.enginehost.plugin.rpgmaker.web.v1.slot1"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
