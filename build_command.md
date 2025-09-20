# AnySoftKeyboard Build Command

## Required Java Version Setup
This project requires Java 17 to build successfully due to JaCoCo compatibility issues with newer Java versions.

### Setting Java 17 Environment
```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
export PATH="/usr/lib/jvm/java-17-openjdk/bin:$PATH"
```

### Verify Java Version
```bash
java -version
# Should show: openjdk version "17.0.x"
```

## Build Commands

### Basic Build (Single Module)
```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk && export PATH="/usr/lib/jvm/java-17-openjdk/bin:$PATH" && ./gradlew :ime:base:build --no-daemon --console=plain
```

### Build App Module (Debug APK)
```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk && export PATH="/usr/lib/jvm/java-17-openjdk/bin:$PATH" && ./gradlew :ime:app:assembleDebug --no-daemon --console=plain
```

### Build App with English Language Pack Only (Debug APK)

**Method 1: Using Minimal Settings.gradle (Recommended for faster builds)**

1. First, backup the full settings.gradle and create minimal version:
```bash
cp settings.gradle settings.gradle.full
```

2. Create minimal settings.gradle (only includes essential projects + English):
```bash
# Copy the content from settings.gradle.minimal to settings.gradle
# Or manually edit to include only essential projects and English language pack
```

3. Build the app:
```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk && export PATH="/usr/lib/jvm/java-17-openjdk/bin:$PATH" && ./gradlew :ime:app:assembleDebug --no-daemon --console=plain
```

4. Restore full settings.gradle when needed:
```bash
cp settings.gradle.full settings.gradle
```

**Method 2: Using Full Settings.gradle (Slower but includes all projects)**
```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk && export PATH="/usr/lib/jvm/java-17-openjdk/bin:$PATH" && ./gradlew :ime:app:assembleDebug :addons:languages:english:pack:assembleDebug --no-daemon --console=plain
```

### Clean Build
```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk && export PATH="/usr/lib/jvm/java-17-openjdk/bin:$PATH" && ./gradlew clean --no-daemon --console=plain
```

### Build with Output to File
```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk && export PATH="/usr/lib/jvm/java-17-openjdk/bin:$PATH" && ./gradlew :ime:base:build --no-daemon --console=plain > build_output.log 2>&1
```

## Important Notes

- **Java Version**: Must use Java 17. The project is configured for Java 17 in `gradle/android_general.gradle`
- **Gradle Caching**: Has been disabled in `gradle.properties` (`org.gradle.caching=false`)
- **Build Output**: Use `> build_output.log 2>&1` to redirect output to a file for long builds
- **No Daemon**: Use `--no-daemon` to avoid Gradle daemon issues
- **Console Output**: Use `--console=plain` for cleaner output

## Troubleshooting

If you encounter JaCoCo compatibility errors:
1. Verify Java version is set to 17
2. Check that JAVA_HOME and PATH are correctly set
3. Use the full export command before each gradle invocation

## Available Java Versions
The system has multiple Java versions available:
- java-8-openjdk
- java-11-openjdk  
- java-17-openjdk (required for this project)
- java-24-openjdk (causes JaCoCo compatibility issues)