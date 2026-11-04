plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

kotlin {
  jvmToolchain(11)
}

android {
  namespace = "com.example"
  compileSdk = 37

  defaultConfig {
    applicationId = "com.aistudio.healthbridge.app"
    minSdk = 26
    targetSdk = 37
    versionCode = 1
    versionName = "1.0"

    buildConfigField("String", "MAPBOX_ACCESS_TOKEN", "\"pk.eyJ1IjoiZG9zZW1hdGUiLCJhIjoiY2x6c2FtdXBhMDAxbDJxb3FsaDRoNmp2biJ9.sample_token\"")
    buildConfigField("String", "GEMINI_API_KEY", "\"MY_GEMINI_API_KEY\"")

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    val customDebugKeystore = file("${rootDir}/debug.keystore")
    if (customDebugKeystore.exists()) {
      create("debugConfig") {
        storeFile = customDebugKeystore
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
      }
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      if (signingConfigs.findByName("debugConfig") != null) {
        signingConfig = signingConfigs.getByName("debugConfig")
      }
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }

  // The entire user interface is built with Kotlin and XML layouts.
  // Jetpack Compose is disabled: the Compose compiler is not applied, so no
  // composable code can be compiled into this project.
  buildFeatures {
    compose = false
    buildConfig = true
  }

  testOptions { unitTests { isIncludeAndroidResources = true } }

  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

dependencies {
  // ---------- Android XML view system ----------
  // These supply the widgets and containers used by every layout in res/layout.
  implementation("androidx.appcompat:appcompat:1.7.0")
  implementation("com.google.android.material:material:1.12.0")
  implementation("androidx.constraintlayout:constraintlayout:2.1.4")
  implementation("androidx.recyclerview:recyclerview:1.3.2")
  implementation("androidx.fragment:fragment-ktx:1.8.5")
  implementation("androidx.activity:activity-ktx:1.9.3")

  // ---------- Platform BOM ----------
  implementation(platform(libs.firebase.bom))

  // ---------- Core and lifecycle ----------
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
  implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.7")

  // ---------- Authentication ----------
  implementation(libs.androidx.credentials)
  implementation(libs.androidx.credentials.play.services)
  implementation(libs.googleid)
  implementation(libs.firebase.auth)

  // ---------- Data ----------
  implementation(libs.firebase.firestore)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.work.runtime.ktx)
  implementation(libs.androidx.health.connect)

  // ---------- Networking ----------
  implementation(libs.retrofit)
  implementation(libs.converter.moshi)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  implementation(libs.logging.interceptor)

  // ---------- AI assistant ----------
  implementation("com.google.ai.client.generativeai:generativeai:0.9.0")

  // ---------- Location and maps ----------
  implementation(libs.play.services.location)
  implementation(libs.mapbox.maps)
  implementation(libs.mapbox.compose)
  implementation(libs.mapbox.common)
  implementation(libs.mapbox.navigation.android)
  implementation(libs.mapbox.navigation.ui.maps)
  implementation(libs.mapbox.navigation.ui.components)
  implementation(libs.mapbox.navigation.voice)
  implementation(libs.mapbox.navigation.tripdata)
  implementation(libs.mapbox.search.android)
  implementation(libs.mapbox.search.ui)
  implementation(libs.mapbox.search.place.autocomplete)

  // ---------- Coroutines ----------
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)

  // ---------- Unit tests ----------
  testImplementation(libs.junit)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.androidx.core)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)

  // ---------- Instrumented tests ----------
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)

  // ---------- Annotation processing ----------
  "ksp"(libs.androidx.room.compiler)
}