plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "app.depenses"
    compileSdk = 34

    defaultConfig {
        applicationId = "app.depenses"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    // Clé de signature fixe (décodée depuis debug.keystore.b64 par le workflow) : sans elle,
    // chaque build GitHub est signé par une clé différente et Android refuse la mise à jour
    // par-dessus l'app installée, ce qui oblige à désinstaller (et perdre les données).
    val sharedKeystore = file("debug.keystore")
    signingConfigs {
        if (sharedKeystore.exists()) {
            create("shared") {
                storeFile = sharedKeystore
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        debug {
            if (sharedKeystore.exists()) signingConfig = signingConfigs.getByName("shared")
        }
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    // L'interface est la même que la version web : le dossier /web du dépôt
    // est embarqué tel quel dans l'application (une seule source de vérité).
    sourceSets {
        getByName("main") {
            assets.srcDirs("../../web")
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.webkit:webkit:1.11.0")

    testImplementation("junit:junit:4.13.2")
}
