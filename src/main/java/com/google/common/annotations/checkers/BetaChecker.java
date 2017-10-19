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

import static com.google.errorprone.BugPattern.Category.GUAVA;
import static com.google.errorprone.BugPattern.LinkType.CUSTOM;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.auto.service.AutoService;
import com.google.errorprone.BugPattern;
import com.google.errorprone.bugpatterns.BugChecker;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Checks for usages of Guava {@code @Beta} APIs, which should never be used in library code.
 *
 * @author Colin Decker
 */
@AutoService(BugChecker.class)
@BugPattern(
  name = "BetaApi",
  summary = "@Beta APIs should not be used in library code as they are subject to change",
  explanation = "@Beta APIs should not be used in library code as they are subject to change.",
  linkType = CUSTOM,
  link = "https://github.com/google/guava/wiki/PhilosophyExplained#beta-apis",
  category = GUAVA,
  severity = ERROR
)
public final class BetaChecker extends AnnotatedApiUsageChecker {

  /** Specific @Beta types to ignore. */
  private static final Set<String> IGNORED_TYPES =
      Collections.unmodifiableSet(
          new HashSet<String>(
              Arrays.asList(
                  "com.google.common.cache.Cache", "com.google.common.cache.LoadingCache")));

  public BetaChecker() {
    super("com.google.common", "com.google.common.annotations.Beta");
  }

  @Override
  protected boolean isIgnoredType(String fullyQualifiedTypeName) {
    // Cache/LoadingCache are currently in a weird beta state where they're frozen for users but
    // not implementers. Since the vast majority of users are likely not implementing Cache and
    // LoadingCache themselves, just suppress this check for those types.
    // TODO(cgdecker): Remove this once they come out of beta.
    return IGNORED_TYPES.contains(fullyQualifiedTypeName);
  }
}
