import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// IDs do AdMob e endereço do servidor (Worker) da IA: em local.properties ou ~/.gradle/gradle.properties
// (veja o README). Sem eles, a build usa os anúncios de teste do Google e fica sem IA.
val TEST_ADMOB_APP = "ca-app-pub-3940256099942544~3347511713"
val TEST_BANNER = "ca-app-pub-3940256099942544/9214589741"
val TEST_NATIVE = "ca-app-pub-3940256099942544/2247696110"
val TEST_REWARDED = "ca-app-pub-3940256099942544/5224354917"
val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
fun cfg(name: String, default: String): String = localProps.getProperty(name) ?: providers.gradleProperty(name).orNull ?: default
// Chave de envio da Play Store: android/keystore.properties (fora do git) aponta para o .jks e as senhas.
// Sem o arquivo, a release é assinada com a chave de debug (só para instalar e testar).
val uploadProps = Properties().apply {
    rootProject.file("keystore.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
fun q(s: String) = "\"" + s.replace("\"", "\\\"") + "\""

android {
    namespace = "app.ficha"
    compileSdk = 37

    defaultConfig {
        // Identificador na Play Store (permanente). O código continua no pacote app.ficha.
        applicationId = "io.github.drakrosmann.ficha"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        manifestPlaceholders["admobAppId"] = cfg("ficha.admob.app", TEST_ADMOB_APP)
        buildConfigField("String", "AD_BANNER", q(cfg("ficha.admob.banner", TEST_BANNER)))
        buildConfigField("String", "AD_NATIVE", q(cfg("ficha.admob.native", TEST_NATIVE)))
        buildConfigField("String", "AD_REWARDED", q(cfg("ficha.admob.rewarded", TEST_REWARDED)))
        buildConfigField("String", "SERVER", q(cfg("ficha.server", "")))
    }

    signingConfigs {
        if (uploadProps.getProperty("storeFile") != null) create("upload") {
            storeFile = file(uploadProps.getProperty("storeFile"))
            storePassword = uploadProps.getProperty("storePassword")
            keyAlias = uploadProps.getProperty("keyAlias")
            keyPassword = uploadProps.getProperty("keyPassword")
        }
    }

    buildTypes {
        debug {
            // Debug: sempre anúncios de teste do Google e o servidor local (adb reverse tcp:8787 tcp:8787)
            manifestPlaceholders["admobAppId"] = TEST_ADMOB_APP
            buildConfigField("String", "AD_BANNER", q(TEST_BANNER))
            buildConfigField("String", "AD_NATIVE", q(TEST_NATIVE))
            buildConfigField("String", "AD_REWARDED", q(TEST_REWARDED))
            buildConfigField("String", "SERVER", q(cfg("ficha.server.debug", "http://localhost:8787")))
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Com keystore.properties: chave de envio da Play Store. Sem ele: chave de debug (instalar e testar).
            signingConfig = signingConfigs.findByName("upload") ?: signingConfigs.getByName("debug")
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
    androidResources {
        // As fotos já estão em WebP: comprimir de novo só deixaria o app mais lento para abrir
        noCompress += "webp"
    }
}

// Fotos dos exercícios: as mesmas do PWA (pasta img/ na raiz do repositório), copiadas para os assets no build
abstract class ExerciseImagesTask : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val source: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Inject
    abstract val fs: FileSystemOperations

    @TaskAction
    fun copy() {
        fs.sync {
            from(source)
            into(outputDir.dir("img"))
        }
    }
}

val exerciseImages = tasks.register<ExerciseImagesTask>("exerciseImages") {
    source.set(layout.projectDirectory.dir("../../img"))
}

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(exerciseImages, ExerciseImagesTask::outputDir)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.material.color.utilities)
    implementation(libs.anthropic.java)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.play.services.code.scanner)
    implementation(libs.androidx.exifinterface)
    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)
    implementation(libs.billing.ktx)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

kotlin {
    compilerOptions {
        optIn.addAll(
            "kotlinx.serialization.ExperimentalSerializationApi",
            "androidx.compose.material3.ExperimentalMaterial3Api",
            "androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
            "androidx.compose.foundation.ExperimentalFoundationApi",
            "androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "androidx.compose.animation.ExperimentalAnimationApi",
            "androidx.compose.ui.text.ExperimentalTextApi",
        )
    }
}
