plugins {
    java
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.24"
    id("com.gradleup.shadow") version "9.6.1"
}

group = "com.loohp"
version = "cbt-1.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    exclusiveContent {
        forRepository { maven("https://repo.loohpjames.com/repository") }
        filter { includeGroup("com.loohp") }
    }
    exclusiveContent {
        forRepository { maven("https://repo.extendedclip.com/content/repositories/placeholderapi/") }
        filter { includeGroup("me.clip") }
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

sourceSets.main {
    java.setSrcDirs(listOf("abstraction/src/main/java", "V26_3/src/main/java", "common/src/main/java"))
    resources.setSrcDirs(listOf("common/src/main/resources"))
}

dependencies {
    paperweight.paperDevBundle("26.3.build.235-stable")
    implementation("com.loohp:PlatformScheduler:1.0.0")
    implementation("com.loohp:YamlConfiguration:1.0.1.4")
    implementation("com.googlecode.json-simple:json-simple:1.1.1")
    implementation("org.awaitility:awaitility:4.2.1")
    implementation("org.apache.commons:commons-text:1.11.0")
    implementation("commons-lang:commons-lang:2.6")
    implementation("org.apache.commons:commons-compress:1.21")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly(files("common/lib/LightAPI-fork-3.5.2.jar"))
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.xerial:sqlite-jdbc:3.51.3.0")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("plugin.yml") {
        filter { it.replace("\u0024{project.version}", project.version.toString()) }
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
    mergeServiceFiles()
    listOf("com.loohp.platformscheduler", "com.loohp.yamlconfiguration", "org.apache.commons", "org.awaitility", "org.json.simple").forEach {
        relocate(it, "com.loohp.interactionvisualizer.libs.$it")
    }
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
}
tasks.jar { archiveClassifier.set("plain") }
tasks.assemble { dependsOn(tasks.shadowJar) }
tasks.test { useJUnitPlatform() }

// Collect the deployable artifact without copying plain/intermediate jars.
val collectPluginArtifact = tasks.register<Copy>("collectPluginArtifact") {
    val artifact = tasks.named<org.gradle.api.tasks.bundling.AbstractArchiveTask>("shadowJar")
    dependsOn(artifact)
    from(artifact.flatMap { it.archiveFile })
    into(layout.projectDirectory.dir("../core/plugin"))
}
tasks.named("shadowJar") { finalizedBy(collectPluginArtifact) }