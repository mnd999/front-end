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
package org.opencypher.v9_0.parser

import org.opencypher.v9_0.ast
import org.opencypher.v9_0.ast.AstConstructionTestSupport
import org.opencypher.v9_0.ast.SubqueryCall
import org.parboiled.scala.Rule1

class SubqueryCallParserTest
  extends ParserAstTest[ast.SubqueryCall]
    with Query
    with Clauses
    with AstConstructionTestSupport {

  implicit val parser: Rule1[SubqueryCall] = SubqueryCall

  test("CALL { RETURN 1 }") {
    gives(subqueryCall(return_(literalInt(1).unaliased)))
  }

  test("CALL { CALL { RETURN 1 as a } }") {
    gives(subqueryCall(subqueryCall(return_(literalInt(1).as("a")))))
  }

  test("CALL { RETURN 1 AS a UNION RETURN 2 AS a }") {
    gives(subqueryCall(unionDistinct(
      singleQuery(return_(literalInt(1).as("a"))),
      singleQuery(return_(literalInt(2).as("a")))
    )))
  }

  test("CALL { }") {
    failsToParse
  }

  test("CALL { CREATE (n:N) }") {
    gives(subqueryCall(create(nodePat("n", "N"))))
  }
}
