pluginManagement {
    repositories { google(); mavenCentral() }
}
dependencyResolutionManagement {
    repositories { google(); mavenCentral() }
}
rootProject.name = "RuLearnDesktop"
include(":shared")
project(":shared").projectDir = file("../shared")
