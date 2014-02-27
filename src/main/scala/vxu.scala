package hwacha

import Chisel._
import Node._
import Constants._

class VXU(implicit conf: HwachaConfiguration) extends Module
{
  val io = new Bundle {
    val irq = new IRQIO
    val xcpt = new XCPTIO().flip

    val vcmdq = new VCMDQIO().flip
    val imem = new rocket.CPUFrontendIO()(conf.vicache)

    val vmu = new VMUIO
    val lreq = new LookAheadPortIO(log2Down(conf.nvlreq)+1)
    val sreq = new LookAheadPortIO(log2Down(conf.nvsreq)+1)
    val lret = new MRTLoadRetireIO
    
    val pending_memop = Bool(OUTPUT)
    val pending_vf = Bool(OUTPUT)

    val aiw = new AIWVXUIO
  }

  val flush = this.reset || io.xcpt.prop.vu.flush_vxu

  val issue = Module(new Issue(resetSignal = flush))
  val hazard = Module(new Hazard(resetSignal = flush))
  val seq = Module(new Sequencer(resetSignal = flush))
  val exp = Module(new Expander)
  val lane = Module(new Lane)

  io.irq <> issue.io.irq

  issue.io.keepcfg := seq.io.busy
  issue.io.xcpt <> io.xcpt
  issue.io.vcmdq <> io.vcmdq
  issue.io.imem <> io.imem

  hazard.io.cfg <> issue.io.cfg
  hazard.io.update <> seq.io.hazard
  hazard.io.update <> exp.io.hazard
  hazard.io.tvec <> issue.io.tvec
  hazard.io.vt <> issue.io.vt

  seq.io.cfg <> issue.io.cfg
  seq.io.xcpt <> io.xcpt
  seq.io.issueop <> hazard.io.issueop

  exp.io.xcpt <> io.xcpt
  exp.io.seqop <> seq.io.seqop

  lane.io.cfg <> issue.io.cfg
  lane.io.op <> exp.io.laneop

  io.vmu <> seq.io.vmu
  io.vmu <> lane.io.vmu
  io.lreq <> seq.io.lreq
  io.sreq <> seq.io.sreq
  io.lret <> lane.io.lret

  io.aiw <> issue.io.aiw
  io.aiw <> seq.io.aiw

  io.pending_memop := hazard.io.pending_memop
  io.pending_vf := issue.io.pending_vf
}
