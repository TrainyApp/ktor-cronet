import com.vanniktech.maven.publish.AndroidMultiVariantLibrary
import org.jetbrains.dokka.gradle.workers.ProcessIsolation
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.*

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.dokka)
    alias(libs.plugins.maven.publish)
}

group = "app.trainy"
version = "1.0"

dependencies {
    api(libs.ktor.client.core)
    api(libs.play.services.cronet)
    compileOnly(libs.cronet.fallback)

    androidTestImplementation(libs.ktor.client.content.negotiation)
    androidTestImplementation(libs.ktor.serialization.kotlinx.json)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestRuntimeOnly(libs.androidx.test.runner)
    androidTestImplementation(kotlin("test-junit"))

    dokkaPlugin(libs.dokka.android.documentation.plugin)
}

kotlin {
    explicitApi()
}

android {
    namespace = "com.trainyapp.cronet"
    compileSdk = 36

    defaultConfig {
        minSdk = 21
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_1_8
    }
}

dokka {
    // Dokka runs out of memory with the default maxHeapSize when ProcessIsolation is used
    (dokkaGeneratorIsolation.get() as? ProcessIsolation)?.maxHeapSize = "1g"

    dokkaSourceSets.configureEach {
        sourceLink {
            localDirectory = project.projectDir
            remoteUrl("https://github.com/trainyapp/ktor-cronet/blob/main")
            remoteLineSuffix = "#L"
        }
    }
}

publishing {
    publications {
        withType<MavenPublication> {
            android.buildTypes.forEach { type ->
                val name =
                    type.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
                val dokkaJar = tasks.register<Jar>("dokka${name}Jar") {
                    archiveClassifier = "${type.name}-javadoc"
                    from(tasks.dokkaGeneratePublicationHtml)
                }

                artifact(dokkaJar)
            }

        }
    }
}

mavenPublishing {
    configure(
        AndroidMultiVariantLibrary(
            publishJavadocJar = false,
            sourcesJar = true
        )
    )
    coordinates("com.trainyapp", "ktor-cronet")
    publishToMavenCentral(automaticRelease = true)

    signAllPublications()

    pom {
        name = "ktor-cronet"
        description = "Ktor engine implementation using Cronet"
        url = "https://github.com/trainyapp/ktor-cronet"

        organization {
            name = "Trainy"
            url = "https://github.com/trainyapp"
        }

        developers {
            developer {
                name = "Michael Rittmeister"
            }
        }

        issueManagement {
            system = "GitHub"
            url = "https://github.com/trainyapp/ktor-cronet/issues"
        }

        licenses {
            license {
                name = "MIT"
                url = "https://opensource.org/licenses/MIT"
            }
        }

        scm {
            connection = "scm:git:ssh://github.com/trainyapp/ktor-cronet.git"
            developerConnection = "scm:git:ssh://git@github.com:rainyapp/ktor-cronet.git"
            url = "https://github.com/trainyapp/ktor-cronet"
        }
    }
}
