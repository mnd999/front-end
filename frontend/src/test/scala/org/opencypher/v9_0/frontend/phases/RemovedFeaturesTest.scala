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
package org.opencypher.v9_0.frontend.phases

import org.opencypher.v9_0.ast.AstConstructionTestSupport
import org.opencypher.v9_0.rewriting.Deprecations
import org.opencypher.v9_0.util.test_helpers.CypherFunSuite

class RemovedFeaturesTest extends CypherFunSuite with AstConstructionTestSupport with RewritePhaseTest {

  override def astRewriteAndAnalyze: Boolean = false

  override def rewriterPhaseUnderTest: Transformer[BaseContext, BaseState, BaseState] = SyntaxDeprecationWarningsAndReplacements(Deprecations.removedFeaturesIn4_0)
  private val deprecatedNameMap4_0 = Deprecations.removedFeaturesIn4_0.removedFunctionsRenames

  test("should rewrite removed function names regardless of casing") {
    for ((oldName, newName) <- deprecatedNameMap4_0) {
      assertRewritten(s"RETURN $oldName($$param) AS f", s"RETURN $newName($$param) AS f")
      assertRewritten(s"RETURN ${oldName.toLowerCase()}($$param) AS f", s"RETURN $newName($$param) AS f")
      assertRewritten(s"RETURN ${oldName.toUpperCase()}($$param) AS f", s"RETURN $newName($$param) AS f")
    }
  }

  test("should not touch new function names regardless of casing") {
    for (newName <- deprecatedNameMap4_0.values) {
      assertNotRewritten(s"RETURN $newName($$param) AS f")
      assertNotRewritten(s"RETURN ${newName.toLowerCase()}($$param) AS f")
      assertNotRewritten(s"RETURN ${newName.toUpperCase()}($$param) AS f")
    }
  }

  test("should rewrite length of strings and collections to size regardless of casing") {
    for (lengthFunc <- Seq("length", "LENGTH", "leNgTh")) {
      assertRewritten(s"RETURN $lengthFunc('a string') AS f", s"RETURN size('a string') AS f")
      assertRewritten(s"RETURN $lengthFunc([1, 2, 3]) AS f", s"RETURN size([1, 2, 3]) AS f")
    }
  }

  test("should rewrite filter to list comprehension") {
    assertRewritten(
      "RETURN filter(x IN ['a', 'aa', 'aaa'] WHERE x STARTS WITH 'aa') AS f",
      "RETURN [x IN ['a', 'aa', 'aaa'] WHERE x STARTS WITH 'aa'] AS f")
  }

  test("should rewrite extract to list comprehension") {
    assertRewritten(
      "RETURN extract(x IN ['a', 'aa', 'aaa'] | size(x)) AS f",
      "RETURN [x IN ['a', 'aa', 'aaa'] | size(x)] AS f")
  }
}
