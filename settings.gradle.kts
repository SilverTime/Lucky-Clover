pluginManagement {
    repositories {
        // 优先使用国内镜像仓库解析 Gradle 插件
        maven {
            url = uri("https://maven.aliyun.com/repository/gradle-plugin")
        }
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "lucky-clover"