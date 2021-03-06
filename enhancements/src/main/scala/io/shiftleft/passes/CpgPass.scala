package io.shiftleft.passes

import gremlin.scala.ScalaGraph
import io.shiftleft.diffgraph.{AppliedDiffGraph, DiffGraph, DiffGraphProtoSerializer}
import io.shiftleft.proto.cpg.Cpg.CpgOverlay
import org.apache.logging.log4j.LogManager

/**
  * Base class for CPG enhancements - provides access to a src graph and destination Diff graph
  * */
abstract class CpgPass(srcGraph: ScalaGraph) {
  protected val logger = LogManager.getLogger(getClass)
  private var startTime: Long = _

  /**
    * Execute the enhancement and apply result to graph
    * */
  def executeAndApply(): Unit = {
    logStart()
    val dstGraphs = run()
    dstGraphs.foreach(applyDiff)
    logEnd()
  }

  /**
    * Main method of enhancement - to be implemented by child class
    * */
  def run(): Stream[DiffGraph]

  /**
    * Apply diff graph to the source graph
    * */
  def applyDiff(dstGraph: DiffGraph): AppliedDiffGraph = {
    dstGraph.applyDiff(srcGraph)
  }

  /**
    * Execute and create a serialized overlay
    * */
  def executeAndCreateOverlay(): Stream[CpgOverlay] = {
    try {
      logStart()
      val dstGraphs = run()
      dstGraphs.map(serializeOverlay)
    } finally {
      logEnd()
    }
  }

  def serializeOverlay(dstGraph: DiffGraph): CpgOverlay = {
    val appliedDiffGraph = applyDiff(dstGraph)
    new DiffGraphProtoSerializer().serialize()(appliedDiffGraph)
  }

  private def logStart(): Unit = {
    logger.info(s"Start of enhancement: ${getClass.getName}")
    startTime = System.currentTimeMillis()
  }

  private def logEnd(): Unit = {
    val endTime = System.currentTimeMillis()
    logger.info(s"End of enhancement: ${getClass.getName}, after ${endTime - startTime}ms")
  }

}
