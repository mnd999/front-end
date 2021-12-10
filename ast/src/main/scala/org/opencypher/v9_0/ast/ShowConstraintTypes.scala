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
package org.opencypher.v9_0.ast

sealed trait ShowConstraintType {
  val output: String
  val prettyPrint: String
  val description: String
}

case object AllConstraints extends ShowConstraintType {
  override val output: String = "ALL"
  override val prettyPrint: String = "ALL"
  override val description: String = "allConstraints"
}

case object UniqueConstraints extends ShowConstraintType {
  override val output: String = "UNIQUENESS"
  override val prettyPrint: String = "UNIQUE"
  override val description: String = "uniquenessConstraints"
}

case class ExistsConstraints(syntax: ExistenceConstraintSyntax) extends ShowConstraintType {
  override val output: String = "PROPERTY_EXISTENCE"
  override val prettyPrint: String = "PROPERTY EXISTENCE"
  override val description: String = "existenceConstraints"
}

case class NodeExistsConstraints(syntax: ExistenceConstraintSyntax = ValidSyntax) extends ShowConstraintType {
  override val output: String = "NODE_PROPERTY_EXISTENCE"
  override val prettyPrint: String = s"NODE PROPERTY EXISTENCE"
  override val description: String = "nodeExistenceConstraints"
}

case class RelExistsConstraints(syntax: ExistenceConstraintSyntax = ValidSyntax) extends ShowConstraintType {
  override val output: String = "RELATIONSHIP_PROPERTY_EXISTENCE"
  override val prettyPrint: String = s"RELATIONSHIP PROPERTY EXISTENCE"
  override val description: String = "relationshipExistenceConstraints"
}

case object NodeKeyConstraints extends ShowConstraintType {
  override val output: String = "NODE_KEY"
  override val prettyPrint: String = "NODE KEY"
  override val description: String = "nodeKeyConstraints"
}

sealed trait ExistenceConstraintSyntax
case object RemovedSyntax extends ExistenceConstraintSyntax
case object ValidSyntax extends ExistenceConstraintSyntax
