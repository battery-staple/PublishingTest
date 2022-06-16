import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("multiplatform") version "1.7.0"
    `maven-publish`
    id("com.palantir.git-version") version "0.12.3"
}


group = "com.batterystaple"

val gitVersion: groovy.lang.Closure<String> by extra

version = gitVersion()
println("version: $version")

repositories {
    mavenCentral()
}

val environment: MutableMap<String, String> = System.getenv() ?: error("Could not get environment")

afterEvaluate {
    extensions.findByType<PublishingExtension>()?.apply {
        repositories {
            maven {
                url = uri(
                    if (isReleaseBuild) {
                        "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
                    } else {
                        "https://s01.oss.sonatype.org/content/repositories/snapshots/"
                    }
                )
                credentials {
                    username = environment["SONATYPE_USERNAME"].toString()
                    password = environment["SONATYPE_PASSWORD"].toString()
                }
            }
        }

        publications.withType<MavenPublication>().configureEach {
            artifact(emptyJavadocJar.get())

            pom {
                name.set("publishingtest")
                description.set("A publishing test")
                url.set("https://github.com/battery-staple/PublishingTest")

                developers {
                    developer {
                        name.set("Rohen Giralt")
                        email.set("batterystapledev@gmail.com")
                        organization {
                            name.set("None")
                            url.set("https://github.com/battery-staple/")
                        }
                    }
                }

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://www.opensource.org/licenses/mit-license.php")
                        distribution.set("repo")
                    }
                }

                scm {
                    url.set("https://github.com/battery-staple/PublishingTest")
                }
            }
        }
    }

    extensions.findByType<SigningExtension>()?.apply {
        val publishing = extensions.findByType<PublishingExtension>() ?: return@apply
        val key = environment["SIGNING_KEY"]?.replace("\\n", "\n")
        val password = environment["SIGNING_PASSWORD"]

        useInMemoryPgpKeys(key, password)
        sign(publishing.publications)
    }

    tasks.withType<Sign>().configureEach {
        onlyIf { isReleaseBuild }
    }
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
        testRuns["test"].executionTask.configure {
            useJUnit()
        }
    }
    js(LEGACY) {
        browser {
            commonWebpackConfig {
                cssSupport.enabled = true
            }
        }
    }
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosX64("native")
        hostOs == "Linux" -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    
    sourceSets {
        val commonMain by getting
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jvmMain by getting
        val jvmTest by getting
        val jsMain by getting
        val jsTest by getting
        val nativeMain by getting
        val nativeTest by getting
    }
}

val emptyJavadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

val isReleaseBuild: Boolean
    get() = !(version as String).endsWith("SNAPSHOT")