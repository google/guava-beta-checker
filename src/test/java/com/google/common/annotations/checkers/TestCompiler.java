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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ListMultimap;
import com.google.common.io.Files;
import com.google.common.primitives.Longs;
import com.google.errorprone.BaseErrorProneJavaCompiler;
import com.google.errorprone.BugPattern;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.scanner.ScannerSupplier;
import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;

/**
 * Compiles Java sources with the {@link BetaChecker} and returns a list of diagnostics produced
 * by the compiler.
 *
 * @author Colin Decker
 */
final class TestCompiler {

  private final Class<? extends BugChecker> checker;

  TestCompiler(Class<? extends BugChecker> checker) {
    this.checker = checker;
  }

  // TODO(cgdecker): Would like to use compile-testing to avoid the need for this class

  /**
   * Compiles the given {@code sources} and returns a list of diagnostics produced by the compiler.
   */
  public List<Diagnostic<? extends JavaFileObject>> compile(JavaFileObject... sources) {
    return compile(Arrays.asList(sources));
  }

  /**
   * Compiles the given {@code sources} and returns a list of diagnostics produced by the compiler.
   */
  public List<Diagnostic<? extends JavaFileObject>> compile(
      Iterable<? extends JavaFileObject> sources) {
    ScannerSupplier scannerSupplier = ScannerSupplier.fromBugCheckerClasses(checker);
    DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
    JavaCompiler compiler = new BaseErrorProneJavaCompiler(scannerSupplier);
    File tmpDir = Files.createTempDir();
    CompilationTask task =
        compiler.getTask(
            new PrintWriter(System.err, true),
            null /*filemanager*/,
            collector,
            ImmutableList.of("-d", tmpDir.getAbsolutePath()),
            null /*classes*/,
            sources);
    try {
      task.call();
      return collector.getDiagnostics();
    } finally {
      File[] files = tmpDir.listFiles();
      if (files != null) {
        for (File file : files) {
          file.delete();
        }
      }
      tmpDir.delete();
    }
  }

  /**
   * Asserts that the given diagnostics contain errors with a message containing "[CheckerName]"
   * on the given lines of the given file. If there should be multiple errors on a line, the line
   * number must appear multiple times. There may not be any errors in any other file.
   */
  public void assertErrorsOnLines(String file,
      List<Diagnostic<? extends JavaFileObject>> diagnostics, long... lines) {
    ListMultimap<String, Long> actualErrors = ArrayListMultimap.create();
    for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics) {
      String message = diagnostic.getMessage(Locale.US);

      // The source may be null, e.g. for diagnostics about command-line flags
      assertNotNull(message, diagnostic.getSource());
      String sourceName = diagnostic.getSource().getName();

      assertEquals(
          "unexpected error in source file " + sourceName + ": " + message,
          file, sourceName);

      actualErrors.put(diagnostic.getSource().getName(), diagnostic.getLineNumber());

      // any errors from the compiler that are not related to this checker should fail
      assertThat(message).contains("[" + checker.getAnnotation(BugPattern.class).name() + "]");
    }

    assertEquals(
        ImmutableMultiset.copyOf(Longs.asList(lines)),
        ImmutableMultiset.copyOf(actualErrors.get(file)));
  }
}
