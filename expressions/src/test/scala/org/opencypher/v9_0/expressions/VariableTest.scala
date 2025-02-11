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
package org.opencypher.v9_0.expressions

import org.opencypher.v9_0.util.InputPosition
import org.opencypher.v9_0.util.test_helpers.CypherFunSuite

class VariableTest extends CypherFunSuite {
  test("variable names are handled by the trait") {
    val _name = "testVariable"
    val variable = new LogicalVariable {
      override def name: String = _name

      override def copyId: LogicalVariable = ???

      override def renameId(newName: String): LogicalVariable = ???

      override def position: InputPosition = ???

      override def productElement(n: Int): Any = ???

      override def productArity: Int = ???

      override def canEqual(that: Any): Boolean = ???
    }

    variable.asCanonicalStringVal should equal(_name)
  }
}
