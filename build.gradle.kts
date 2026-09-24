plugins {
    base
}

allprojects {
    group = "com.romeo"
    version = "1.0.0"
}

subprojects {
    apply(plugin = "java")

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }
}
