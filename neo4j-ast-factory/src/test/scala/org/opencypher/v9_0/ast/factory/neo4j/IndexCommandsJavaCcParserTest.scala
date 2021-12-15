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
package org.opencypher.v9_0.ast.factory.neo4j

import org.opencypher.v9_0.ast
import org.opencypher.v9_0.ast.AstConstructionTestSupport
import org.opencypher.v9_0.ast.NoOptions
import org.opencypher.v9_0.ast.Options
import org.opencypher.v9_0.ast.factory.ASTExceptionFactory
import org.opencypher.v9_0.expressions
import org.opencypher.v9_0.expressions.LabelName
import org.opencypher.v9_0.expressions.RelTypeName
import org.opencypher.v9_0.expressions.Variable
import org.opencypher.v9_0.util.InputPosition
import org.opencypher.v9_0.util.test_helpers.TestName
import org.scalatest.FunSuiteLike

class IndexCommandsJavaCcParserTest extends ParserComparisonTestBase with FunSuiteLike with TestName with AstConstructionTestSupport {

  // Create node index (old syntax)

  test("CREATE INDEX ON :Person(name)") {
    assertSameAST(testName, comparePosition = false)
  }

  test("CREATE INDEX ON :Person(name,age)") {
    assertSameAST(testName, comparePosition = false)
  }

  test("CREATE INDEX my_index ON :Person(name)") {
    assertSameAST(testName)
  }

  test("CREATE INDEX my_index ON :Person(name,age)") {
    assertSameAST(testName)
  }

  test("CREATE OR REPLACE INDEX ON :Person(name)") {
    assertJavaCCException(testName, "'REPLACE' is not allowed for this index syntax (line 1, column 1 (offset: 0))")
  }

  // Create index

  test("CrEATe INDEX FOR (n1:Person) ON (n2.name)") {
    assertSameAST(testName)
  }

  // default type loop (parses as range, planned as btree)
  Seq(
    ("(n1:Person)", rangeNodeIndex: CreateRangeIndexFunction),
    ("()-[n1:R]-()", rangeRelIndex: CreateRangeIndexFunction),
    ("()-[n1:R]->()", rangeRelIndex: CreateRangeIndexFunction),
    ("()<-[n1:R]-()", rangeRelIndex: CreateRangeIndexFunction)
  ).foreach {
    case (pattern, createIndex: CreateRangeIndexFunction) =>
      test(s"CREATE INDEX FOR $pattern ON (n2.name)") {
        assertSameAST(testName)
      }

      test(s"USE neo4j CREATE INDEX FOR $pattern ON (n2.name)") {
        assertSameAST(testName)
      }

      test(s"CREATE INDEX FOR $pattern ON (n2.name, n3.age)") {
        assertSameAST(testName)
      }

      test(s"CREATE INDEX my_index FOR $pattern ON (n2.name)") {
        assertSameAST(testName)
      }

      test(s"CREATE INDEX my_index FOR $pattern ON (n2.name, n3.age)") {
        assertSameAST(testName)
      }

      test(s"CREATE INDEX `$$my_index` FOR $pattern ON (n2.name)") {
        assertSameAST(testName)
      }

      test(s"CREATE INDEX ON FOR $pattern ON (n2.name)") {
        assertSameAST(testName)
      }

      test(s"CREATE OR REPLACE INDEX FOR $pattern ON (n2.name)") {
        assertSameAST(testName)
      }

      test(s"CREATE OR REPLACE INDEX my_index FOR $pattern ON (n2.name, n3.age)") {
        assertSameAST(testName)
      }

      test(s"CREATE OR REPLACE INDEX IF NOT EXISTS FOR $pattern ON (n2.name)") {
        assertSameAST(testName)
      }

      test(s"CREATE OR REPLACE INDEX my_index IF NOT EXISTS FOR $pattern ON (n2.name)") {
        assertSameAST(testName)
      }

      test(s"CREATE INDEX IF NOT EXISTS FOR $pattern ON (n2.name, n3.age)") {
        assertSameAST(testName)
      }

      test(s"CREATE INDEX my_index IF NOT EXISTS FOR $pattern ON (n2.name)") {
        assertSameAST(testName)
      }

      test(s"CREATE INDEX FOR $pattern ON (n2.name) OPTIONS {indexProvider : 'native-btree-1.0'}") {
        assertSameAST(testName)
      }

      test(s"CREATE INDEX FOR $pattern ON (n2.name) OPTIONS {indexProvider : 'lucene+native-3.0', indexConfig : {`spatial.cartesian.max`: [100.0,100.0], `spatial.cartesian.min`: [-100.0,-100.0] }}") {
        assertSameAST(testName)
      }

      test(s"CREATE INDEX FOR $pattern ON (n2.name) OPTIONS {indexConfig : {`spatial.cartesian.max`: [100.0,100.0], `spatial.cartesian.min`: [-100.0,-100.0] }, indexProvider : 'lucene+native-3.0'}") {
        assertSameAST(testName)
      }

      test(s"CREATE INDEX FOR $pattern ON (n2.name) OPTIONS {indexConfig : {`spatial.wgs-84.max`: [60.0,60.0], `spatial.wgs-84.min`: [-40.0,-40.0] }}") {
        assertSameAST(testName)
      }

      test(s"CREATE INDEX FOR $pattern ON (n2.name) OPTIONS $$options") {
        assertSameAST(testName)
      }

      test(s"CREATE INDEX FOR $pattern ON (n2.name) OPTIONS {nonValidOption : 42}") {
        assertSameAST(testName)
      }

      test(s"CREATE INDEX my_index FOR $pattern ON (n2.name) OPTIONS {}") {
        assertSameAST(testName)
      }

      test(s"CREATE INDEX $$my_index FOR $pattern ON (n2.name)") {
        assertJavaCCExceptionStart(testName, """Invalid input '$': expected "ON" or an identifier""")
      }

      test(s"CREATE INDEX FOR $pattern ON n2.name") {
        assertJavaCCAST(testName,
          createIndex(List(prop("n2", "name", posN2(testName))), None, posN1(testName),
            ast.IfExistsThrowError, NoOptions, true)(defaultPos))
      }

      test(s"CREATE INDEX my_index FOR $pattern ON n2.name") {
        assertJavaCCAST(testName,
          createIndex(List(prop("n2", "name", posN2(testName))), Some("my_index"), posN1(testName),
            ast.IfExistsThrowError, NoOptions, true)(defaultPos))
      }

      test(s"CREATE OR REPLACE INDEX IF NOT EXISTS FOR $pattern ON n2.name") {
        assertJavaCCAST(testName, createIndex(List(prop("n2", "name", posN2(testName))), None, posN1(testName),
          ast.IfExistsInvalidSyntax, NoOptions, true)(defaultPos))
      }

      test(s"CREATE INDEX FOR $pattern ON (n2.name) {indexProvider : 'native-btree-1.0'}") {
        assertSameAST(testName)
      }

      test(s"CREATE INDEX FOR $pattern ON (n2.name) OPTIONS") {
        assertSameAST(testName)
      }
  }

  // range loop
  Seq(
    ("(n1:Person)", rangeNodeIndex: CreateRangeIndexFunction),
    ("()-[n1:R]-()", rangeRelIndex: CreateRangeIndexFunction),
    ("()-[n1:R]->()", rangeRelIndex: CreateRangeIndexFunction),
    ("()<-[n1:R]-()", rangeRelIndex: CreateRangeIndexFunction)
  ).foreach {
    case (pattern, createIndex: CreateRangeIndexFunction) =>
      test(s"CREATE RANGE INDEX FOR $pattern ON (n2.name)") {
        assertSameAST(testName)
      }

      test(s"USE neo4j CREATE RANGE INDEX FOR $pattern ON (n2.name)") {
        assertSameAST(testName)
      }

      test(s"CREATE RANGE INDEX FOR $pattern ON (n2.name, n3.age)") {
        assertSameAST(testName)
      }

      test(s"CREATE RANGE INDEX my_index FOR $pattern ON (n2.name)") {
        assertSameAST(testName)
      }

      test(s"CREATE RANGE INDEX my_index FOR $pattern ON (n2.name, n3.age)") {
        assertSameAST(testName)
      }

      test(s"CREATE RANGE INDEX `$$my_index` FOR $pattern ON (n2.name)") {
        assertSameAST(testName)
      }

      test(s"CREATE OR REPLACE RANGE INDEX FOR $pattern ON (n2.name)") {
        assertSameAST(testName)
      }

      test(s"CREATE OR REPLACE RANGE INDEX my_index FOR $pattern ON (n2.name, n3.age)") {
        assertSameAST(testName)
      }

      test(s"CREATE OR REPLACE RANGE INDEX IF NOT EXISTS FOR $pattern ON (n2.name)") {
        assertSameAST(testName)
      }

      test(s"CREATE OR REPLACE RANGE INDEX my_index IF NOT EXISTS FOR $pattern ON (n2.name)") {
        assertSameAST(testName)
      }

      test(s"CREATE RANGE INDEX IF NOT EXISTS FOR $pattern ON (n2.name, n3.age)") {
        assertSameAST(testName)
      }

      test(s"CREATE RANGE INDEX my_index IF NOT EXISTS FOR $pattern ON (n2.name)") {
        assertSameAST(testName)
      }

      test(s"CREATE RANGE INDEX FOR $pattern ON (n2.name) OPTIONS {indexProvider : 'range-1.0'}") {
        assertSameAST(testName)
      }

      test(s"CREATE RANGE INDEX FOR $pattern ON (n2.name) OPTIONS {indexProvider : 'range-1.0', indexConfig : {someConfig: 'toShowItCanBePrettified'}}") {
        assertSameAST(testName)
      }

      test(s"CREATE RANGE INDEX FOR $pattern ON (n2.name) OPTIONS {indexConfig : {someConfig: 'toShowItCanBePrettified'}, indexProvider : 'range-1.0'}") {
        assertSameAST(testName)
      }

      test(s"CREATE RANGE INDEX FOR $pattern ON (n2.name) OPTIONS {indexConfig : {}}") {
        assertSameAST(testName)
      }

      test(s"CREATE RANGE INDEX FOR $pattern ON (n2.name) OPTIONS $$options") {
        assertSameAST(testName)
      }

      test(s"CREATE RANGE INDEX FOR $pattern ON (n2.name) OPTIONS {nonValidOption : 42}") {
        assertSameAST(testName)
      }

      test(s"CREATE RANGE INDEX my_index FOR $pattern ON (n2.name) OPTIONS {}") {
        assertSameAST(testName)
      }

      test(s"CREATE RANGE INDEX $$my_index FOR $pattern ON (n2.name)") {
        assertJavaCCExceptionStart(testName, """Invalid input '$': expected "FOR", "IF" or an identifier""")
      }

      test(s"CREATE RANGE INDEX FOR $pattern ON n2.name") {
        assertJavaCCAST(testName, createIndex(List(prop("n2", "name", posN2(testName))), None, posN1(testName),
          ast.IfExistsThrowError, NoOptions, false)(defaultPos))
      }

      test(s"CREATE RANGE INDEX my_index FOR $pattern ON n2.name") {
        assertJavaCCAST(testName,
          createIndex(List(prop("n2", "name")), Some("my_index"), pos, ast.IfExistsThrowError, NoOptions, false),
          comparePosition = false)
      }

      test(s"CREATE OR REPLACE RANGE INDEX IF NOT EXISTS FOR $pattern ON n2.name") {
        assertJavaCCAST(testName,
          createIndex(List(prop("n2", "name")), None, pos, ast.IfExistsInvalidSyntax, NoOptions, false),
          comparePosition = false)
      }

      test(s"CREATE RANGE INDEX FOR $pattern ON (n2.name) {indexProvider : 'range-1.0'}") {
        assertSameAST(testName)
      }

      test(s"CREATE RANGE INDEX FOR $pattern ON (n2.name) OPTIONS") {
        assertSameAST(testName)
      }
  }

  // btree loop
  Seq(
    ("(n1:Person)", btreeNodeIndex: CreateIndexFunction),
    ("()-[n1:R]-()", btreeRelIndex: CreateIndexFunction),
    ("()-[n1:R]->()", btreeRelIndex: CreateIndexFunction),
    ("()<-[n1:R]-()", btreeRelIndex: CreateIndexFunction)
  ).foreach {
    case (pattern, createIndex: CreateIndexFunction) =>
      test(s"CREATE BTREE INDEX FOR $pattern ON (n2.name)") {
        assertSameAST(testName)
      }

      test(s"USE neo4j CREATE BTREE INDEX FOR $pattern ON (n2.name)") {
        assertSameAST(testName)
      }

      test(s"CREATE BTREE INDEX FOR $pattern ON (n2.name, n3.age)") {
        assertSameAST(testName)
      }

      test(s"CREATE BTREE INDEX my_index FOR $pattern ON (n2.name)") {
        assertSameAST(testName)
      }

      test(s"CREATE BTREE INDEX my_index FOR $pattern ON (n2.name, n3.age)") {
        assertSameAST(testName)
      }

      test(s"CREATE BTREE INDEX `$$my_index` FOR $pattern ON (n2.name)") {
        assertSameAST(testName)
      }

      test(s"CREATE OR REPLACE BTREE INDEX FOR $pattern ON (n2.name)") {
        assertSameAST(testName)
      }

      test(s"CREATE OR REPLACE BTREE INDEX my_index FOR $pattern ON (n2.name, n3.age)") {
        assertSameAST(testName)
      }

      test(s"CREATE OR REPLACE BTREE INDEX IF NOT EXISTS FOR $pattern ON (n2.name)") {
        assertSameAST(testName)
      }

      test(s"CREATE OR REPLACE BTREE INDEX my_index IF NOT EXISTS FOR $pattern ON (n2.name)") {
        assertSameAST(testName)
      }

      test(s"CREATE BTREE INDEX IF NOT EXISTS FOR $pattern ON (n2.name, n3.age)") {
        assertSameAST(testName)
      }

      test(s"CREATE BTREE INDEX my_index IF NOT EXISTS FOR $pattern ON (n2.name)") {
        assertSameAST(testName)
      }

      test(s"CREATE BTREE INDEX FOR $pattern ON (n2.name) OPTIONS {indexProvider : 'native-btree-1.0'}") {
        assertSameAST(testName)
      }

      test(s"CREATE BTREE INDEX FOR $pattern ON (n2.name) OPTIONS {indexProvider : 'lucene+native-3.0', indexConfig : {`spatial.cartesian.max`: [100.0,100.0], `spatial.cartesian.min`: [-100.0,-100.0] }}") {
        assertSameAST(testName)
      }

      test(s"CREATE BTREE INDEX FOR $pattern ON (n2.name) OPTIONS {indexConfig : {`spatial.cartesian.max`: [100.0,100.0], `spatial.cartesian.min`: [-100.0,-100.0] }, indexProvider : 'lucene+native-3.0'}") {
        assertSameAST(testName)
      }

      test(s"CREATE BTREE INDEX FOR $pattern ON (n2.name) OPTIONS {indexConfig : {`spatial.wgs-84.max`: [60.0,60.0], `spatial.wgs-84.min`: [-40.0,-40.0] }}") {
        assertSameAST(testName)
      }

      test(s"CREATE BTREE INDEX FOR $pattern ON (n2.name) OPTIONS $$options") {
        assertSameAST(testName)
      }

      test(s"CREATE BTREE INDEX FOR $pattern ON (n2.name) OPTIONS {nonValidOption : 42}") {
        assertSameAST(testName)
      }

      test(s"CREATE BTREE INDEX my_index FOR $pattern ON (n2.name) OPTIONS {}") {
        assertSameAST(testName)
      }

      test(s"CREATE BTREE INDEX $$my_index FOR $pattern ON (n2.name)") {
        assertJavaCCExceptionStart(testName, """Invalid input '$': expected "FOR", "IF" or an identifier""")
      }

      test(s"CREATE BTREE INDEX FOR $pattern ON n2.name") {
        assertJavaCCAST(testName, createIndex(List(prop("n2", "name", posN2(testName))), None, posN1(testName),
          ast.IfExistsThrowError, NoOptions)(defaultPos))
      }

      test(s"CREATE BTREE INDEX my_index FOR $pattern ON n2.name") {
        assertJavaCCAST(testName, createIndex(List(prop("n2", "name", posN2(testName))), Some("my_index"), posN1(testName),
          ast.IfExistsThrowError, NoOptions)(defaultPos))
      }

      test(s"CREATE OR REPLACE BTREE INDEX IF NOT EXISTS FOR $pattern ON n2.name") {
        assertJavaCCAST(testName, createIndex(List(prop("n2", "name", posN2(testName))), None, posN1(testName),
          ast.IfExistsInvalidSyntax, NoOptions)(defaultPos))
      }

      test(s"CREATE BTREE INDEX FOR $pattern ON (n2.name) {indexProvider : 'native-btree-1.0'}") {
        assertSameAST(testName)
      }

      test(s"CREATE BTREE INDEX FOR $pattern ON (n2.name) OPTIONS") {
        assertSameAST(testName)
      }
  }

  // lookup loop
  Seq(
    ("(n1)", "labels(n2)"),
    ("()-[r1]-()", "type(r2)"),
    ("()-[r1]->()", "type(r2)"),
    ("()<-[r1]-()", "type(r2)")
  ).foreach {
    case (pattern, function) =>
      test(s"CREATE LOOKUP INDEX FOR $pattern ON EACH $function") {
        assertSameAST(testName)
      }

      test(s"USE neo4j CREATE LOOKUP INDEX FOR $pattern ON EACH $function") {
        assertSameAST(testName)
      }

      test(s"CREATE LOOKUP INDEX my_index FOR $pattern ON EACH $function") {
        assertSameAST(testName)
      }

      test(s"CREATE LOOKUP INDEX `$$my_index` FOR $pattern ON EACH $function") {
        assertSameAST(testName)
      }

      test(s"CREATE OR REPLACE LOOKUP INDEX FOR $pattern ON EACH $function") {
        assertSameAST(testName)
      }

      test(s"CREATE OR REPLACE LOOKUP INDEX my_index FOR $pattern ON EACH $function") {
        assertSameAST(testName)
      }

      test(s"CREATE OR REPLACE LOOKUP INDEX IF NOT EXISTS FOR $pattern ON EACH $function") {
        assertSameAST(testName)
      }

      test(s"CREATE OR REPLACE LOOKUP INDEX my_index IF NOT EXISTS FOR $pattern ON EACH $function") {
        assertSameAST(testName)
      }

      test(s"CREATE LOOKUP INDEX IF NOT EXISTS FOR $pattern ON EACH $function") {
        assertSameAST(testName)
      }

      test(s"CREATE LOOKUP INDEX my_index IF NOT EXISTS FOR $pattern ON EACH $function") {
        assertSameAST(testName)
      }

      test(s"CREATE LOOKUP INDEX FOR $pattern ON EACH $function OPTIONS {anyOption : 42}") {
        assertSameAST(testName)
      }

      test(s"CREATE LOOKUP INDEX my_index FOR $pattern ON EACH $function OPTIONS {}") {
        assertSameAST(testName)
      }

      test(s"CREATE LOOKUP INDEX $$my_index FOR $pattern ON EACH $function") {
        assertSameAST(testName)
      }

      test(s"CREATE LOOKUP INDEX FOR $pattern ON EACH $function {indexProvider : 'native-btree-1.0'}") {
        assertSameAST(testName)
      }

      test(s"CREATE LOOKUP INDEX FOR $pattern ON EACH $function OPTIONS") {
        assertSameAST(testName)
      }
  }

  // fulltext loop
  Seq(
    "(n1:Person)",
    "(n1:Person|Colleague|Friend)",
    "()-[n1:R]->()",
    "()<-[n1:R|S]-()"
  ).foreach {
    pattern =>
      test(s"CREATE FULLTEXT INDEX FOR $pattern ON EACH [n2.name]") {
        assertSameAST(testName)
      }

      test(s"USE neo4j CREATE FULLTEXT INDEX FOR $pattern ON EACH [n2.name]") {
        assertSameAST(testName)
      }

      test(s"CREATE FULLTEXT INDEX FOR $pattern ON EACH [n2.name, n3.age]") {
        assertSameAST(testName)
      }

      test(s"CREATE FULLTEXT INDEX my_index FOR $pattern ON EACH [n2.name]") {
        assertSameAST(testName)
      }

      test(s"CREATE FULLTEXT INDEX my_index FOR $pattern ON EACH [n2.name, n3.age]") {
        assertSameAST(testName)
      }

      test(s"CREATE FULLTEXT INDEX `$$my_index` FOR $pattern ON EACH [n2.name]") {
        assertSameAST(testName)
      }

      test(s"CREATE OR REPLACE FULLTEXT INDEX FOR $pattern ON EACH [n2.name]") {
        assertSameAST(testName)
      }

      test(s"CREATE OR REPLACE FULLTEXT INDEX my_index FOR $pattern ON EACH [n2.name, n3.age]") {
        assertSameAST(testName)
      }

      test(s"CREATE OR REPLACE FULLTEXT INDEX IF NOT EXISTS FOR $pattern ON EACH [n2.name]") {
        assertSameAST(testName)
      }

      test(s"CREATE OR REPLACE FULLTEXT INDEX my_index IF NOT EXISTS FOR $pattern ON EACH [n2.name]") {
        assertSameAST(testName)
      }

      test(s"CREATE FULLTEXT INDEX IF NOT EXISTS FOR $pattern ON EACH [n2.name, n3.age]") {
        assertSameAST(testName)
      }

      test(s"CREATE FULLTEXT INDEX my_index IF NOT EXISTS FOR $pattern ON EACH [n2.name]") {
        assertSameAST(testName)
      }

      test(s"CREATE FULLTEXT INDEX FOR $pattern ON EACH [n2.name] OPTIONS {indexProvider : 'fulltext-1.0'}") {
        assertSameAST(testName)
      }

      test(s"CREATE FULLTEXT INDEX FOR $pattern ON EACH [n2.name] OPTIONS {indexProvider : 'fulltext-1.0', indexConfig : {`fulltext.analyzer`: 'some_analyzer'}}") {
        assertSameAST(testName)
      }

      test(s"CREATE FULLTEXT INDEX FOR $pattern ON EACH [n2.name] OPTIONS {indexConfig : {`fulltext.eventually_consistent`: false}, indexProvider : 'fulltext-1.0'}") {
        assertSameAST(testName)
      }

      test(s"CREATE FULLTEXT INDEX FOR $pattern ON EACH [n2.name] OPTIONS {indexConfig : {`fulltext.analyzer`: 'some_analyzer', `fulltext.eventually_consistent`: true}}") {
        assertSameAST(testName)
      }

      test(s"CREATE FULLTEXT INDEX FOR $pattern ON EACH [n2.name] OPTIONS {nonValidOption : 42}") {
        assertSameAST(testName)
      }

      test(s"CREATE FULLTEXT INDEX my_index FOR $pattern ON EACH [n2.name] OPTIONS {}") {
        assertSameAST(testName)
      }

      test(s"CREATE FULLTEXT INDEX my_index FOR $pattern ON EACH [n2.name] OPTIONS $$options") {
        assertSameAST(testName)
      }

      test(s"CREATE FULLTEXT INDEX $$my_index FOR $pattern ON EACH [n2.name]") {
        assertSameAST(testName)
      }

      test(s"CREATE FULLTEXT INDEX FOR $pattern ON EACH [n2.name] {indexProvider : 'fulltext-1.0'}") {
        assertSameAST(testName)
      }

      test(s"CREATE FULLTEXT INDEX FOR $pattern ON EACH [n2.name] OPTIONS") {
        assertSameAST(testName)
      }

      test(s"CREATE FULLTEXT INDEX FOR $pattern ON EACH (n2.name)") {
        assertSameAST(testName)
      }

      test(s"CREATE FULLTEXT INDEX FOR $pattern ON EACH n2.name") {
        assertSameAST(testName)
      }

      test(s"CREATE FULLTEXT INDEX FOR $pattern ON EACH []") {
        assertSameAST(testName)
      }

      test(s"CREATE FULLTEXT INDEX FOR $pattern ON EACH") {
        assertSameAST(testName)
      }

      test(s"CREATE FULLTEXT INDEX FOR $pattern ON [n2.name]") {
        assertSameAST(testName)
      }

      test(s"CREATE INDEX FOR $pattern ON EACH [n2.name]") {
        assertJavaCCExceptionStart(testName, "Invalid input") //different failures depending on pattern
      }

      // Missing escaping around `fulltext.analyzer`
      test(s"CREATE FULLTEXT INDEX FOR $pattern ON EACH [n2.name] OPTIONS {indexConfig : {fulltext.analyzer: 'some_analyzer'}}") {
        assertJavaCCExceptionStart(testName, "Invalid input '{': expected \"+\" or \"-\"")
      }
  }

  // text loop
  Seq(
    ("(n1:Person)", textNodeIndex: CreateIndexFunction),
    ("()-[n1:R]-()", textRelIndex: CreateIndexFunction),
    ("()-[n1:R]->()", textRelIndex: CreateIndexFunction),
    ("()<-[n1:R]-()", textRelIndex: CreateIndexFunction)
  ).foreach {
    case (pattern, createIndex: CreateIndexFunction) =>
      test(s"CREATE TEXT INDEX FOR $pattern ON (n2.name)") {
        assertSameAST(testName)
      }

      test(s"USE neo4j CREATE TEXT INDEX FOR $pattern ON (n2.name)") {
        assertSameAST(testName)
      }

      test(s"CREATE TEXT INDEX FOR $pattern ON (n2.name, n3.age)") {
        assertSameAST(testName)
      }

      test(s"CREATE TEXT INDEX my_index FOR $pattern ON (n2.name)") {
        assertSameAST(testName)
      }

      test(s"CREATE TEXT INDEX my_index FOR $pattern ON (n2.name, n3.age)") {
        assertSameAST(testName)
      }

      test(s"CREATE TEXT INDEX `$$my_index` FOR $pattern ON (n2.name)") {
        assertSameAST(testName)
      }

      test(s"CREATE OR REPLACE TEXT INDEX FOR $pattern ON (n2.name)") {
        assertSameAST(testName)
      }

      test(s"CREATE OR REPLACE TEXT INDEX my_index FOR $pattern ON (n2.name, n3.age)") {
        assertSameAST(testName)
      }

      test(s"CREATE OR REPLACE TEXT INDEX IF NOT EXISTS FOR $pattern ON (n2.name)") {
        assertSameAST(testName)
      }

      test(s"CREATE OR REPLACE TEXT INDEX my_index IF NOT EXISTS FOR $pattern ON (n2.name)") {
        assertSameAST(testName)
      }

      test(s"CREATE TEXT INDEX IF NOT EXISTS FOR $pattern ON (n2.name, n3.age)") {
        assertSameAST(testName)
      }

      test(s"CREATE TEXT INDEX my_index IF NOT EXISTS FOR $pattern ON (n2.name)") {
        assertSameAST(testName)
      }

      test(s"CREATE TEXT INDEX FOR $pattern ON (n2.name) OPTIONS {indexProvider : 'text-1.0'}") {
        assertSameAST(testName)
      }

      test(s"CREATE TEXT INDEX FOR $pattern ON (n2.name) OPTIONS {indexProvider : 'text-1.0', indexConfig : {`spatial.cartesian.max`: [100.0,100.0], `spatial.cartesian.min`: [-100.0,-100.0] }}") {
        assertSameAST(testName)
      }

      test(s"CREATE TEXT INDEX FOR $pattern ON (n2.name) OPTIONS {indexConfig : {`spatial.cartesian.max`: [100.0,100.0], `spatial.cartesian.min`: [-100.0,-100.0] }, indexProvider : 'text-1.0'}") {
        assertSameAST(testName)
      }

      test(s"CREATE TEXT INDEX FOR $pattern ON (n2.name) OPTIONS {indexConfig : {`spatial.wgs-84.max`: [60.0,60.0], `spatial.wgs-84.min`: [-40.0,-40.0] }}") {
        assertSameAST(testName)
      }

      test(s"CREATE TEXT INDEX FOR $pattern ON (n2.name) OPTIONS $$options") {
        assertSameAST(testName)
      }

      test(s"CREATE TEXT INDEX FOR $pattern ON (n2.name) OPTIONS {nonValidOption : 42}") {
        assertSameAST(testName)
      }

      test(s"CREATE TEXT INDEX my_index FOR $pattern ON (n2.name) OPTIONS {}") {
        assertSameAST(testName)
      }

      test(s"CREATE TEXT INDEX $$my_index FOR $pattern ON (n2.name)") {
        assertJavaCCExceptionStart(testName, """Invalid input '$': expected "FOR", "IF" or an identifier""")
      }

      test(s"CREATE TEXT INDEX FOR $pattern ON n2.name") {
        assertJavaCCAST(testName, createIndex(List(prop("n2", "name", posN2(testName))), None, posN1(testName),
          ast.IfExistsThrowError, NoOptions)(defaultPos))
      }

      test(s"CREATE TEXT INDEX my_index FOR $pattern ON n2.name") {
        assertJavaCCAST(testName, createIndex(List(prop("n2", "name", posN2(testName))), Some("my_index"), posN1(testName),
          ast.IfExistsThrowError, NoOptions)(defaultPos))
      }

      test(s"CREATE OR REPLACE TEXT INDEX IF NOT EXISTS FOR $pattern ON n2.name") {
        assertJavaCCAST(testName, createIndex(List(prop("n2", "name", posN2(testName))), None, posN1(testName),
          ast.IfExistsInvalidSyntax, NoOptions)(defaultPos))
      }

      test(s"CREATE TEXT INDEX my_index FOR $pattern ON n2.name, n3.age") {
        assertJavaCCExceptionStart(testName, """Invalid input ',': expected "OPTIONS" or <EOF>""")
      }

      test(s"CREATE TEXT INDEX FOR $pattern ON (n2.name) {indexProvider : 'text-1.0'}") {
        assertSameAST(testName)
      }

      test(s"CREATE TEXT INDEX FOR $pattern ON (n2.name) OPTIONS") {
        assertSameAST(testName)
      }
  }

  // point loop
  Seq(
    ("(n1:Person)", pointNodeIndex: CreateIndexFunction),
    ("()-[n1:R]-()", pointRelIndex: CreateIndexFunction),
    ("()-[n1:R]->()", pointRelIndex: CreateIndexFunction),
    ("()<-[n1:R]-()", pointRelIndex: CreateIndexFunction)
  ).foreach {
    case (pattern, createIndex: CreateIndexFunction) =>
      test(s"CREATE POINT INDEX FOR $pattern ON (n2.name)") {
        assertSameAST(testName)
      }

      test(s"USE neo4j CREATE POINT INDEX FOR $pattern ON (n2.name)") {
        assertSameAST(testName)
      }

      test(s"CREATE POINT INDEX FOR $pattern ON (n2.name, n3.age)") {
        assertSameAST(testName)
      }

      test(s"CREATE POINT INDEX my_index FOR $pattern ON (n2.name)") {
        assertSameAST(testName)
      }

      test(s"CREATE POINT INDEX my_index FOR $pattern ON (n2.name, n3.age)") {
        assertSameAST(testName)
      }

      test(s"CREATE POINT INDEX `$$my_index` FOR $pattern ON (n2.name)") {
        assertSameAST(testName)
      }

      test(s"CREATE OR REPLACE POINT INDEX FOR $pattern ON (n2.name)") {
        assertSameAST(testName)
      }

      test(s"CREATE OR REPLACE POINT INDEX my_index FOR $pattern ON (n2.name, n3.age)") {
        assertSameAST(testName)
      }

      test(s"CREATE OR REPLACE POINT INDEX IF NOT EXISTS FOR $pattern ON (n2.name)") {
        assertSameAST(testName)
      }

      test(s"CREATE OR REPLACE POINT INDEX my_index IF NOT EXISTS FOR $pattern ON (n2.name)") {
        assertSameAST(testName)
      }

      test(s"CREATE POINT INDEX IF NOT EXISTS FOR $pattern ON (n2.name, n3.age)") {
        assertSameAST(testName)
      }

      test(s"CREATE POINT INDEX my_index IF NOT EXISTS FOR $pattern ON (n2.name)") {
        assertSameAST(testName)
      }

      test(s"CREATE POINT INDEX FOR $pattern ON (n2.name) OPTIONS {indexProvider : 'point-1.0'}") {
        assertSameAST(testName)
      }

      test(s"CREATE POINT INDEX FOR $pattern ON (n2.name) OPTIONS {indexProvider : 'point-1.0', indexConfig : {`spatial.cartesian.max`: [100.0,100.0], `spatial.cartesian.min`: [-100.0,-100.0] }}") {
        assertSameAST(testName)
      }

      test(s"CREATE POINT INDEX FOR $pattern ON (n2.name) OPTIONS {indexConfig : {`spatial.cartesian.max`: [100.0,100.0], `spatial.cartesian.min`: [-100.0,-100.0] }, indexProvider : 'point-1.0'}") {
        assertSameAST(testName)
      }

      test(s"CREATE POINT INDEX FOR $pattern ON (n2.name) OPTIONS {indexConfig : {`spatial.wgs-84.max`: [60.0,60.0], `spatial.wgs-84.min`: [-40.0,-40.0] }}") {
        assertSameAST(testName)
      }

      test(s"CREATE POINT INDEX FOR $pattern ON (n2.name) OPTIONS $$options") {
        assertSameAST(testName)
      }

      test(s"CREATE POINT INDEX FOR $pattern ON (n2.name) OPTIONS {nonValidOption : 42}") {
        assertSameAST(testName)
      }

      test(s"CREATE POINT INDEX my_index FOR $pattern ON (n2.name) OPTIONS {}") {
        assertSameAST(testName)
      }

      test(s"CREATE POINT INDEX $$my_index FOR $pattern ON (n2.name)") {
        assertJavaCCExceptionStart(testName, """Invalid input '$': expected "FOR", "IF" or an identifier""")
      }

      test(s"CREATE POINT INDEX FOR $pattern ON n2.name") {
        assertJavaCCAST(testName, createIndex(List(prop("n2", "name", posN2(testName))), None, posN1(testName),
          ast.IfExistsThrowError, NoOptions)(defaultPos))
      }

      test(s"CREATE POINT INDEX my_index FOR $pattern ON n2.name") {
        assertJavaCCAST(testName, createIndex(List(prop("n2", "name", posN2(testName))), Some("my_index"), posN1(testName),
          ast.IfExistsThrowError, NoOptions)(defaultPos))
      }

      test(s"CREATE OR REPLACE POINT INDEX IF NOT EXISTS FOR $pattern ON n2.name") {
        assertJavaCCAST(testName, createIndex(List(prop("n2", "name", posN2(testName))), None, posN1(testName),
          ast.IfExistsInvalidSyntax, NoOptions)(defaultPos))
      }

      test(s"CREATE POINT INDEX my_index FOR $pattern ON n2.name, n3.age") {
        assertJavaCCExceptionStart(testName, """Invalid input ',': expected "OPTIONS" or <EOF>""")
      }

      test(s"CREATE POINT INDEX FOR $pattern ON (n2.name) {indexProvider : 'point-1.0'}") {
        assertSameAST(testName)
      }

      test(s"CREATE POINT INDEX FOR $pattern ON (n2.name) OPTIONS") {
        assertSameAST(testName)
      }
  }

  test("CREATE LOOKUP INDEX FOR (x1) ON EACH labels(x2)") {
    assertSameAST(testName)
  }

  test("CREATE LOOKUP INDEX FOR ()-[x1]-() ON EACH type(x2)") {
    assertSameAST(testName)
  }

  test("CREATE LOOKUP INDEX FOR (n1) ON EACH count(n2)") {
    assertSameAST(testName)
  }

  test("CREATE LOOKUP INDEX FOR (n1) ON EACH type(n2)") {
    assertSameAST(testName)
  }

  test("CREATE LOOKUP INDEX FOR (n) ON EACH labels(x)") {
    assertSameAST(testName)
  }

  test("CREATE LOOKUP INDEX FOR ()-[r1]-() ON EACH count(r2)") {
    assertSameAST(testName)
  }

  test("CREATE LOOKUP INDEX FOR ()-[r1]-() ON EACH labels(r2)") {
    assertSameAST(testName)
  }

  test("CREATE LOOKUP INDEX FOR ()-[r]-() ON EACH type(x)") {
    assertSameAST(testName)
  }

  test("CREATE LOOKUP INDEX FOR ()-[r1]-() ON type(r2)") {
    assertSameAST(testName)
  }

  test("CREATE LOOKUP INDEX FOR (x) ON EACH EACH(x)") {
    assertSameAST(testName)
  }

  test("CREATE LOOKUP INDEX FOR ()-[x]-() ON EACH EACH(x)") {
    assertSameAST(testName)
  }

  test("CREATE LOOKUP INDEX FOR ()-[x]-() ON EACH(x)") {
    // Thinks it is missing the function name since `EACH` is parsed as keyword
    assertSameAST(testName)
  }


  test("CREATE INDEX FOR n1:Person ON (n2.name)") {
    assertSameAST(testName)
  }

  test("CREATE INDEX FOR -[r1:R]-() ON (r2.name)") {
    assertSameAST(testName)
  }

  test("CREATE INDEX FOR ()-[r1:R]- ON (r2.name)") {
    //parboiled expects a space here, whereas java cc is whitespace ignorant
    assertJavaCCException(testName, "Invalid input 'ON': expected \"(\", \">\" or <ARROW_RIGHT_HEAD> (line 1, column 29 (offset: 28))")
  }

  test("CREATE INDEX FOR -[r1:R]- ON (r2.name)") {
    assertSameAST(testName)
  }

  test("CREATE INDEX FOR [r1:R] ON (r2.name)") {
    assertSameAST(testName)
  }

  test("CREATE TEXT INDEX FOR n1:Person ON (n2.name)") {
    assertSameAST(testName)
  }

  test("CREATE TEXT INDEX FOR -[r1:R]-() ON (r2.name)") {
    assertSameAST(testName)
  }

  test("CREATE TEXT INDEX FOR ()-[r1:R]- ON (r2.name)") {
    //parboiled expects a space here, whereas java cc is whitespace ignorant
    assertJavaCCException(testName, "Invalid input 'ON': expected \"(\", \">\" or <ARROW_RIGHT_HEAD> (line 1, column 34 (offset: 33))")
  }

  test("CREATE TEXT INDEX FOR -[r1:R]- ON (r2.name)") {
    assertSameAST(testName)
  }

  test("CREATE TEXT INDEX FOR [r1:R] ON (r2.name)") {
    assertSameAST(testName)
  }

  test("CREATE POINT INDEX FOR n1:Person ON (n2.name)") {
    assertSameAST(testName)
  }

  test("CREATE POINT INDEX FOR -[r1:R]-() ON (r2.name)") {
    assertSameAST(testName)
  }

  test("CREATE POINT INDEX FOR ()-[r1:R]- ON (r2.name)") {
    //parboiled expects a space here, whereas java cc is whitespace ignorant
    assertJavaCCException(testName, "Invalid input 'ON': expected \"(\", \">\" or <ARROW_RIGHT_HEAD> (line 1, column 35 (offset: 34))")
  }

  test("CREATE POINT INDEX FOR -[r1:R]- ON (r2.name)") {
    assertSameAST(testName)
  }

  test("CREATE POINT INDEX FOR [r1:R] ON (r2.name)") {
    assertSameAST(testName)
  }

  test("CREATE LOOKUP INDEX FOR n1 ON EACH labels(n2)") {
    assertSameAST(testName)
  }

  test("CREATE LOOKUP INDEX FOR -[r1]-() ON EACH type(r2)") {
    assertSameAST(testName)
  }

  test("CREATE LOOKUP INDEX FOR ()-[r1]- ON EACH type(r2)") {
    //parboiled expects a space here, whereas java cc is whitespace ignorant
    assertJavaCCException(testName, "Invalid input 'ON': expected \"(\", \">\" or <ARROW_RIGHT_HEAD> (line 1, column 34 (offset: 33))")
  }

  test("CREATE LOOKUP INDEX FOR -[r1]- ON EACH type(r2)") {
    assertSameAST(testName)
  }

  test("CREATE LOOKUP INDEX FOR [r1] ON EACH type(r2)") {
    assertSameAST(testName)
  }

  test("CREATE LOOKUP INDEX FOR (n1) EACH labels(n2)") {
    assertSameAST(testName)
  }

  test("CREATE LOOKUP INDEX FOR ()-[r1]-() EACH type(r2)") {
    assertSameAST(testName)
  }

  test("CREATE LOOKUP INDEX FOR (n1) ON labels(n2)") {
    assertSameAST(testName)
  }

  test("CREATE INDEX FOR (n1) ON EACH labels(n2)") {
    assertSameAST(testName)
  }

  test("CREATE INDEX FOR ()-[r1]-() ON EACH type(r2)") {
    assertSameAST(testName)
  }

  test("CREATE FULLTEXT INDEX FOR (n1) ON EACH [n2.x]") {
    assertSameAST(testName)
  }

  test("CREATE FULLTEXT INDEX FOR ()-[n1]-() ON EACH [n2.x]") {
    assertSameAST(testName)
  }

  test("CREATE FULLTEXT INDEX FOR (n1|:A) ON EACH [n2.x]") {
    assertSameAST(testName)
  }

  test("CREATE FULLTEXT INDEX FOR ()-[n1|:R]-() ON EACH [n2.x]") {
    assertSameAST(testName)
  }

  test("CREATE FULLTEXT INDEX FOR (n1:A|:B) ON EACH [n2.x]") {
    assertSameAST(testName)
  }

  test("CREATE FULLTEXT INDEX FOR ()-[n1:R|:S]-() ON EACH [n2.x]") {
    assertSameAST(testName)
  }

  test("CREATE FULLTEXT INDEX FOR (n1:A||B) ON EACH [n2.x]") {
    assertSameAST(testName)
  }

  test("CREATE FULLTEXT INDEX FOR ()-[n1:R||S]-() ON EACH [n2.x]") {
    assertSameAST(testName)
  }

  test("CREATE FULLTEXT INDEX FOR (n1:A:B) ON EACH [n2.x]") {
    assertSameAST(testName)
  }

  test("CREATE FULLTEXT INDEX FOR ()-[n1:R:S]-() ON EACH [n2.x]") {
    assertSameAST(testName)
  }

  test("CREATE FULLTEXT INDEX FOR (n1:A&B) ON EACH [n2.x]") {
    assertSameAST(testName)
  }

  test("CREATE FULLTEXT INDEX FOR ()-[n1:R&S]-() ON EACH [n2.x]") {
    assertSameAST(testName)
  }

  test("CREATE FULLTEXT INDEX FOR (n1:A B) ON EACH [n2.x]") {
    assertSameAST(testName)
  }

  test("CREATE FULLTEXT INDEX FOR ()-[n1:R S]-() ON EACH [n2.x]") {
    assertSameAST(testName)
  }

  // Drop index

  test("DROP INDEX ON :Person(name)") {
    assertSameAST(testName, comparePosition = false)
  }

  test("DROP INDEX ON :Person(name, age)") {
    assertSameAST(testName, comparePosition = false)
  }

  test("DROP INDEX my_index") {
    assertSameAST(testName)
  }

  test("DROP INDEX `$my_index`") {
    assertSameAST(testName)
  }

  test("DROP INDEX my_index IF EXISTS") {
    assertSameAST(testName)
  }

  test("DROP INDEX $my_index") {
    assertSameAST(testName)
  }

  test("DROP INDEX my_index ON :Person(name)") {
    assertSameAST(testName)
  }

  test("DROP INDEX ON (:Person(name))") {
    assertSameAST(testName)
  }

  test("DROP INDEX ON (:Person {name})") {
    assertSameAST(testName)
  }

  test("DROP INDEX ON [:Person(name)]") {
    assertSameAST(testName)
  }

  test("DROP INDEX ON -[:Person(name)]-") {
    assertSameAST(testName)
  }

  test("DROP INDEX ON ()-[:Person(name)]-()") {
    assertSameAST(testName)
  }

  test("DROP INDEX ON [:Person {name}]") {
    assertSameAST(testName)
  }

  test("DROP INDEX ON -[:Person {name}]-") {
    assertSameAST(testName)
  }

  test("DROP INDEX ON ()-[:Person {name}]-()") {
    assertSameAST(testName)
  }

  test("DROP INDEX on IF EXISTS") {
    assertSameAST(testName)
  }

  test("DROP INDEX on") {
    assertSameAST(testName)
  }

  test("DROP INDEX ON :if(exists)") {
    assertSameAST(testName, comparePosition = false)
  }

  // help methods

  type CreateIndexFunction = (List[expressions.Property], Option[String], InputPosition, ast.IfExistsDo, Options) => InputPosition => ast.CreateIndex

  private def btreeNodeIndex(props: List[expressions.Property],
                             name: Option[String],
                             varPos: InputPosition,
                             ifExistsDo: ast.IfExistsDo,
                             options: Options): InputPosition => ast.CreateIndex =
    ast.CreateBtreeNodeIndex(Variable("n1")(varPos), LabelName("Person")(increasePos(varPos, 3)), props, name, ifExistsDo, options)

  private def btreeRelIndex(props: List[expressions.Property],
                            name: Option[String],
                            varPos: InputPosition,
                            ifExistsDo: ast.IfExistsDo,
                            options: Options): InputPosition => ast.CreateIndex =
    ast.CreateBtreeRelationshipIndex(Variable("n1")(varPos), RelTypeName("R")(increasePos(varPos, 3)), props, name, ifExistsDo, options)

  type CreateRangeIndexFunction = (List[expressions.Property], Option[String], InputPosition, ast.IfExistsDo, Options, Boolean) => InputPosition => ast.CreateIndex

  private def rangeNodeIndex(props: List[expressions.Property],
                             name: Option[String],
                             varPos: InputPosition,
                             ifExistsDo: ast.IfExistsDo,
                             options: Options,
                             fromDefault: Boolean): InputPosition => ast.CreateIndex =
    ast.CreateRangeNodeIndex(Variable("n1")(varPos), LabelName("Person")(increasePos(varPos, 3)), props, name, ifExistsDo, options, fromDefault)

  private def rangeRelIndex(props: List[expressions.Property],
                            name: Option[String],
                            varPos: InputPosition,
                            ifExistsDo: ast.IfExistsDo,
                            options: Options,
                            fromDefault: Boolean): InputPosition => ast.CreateIndex =
    ast.CreateRangeRelationshipIndex(Variable("n1")(varPos), RelTypeName("R")(increasePos(varPos, 3)), props, name, ifExistsDo, options, fromDefault)

  private def textNodeIndex(props: List[expressions.Property],
                            name: Option[String],
                            varPos: InputPosition,
                            ifExistsDo: ast.IfExistsDo,
                            options: Options): InputPosition => ast.CreateIndex =
    ast.CreateTextNodeIndex(Variable("n1")(varPos), LabelName("Person")(increasePos(varPos, 3)), props, name, ifExistsDo, options)

  private def textRelIndex(props: List[expressions.Property],
                           name: Option[String],
                           varPos: InputPosition,
                           ifExistsDo: ast.IfExistsDo,
                           options: Options): InputPosition => ast.CreateIndex =
    ast.CreateTextRelationshipIndex(Variable("n1")(varPos), RelTypeName("R")(increasePos(varPos, 3)), props, name, ifExistsDo, options)

  private def pointNodeIndex(props: List[expressions.Property],
                             name: Option[String],
                             varPos: InputPosition,
                             ifExistsDo: ast.IfExistsDo,
                             options: Options): InputPosition => ast.CreateIndex =
    ast.CreatePointNodeIndex(Variable("n1")(varPos), LabelName("Person")(increasePos(varPos, 3)), props, name, ifExistsDo, options)

  private def pointRelIndex(props: List[expressions.Property],
                            name: Option[String],
                            varPos: InputPosition,
                            ifExistsDo: ast.IfExistsDo,
                            options: Options): InputPosition => ast.CreateIndex = {
    ast.CreatePointRelationshipIndex(Variable("n1")(varPos), RelTypeName("R")(increasePos(varPos, 3)), props, name, ifExistsDo, options)
  }

  private def pos(offset: Int): InputPosition = (1, offset + 1, offset)
  private def posN1(query: String): InputPosition = pos(query.indexOf("n1"))
  private def posN2(query: String): InputPosition = pos(query.indexOf("n2"))
}
