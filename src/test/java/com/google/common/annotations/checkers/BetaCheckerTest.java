/*
 * Copyright (C) 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.annotations.checkers;

import static com.google.common.truth.Truth.assertThat;

import com.google.testing.compile.JavaFileObjects;
import java.util.List;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link AnnotatedApiUsageChecker} via the {@link BetaChecker} concrete class.
 *
 * @author Colin Decker
 */

@RunWith(JUnit4.class)
public class BetaCheckerTest {

  private final TestCompiler compiler = new TestCompiler(BetaChecker.class);

  /**
   * Equivalent to the real @Beta annotation from Guava.
   */
  public static final JavaFileObject BETA = JavaFileObjects.forSourceLines(
      "com.google.common.annotations.Beta",
      "package com.google.common.annotations;",
      "",
      "import java.lang.annotation.ElementType;",
      "import java.lang.annotation.Retention;",
      "import java.lang.annotation.RetentionPolicy;",
      "import java.lang.annotation.Target;",
      "",
      "@Retention(RetentionPolicy.CLASS)",
      "@Target({",
      "    ElementType.ANNOTATION_TYPE,",
      "    ElementType.CONSTRUCTOR,",
      "    ElementType.FIELD,",
      "    ElementType.METHOD,",
      "    ElementType.TYPE})",
      "public @interface Beta {}");

  /**
   * Class in a com.google.common package that is annotated with @Beta.
   */
  public static final JavaFileObject ANNOTATED_CLASS = JavaFileObjects.forSourceLines(
      "com.google.common.foo.AnnotatedClass",
      "package com.google.common.foo;",
      "",
      "import com.google.common.annotations.Beta;",
      "",
      "@Beta",
      "public class AnnotatedClass {",
      "",
      "  public static final String STATIC_FIELD = \"foo\";",
      "",
      "  public static String staticMethod() {",
      "    return \"foo\";",
      "  }",
      "",
      " public final String instanceField = \"foo\";",
      "",
      "  public String instanceMethod() {",
      "    return \"foo\";",
      "  }",
      "}");

  @Test
  public void testCleanClass() {
    List<Diagnostic<? extends JavaFileObject>> diagnostics = compiler.compile(BETA,
        JavaFileObjects.forSourceLines(
            "example.Test",
            "package example;",
            "",
            "import java.util.Arrays;",
            "",
            "public class Test {",
            "  public static void main(String[] args) {",
            "    System.out.println(Arrays.asList(args));",
            "  }",
            "}")
    );

    assertThat(diagnostics).isEmpty();
  }

  @Test
  public void testAnnotatedClass_asParameter() {
    List<Diagnostic<? extends JavaFileObject>> diagnostics = compiler.compile(
        BETA, ANNOTATED_CLASS,
        JavaFileObjects.forSourceLines(
            "example.Test",
            "package example;",
            "",
            "import com.google.common.foo.AnnotatedClass;",
            "",
            "public class Test {",
            "  public static void foo(AnnotatedClass annotated) {", // error
            "  }",
            "}")
    );

    compiler.assertErrorsOnLines("example/Test.java", diagnostics, 6);
  }

  @Test
  public void testAnnotatedClass_asTypeArgument() {
    List<Diagnostic<? extends JavaFileObject>> diagnostics = compiler.compile(
        BETA, ANNOTATED_CLASS,
        JavaFileObjects.forSourceLines(
            "example.Test",
            "package example;",
            "",
            "import com.google.common.foo.AnnotatedClass;",
            "",
            "import java.util.List;",
            "",
            "public class Test {",
            "  public static void foo(List<AnnotatedClass> stuff) {", // error
            "  }",
            "}")
    );

    compiler.assertErrorsOnLines("example/Test.java", diagnostics, 8);
  }

  @Test
  public void testAnnotatedClass_extending() {
    List<Diagnostic<? extends JavaFileObject>> diagnostics = compiler.compile(
        BETA, ANNOTATED_CLASS,
        JavaFileObjects.forSourceLines(
            "example.Test",
            "package example;",
            "",
            "import com.google.common.foo.AnnotatedClass;",
            "",
            "public class Test extends AnnotatedClass {", // error
            "}")
    );

    compiler.assertErrorsOnLines("example/Test.java", diagnostics, 5);
  }

  @Test
  public void testAnnotatedClass_extending_generatedSuperCallIsNotMatched() {
    List<Diagnostic<? extends JavaFileObject>> diagnostics = compiler.compile(
        BETA, ANNOTATED_CLASS,
        JavaFileObjects.forSourceLines(
            "example.Test",
            "package example;",
            "",
            "import com.google.common.foo.AnnotatedClass;",
            "",
            "public class Test extends AnnotatedClass {", // error
            "",
            "  private final String foo;",
            "",
            "  public Test(String foo) {", // no error for implicit super() here
            "    this.foo = foo;",
            "  }",
            "}")
    );

    compiler.assertErrorsOnLines("example/Test.java", diagnostics, 5);
  }

  @Test
  public void testAnnotatedClass_asBoundInGenericType() {
    List<Diagnostic<? extends JavaFileObject>> diagnostics = compiler.compile(
        BETA, ANNOTATED_CLASS,
        JavaFileObjects.forSourceLines("example.Test",
            "package example;",
            "",
            "import com.google.common.foo.AnnotatedClass;",
            "",
            "public class Test<T extends AnnotatedClass> {", // error
            "}")
    );

    compiler.assertErrorsOnLines("example/Test.java", diagnostics, 5);
  }

  @Test
  public void testAnnotatedClass_asTypeArgInImplementedGenericInterface() {
    List<Diagnostic<? extends JavaFileObject>> diagnostics = compiler.compile(
        BETA, ANNOTATED_CLASS,
        JavaFileObjects.forSourceLines("example.Test",
            "package example;",
            "",
            "import com.google.common.foo.AnnotatedClass;",
            "",
            "import java.util.List;",
            "",
            "public abstract class Test implements List<AnnotatedClass> {", // error
            "}")
    );

    compiler.assertErrorsOnLines("example/Test.java", diagnostics, 7);
  }

  @Test
  public void testAnnotatedClass_asBoundInGenericMethod() {
    List<Diagnostic<? extends JavaFileObject>> diagnostics = compiler.compile(
        BETA, ANNOTATED_CLASS,
        JavaFileObjects.forSourceLines("example.Test",
            "package example;",
            "",
            "import com.google.common.foo.AnnotatedClass;",
            "",
            "public class Test {",
            "  public static <T extends AnnotatedClass> T foo(T t) {", // error
            "    return null;",
            "  }",
            "}")
    );

    compiler.assertErrorsOnLines("example/Test.java", diagnostics, 6);
  }

  @Test
  public void testAnnotatedClass_asBoundInGenericParameter() {
    List<Diagnostic<? extends JavaFileObject>> diagnostics = compiler.compile(
        BETA, ANNOTATED_CLASS,
        JavaFileObjects.forSourceLines("example.Test",
            "package example;",
            "",
            "import com.google.common.foo.AnnotatedClass;",
            "",
            "import java.util.List;",
            "",
            "public class Test {",
            "  public static void foo(List<? super AnnotatedClass> list) {", // error
            "  }",
            "}")
    );

    compiler.assertErrorsOnLines("example/Test.java", diagnostics, 8);
  }

  @Test
  public void testAnnotatedClass_instantiation() {
    List<Diagnostic<? extends JavaFileObject>> diagnostics = compiler.compile(
        BETA, ANNOTATED_CLASS,
        JavaFileObjects.forSourceLines("example.Test",
            "package example;",
            "",
            "import com.google.common.foo.AnnotatedClass;",
            "",
            "public class Test {",
            "  public static void main(String[] args) {",
            "    System.out.println(new AnnotatedClass());", // error
            "  }",
            "}")
    );

    compiler.assertErrorsOnLines("example/Test.java", diagnostics, 7);
  }

  @Test
  public void testAnnotatedClass_staticMethodCall() {
    List<Diagnostic<? extends JavaFileObject>> diagnostics = compiler.compile(
        BETA, ANNOTATED_CLASS,
        JavaFileObjects.forSourceLines("example.Test",
            "package example;",
            "",
            "import com.google.common.foo.AnnotatedClass;",
            "",
            "public class Test {",
            "  public static void main(String[] args) {",
            "    AnnotatedClass.staticMethod();", // 2 errors
            "  }",
            "}")
    );

    compiler.assertErrorsOnLines("example/Test.java", diagnostics, 7, 7);
  }

  @Test
  public void testAnnotatedClass_instanceMethodCall() {
    List<Diagnostic<? extends JavaFileObject>> diagnostics = compiler.compile(
        BETA, ANNOTATED_CLASS,
        JavaFileObjects.forSourceLines("example.Test",
            "package example;",
            "",
            "import com.google.common.foo.AnnotatedClass;",
            "",
            "public class Test {",
            "  public static void foo(AnnotatedClass a) {", // error
            "    a.instanceMethod();", // error
            "  }",
            "}")
    );

    compiler.assertErrorsOnLines("example/Test.java", diagnostics, 6, 7);
  }

  @Test
  public void testAnnotatedClass_staticMethodCall_fromStaticImport() {
    List<Diagnostic<? extends JavaFileObject>> diagnostics = compiler.compile(
        BETA, ANNOTATED_CLASS,
        JavaFileObjects.forSourceLines("example.Test",
            "package example;",
            "",
            "import static com.google.common.foo.AnnotatedClass.staticMethod;",
            "",
            "public class Test {",
            "  public static void main(String[] args) {",
            "    staticMethod();", // error
            "  }",
            "}")
    );

    compiler.assertErrorsOnLines("example/Test.java", diagnostics, 7);
  }

  @Test
  public void testAnnotatedClass_staticMethodCall_fromInstanceVariable() {
    List<Diagnostic<? extends JavaFileObject>> diagnostics = compiler.compile(
        BETA, ANNOTATED_CLASS,
        JavaFileObjects.forSourceLines("example.Test",
            "package example;",
            "",
            "import com.google.common.foo.AnnotatedClass;",
            "",
            "public class Test {",
            "  public static void foo(AnnotatedClass a) {", // error
            "    a.staticMethod();", // error
            "  }",
            "}")
    );

    compiler.assertErrorsOnLines("example/Test.java", diagnostics, 6, 7);
  }

  @Test
  public void testAnnotatedClass_fullyQualifiedReferences() {
    List<Diagnostic<? extends JavaFileObject>> diagnostics = compiler.compile(
        BETA, ANNOTATED_CLASS,
        JavaFileObjects.forSourceLines("example.Test",
            "package example;",
            "",
            "public class Test {",
            "  public static void foo(com.google.common.foo.AnnotatedClass parameter) {", // error
            "    com.google.common.foo.AnnotatedClass c = ", // error
            "        new com.google.common.foo.AnnotatedClass();", // error
            "    String s = com.google.common.foo.AnnotatedClass.staticMethod();", // 2 errors
            "  }",
            "}")
    );

    compiler.assertErrorsOnLines("example/Test.java", diagnostics, 4, 5, 6, 7, 7);
  }

  @Test
  public void testAnnotatedClass_importsAreNotMatched() {
    List<Diagnostic<? extends JavaFileObject>> diagnostics = compiler.compile(
        BETA, ANNOTATED_CLASS,
        JavaFileObjects.forSourceLines("example.Test",
            "package example;",
            "",
            "import static com.google.common.foo.AnnotatedClass.staticMethod;",
            "",
            "import com.google.common.foo.AnnotatedClass;",
            "",
            "public class Test {",
            "  public static void foo() {",
            "    AnnotatedClass c = new AnnotatedClass();", // 2 errors
            "    String s = staticMethod();", // error
            "  }",
            "}")
    );

    compiler.assertErrorsOnLines("example/Test.java", diagnostics, 9, 9, 10);
  }

  @Test
  public void testAnnotatedClass_methodReference() {
    List<Diagnostic<? extends JavaFileObject>> diagnostics = compiler.compile(
        BETA, ANNOTATED_CLASS,
        JavaFileObjects.forSourceLines("example.Test",
            "package example;",
            "",
            "import static java.util.stream.Collectors.joining;",
            "",
            "import com.google.common.foo.AnnotatedClass;",
            "import java.util.List;",
            "",
            "public class Test {",
            "  public static void foo(List<? extends AnnotatedClass> list) {", // error
            "    String s = list.stream()",
            "        .map(AnnotatedClass::instanceMethod)", // error
            "        .collect(joining(", "));",
            "  }",
            "}")
    );

    compiler.assertErrorsOnLines("example/Test.java", diagnostics, 9, 11);
  }

  @Test
  public void testAnnotatedClass_constructorReference() {
    List<Diagnostic<? extends JavaFileObject>> diagnostics = compiler.compile(
        BETA, ANNOTATED_CLASS,
        JavaFileObjects.forSourceLines("example.Test",
            "package example;",
            "",
            "import com.google.common.foo.AnnotatedClass;",
            "import java.util.Optional;",
            "",
            "public class Test {",
            "  public static void foo(Optional<AnnotatedClass> optional) {", // error
            "    String s = optional",
            "        .orElseGet(AnnotatedClass::new)", // error
            "        .toString();",
            "  }",
            "}")
    );

    compiler.assertErrorsOnLines("example/Test.java", diagnostics, 7, 9);
  }

  /**
   * A class in a com.google.common package with some members that are annotated @Beta and some
   * that aren't.
   */
  public static final JavaFileObject ANNOTATED_MEMBERS = JavaFileObjects.forSourceLines(
      "com.google.common.foo.AnnotatedMembers",
      "package com.google.common.foo;",
      "",
      "import com.google.common.annotations.Beta;",
      "",
      "public class AnnotatedMembers {",
      "  public static final String STATIC_FIELD = \"foo\";",
      "",
      "  @Beta",
      "  public static final String ANNOTATED_STATIC_FIELD = \"foo\";",
      "",
      "  public final String instanceField = \"foo\";",
      "",
      "  @Beta",
      "  public final String annotatedInstanceField = \"foo\";",
      "",
      "  @Beta",
      "  public AnnotatedMembers() {}",
      "",
      "  public static String staticMethod() {",
      "    return \"foo\";",
      "  }",
      "",
      "  @Beta",
      "  public static String annotatedStaticMethod() {",
      "    return \"foo\";",
      "  }",
      "",
      "  public String instanceMethod() {",
      "    return \"foo\";",
      "  }",
      "",
      "  @Beta",
      "  public String annotatedInstanceMethod() {",
      "    return \"foo\";",
      "  }",
      "}");

  @Test
  public void testNonAnnotatedMembers() {
    List<Diagnostic<? extends JavaFileObject>> diagnostics = compiler.compile(
        BETA, ANNOTATED_MEMBERS,
        JavaFileObjects.forSourceLines("example.Test",
            "package example;",
            "",
            "import com.google.common.foo.AnnotatedMembers;",
            "",
            "public class Test {",
            "  public static void foo(AnnotatedMembers instance) {",
            "    String a = AnnotatedMembers.staticMethod();",
            "    String b = instance.instanceMethod();",
            "    String c = AnnotatedMembers.STATIC_FIELD;",
            "    String d = instance.instanceField;",
            "    String e = instance.staticMethod();",
            "    String f = instance.STATIC_FIELD;",
            "  }",
            "}")
    );

    assertThat(diagnostics).isEmpty();
  }

  @Test
  public void testAnnotatedMembers_staticField() {
    List<Diagnostic<? extends JavaFileObject>> diagnostics = compiler.compile(
        BETA, ANNOTATED_MEMBERS,
        JavaFileObjects.forSourceLines("example.Test",
            "package example;",
            "",
            "import com.google.common.foo.AnnotatedMembers;",
            "",
            "public class Test {",
            "  public static void main(String[] args) {",
            "    System.out.println(AnnotatedMembers.ANNOTATED_STATIC_FIELD);", // error
            "  }",
            "}")
    );

    compiler.assertErrorsOnLines("example/Test.java", diagnostics, 7);
  }

  @Test
  public void testAnnotatedMembers_instanceField() {
    List<Diagnostic<? extends JavaFileObject>> diagnostics = compiler.compile(
        BETA, ANNOTATED_MEMBERS,
        JavaFileObjects.forSourceLines("example.Test",
            "package example;",
            "",
            "import com.google.common.foo.AnnotatedMembers;",
            "",
            "public class Test {",
            "  public static void foo(AnnotatedMembers instance) {",
            "    System.out.println(instance.annotatedInstanceField);", // error
            "  }",
            "}")
    );

    compiler.assertErrorsOnLines("example/Test.java", diagnostics, 7);
  }

  @Test
  public void testAnnotatedMembers_staticMethod() {
    List<Diagnostic<? extends JavaFileObject>> diagnostics = compiler.compile(
        BETA, ANNOTATED_MEMBERS,
        JavaFileObjects.forSourceLines("example.Test",
            "package example;",
            "",
            "import com.google.common.foo.AnnotatedMembers;",
            "",
            "public class Test {",
            "  public static void main(String[] args) {",
            "    System.out.println(AnnotatedMembers.annotatedStaticMethod());", // error
            "  }",
            "}")
    );

    compiler.assertErrorsOnLines("example/Test.java", diagnostics, 7);
  }

  @Test
  public void testAnnotatedMembers_instanceMethod() {
    List<Diagnostic<? extends JavaFileObject>> diagnostics = compiler.compile(
        BETA, ANNOTATED_MEMBERS,
        JavaFileObjects.forSourceLines("example.Test",
            "package example;",
            "",
            "import com.google.common.foo.AnnotatedMembers;",
            "",
            "public class Test {",
            "  public static void foo(AnnotatedMembers instance) {",
            "    System.out.println(instance.annotatedInstanceMethod());", // error
            "  }",
            "}")
    );

    compiler.assertErrorsOnLines("example/Test.java", diagnostics, 7);
  }

  @Test
  public void testAnnotatedMembers_staticField_fromStaticImport() {
    List<Diagnostic<? extends JavaFileObject>> diagnostics = compiler.compile(
        BETA, ANNOTATED_MEMBERS,
        JavaFileObjects.forSourceLines("example.Test",
            "package example;",
            "",
            "import static com.google.common.foo.AnnotatedMembers.ANNOTATED_STATIC_FIELD;",
            "",
            "public class Test {",
            "  public static void main(String[] args) {",
            "    System.out.println(ANNOTATED_STATIC_FIELD);", // error
            "  }",
            "}")
    );

    compiler.assertErrorsOnLines("example/Test.java", diagnostics, 7);
  }

  @Test
  public void testAnnotatedMembers_staticMethod_fromStaticImport() {
    List<Diagnostic<? extends JavaFileObject>> diagnostics = compiler.compile(
        BETA, ANNOTATED_MEMBERS,
        JavaFileObjects.forSourceLines("example.Test",
            "package example;",
            "",
            "import static com.google.common.foo.AnnotatedMembers.annotatedStaticMethod;",
            "",
            "public class Test {",
            "  public static void main(String[] args) {",
            "    System.out.println(annotatedStaticMethod());", // error
            "  }",
            "}")
    );

    compiler.assertErrorsOnLines("example/Test.java", diagnostics, 7);
  }

  @Test
  public void testAnnotatedMembers_methodReference() {
    List<Diagnostic<? extends JavaFileObject>> diagnostics = compiler.compile(
        BETA, ANNOTATED_MEMBERS,
        JavaFileObjects.forSourceLines("example.Test",
            "package example;",
            "",
            "import static java.util.stream.Collectors.joining;",
            "",
            "import com.google.common.foo.AnnotatedMembers;",
            "import java.util.List;",
            "",
            "public class Test {",
            "  public static void foo(List<? extends AnnotatedMembers> list) {",
            "    String s = list.stream()",
            "        .map(AnnotatedMembers::annotatedInstanceMethod)", // error
            "        .collect(joining(", "));",
            "  }",
            "}")
    );

    compiler.assertErrorsOnLines("example/Test.java", diagnostics, 11);
  }

  @Test
  public void testAnnotatedMembers_constructorReference() {
    List<Diagnostic<? extends JavaFileObject>> diagnostics = compiler.compile(
        BETA, ANNOTATED_MEMBERS,
        JavaFileObjects.forSourceLines("example.Test",
            "package example;",
            "",
            "import com.google.common.foo.AnnotatedMembers;",
            "import java.util.Optional;",
            "",
            "public class Test {",
            "  public static void foo(Optional<AnnotatedMembers> optional) {",
            "    String s = optional",
            "        .orElseGet(AnnotatedMembers::new)", // error
            "        .toString();",
            "  }",
            "}")
    );

    compiler.assertErrorsOnLines("example/Test.java", diagnostics, 9);
  }

  /**
   * An @Beta interface in a com.google.common package.
   */
  private static final JavaFileObject ANNOTATED_INTERFACE = JavaFileObjects.forSourceLines(
      "com.google.common.foo.AnnotatedInterface",
      "package com.google.common.foo;",
      "",
      "import com.google.common.annotations.Beta;",
      "",
      "@Beta",
      "public interface AnnotatedInterface {",
      "}");

  @Test
  public void testAnnotatedInterface_implementing() {
    List<Diagnostic<? extends JavaFileObject>> diagnostics = compiler.compile(
        BETA, ANNOTATED_INTERFACE,
        JavaFileObjects.forSourceLines("example.Test",
            "package example;",
            "",
            "import com.google.common.foo.AnnotatedInterface;",
            "",
            "public class Test implements AnnotatedInterface {", // error
            "}")
    );

    compiler.assertErrorsOnLines("example/Test.java", diagnostics, 5);
  }

  @Test
  public void testAnnotatedInterface_extending() {
    List<Diagnostic<? extends JavaFileObject>> diagnostics = compiler.compile(
        BETA, ANNOTATED_INTERFACE,
        JavaFileObjects.forSourceLines("example.Test",
            "package example;",
            "",
            "import com.google.common.foo.AnnotatedInterface;",
            "",
            "public interface Test extends AnnotatedInterface {", // error
            "}")
    );

    compiler.assertErrorsOnLines("example/Test.java", diagnostics, 5);
  }

  /**
   * These checkers only match usages in packages matching a specific regex, to avoid giving errors
   * on code with the annotations in other libraries or even in the user's code. The @Beta checker
   * should only match usages in packages under com.google.common.
   */
  private static final JavaFileObject CLASS_IN_OTHER_PACKAGE = JavaFileObjects.forSourceLines(
      "foo.ClassInOtherPackage",
      "package foo;",
      "",
      "import com.google.common.annotations.Beta;",
      "",
      "@Beta",
      "public class ClassInOtherPackage {}");

  @Test
  public void testUsageByPackage_nonMatchingPackage_doesNotMatch() {
    List<Diagnostic<? extends JavaFileObject>> diagnostics = compiler.compile(
        BETA, CLASS_IN_OTHER_PACKAGE,
        JavaFileObjects.forSourceLines("example.Test",
            "package example;",
            "",
            "import foo.ClassInOtherPackage;",
            "",
            "public class Test {",
            "  public static void main(String[] args) {",
            "    System.out.println(new ClassInOtherPackage());",
            "  }",
            "}")
    );

    assertThat(diagnostics).isEmpty();
  }

  private static final JavaFileObject IGNORED_TYPE = JavaFileObjects.forSourceLines(
      "com.google.common.cache.Cache",
      "package com.google.common.cache;",
      "",
      "import com.google.common.annotations.Beta;",
      "",
      "@Beta",
      "public interface Cache<K, V> {",
      "  V get(K key);",
      "}");

  @Test
  public void testUsage_ignoredType() {
    List<Diagnostic<? extends JavaFileObject>> diagnostics = compiler.compile(
        BETA, IGNORED_TYPE,
        JavaFileObjects.forSourceLines("example.Test",
            "package example;",
            "",
            "import com.google.common.cache.Cache;",
            "",
            "public class Test {",
            "  public static void test(Cache<String, String> cache) {",
            "    System.out.println(cache.get(\"hello\"));",
            "  }",
            "}")
    );

    assertThat(diagnostics).isEmpty();
  }
}
