package io.shiftleft.passes.linking.capturinglinker

import gremlin.scala.ScalaGraph
import io.shiftleft.codepropertygraph.generated.{EdgeTypes, NodeTypes, nodes}
import io.shiftleft.diffgraph.DiffGraph
import io.shiftleft.passes.CpgPass

class CapturingLinker(graph: ScalaGraph) extends CpgPass(graph) {

  override def run(): Stream[DiffGraph] = {
    var idToClosureBinding = Map[String, nodes.ClosureBinding]()
    val dstGraph = new DiffGraph

    graph.V
      .hasLabel(NodeTypes.CLOSURE_BINDING)
      .sideEffect {
        case closureBinding: nodes.ClosureBinding =>
          idToClosureBinding += ((closureBinding.closureBindingId.get, closureBinding))
      }
      .iterate()

    graph.V
      .hasLabel(NodeTypes.LOCAL)
      .sideEffect {
        case local: nodes.Local =>
          local.closureBindingId.foreach { closureBindingId =>
            idToClosureBinding.get(closureBindingId) match {
              case Some(closureBindingNode) =>
                dstGraph.addEdgeInOriginal(local, closureBindingNode, EdgeTypes.CAPTURED_BY)
              case None =>
                logger.error(s"Missing CLOSURE_BINDING node or invalid closureBindingId=$closureBindingId")
            }
          }
        case _ =>
      }
      .iterate()
    Stream(dstGraph)
  }
}
