# Guava Beta Checker

An [error-prone] plugin that checks for usages of [Guava] APIs that are
annotated with the [`@Beta`] annotation. Such APIs should _never_ be used in
library code that other projects may depend on; this checker is to help library
projects ensure that they don't use them.

Example error:

```
src/main/java/foo/MyClass.java:14: error: [BetaApi] @Beta APIs should not be used in library code as they are subject to change.
    Files.copy(a, b);
    ^
    (see https://github.com/google/guava/wiki/PhilosophyExplained#beta-apis)
```

## Usage

Using the checker requires configuring your project to build with the
error-prone Java compiler. By default, this enables a lot of useful
checks for a variety of common bugs. However, if you just want to use the
`@Beta` checker, the other checks can be disabled.

The usage examples below will show how to use the `@Beta` checker only,
with notes for what to remove if you want all checks.

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
               latest error-prone version -->
          <version>${errorprone.version}</version>
        </dependency>
      </dependencies>
    </plugin>
  </plugins>
</build>
```

TODO: Add examples for at least Gradle and Bazel.

[error-prone]: https://github.com/google/error-prone
[Guava]: https://github.com/google/guava
[`@Beta`]: http://google.github.io/guava/releases/snapshot-jre/api/docs/com/google/common/annotations/Beta.html
