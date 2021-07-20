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
package org.opencypher.v9_0.ast.semantics

import org.opencypher.v9_0.ast.semantics
import org.opencypher.v9_0.expressions.LogicalVariable
import org.opencypher.v9_0.util.symbols.CTInteger
import org.opencypher.v9_0.util.symbols.CTList
import org.opencypher.v9_0.util.symbols.CTNode
import org.opencypher.v9_0.util.symbols.CTPath
import org.opencypher.v9_0.util.symbols.CTString
import org.opencypher.v9_0.util.symbols.TypeSpec

object ScopeTestHelper {

  def scope(entries: semantics.Symbol*)(children: Scope*): Scope =
    Scope(entries.map { symbol => symbol.name -> symbol }.toMap, children.toSeq)

  def nodeSymbol(name: String, definition: LogicalVariable, readingUses: LogicalVariable*): semantics.Symbol =
    typedSymbol(name, TypeSpec.exact(CTNode), definition, readingUses: _*)

  def allSymbol(name: String, definition: LogicalVariable, readingUses: LogicalVariable*): semantics.Symbol =
    typedSymbol(name, TypeSpec.all, definition, readingUses: _*)

  def intSymbol(name: String, definition: LogicalVariable, readingUses: LogicalVariable*): semantics.Symbol =
    typedSymbol(name, TypeSpec.exact(CTInteger), definition, readingUses: _*)

  def stringSymbol(name: String, definition: LogicalVariable, readingUses: LogicalVariable*): semantics.Symbol =
    typedSymbol(name, TypeSpec.exact(CTString), definition, readingUses: _*)

  def intCollectionSymbol(name: String, definition: LogicalVariable, readingUses: LogicalVariable*): semantics.Symbol =
    typedSymbol(name, TypeSpec.exact(CTList(CTInteger)), definition, readingUses: _*)

  def pathCollectionSymbol(name: String, definition: LogicalVariable, readingUses: LogicalVariable*): semantics.Symbol =
    typedSymbol(name, TypeSpec.exact(CTList(CTPath)), definition, readingUses: _*)

  def intCollectionCollectionSymbol(name: String, definition: LogicalVariable, readingUses: LogicalVariable*): semantics.Symbol =
    typedSymbol(name, TypeSpec.exact(CTList(CTList(CTInteger))), definition, readingUses: _*)

  def typedSymbol(name: String, typeSpec: TypeSpec, definition: LogicalVariable, readingUses: LogicalVariable*): Symbol =
    semantics.Symbol(name, typeSpec, SymbolUse(definition), readingUses.map(SymbolUse(_)).toSet)
}
