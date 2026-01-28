# Building with Gradle

This document describes how to build openpdfx using Gradle, as an alternative to Maven.

## Prerequisites

- Java Development Kit (JDK) 21 or later
- Gradle is not required to be installed - the Gradle Wrapper (`gradlew`) is included

## Quick Start

### Building the Project

To build all modules:

```bash
./gradlew build
```

To build without running tests:

```bash
./gradlew build -x test
```

To clean and build:

```bash
./gradlew clean build
```

### Building Individual Modules

You can build specific modules:

```bash
./gradlew :openpdf-core:build
./gradlew :openpdf-html:build
./gradlew :pdf-swing:build
```

### Running Tests

To run all tests:

```bash
./gradlew test
```

To run tests for a specific module:

```bash
./gradlew :openpdf-core:test
```

### Creating JAR Files

The build process automatically creates:
- Main JAR files (e.g., `openpdf-core/build/libs/openpdfx-3.0.8-SNAPSHOT.jar`)
- Sources JAR files (`*-sources.jar`)
- Javadoc JAR files (`*-javadoc.jar`)

### Installing to Local Maven Repository

To install all artifacts to your local Maven repository:

```bash
./gradlew publishToMavenLocal
```

## Available Modules

The project consists of 7 modules:

1. **openpdf-core** - Core PDF manipulation library
2. **pdf-swing** - Swing-based PDF rendering components
3. **pdf-toolbox** - PDF utility toolbox and examples
4. **openpdf-fonts-extra** - Extended font support
5. **openpdf-html** - HTML to PDF conversion
6. **openpdf-renderer** - PDF rendering engine
7. **openpdfx-ai** - AI-powered PDF features

## Gradle Tasks

Common Gradle tasks:

- `./gradlew tasks` - List all available tasks
- `./gradlew build` - Compile, test, and package all modules
- `./gradlew clean` - Remove all build artifacts
- `./gradlew test` - Run all tests
- `./gradlew javadoc` - Generate Javadoc for all modules
- `./gradlew dependencies` - Show dependency tree
- `./gradlew projects` - List all subprojects

## Configuration

The build is configured through:

- `build.gradle` - Root build configuration
- `settings.gradle` - Project structure definition
- `gradle.properties` - Build properties and dependency versions
- `<module>/build.gradle` - Module-specific configurations

## Gradle vs Maven

Both build systems are supported. Key differences:

| Feature | Gradle | Maven |
|---------|--------|-------|
| Build files | `build.gradle` | `pom.xml` |
| Build command | `./gradlew build` | `mvn install` |
| Clean build | `./gradlew clean build` | `mvn clean install` |
| Local install | `./gradlew publishToMavenLocal` | `mvn install` |

The Gradle build is configured to produce the same artifacts as Maven, with identical groupId, artifactId, and version.

## CI/CD Integration

A GitHub Actions workflow is available at `.github/workflows/gradle.yml` that automatically builds the project using Gradle on every push and pull request.

## Troubleshooting

### Java Version Issues

If you encounter Java version errors, ensure you're using JDK 21 or later:

```bash
java -version
```

### Gradle Daemon Issues

If you experience issues with the Gradle daemon, you can disable it:

```bash
./gradlew build --no-daemon
```

Or stop all running daemons:

```bash
./gradlew --stop
```

### Build Cache Issues

To disable the build cache:

```bash
./gradlew build --no-build-cache
```

## Additional Resources

- [Gradle Documentation](https://docs.gradle.org/)
- [Gradle Build Language Reference](https://docs.gradle.org/current/dsl/)
- [OpenPDFX GitHub Repository](https://github.com/openpdfx/openpdfx)
