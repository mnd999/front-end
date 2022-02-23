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
package org.opencypher.v9_0.frontend.helpers

import org.opencypher.v9_0.ast
import org.opencypher.v9_0.ast.semantics.SemanticState
import org.opencypher.v9_0.ast.semantics.SemanticTable
import org.opencypher.v9_0.frontend.PlannerName
import org.opencypher.v9_0.frontend.phases.BaseContains
import org.opencypher.v9_0.frontend.phases.BaseContext
import org.opencypher.v9_0.frontend.phases.BaseState
import org.opencypher.v9_0.frontend.phases.CompilationPhaseTracer.CompilationPhase.PARSING
import org.opencypher.v9_0.frontend.phases.Phase
import org.opencypher.v9_0.parser.Expressions
import org.opencypher.v9_0.parser.Statement
import org.opencypher.v9_0.util.AnonymousVariableNameGenerator
import org.opencypher.v9_0.util.CypherException
import org.opencypher.v9_0.util.CypherExceptionFactory
import org.opencypher.v9_0.util.InputPosition
import org.opencypher.v9_0.util.ObfuscationMetadata
import org.opencypher.v9_0.util.StepSequencer
import org.opencypher.v9_0.util.symbols.CypherType
import org.parboiled.scala.EOI
import org.parboiled.scala.Parser
import org.parboiled.scala.Rule1
import org.parboiled.scala.group

/**
 * Parse text into an AST object.
 */
case object InputDataStreamTestParsing extends Phase[BaseContext, BaseState, BaseState] {
  private val parser = new InputDataStreamTestCypherParser

  override def process(in: BaseState, context: BaseContext): BaseState = {
    val idsIn = in.asInstanceOf[InputDataStreamTestInitialState]
    idsIn.withStatement(parser.parse(idsIn.idsQueryText, context.cypherExceptionFactory, in.startPosition))
  }

  override val phase = PARSING

  override def postConditions = Set(BaseContains[ast.Statement])

}

case class InputDataStreamTestInitialState(idsQueryText: String,
                                           queryText: String,
                                           startPosition: Option[InputPosition],
                                           plannerName: PlannerName,
                                           anonymousVariableNameGenerator: AnonymousVariableNameGenerator,
                                           initialFields: Map[String, CypherType] = Map.empty,
                                           maybeStatement: Option[ast.Statement] = None,
                                           maybeSemantics: Option[SemanticState] = None,
                                           maybeExtractedParams: Option[Map[String, Any]] = None,
                                           maybeSemanticTable: Option[SemanticTable] = None,
                                           accumulatedConditions: Set[StepSequencer.Condition] = Set.empty,
                                           maybeReturnColumns: Option[Seq[String]] = None,
                                           maybeObfuscationMetadata: Option[ObfuscationMetadata] = None) extends BaseState {


  override def withStatement(s: ast.Statement): InputDataStreamTestInitialState = {
    // the unmodified parser is part of the pipeline and it will try to set the result of parsing 'RETURN 1'
    // we simply ignore statements that do not contain InputDataStream AST node
    if (s.findAllByClass[ast.InputDataStream].isEmpty) {
      copy()
    } else {
      copy(maybeStatement = Some(s))
    }
  }

  override def withSemanticTable(s: SemanticTable): InputDataStreamTestInitialState = copy(maybeSemanticTable = Some(s))

  override def withSemanticState(s: SemanticState): InputDataStreamTestInitialState = copy(maybeSemantics = Some(s))

  override def withParams(p: Map[String, Any]): InputDataStreamTestInitialState = copy(maybeExtractedParams = Some(p))

  override def withReturnColumns(cols: Seq[String]): InputDataStreamTestInitialState = copy(maybeReturnColumns = Some(cols))

  override def withObfuscationMetadata(o: ObfuscationMetadata): InputDataStreamTestInitialState = copy(maybeObfuscationMetadata = Some(o))
}

class InputDataStreamTestCypherParser extends Parser
  with Statement
  with Expressions {


  @throws(classOf[CypherException])
  def parse(queryText: String, cypherExceptionFactory: CypherExceptionFactory, offset: Option[InputPosition] = None): ast.Statement =
    parseOrThrow(queryText, cypherExceptionFactory, offset, InputDataStreamTestCypherParser.Statements)
}

object InputDataStreamTestCypherParser extends Parser with Statement with Expressions {
  val Statements: Rule1[Seq[ast.Statement]] = rule {
    oneOrMore(WS ~ Statement ~ WS, separator = ch(';')) ~~ optional(ch(';')) ~~ EOI.label("end of input")
  }

  override def Clause: Rule1[ast.Clause] = (
    Unwind
      | With
      | Match
      | Call
      | Return
      | InputDataStream
    )

  def InputDataStream: Rule1[ast.InputDataStream] = rule("INPUT DATA STREAM") {
    group(keyword("INPUT DATA STREAM") ~~ oneOrMore(Variable, separator = CommaSep)) ~~>> (ast.InputDataStream(_))
  }
}
