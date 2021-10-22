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
package org.opencypher.v9_0.ast.factory.neo4j.privilege

import org.opencypher.v9_0.ast.factory.neo4j.ParserComparisonTestBase
import org.opencypher.v9_0.util.test_helpers.TestName
import org.scalatest.FunSuiteLike

class PropertyPrivilegeAdministrationCommandJavaCcParserTest extends ParserComparisonTestBase with FunSuiteLike with TestName {

  Seq(
    ("GRANT", "TO"),
    ("DENY", "TO"),
    ("REVOKE GRANT", "FROM"),
    ("REVOKE DENY", "FROM"),
    ("REVOKE", "FROM")
  ).foreach {
    case (verb: String, preposition: String) =>

      test(s"$verb SET PROPERTY { prop } ON GRAPH foo $preposition role") {
        assertSameAST(testName)
      }

      // Multiple properties should be allowed

      test(s"$verb SET PROPERTY { * } ON GRAPH foo $preposition role") {
        assertSameAST(testName)
      }

      test(s"$verb SET PROPERTY { prop1, prop2 } ON GRAPH foo $preposition role") {
        assertSameAST(testName)
      }

      // Home graph should be allowed

      test(s"$verb SET PROPERTY { * } ON HOME GRAPH $preposition role") {
        assertSameAST(testName)
      }

      test(s"$verb SET PROPERTY { prop } ON HOME GRAPH $preposition role") {
        assertSameAST(testName)
      }

      test(s"$verb SET PROPERTY { prop } ON HOME GRAPH NODES A,B $preposition role") {
        assertSameAST(testName)
      }

      // Default graph should be allowed

      test(s"$verb SET PROPERTY { * } ON DEFAULT GRAPH $preposition role") {
        assertSameAST(testName)
      }

      test(s"$verb SET PROPERTY { prop } ON DEFAULT GRAPH $preposition role") {
        assertSameAST(testName)
      }

      test(s"$verb SET PROPERTY { prop } ON DEFAULT GRAPH NODES A,B $preposition role") {
        assertSameAST(testName)
      }

      // Multiple graphs should be allowed

      test(s"$verb SET PROPERTY { prop } ON GRAPHS * $preposition role") {
        assertSameAST(testName)
      }

      test(s"$verb SET PROPERTY { prop } ON GRAPHS foo,baz $preposition role") {
        assertSameAST(testName)
      }

      // Qualifiers

      test(s"$verb SET PROPERTY { prop } ON GRAPHS foo ELEMENTS A,B $preposition role") {
        assertSameAST(testName)
      }

      test(s"$verb SET PROPERTY { prop } ON GRAPHS foo NODES A,B $preposition role") {
        assertSameAST(testName)
      }

      test(s"$verb SET PROPERTY { prop } ON GRAPHS foo NODES * $preposition role") {
        assertSameAST(testName)
      }

      test(s"$verb SET PROPERTY { prop } ON GRAPHS foo RELATIONSHIPS A,B $preposition role") {
        assertSameAST(testName)
      }

      test(s"$verb SET PROPERTY { prop } ON GRAPHS foo RELATIONSHIPS * $preposition role") {
        assertSameAST(testName)
      }

      // Multiple roles should be allowed

      test(s"$verb SET PROPERTY { prop } ON GRAPHS foo $preposition role1, role2") {
        assertSameAST(testName)
      }

      // Parameter values

      test(s"$verb SET PROPERTY { prop } ON GRAPH $$foo $preposition role") {
        assertSameAST(testName)
      }

      test(s"$verb SET PROPERTY { prop } ON GRAPH foo $preposition $$role") {
        assertSameAST(testName)
      }

      // PROPERTYS/PROPERTIES instead of PROPERTY

      test(s"$verb SET PROPERTYS { prop } ON GRAPH * $preposition role") {
        val offset = verb.length + 5
        assertJavaCCException(testName,
          s"""Invalid input 'PROPERTYS': expected
             |  "DATABASE"
             |  "LABEL"
             |  "PASSWORD"
             |  "PASSWORDS"
             |  "PROPERTY"
             |  "USER" (line 1, column ${offset + 1} (offset: $offset))""".stripMargin)
      }

      test(s"$verb SET PROPERTIES { prop } ON GRAPH * $preposition role") {
        val offset = verb.length + 5
        assertJavaCCException(testName,
          s"""Invalid input 'PROPERTIES': expected
             |  "DATABASE"
             |  "LABEL"
             |  "PASSWORD"
             |  "PASSWORDS"
             |  "PROPERTY"
             |  "USER" (line 1, column ${offset + 1} (offset: $offset))""".stripMargin)
      }

      // Database instead of graph keyword

      test(s"$verb SET PROPERTY { prop } ON DATABASES * $preposition role") {
        val offset = verb.length + 26
        assertJavaCCException(testName, s"""Invalid input 'DATABASES': expected "DEFAULT", "GRAPH", "GRAPHS" or "HOME" (line 1, column ${offset + 1} (offset: $offset))""")
      }

      test(s"$verb SET PROPERTY { prop } ON DATABASE foo $preposition role") {
        val offset = verb.length + 26
        assertJavaCCException(testName, s"""Invalid input 'DATABASE': expected "DEFAULT", "GRAPH", "GRAPHS" or "HOME" (line 1, column ${offset + 1} (offset: $offset))""")
      }

      test(s"$verb SET PROPERTY { prop } ON HOME DATABASE $preposition role") {
        assertSameAST(testName)
      }

      test(s"$verb SET PROPERTY { prop } ON DEFAULT DATABASE $preposition role") {
        assertSameAST(testName)
      }
  }
}
