plugins {
    java
    id("org.springframework.boot") version "3.5.8"
    id("io.spring.dependency-management") version "1.1.7"
    id("io.freefair.lombok") version "8.11"
    id("org.liquibase.gradle") version "2.2.0"
}

group = "ru.analytics"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
    liquibaseRuntime {
        extendsFrom(configurations.runtimeClasspath.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-cache")

    // Кэш
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

    // База данных
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.liquibase:liquibase-core")

    // QueryDSL - правильная конфигурация для Spring Boot 3.x
    implementation("com.querydsl:querydsl-jpa:5.1.0:jakarta")
    annotationProcessor("com.querydsl:querydsl-apt:5.1.0:jakarta")
    annotationProcessor("jakarta.annotation:jakarta.annotation-api")
    annotationProcessor("jakarta.persistence:jakarta.persistence-api")

    // Утилиты
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("org.apache.commons:commons-lang3:3.17.0")
    implementation("com.google.guava:guava:33.4.0-jre")
    implementation("org.hibernate:hibernate-jpamodelgen:6.5.2.Final")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.core:jackson-core")
    implementation("com.fasterxml.jackson.core:jackson-annotations")

    //Мониторинг и метрики
    implementation("io.micrometer:micrometer-core")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.micrometer:micrometer-tracing-bridge-brave")
    implementation("io.zipkin.reporter2:zipkin-reporter-brave")

    // Retry
    implementation("org.springframework.retry:spring-retry")
    implementation("org.aspectj:aspectjweaver")

    // OpenAPI/Swagger документация
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.6")

    // Тестирование
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.awaitility:awaitility:4.2.2")
    testImplementation("org.mockito:mockito-core")
    testImplementation("com.h2database:h2")

    // Зависимости для Liquibase runtime (для плагина)
    liquibaseRuntime("org.liquibase:liquibase-core")
    liquibaseRuntime("org.postgresql:postgresql")
    liquibaseRuntime("info.picocli:picocli:4.7.5")
}

liquibase {
    activities {
        create("main") {
            this.arguments = mapOf(
                "changelogFile" to "src/main/resources/db/changelog/db.changelog-master.yaml",
                "url" to "jdbc:postgresql://localhost:5432/fintech_analytics",
                "username" to "postgres",
                "password" to "postgres",
                "driver" to "org.postgresql.Driver",
                "classpath" to sourceSets.main.get().runtimeClasspath.asPath
            )
        }
    }
}

// QueryDSL конфигурация - более простая версия
val querydslDir = "build/generated/querydsl"

tasks.withType<JavaCompile> {
    options.generatedSourceOutputDirectory.set(file(querydslDir))
    options.compilerArgs.addAll(listOf("-parameters", "-Xlint:unchecked"))
}

// Добавляем сгенерированные исходники в sourceSets
sourceSets {
    main {
        java {
            srcDirs(querydslDir)
        }
    }
}

// Очистка сгенерированных файлов QueryDSL
tasks.clean {
    doFirst {
        delete(querydslDir)
    }
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}

// Для Spring Boot 3.x нужно также убедиться, что annotationProcessor найдет пути
tasks.compileJava {
    dependsOn(tasks.processResources)
}

//// Если всё равно есть проблемы, можно попробовать такую конфигурацию:
//tasks.named("compileJava") {
//    // Генерация Q-классов QueryDSL
//    options.annotationProcessorGeneratedSourcesDirectory = file(querydslDir)
//}