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
Checker with Bazel, you'll need to add a `maven_jar` dependency on the Beta
Checker, then create a `java_plugin` target for it, and finally add that target
to the `plugins` attribute of any Java targets it should run on.

#### Example

You'll need a `java_library` for the Beta Checker. You can get this using
[generate-workspace], by running a command like:

```shell
bazel run //generate_workspace -- \
    -a com.google.guava:guava:$GUAVA_VERSION \
    -a com.google.guava:guava-beta-checker:$BETA_CHECKER_VERSION \
    -r https://repo.maven.apache.org/maven2/
```

After putting the generated `generate_workspace.bzl` file in your project as
described in the documentation, put the following in `third_party/BUILD`:

```bazel
load("//:generate_workspace.bzl", "generated_java_libraries")
generated_java_libraries()

java_plugin(
    name = "guava_beta_checker_plugin",
    deps = [":com_google_guava_guava_beta_checker"],
    visibility = ["//visibility:public"],
)
```

Finally, add the plugin to the `plugins` attribute of any Java target you want
to be checked for usages of `@Beta` APIs:

```bazel
java_library(
    name = "foo",
    srcs = glob(["*.java"]),
    deps = [
        "//third_party:com_google_guava_guava",
    ],
    plugins = [
        "//third_party:guava_beta_checker_plugin",
    ],
)
```

[Error Prone]: https://github.com/google/error-prone
[Guava]: https://github.com/google/guava
[`@Beta`]: http://google.github.io/guava/releases/snapshot-jre/api/docs/com/google/common/annotations/Beta.html
[generate-workspace]: https://docs.bazel.build/versions/master/generate-workspace.html
