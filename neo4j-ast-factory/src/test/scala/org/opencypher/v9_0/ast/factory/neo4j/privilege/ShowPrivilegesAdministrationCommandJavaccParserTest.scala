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

class ShowPrivilegesAdministrationCommandJavaccParserTest extends ParserComparisonTestBase with FunSuiteLike with TestName {

  // Show privileges

  test("SHOW PRIVILEGES") {
    assertSameAST(testName)
  }

  test("SHOW PRIVILEGE") {
    assertSameAST(testName)
  }

  test("use system show privileges") {
    assertSameAST(testName)
  }

  test("SHOW ALL PRIVILEGES") {
    assertSameAST(testName)
  }

  // Show user privileges

  test("SHOW USER user PRIVILEGES") {
    assertSameAST(testName)
  }

  test("SHOW USERS $user PRIVILEGES") {
    assertSameAST(testName)
  }

  test("SHOW USER `us%er` PRIVILEGES") {
    assertSameAST(testName)
  }

  test("SHOW USER user, $user PRIVILEGES") {
    assertSameAST(testName)
  }

  test("SHOW USER user, $user PRIVILEGE") {
    assertSameAST(testName)
  }

  test("SHOW USERS user1, $user, user2 PRIVILEGES") {
    assertSameAST(testName)
  }

  test("SHOW USER PRIVILEGES") {
    assertSameAST(testName)
  }

  test("SHOW USERS PRIVILEGES") {
    assertSameAST(testName)
  }

  test("SHOW USER privilege PRIVILEGE") {
    assertSameAST(testName)
  }

  test("SHOW USER privilege, privileges PRIVILEGES") {
    assertSameAST(testName)
  }

  test(s"SHOW USER defined PRIVILEGES") {
    assertSameAST(testName)
  }

  test(s"SHOW USERS yield, where PRIVILEGES") {
    assertSameAST(testName)
  }

  // yield / skip / limit / order by / where

  Seq(
    "",
    "ALL",
    "USER",
    "USER neo4j",
    "USERS neo4j, $user",
  ).foreach { privType =>
    Seq(
      "PRIVILEGE",
      "PRIVILEGES"
    ).foreach { privilegeOrPrivileges =>
      test(s"SHOW $privType $privilegeOrPrivileges WHERE access = 'GRANTED'") {
        assertSameAST(testName)
      }

      test(s"SHOW $privType $privilegeOrPrivileges WHERE access = 'GRANTED' AND action = 'match'") {
        assertSameAST(testName)
      }

      test(s"SHOW $privType $privilegeOrPrivileges YIELD access ORDER BY access") {
        assertSameAST(testName)
      }

      test(s"SHOW $privType $privilegeOrPrivileges YIELD access ORDER BY access WHERE access ='none'") {
        assertSameAST(testName)
      }

      test(s"SHOW $privType $privilegeOrPrivileges YIELD access ORDER BY access SKIP 1 LIMIT 10 WHERE access ='none'") {
        assertSameAST(testName)
      }

      test(s"SHOW $privType $privilegeOrPrivileges YIELD access SKIP -1") {
        assertSameAST(testName)
      }

      test(s"SHOW $privType $privilegeOrPrivileges YIELD access, action RETURN access, count(action) ORDER BY access") {
        assertSameAST(testName)
      }

      test(s"SHOW $privType $privilegeOrPrivileges YIELD access, action SKIP 1 RETURN access, action") {
        assertSameAST(testName)
      }

      test(s"SHOW $privType $privilegeOrPrivileges YIELD access, action WHERE access = 'none' RETURN action") {
        assertSameAST(testName)
      }

      test(s"SHOW $privType $privilegeOrPrivileges YIELD * RETURN *") {
        assertSameAST(testName)
      }
    }
  }

  // yield and where edge cases

  Seq(
    "USER",
    "USERS"
  ).foreach { privType =>
    test(s"SHOW $privType yield PRIVILEGES YIELD access RETURN *") {
      assertSameAST(testName)
    }

    test(s"SHOW $privType yield, where PRIVILEGES YIELD access RETURN *") {
      assertSameAST(testName)
    }

    test(s"SHOW $privType where PRIVILEGE WHERE access = 'none'") {
      assertSameAST(testName)
    }

    test(s"SHOW $privType privilege PRIVILEGE YIELD access RETURN *") {
      assertSameAST(testName)
    }

    test(s"SHOW $privType privileges PRIVILEGES YIELD access RETURN *") {
      assertSameAST(testName)
    }

    test(s"SHOW $privType privilege, privileges PRIVILEGES YIELD access RETURN *") {
      assertSameAST(testName)
    }
  }

  // Fails to parse

  test("SHOW PRIVILAGES") {
    val exceptionMessage =
      s"""Invalid input 'PRIVILAGES': expected
         |  "ALL"
         |  "BTREE"
         |  "BUILT"
         |  "CONSTRAINT"
         |  "CONSTRAINTS"
         |  "CURRENT"
         |  "DATABASE"
         |  "DATABASES"
         |  "DEFAULT"
         |  "EXIST"
         |  "EXISTENCE"
         |  "EXISTS"
         |  "FULLTEXT"
         |  "FUNCTION"
         |  "FUNCTIONS"
         |  "HOME"
         |  "INDEX"
         |  "INDEXES"
         |  "LOOKUP"
         |  "NODE"
         |  "POINT"
         |  "POPULATED"
         |  "PRIVILEGE"
         |  "PRIVILEGES"
         |  "PROCEDURE"
         |  "PROCEDURES"
         |  "PROPERTY"
         |  "RANGE"
         |  "REL"
         |  "RELATIONSHIP"
         |  "ROLES"
         |  "TEXT"
         |  "TRANSACTION"
         |  "TRANSACTIONS"
         |  "UNIQUE"
         |  "USER"
         |  "USERS" (line 1, column 6 (offset: 5))""".stripMargin

    assertJavaCCException(testName, exceptionMessage)
  }

  test("SHOW ALL USER user PRIVILEGES") {
    val exceptionMessage =
      s"""Invalid input 'USER': expected
         |  "CONSTRAINT"
         |  "CONSTRAINTS"
         |  "FUNCTION"
         |  "FUNCTIONS"
         |  "INDEX"
         |  "INDEXES"
         |  "PRIVILEGE"
         |  "PRIVILEGES"
         |  "ROLES" (line 1, column 10 (offset: 9))""".stripMargin

    assertJavaCCException(testName, exceptionMessage)
  }

  test("SHOW USER us%er PRIVILEGES") {
    assertJavaCCException(testName, """Invalid input '%': expected ",", "PRIVILEGE" or "PRIVILEGES" (line 1, column 13 (offset: 12))""")
  }

  test("SHOW USER user PRIVILEGES YIELD *, blah RETURN user") {
    val exceptionMessage =
      s"""Invalid input ',': expected
         |  "LIMIT"
         |  "ORDER"
         |  "RETURN"
         |  "SKIP"
         |  "WHERE"
         |  <EOF> (line 1, column 34 (offset: 33))""".stripMargin

    assertJavaCCException(testName, exceptionMessage)
  }
}
