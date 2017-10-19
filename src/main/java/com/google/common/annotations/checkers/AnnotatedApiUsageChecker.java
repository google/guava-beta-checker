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

import static com.google.errorprone.matchers.Description.NO_MATCH;
import static javax.lang.model.element.ElementKind.ANNOTATION_TYPE;
import static javax.lang.model.element.ElementKind.CLASS;
import static javax.lang.model.element.ElementKind.CONSTRUCTOR;
import static javax.lang.model.element.ElementKind.ENUM;
import static javax.lang.model.element.ElementKind.ENUM_CONSTANT;
import static javax.lang.model.element.ElementKind.FIELD;
import static javax.lang.model.element.ElementKind.INTERFACE;
import static javax.lang.model.element.ElementKind.METHOD;

import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.IdentifierTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MemberSelectTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.Name;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ElementKind;

/**
 * Abstract check for usages of APIs that are annotated with a specific annotation.
 *
 * @author Colin Decker
 */
public abstract class AnnotatedApiUsageChecker extends BugChecker
    implements MemberSelectTreeMatcher, IdentifierTreeMatcher {

  private final String basePackage;
  private final String basePackagePlusDot; // Just to avoid creating this string repeatedly

  protected final String annotationType;

  protected AnnotatedApiUsageChecker(String basePackage, String annotationType) {
    this.basePackage = basePackage;
    this.basePackagePlusDot = basePackage + ".";
    this.annotationType = annotationType;
  }

  @Override
  public final Description matchMemberSelect(MemberSelectTree tree, VisitorState state) {
    if (ASTHelpers.findEnclosingNode(state.getPath(), ImportTree.class) != null) {
      return Description.NO_MATCH;
    }
    return matchTree(tree);
  }

  @Override
  public final Description matchIdentifier(IdentifierTree tree, VisitorState state) {
    // We don't match any call to super() because currently we have no good way of identifying
    // super() calls that are generated and not actually present in the source code. Originally,
    // we were using state.getEndPosition to check if the end position in source for the tree was
    // <= 0, but end positions are not guaranteed to be enabled in error-prone, in which case all
    // super() calls would be matched anyway. This isn't likely to matter much in practice unless
    // a class is subclassing a non-annotated class that has an annotated no-arg constructor.
    // TODO(cgdecker): Revisit this if/when we have a way of detecting generated super() calls.
    return isSuperCall(tree) ? NO_MATCH : matchTree(tree);
  }

  private Description matchTree(Tree tree) {
    Symbol symbol = ASTHelpers.getSymbol(tree);
    if (symbol != null && isInMatchingPackage(symbol) && isAnnotatedApi(symbol)) {
      return describeMatch(tree);
    }
    return NO_MATCH;
  }

  /**
   * Returns true if the given tree is a call to {@code super} in a constructor.
   */
  private static boolean isSuperCall(IdentifierTree tree) {
    return tree.getName().contentEquals("super");
  }

  /**
   * Returns true if the given symbol belongs to the base package for this checker or a package
   * under it.
   */
  private boolean isInMatchingPackage(Symbol symbol) {
    String packageName = symbol.packge().fullname.toString();
    return !isIgnoredPackage(packageName)
        && (packageName.equals(basePackage) || packageName.startsWith(basePackagePlusDot));
  }

  /**
   * May be overridden to ignore APIs under specific packages. Returns false by default.
   */
  protected boolean isIgnoredPackage(String packageName) {
    return false;
  }

  /**
   * May be overridden to ignore specific types and their members. Returns false by default.
   */
  protected boolean isIgnoredType(String fullyQualifiedTypeName) {
    return false;
  }

  /**
   * Returns true if the given symbol is annotated with the annotation or if it's a member of a type
   * annotated with the annotation.
   */
  private boolean isAnnotatedApi(Symbol symbol) {
    Name name = symbol.getQualifiedName();
    if (name != null && isIgnoredType(name.toString())) {
      return false;
    }

    for (AnnotationMirror annotation : symbol.getAnnotationMirrors()) {
      if (annotation.getAnnotationType().toString().equals(annotationType)) {
        return true;
      }
    }

    return isMemberOfAnnotatedApi(symbol);
  }

  /**
   * Kinds of elements that should be considered annotated if the element's owner (i.e. the class
   * it's declared in) is annotated. This is used to prevent things like type parameters that happen
   * to be declared in an annotated class from being flagged.
   */
  private static final Set<ElementKind> INHERITS_ANNOTATION_FROM_OWNER =
      Collections.unmodifiableSet(
          EnumSet.of(
              FIELD, METHOD, CONSTRUCTOR, ENUM_CONSTANT, CLASS, INTERFACE, ENUM, ANNOTATION_TYPE));

  /**
   * Returns true if the given element is a member of an annotated class or interface.
   */
  private boolean isMemberOfAnnotatedApi(Symbol symbol) {
    return symbol != null
        && INHERITS_ANNOTATION_FROM_OWNER.contains(symbol.getKind())
        && isAnnotatedApi(symbol.owner);
  }
}
