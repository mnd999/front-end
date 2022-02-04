/*
 * Copyright (c) Neo4j Sweden AB (http://neo4j.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opencypher.v9_0.util.helpers

import org.opencypher.v9_0.util.AnonymousVariableNameGenerator
import org.opencypher.v9_0.util.Rewriter
import org.opencypher.v9_0.util.topDown

import scala.util.matching.Regex

object NameDeduplicator {

  def nameGeneratorRegex(generatorName: String): Regex =
    s""" {2}($generatorName)(\\d+)""".r

  val UNNAMED_PATTERN: Regex = {
    nameGeneratorRegex(AnonymousVariableNameGenerator.generatorName)
  }

  private val UNNAMED_PARAMS_PATTERN = """ {2}(AUTOINT|AUTODOUBLE|AUTOSTRING|AUTOLIST)(\d+)""".r
  private val DEDUP_PATTERN = """ {2}([^\s]+)@\d+(?:\(.*?\))?""".r

  private def transformGeneratedNamesRewriter(transformation: String => String): Rewriter = topDown(Rewriter.lift {
    case s: String => transformation(s)
  })

  private val deduplicateVariableNames: String => String = fixedPoint { DEDUP_PATTERN.replaceAllIn(_, "$1") }

  /**
   * Removes planner-generated uniquely identifying elements from Strings.
   *
   * E.g. the String "  var@23(<uuid>)" becomes "var".
   */
  def removeGeneratedNamesAndParams(s: String): String = {
    val paramNamed = UNNAMED_PARAMS_PATTERN.replaceAllIn(s, m => s"${(m group 1).toLowerCase()}_${m group 2}")
    val named = UNNAMED_PATTERN.replaceAllIn(paramNamed, m => s"anon_${m group 2}")

    deduplicateVariableNames(named)
  }

  /**
   * Replaces planner-generated uniquely identifying variable names with empty string.
   *
   * E.g. the String "  UNNAMED23" becomes "".
   */
  def eraseGeneratedNames(s: String): String = UNNAMED_PATTERN.replaceAllIn(s, "")

  /**
   * Removes planner-generated uniquely identifying elements from any Strings found while traversing the tree of the given argument.
   */
  def removeGeneratedNamesAndParamsOnTree[M <: AnyRef](a: M): M = {
    transformGeneratedNamesRewriter(removeGeneratedNamesAndParams).apply(a).asInstanceOf[M]
  }

  def eraseGeneratedNamesOnTree[M <: AnyRef](a: M): M = {
    transformGeneratedNamesRewriter(eraseGeneratedNames).apply(a).asInstanceOf[M]
  }
}
