plugins {
    `java-library`
    application
}

dependencies {
    api(project(":slot-engine"))
    implementation("org.slf4j:slf4j-api:2.0.17")
    testImplementation("org.slf4j:slf4j-simple:2.0.17")
}

application {
    mainClass.set("com.slotengine.math.cli.MathCli")
}
