plugins {
    kotlin("jvm") version "2.3.10"
    id("com.vanniktech.maven.publish") version "0.31.0"
}

group = "com.mameli"
version = "1.0.0"

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.4.3"))
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-beans")
    implementation("org.springframework:spring-tx")
    implementation("org.springframework.data:spring-data-jpa")
    implementation("jakarta.persistence:jakarta.persistence-api")
    implementation("org.hibernate.orm:hibernate-core")
    implementation("org.slf4j:slf4j-api")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.h2database:h2")
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()
    coordinates(group.toString(), rootProject.name, version.toString())
    pom {
        name = "Spring JPA Listener"
        description = "Automatic entity change detection and domain events for Spring Data JPA"
        inceptionYear = "2026"
        url = "https://github.com/renatomameli/spring-jpa-listener"
        licenses {
            license {
                name = "MIT License"
                url = "https://opensource.org/licenses/MIT"
            }
        }
        developers {
            developer {
                id = "renatomameli"
                name = "Renato Mameli"
                url = "https://github.com/renatomameli"
                email = "renatomamel410@gmail.com"
                organization = "renatomameli"
                organizationUrl = "https://github.com/renatomameli"
            }
        }
        scm {
            url = "https://github.com/renatomameli/spring-jpa-listener"
            connection = "scm:git:git://github.com/renatomameli/spring-jpa-listener.git"
            developerConnection = "scm:git:ssh://git@github.com/renatomameli/spring-jpa-listener.git"
        }
    }
}
