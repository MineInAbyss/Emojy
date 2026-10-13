plugins {
    id("maven-publish")
    alias(miaLibs.plugins.mia.kotlin.jvm)
}

dependencies {
    compileOnly(libs.adventure.api)
    compileOnly(libs.adventure.minimessage)
    compileOnly(libs.gson)
    compileOnly(miaLibs.idofront.fonts)
}

publishing {
    repositories {
        maven {
            val repo = "https://repo.mineinabyss.com/"
            val isSnapshot = System.getenv("IS_SNAPSHOT") == "true"
            val url = if (isSnapshot) repo + "snapshots" else repo + "releases"
            setUrl(url)
            credentials {
                username = project.findProperty("mineinabyssMavenUsername") as String?
                password = project.findProperty("mineinabyssMavenPassword") as String?
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = rootProject.group.toString()
            artifactId = "${rootProject.name}-common"
            version = rootProject.version.toString()
        }
    }
}
