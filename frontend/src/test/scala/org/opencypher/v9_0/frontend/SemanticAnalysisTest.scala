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

import org.opencypher.v9_0.ast.semantics.SemanticError
import org.opencypher.v9_0.ast.semantics.SemanticFeature
import org.opencypher.v9_0.frontend.helpers.ErrorCollectingContext
import org.opencypher.v9_0.frontend.helpers.ErrorCollectingContext.failWith
import org.opencypher.v9_0.frontend.helpers.NoPlannerName
import org.opencypher.v9_0.frontend.phases.BaseContext
import org.opencypher.v9_0.frontend.phases.BaseState
import org.opencypher.v9_0.frontend.phases.CompilationPhaseTracer
import org.opencypher.v9_0.frontend.phases.CompilationPhaseTracer.CompilationPhase.AST_REWRITE
import org.opencypher.v9_0.frontend.phases.InitialState
import org.opencypher.v9_0.frontend.phases.OpenCypherJavaCCWithFallbackParsing
import org.opencypher.v9_0.frontend.phases.Parsing
import org.opencypher.v9_0.frontend.phases.Phase
import org.opencypher.v9_0.frontend.phases.SemanticAnalysis
import org.opencypher.v9_0.rewriting.rewriters.projectNamedPaths
import org.opencypher.v9_0.util.AnonymousVariableNameGenerator
import org.opencypher.v9_0.util.InputPosition
import org.opencypher.v9_0.util.StepSequencer
import org.opencypher.v9_0.util.test_helpers.CypherFunSuite

class SemanticAnalysisTest extends CypherFunSuite {

  // This test invokes SemanticAnalysis twice because that's what the production pipeline does
  private def pipelineWithSemanticFeatures(semanticFeatures: SemanticFeature*) =
    OpenCypherJavaCCWithFallbackParsing andThen SemanticAnalysis(warn = true, semanticFeatures:_*) andThen SemanticAnalysis(warn = false, semanticFeatures:_*)

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
    // Query without proper escaping: CREATE CONSTRAINT abc123` FOR (n:``Label) REQUIRE (n.pr``op) IS NODE KEY
    val query = "CREATE CONSTRAINT `abc123``` FOR (n:`````Label`) REQUIRE (n.`pr````op`) IS NODE KEY"

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

  test("unit CALL { ... } IN TRANSACTIONS") {
    val query = "CALL { CREATE () } IN TRANSACTIONS RETURN 1 AS result"

    val startState = initStartState(query)
    val context = new ErrorCollectingContext()

    pipeline.transform(startState, context)

    context.errors.map(_.msg) shouldBe Seq(
      "The CALL { ... } IN TRANSACTIONS clause is not available in this implementation of Cypher due to lack of support for running subqueries in separate transactions."
    )
  }

  test("unit CALL { ... } IN TRANSACTIONS with feature enabled") {
    val query = "CALL { CREATE () } IN TRANSACTIONS RETURN 1 AS result"

    val startState = initStartState(query)
    val context = new ErrorCollectingContext()

    val pipeline = pipelineWithSemanticFeatures(SemanticFeature.CallSubqueryInTransactions)
    pipeline.transform(startState, context)

    context.errors.map(_.msg) shouldBe empty
  }

  test("returning CALL { ... } IN TRANSACTIONS") {
    val query = "CALL { MATCH (n) RETURN n AS n } IN TRANSACTIONS RETURN n AS n"

    val startState = initStartState(query)
    val context = new ErrorCollectingContext()

    val pipeline = pipelineWithSemanticFeatures(SemanticFeature.CorrelatedSubQueries, SemanticFeature.CallSubqueryInTransactions)
    pipeline.transform(startState, context)

    context.errors.map(_.msg) shouldBe Seq(
      "The returning CALL { ... } IN TRANSACTIONS clause is not available in this implementation of Cypher due to lack of support for running returning subqueries in separate transactions."
    )
  }

  test("returning CALL { ... } IN TRANSACTIONS with feature enabled") {
    val query = "WITH 1 AS x CALL { WITH x AS x MATCH (n) RETURN n AS n } IN TRANSACTIONS RETURN n AS n"

    val startState = initStartState(query)
    val context = new ErrorCollectingContext()

    val pipeline = pipelineWithSemanticFeatures(SemanticFeature.CorrelatedSubQueries, SemanticFeature.CallSubqueryInTransactions, SemanticFeature.CallReturningSubqueryInTransactions)
    pipeline.transform(startState, context)

    context.errors.map(_.msg) shouldBe empty
  }

  test("nested CALL { ... } IN TRANSACTIONS") {
    val query = "CALL { CALL { CREATE (x) } IN TRANSACTIONS } IN TRANSACTIONS RETURN 1 AS result"

    val startState = initStartState(query)
    val context = new ErrorCollectingContext()

    val pipeline = pipelineWithSemanticFeatures(SemanticFeature.CallSubqueryInTransactions)
    pipeline.transform(startState, context)

    context.errors shouldBe Seq(
      SemanticError("Nested CALL { ... } IN TRANSACTIONS is not supported", InputPosition(7, 1, 8))
    )
  }

  test("regular CALL nested in CALL { ... } IN TRANSACTIONS") {
    val query = "CALL { CALL { CREATE (x) } } IN TRANSACTIONS RETURN 1 AS result"

    val startState = initStartState(query)
    val context = new ErrorCollectingContext()

    val pipeline = pipelineWithSemanticFeatures(SemanticFeature.CallSubqueryInTransactions)
    pipeline.transform(startState, context)

    context.errors shouldBe empty
  }

  test("CALL { ... } IN TRANSACTIONS nested in a regular CALL") {
    val query = "CALL { CALL { CREATE (x) } IN TRANSACTIONS } RETURN 1 AS result"

    val startState = initStartState(query)
    val context = new ErrorCollectingContext()

    val pipeline = pipelineWithSemanticFeatures(SemanticFeature.CallSubqueryInTransactions)
    pipeline.transform(startState, context)

    context.errors shouldBe Seq(
      SemanticError("CALL { ... } IN TRANSACTIONS nested in a regular CALL is not supported", InputPosition(7, 1, 8))
    )
  }
  
  test("CALL { ... } IN TRANSACTIONS nested in a regular CALL and nested CALL { ... } IN TRANSACTIONS") {
    val query = "CALL { CALL { CALL { CREATE (x) } IN TRANSACTIONS } IN TRANSACTIONS } RETURN 1 AS result"

    val startState = initStartState(query)
    val context = new ErrorCollectingContext()

    val pipeline = pipelineWithSemanticFeatures(SemanticFeature.CallSubqueryInTransactions)
    pipeline.transform(startState, context)

    context.errors shouldBe Seq(
      SemanticError("Nested CALL { ... } IN TRANSACTIONS is not supported", InputPosition(14, 1, 15)),
      SemanticError("CALL { ... } IN TRANSACTIONS nested in a regular CALL is not supported", InputPosition(7, 1, 8))
    )
  }

  test("CALL { ... } IN TRANSACTIONS in a UNION") {
    val query =
      """CALL { CREATE (x) } IN TRANSACTIONS
        |RETURN 1 AS result
        |UNION
        |CALL { CREATE (x) } IN TRANSACTIONS
        |RETURN 2 AS result""".stripMargin

    val startState = initStartState(query)
    val context = new ErrorCollectingContext()

    val pipeline = pipelineWithSemanticFeatures(SemanticFeature.CallSubqueryInTransactions)
    pipeline.transform(startState, context)

    context.errors shouldBe Seq(
      SemanticError("CALL { ... } IN TRANSACTIONS in a UNION is not supported", InputPosition(0, 1, 1)),
      SemanticError("CALL { ... } IN TRANSACTIONS in a UNION is not supported", InputPosition(61, 4, 1))
    )
  }

  test("CALL { ... } IN TRANSACTIONS in first part of UNION") {
    val query =
      """CALL { CREATE (x) } IN TRANSACTIONS
        |RETURN 1 AS result
        |UNION
        |RETURN 2 AS result""".stripMargin

    val startState = initStartState(query)
    val context = new ErrorCollectingContext()

    val pipeline = pipelineWithSemanticFeatures(SemanticFeature.CallSubqueryInTransactions)
    pipeline.transform(startState, context)

    context.errors shouldBe Seq(
      SemanticError("CALL { ... } IN TRANSACTIONS in a UNION is not supported", InputPosition(0, 1, 1))
    )
  }

  test("CALL { ... } IN TRANSACTIONS in second part of UNION") {
    val query =
      """RETURN 1 AS result
        |UNION
        |CALL { CREATE (x) } IN TRANSACTIONS
        |RETURN 2 AS result""".stripMargin

    val startState = initStartState(query)
    val context = new ErrorCollectingContext()

    val pipeline = pipelineWithSemanticFeatures(SemanticFeature.CallSubqueryInTransactions)
    pipeline.transform(startState, context)

    context.errors shouldBe Seq(
      SemanticError("CALL { ... } IN TRANSACTIONS in a UNION is not supported", InputPosition(25, 3, 1))
    )
  }

  test("CALL { ... } IN TRANSACTIONS with a preceding write clause") {
    val query =
      """CREATE (foo)
        |WITH foo AS foo
        |CALL { CREATE (x) } IN TRANSACTIONS
        |RETURN foo AS foo""".stripMargin

    val startState = initStartState(query)
    val context = new ErrorCollectingContext()

    val pipeline = pipelineWithSemanticFeatures(SemanticFeature.CallSubqueryInTransactions)
    pipeline.transform(startState, context)

    context.errors shouldBe Seq(
      SemanticError("CALL { ... } IN TRANSACTIONS after a write clause is not supported", InputPosition(29, 3, 1))
    )
  }

  test("Multiple CALL { ... } IN TRANSACTIONS with preceding write clauses") {
    val query =
      """CREATE (foo)
        |WITH foo AS foo
        |CALL { CREATE (x) } IN TRANSACTIONS
        |CALL { CREATE (x) } IN TRANSACTIONS
        |RETURN foo AS foo""".stripMargin

    val startState = initStartState(query)
    val context = new ErrorCollectingContext()

    val pipeline = pipelineWithSemanticFeatures(SemanticFeature.CallSubqueryInTransactions)
    pipeline.transform(startState, context)

    context.errors shouldBe Seq(
      SemanticError("CALL { ... } IN TRANSACTIONS after a write clause is not supported", InputPosition(29, 3, 1)),
      SemanticError("CALL { ... } IN TRANSACTIONS after a write clause is not supported", InputPosition(65, 4, 1)),
    )
  }

  test("Multiple CALL { ... } IN TRANSACTIONS with a write clause between them") {
    val query =
      """CALL { CREATE (x) } IN TRANSACTIONS
        |CREATE (foo)
        |WITH foo AS foo
        |CALL { CREATE (x) } IN TRANSACTIONS
        |RETURN foo AS foo""".stripMargin

    val startState = initStartState(query)
    val context = new ErrorCollectingContext()

    val pipeline = pipelineWithSemanticFeatures(SemanticFeature.CallSubqueryInTransactions)
    pipeline.transform(startState, context)

    context.errors shouldBe Seq(
      SemanticError("CALL { ... } IN TRANSACTIONS after a write clause is not supported", InputPosition(65, 4, 1)),
    )
  }

  test("CALL { ... } IN TRANSACTIONS with a preceding nested write clause") {
    val query =
      """CALL { CREATE (foo) RETURN foo AS foo }
        |WITH foo AS foo
        |CALL { CREATE (x) } IN TRANSACTIONS
        |RETURN foo AS foo""".stripMargin

    val startState = initStartState(query)
    val context = new ErrorCollectingContext()

    val pipeline = pipelineWithSemanticFeatures(SemanticFeature.CallSubqueryInTransactions)
    pipeline.transform(startState, context)

    context.errors shouldBe Seq(
      SemanticError("CALL { ... } IN TRANSACTIONS after a write clause is not supported", InputPosition(56, 3, 1))
    )
  }

  test("CALL { ... } IN TRANSACTIONS with a preceding nested write clause in a unit subquery") {
    val query =
      """CALL { CREATE (x) }
        |WITH 1 AS foo
        |CALL { CREATE (x) } IN TRANSACTIONS
        |RETURN foo AS foo""".stripMargin

    val startState = initStartState(query)
    val context = new ErrorCollectingContext()

    val pipeline = pipelineWithSemanticFeatures(SemanticFeature.CallSubqueryInTransactions)
    pipeline.transform(startState, context)

    context.errors shouldBe Seq(
      SemanticError("CALL { ... } IN TRANSACTIONS after a write clause is not supported", InputPosition(34, 3, 1))
    )
  }

  test("Multiple CALL { ... } IN TRANSACTIONS that contain write clauses, but no write clauses in between") {
    val query =
      """CALL { CREATE (x) } IN TRANSACTIONS
        |WITH 1 AS foo
        |CALL { CREATE (y) } IN TRANSACTIONS
        |RETURN foo AS foo""".stripMargin

    val startState = initStartState(query)
    val context = new ErrorCollectingContext()

    val pipeline = pipelineWithSemanticFeatures(SemanticFeature.CallSubqueryInTransactions)
    pipeline.transform(startState, context)

    context.errors shouldBe empty
  }

  test("CALL { ... } IN TRANSACTIONS with a following write clause") {
    val query =
      """CALL { CREATE (x) } IN TRANSACTIONS
        |CREATE (foo)
        |RETURN foo AS foo""".stripMargin

    val startState = initStartState(query)
    val context = new ErrorCollectingContext()

    val pipeline = pipelineWithSemanticFeatures(SemanticFeature.CallSubqueryInTransactions)
    pipeline.transform(startState, context)

    context.errors shouldBe empty
  }

  test("should allow node pattern predicates in MATCH") {
    val query = "WITH 123 AS minValue MATCH (n {prop: 42} WHERE n.otherProp > minValue)-->(m:Label WHERE m.prop = 42) RETURN n AS result"

    val startState = initStartState(query)
    val context = new ErrorCollectingContext()
    pipeline.transform(startState, context)

    context.errors shouldBe empty
  }

  test("should not allow node pattern predicates in MATCH to refer to other nodes") {
    val query = "MATCH (start)-->(end:Label WHERE start.prop = 42) RETURN start AS result"

    val startState = initStartState(query)
    val context = new ErrorCollectingContext()
    pipeline.transform(startState, context)

    context.errors.map(_.msg) shouldBe Seq(
      "Variable `start` not defined"
    )
  }

  test("should not allow node pattern predicates in CREATE") {
    val query = "CREATE (n WHERE n.prop = 123)"

    val startState = initStartState(query)
    val context = new ErrorCollectingContext()
    pipeline.transform(startState, context)

    context.errors.map(_.msg) shouldBe Seq(
      "Node pattern predicates are not allowed in CREATE, but only in MATCH clause or inside a pattern comprehension"
    )
  }

  test("should not allow node pattern predicates in MERGE") {
    val query = "MERGE (n WHERE n.prop = 123)"

    val startState = initStartState(query)
    val context = new ErrorCollectingContext()
    pipeline.transform(startState, context)

    context.errors.map(_.msg) shouldBe Seq(
      "Node pattern predicates are not allowed in MERGE, but only in MATCH clause or inside a pattern comprehension"
    )
  }

  test("should allow node pattern predicates in pattern comprehension") {
    val query = "WITH 123 AS minValue RETURN [(n {prop: 42} WHERE n.otherProp > minValue)-->(m:Label WHERE m.prop = 42) | n] AS result"

    val startState = initStartState(query)
    val context = new ErrorCollectingContext()
    pipeline.transform(startState, context)

    context.errors shouldBe empty
  }

  test("should not allow node pattern predicates in pattern comprehension to refer to other nodes") {
    val query = "RETURN [(start)-->(end:Label WHERE start.prop = 42) | start] AS result"

    val startState = initStartState(query)
    val context = new ErrorCollectingContext()
    pipeline.transform(startState, context)

    context.errors.map(_.msg) shouldBe Seq(
      "Variable `start` not defined"
    )
  }

  test("should not allow node pattern predicates in pattern expression") {
    val query =
      """MATCH (a), (b)
        |RETURN exists((a WHERE a.prop > 123)-->(b)) AS result""".stripMargin

    val startState = initStartState(query)
    val context = new ErrorCollectingContext()
    pipeline.transform(startState, context)

    context.errors.map(_.msg) shouldBe Seq(
      "Node pattern predicates are not allowed in expression, but only in MATCH clause or inside a pattern comprehension"
    )
  }

  test("should allow node pattern predicates in MATCH with shortestPath") {
    val query =
      """
        |WITH 123 AS minValue
        |MATCH p = shortestPath((n {prop: 42} WHERE n.otherProp > minValue)-[:REL*]->(m:Label WHERE m.prop = 42))
        |RETURN n AS result
        |""".stripMargin

    val startState = initStartState(query)
    val context = new ErrorCollectingContext()
    pipeline.transform(startState, context)

    context.errors shouldBe empty
  }

  test("should not allow node pattern predicates in MATCH with shortestPath to refer to other nodes") {
    val query =
      """
        |MATCH p = shortestPath((start)-[:REL*]->(end:Label WHERE start.prop = 42))
        |RETURN start AS result""".stripMargin

    val startState = initStartState(query)
    val context = new ErrorCollectingContext()
    pipeline.transform(startState, context)

    context.errors.map(_.msg) shouldBe Seq(
      "Variable `start` not defined"
    )
  }

  test("should not allow node pattern predicates in shortestPath expression") {
    val query =
      """
        |MATCH (a), (b)
        |WITH shortestPath((a WHERE a.prop > 123)-[:REL*]->(b)) AS p
        |RETURN length(p) AS result""".stripMargin

    val startState = initStartState(query)
    val context = new ErrorCollectingContext()
    pipeline.transform(startState, context)

    context.errors.map(_.msg) shouldBe Seq(
      "Node pattern predicates are not allowed in expression, but only in MATCH clause or inside a pattern comprehension"
    )
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
