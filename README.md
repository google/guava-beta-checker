# Guava Beta Checker

An [Error Prone] plugin that checks for usages of [Guava] APIs that are
annotated with the [`@Beta`] annotation. Such APIs should _never_ be used in
library code that other projects may depend on; using the Beta Checker can help
library projects ensure that they don't use them.

Example error:

```
src/main/java/foo/MyClass.java:14: error: [BetaApi] @Beta APIs should not be used in library code as they are subject to change.
    Files.copy(a, b);
    ^
    (see https://github.com/google/guava/wiki/PhilosophyExplained#beta-apis)
```

## Usage


Using the Beta Checker requires configuring your project to build with the Error
Prone Java compiler. By default, this enables a lot of useful checks for a
variety of common bugs. However, if you just want to use the Beta Checker, the
other checks can be disabled.

The usage examples below will show how to use the Beta Checker only, with notes
for what to remove if you want all checks.

### Maven

In `pom.xml`:

```xml
<build>
  <plugins>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-compiler-plugin</artifactId>
      <version>3.5.1</version>
      <configuration>
        <compilerId>javac-with-errorprone</compilerId>
        <forceJavacCompilerUse>true</forceJavacCompilerUse>
        <source>1.8</source>
        <target>1.8</target>
        <annotationProcessorPaths>
          <path>
            <groupId>com.google.guava</groupId>
            <artifactId>guava-beta-checker</artifactId>
            <version>${betachecker.version}</version>
          </path>
        </annotationProcessorPaths>
        <!-- Remove these compilerArgs to keep all checks enabled -->
        <compilerArgs>
          <arg>-XepDisableAllChecks</arg>
          <arg>-Xep:BetaApi:ERROR</arg>
        </compilerArgs>
      </configuration>
      <dependencies>
        <dependency>
          <groupId>org.codehaus.plexus</groupId>
          <artifactId>plexus-compiler-javac-errorprone</artifactId>
          <version>2.5</version>
        </dependency>
        <dependency>
          <groupId>com.google.errorprone</groupId>
          <artifactId>error_prone_core</artifactId>
          <!-- override plexus-compiler-javac-errorprone's dependency with the
               latest Error Prone version -->
          <version>${errorprone.version}</version>
        </dependency>
      </dependencies>
    </plugin>
  </plugins>
</build>
```

### Gradle

Your `build.gradle` file(s) should have the following things. Add them to what's
already in your files as appropriate.

```groovy
// Add the gradle plugins that are needed for Error Prone plugin support
buildscript {
  repositories {
    maven {
      url "https://plugins.gradle.org/m2/"
    }
  }
  dependencies {
    classpath "net.ltgt.gradle:gradle-errorprone-plugin:0.0.13"
    classpath "net.ltgt.gradle:gradle-apt-plugin:0.12"
  }
}

repositories {
  mavenCentral()
}

apply plugin: 'java'

// Enable Error Prone and APT plugins
apply plugin: 'net.ltgt.errorprone'
apply plugin: 'net.ltgt.apt'

dependencies {
  // Add an APT dependency on the beta checker
  apt 'com.google.guava:guava-beta-checker:$betaCheckerVersion'
}

configurations.errorprone {
  resolutionStrategy.force 'com.google.errorprone:error_prone_core:2.1.2'
}

compileJava {
  // Remove these compilerArgs to keep all checks enabled
  options.compilerArgs += ["-XepDisableAllChecks", "-Xep:BetaApi:ERROR"]
}
```

### Bazel

Bazel Java targets use the Error Prone compiler by default. To use the Beta
Checker with Bazel, you'll need to create a `java_plugin` target for the
Beta Checker and then add that target to the `plugins` attribute of any
Java targets it should run on.

TODO: Add an example of how to do this.

[Error Prone]: https://github.com/google/error-prone
[Guava]: https://github.com/google/guava
[`@Beta`]: http://google.github.io/guava/releases/snapshot-jre/api/docs/com/google/common/annotations/Beta.html
