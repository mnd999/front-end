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
package org.opencypher.v9_0.frontend

import org.opencypher.v9_0.ast.semantics.SemanticFeature
import org.opencypher.v9_0.frontend.helpers.ErrorCollectingContext
import org.opencypher.v9_0.frontend.helpers.ErrorCollectingContext.failWith
import org.opencypher.v9_0.frontend.helpers.NoPlannerName
import org.opencypher.v9_0.frontend.phases.BaseContext
import org.opencypher.v9_0.frontend.phases.BaseState
import org.opencypher.v9_0.frontend.phases.CompilationPhaseTracer
import org.opencypher.v9_0.frontend.phases.CompilationPhaseTracer.CompilationPhase.AST_REWRITE
import org.opencypher.v9_0.frontend.phases.InitialState
import org.opencypher.v9_0.frontend.phases.Parsing
import org.opencypher.v9_0.frontend.phases.Phase
import org.opencypher.v9_0.frontend.phases.SemanticAnalysis
import org.opencypher.v9_0.rewriting.rewriters.projectNamedPaths
import org.opencypher.v9_0.util.AnonymousVariableNameGenerator
import org.opencypher.v9_0.util.StepSequencer
import org.opencypher.v9_0.util.test_helpers.CypherFunSuite

class SemanticAnalysisTest extends CypherFunSuite {

  // This test invokes SemanticAnalysis twice because that's what the production pipeline does
  private def pipelineWithSemanticFeatures(semanticFeatures: SemanticFeature*) =
    Parsing andThen SemanticAnalysis(warn = true, semanticFeatures:_*) andThen SemanticAnalysis(warn = false, semanticFeatures:_*)

  private val pipeline = pipelineWithSemanticFeatures()

  test("should fail for max() with no arguments") {
    val query = "RETURN max() AS max"

    val startState = initStartState(query)
    val context = new ErrorCollectingContext()
    pipeline.transform(startState, context)

    context should failWith("Insufficient parameters for function 'max'")
  }

  test("Should allow overriding variable name in RETURN clause with an ORDER BY") {
    val query = "MATCH (n) RETURN n.prop AS n ORDER BY n + 2"

    val startState = initStartState(query)
    val context = new ErrorCollectingContext()

    pipeline.transform(startState, context)

    context.errors shouldBe empty
  }

  test("Should not allow multiple columns with the same name in WITH") {
    val query = "MATCH (n) WITH n.prop AS n, n.foo AS n ORDER BY n + 2 RETURN 1 AS one"

    val startState = initStartState(query)
    val context = new ErrorCollectingContext()

    pipeline.transform(startState, context)

    context.errors.map(_.msg) should equal(List("Multiple result columns with the same name are not supported"))
  }

  test("Should not allow duplicate variable name") {
    val query = "CREATE (n),(n) RETURN 1 as one"

    val startState = initStartState(query)
    val context = new ErrorCollectingContext()

    pipeline.transform(startState, context)

    context.errors.map(_.msg) should equal(List("Variable `n` already declared"))
  }

  test("Should allow parameter as valid predicate in FilteringExpression") {
    val queries = Seq(
      "RETURN [x IN [1,2,3] WHERE $p | x + 1] AS foo",
      "RETURN all(x IN [1,2,3] WHERE $p) AS foo",
      "RETURN any(x IN [1,2,3] WHERE $p) AS foo",
      "RETURN none(x IN [1,2,3] WHERE $p) AS foo",
      "RETURN single(x IN [1,2,3] WHERE $p) AS foo",
    )
    queries.foreach { query =>
      withClue(query) {
        val context = new ErrorCollectingContext()
        pipeline.transform(initStartState(query).withParams(Map("p" -> 42)), context)
        context.errors shouldBe empty
      }
    }
  }

  test("Should allow pattern as valid predicate in FilteringExpression") {
    val queries = Seq(
      "MATCH (n) RETURN [x IN [1,2,3] WHERE (n)--() | x + 1] AS foo",
      "MATCH (n) RETURN all(x IN [1,2,3] WHERE (n)--()) AS foo",
      "MATCH (n) RETURN any(x IN [1,2,3] WHERE (n)--()) AS foo",
      "MATCH (n) RETURN none(x IN [1,2,3] WHERE (n)--()) AS foo",
      "MATCH (n) RETURN single(x IN [1,2,3] WHERE (n)--()) AS foo",
    )
    queries.foreach { query =>
      withClue(query) {
        val context = new ErrorCollectingContext()
        pipeline.transform(initStartState(query), context)
        context.errors shouldBe empty
      }
    }
  }

  // Escaped backticks in tokens

  test("Should allow escaped backticks in node property key name") {
    // Property without escaping: `abc123``
    val query = "CREATE ({prop: 5, ```abc123`````: 1})"

    val startState = initStartState(query)
    val context = new ErrorCollectingContext()

    pipeline.transform(startState, context)

    context.errors should be(empty)
  }

  test("Should allow escaped backticks in relationship property key name") {
    // Property without escaping: abc`123
    val query = "MATCH ()-[r]->() RETURN r.`abc``123` as result"

    val startState = initStartState(query)
    val context = new ErrorCollectingContext()

    pipeline.transform(startState, context)

    context.errors should be(empty)
  }

  test("Should allow escaped backticks in label") {
    // Label without escaping: `abc123
    val query = "MATCH (n) SET n:```abc123`"

    val startState = initStartState(query)
    val context = new ErrorCollectingContext()

    pipeline.transform(startState, context)

    context.errors should be(empty)
  }

  test("Should allow escaped backtick in relationship type") {
    // Relationship type without escaping: abc123``
    val query = "MERGE ()-[r:`abc123`````]->()"

    val startState = initStartState(query)
    val context = new ErrorCollectingContext()

    pipeline.transform(startState, context)

    context.errors should be(empty)
  }

  test("Should allow escaped backtick in indexes") {
    // Query without proper escaping: CREATE INDEX `abc`123`` FOR (n:`Per`son`) ON (n.first``name`, n.``last`name)
    val query = "CREATE INDEX ```abc``123````` FOR (n:```Per``son```) ON (n.`first````name```, n.`````last``name`)"
    val startState = initStartState(query)
    val context = new ErrorCollectingContext()

    pipeline.transform(startState, context)

    context.errors should be(empty)
  }

  test("Should allow escaped backtick in constraints") {
    // Query without proper escaping: CREATE CONSTRAINT abc123` ON (n:``Label) ASSERT (n.pr``op) IS NODE KEY
    val query = "CREATE CONSTRAINT `abc123``` ON (n:`````Label`) ASSERT (n.`pr````op`) IS NODE KEY"

    val startState = initStartState(query)
    val context = new ErrorCollectingContext()

    pipeline.transform(startState, context)

    context.errors should be(empty)
  }

  test("Should register uses in PathExpressions") {
    val query = "MATCH p = (a)-[r]-(b) RETURN p AS p"

    val startState = initStartState(query)
    val context = new ErrorCollectingContext()

    val pipeline = Parsing andThen ProjectNamedPathsPhase andThen SemanticAnalysis(warn = true)

    val result = pipeline.transform(startState, context)
    val scopeTree = result.semantics().scopeTree

    Set("a", "r", "b").foreach { name =>
      scopeTree.allSymbols(name).head.uses shouldNot be(empty)
    }
  }

  test("CALL { ... } IN TRANSACTIONS") {
    val query = "CALL { MATCH (n) RETURN n AS n } IN TRANSACTIONS RETURN n AS n"

    val startState = initStartState(query)
    val context = new ErrorCollectingContext()

    pipeline.transform(startState, context)

    context.errors.map(_.msg) shouldBe Seq(
      "The CALL { ... } IN TRANSACTIONS clause is not available in this implementation of Cypher due to lack of support for running subqueries in separate transactions."
    )
  }

  test("CALL { ... } IN TRANSACTIONS with feature enabled") {
    val query = "CALL { MATCH (n) RETURN n AS n } IN TRANSACTIONS RETURN n AS n"

    val startState = initStartState(query)
    val context = new ErrorCollectingContext()

    val pipeline = pipelineWithSemanticFeatures(SemanticFeature.CallSubqueryInTransactions)
    pipeline.transform(startState, context)

    context.errors.map(_.msg) shouldBe empty
  }

  test("CALL { WITH ... } IN TRANSACTIONS") {
    val query = "WITH 1 AS x CALL { WITH x AS x MATCH (n) RETURN n AS n } IN TRANSACTIONS RETURN n AS n"

    val startState = initStartState(query)
    val context = new ErrorCollectingContext()

    val pipeline = pipelineWithSemanticFeatures(SemanticFeature.CorrelatedSubQueries, SemanticFeature.CallSubqueryInTransactions)
    pipeline.transform(startState, context)

    context.errors.map(_.msg) shouldBe Seq(
      "The CALL { WITH ... } IN TRANSACTIONS clause is not available in this implementation of Cypher due to lack of support for running correlated subqueries in separate transactions."
    )
  }

  test("CALL { WITH ... } IN TRANSACTIONS with feature enabled") {
    val query = "WITH 1 AS x CALL { WITH x AS x MATCH (n) RETURN n AS n } IN TRANSACTIONS RETURN n AS n"

    val startState = initStartState(query)
    val context = new ErrorCollectingContext()

    val pipeline = pipelineWithSemanticFeatures(SemanticFeature.CorrelatedSubQueries, SemanticFeature.CallSubqueryInTransactions, SemanticFeature.CallCorrelatedSubqueryInTransactions)
    pipeline.transform(startState, context)

    context.errors.map(_.msg) shouldBe empty
  }

  private def initStartState(query: String) =
    InitialState(query, None, NoPlannerName, new AnonymousVariableNameGenerator)

  final case object ProjectNamedPathsPhase extends Phase[BaseContext, BaseState, BaseState] {
    override def phase: CompilationPhaseTracer.CompilationPhase = AST_REWRITE
    override def process(from: BaseState, context: BaseContext): BaseState = {
      from.withStatement(from.statement().endoRewrite(projectNamedPaths))
    }
    override def postConditions: Set[StepSequencer.Condition] = Set.empty
  }
}
