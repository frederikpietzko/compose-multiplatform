# Spring hydration example

Small Compose Multiplatform example using Spring Boot for server-side rendering and Kotlin/JS for client-side hydration.

## Run

From the `html` directory, start both processes in separate terminals.

```bash
./gradlew :examples:hydration-spring:jsBrowserDevelopmentRun
```

This starts the JavaScript development server at <http://localhost:8081>.

```bash
./gradlew :examples:hydration-spring:jvmRun
```

This starts the Spring Boot application at <http://localhost:8080>.

Open <http://localhost:8080> in a browser. Keep both Gradle tasks running while developing; stop them with `Ctrl+C`.
