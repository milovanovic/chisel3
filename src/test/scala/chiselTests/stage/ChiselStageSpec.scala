// SPDX-License-Identifier: Apache-2.0

package chiselTests.stage

import chisel3._
import chisel3.stage.ChiselStage
import chisel3.testers.TesterDriver.createTestDirectory

import chiselTests.Utils

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import firrtl.options.Dependency

object ChiselStageSpec {

  class Bar extends Module {
    val in = IO(Input(UInt(4.W)))
    val out = IO(Output(UInt(4.W)))
    out := ~in
  }

  class Foo extends Module {
    val addr = IO(Input(UInt(4.W)))
    val out = IO(Output(Bool()))
    val memory = SyncReadMem(8, Bool())
    val bar = Module(new Bar)
    bar.in := addr
    out := memory(bar.out)
  }

}

class ChiselStageSpec extends AnyFlatSpec with Matchers with Utils {

  import ChiselStageSpec._

  private trait ChiselStageFixture {
    val stage = new ChiselStage
  }

  behavior of "ChiselStage.emitChirrtl"

  it should "return a CHIRRTL string" in {
    ChiselStage.emitChirrtl(new Foo) should include ("infer mport")
  }

  behavior of "ChiselStage.emitFirrtl"

  it should "return a High FIRRTL string" in {
    ChiselStage.emitFirrtl(new Foo) should include ("mem memory")
  }

  it should "return a flattened FIRRTL string with '-e high'" in {
    val args = Array("-e", "high", "-td", createTestDirectory(this.getClass.getSimpleName).toString)
    (new ChiselStage)
      .emitFirrtl(new Foo, args) should include ("module Bar")
  }

  behavior of "ChiselStage.emitVerilog"

  it should "return a Verilog string" in {
    ChiselStage.emitVerilog(new Foo) should include ("endmodule")
  }

  it should "return a flattened Verilog string with '-e verilog'" in {
    val args = Array("-e", "verilog", "-td", createTestDirectory(this.getClass.getSimpleName).toString)
    (new ChiselStage)
      .emitVerilog(new Foo, args) should include ("module Bar")
  }

  behavior of "ChiselStage$.elaborate"

  ignore should "generate a Chisel circuit from a Chisel module" in {
    info("no files were written")
    catchWrites { ChiselStage.elaborate(new Foo) } shouldBe a[Right[_, _]]
  }

  behavior of "ChiselStage$.convert"

  ignore should "generate a CHIRRTL circuit from a Chisel module" in {
    info("no files were written")
    catchWrites { ChiselStage.convert(new Foo) } shouldBe a[Right[_, _]]
  }

  behavior of "ChiselStage$.emitChirrtl"

  ignore should "generate a CHIRRTL string from a Chisel module" in {
    val wrapped = catchWrites { ChiselStage.emitChirrtl(new Foo) }

    info("no files were written")
    wrapped shouldBe a[Right[_, _]]

    info("returned string looks like FIRRTL")
    wrapped.right.get should include ("circuit")
  }

  behavior of "ChiselStage$.emitFirrtl"

  ignore should "generate a FIRRTL string from a Chisel module" in {
    val wrapped = catchWrites { ChiselStage.emitFirrtl(new Foo) }

    info("no files were written")
    wrapped shouldBe a[Right[_, _]]

    info("returned string looks like FIRRTL")
    wrapped.right.get should include ("circuit")
  }

  behavior of "ChiselStage$.emitVerilog"

  ignore should "generate a Verilog string from a Chisel module" in {
    val wrapped = catchWrites { ChiselStage.emitVerilog(new Foo) }

    info("no files were written")
    wrapped shouldBe a[Right[_, _]]

    info("returned string looks like Verilog")
    wrapped.right.get should include ("endmodule")
  }

  behavior of "ChiselStage$.emitSystemVerilog"

  ignore should "generate a SystemvVerilog string from a Chisel module" in {
    val wrapped = catchWrites { ChiselStage.emitSystemVerilog(new Foo) }
    info("no files were written")
    wrapped shouldBe a[Right[_, _]]

    info("returned string looks like Verilog")
    wrapped.right.get should include ("endmodule")
  }

  behavior of "ChiselStage phase ordering"

  it should "only run elaboration once" in new ChiselStageFixture {
    info("Phase order is:\n" + stage.phaseManager.prettyPrint("    "))

    val order = stage.phaseManager.flattenedTransformOrder.map(Dependency.fromTransform)

    info("Elaborate only runs once")
    exactly (1, order) should be (Dependency[chisel3.stage.phases.Elaborate])
  }

}
