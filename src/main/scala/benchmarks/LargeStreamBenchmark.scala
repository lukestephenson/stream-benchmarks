package benchmarks

import java.io.{File, FileInputStream, FileOutputStream}
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

import benchmarks.NIO.{DefaultBufferSize, src, target}
import cats.effect.IO
import monix.execution.Scheduler.Implicits.global
import monix.nio.file.{readAsync, writeAsync}
import monix.reactive.OverflowStrategy.BackPressure
import monix.reactive.{Consumer, Observable}
import org.apache.commons.io.IOUtils
import org.openjdk.jmh.annotations._

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Run with:
  * sbt
  * > jmh:run -i 3 -wi 3 -f1 -t1 .*Benchmark
  *
  * [info] # Warmup: 3 iterations, 1 s each
  * [info] # Measurement: 3 iterations, 1 s each
  * [info] # Timeout: 10 min per iteration
  * [info] # Threads: 1 thread, will synchronize iterations
  * [info] # Benchmark mode: Average time, time/op
  * [info] # Benchmark: benchmarks.LargeStreamBenchmark.commonsIo
  * [info] # Run progress: 0.00% complete, ETA 00:00:18
  * [info] # Fork: 1 of 1
  * [info] # Warmup Iteration   1: 4.934 s/op
  * [info] # Warmup Iteration   2: 3.352 s/op
  * [info] # Warmup Iteration   3: 3.391 s/op
  * [info] Iteration   1: 3.493 s/op
  * [info] Iteration   2: 3.226 s/op
  * [info] Iteration   3: 3.266 s/op
  * [info] Result "benchmarks.LargeStreamBenchmark.commonsIo":
  * [info]   3.328 ±(99.9%) 2.629 s/op [Average]
  * [info]   (min, avg, max) = (3.226, 3.328, 3.493), stdev = 0.144
  * [info]   CI (99.9%): [0.699, 5.957] (assumes normal distribution)
  * [info] # JMH version: 1.19
  * [info] # VM version: JDK 1.8.0_144, VM 25.144-b01
  * [info] # VM invoker: /Library/Java/JavaVirtualMachines/jdk1.8.0_144.jdk/Contents/Home/jre/bin/java
  * [info] # VM options: <none>
  * [info] # Warmup: 3 iterations, 1 s each
  * [info] # Measurement: 3 iterations, 1 s each
  * [info] # Timeout: 10 min per iteration
  * [info] # Threads: 1 thread, will synchronize iterations
  * [info] # Benchmark mode: Average time, time/op
  * [info] # Benchmark: benchmarks.LargeStreamBenchmark.fs2Stream
  * [info] # Run progress: 33.33% complete, ETA 00:00:44
  * [info] # Fork: 1 of 1
  * [info] # Warmup Iteration   1: 44.996 s/op
  * [info] # Warmup Iteration   2: 38.361 s/op
  * [info] # Warmup Iteration   3: 31.052 s/op
  * [info] Iteration   1: 32.410 s/op
  * [info] Iteration   2: 29.455 s/op
  * [info] Iteration   3: 29.975 s/op
  * [info] Result "benchmarks.LargeStreamBenchmark.fs2Stream":
  * [info]   30.614 ±(99.9%) 28.784 s/op [Average]
  * [info]   (min, avg, max) = (29.455, 30.614, 32.410), stdev = 1.578
  * [info]   CI (99.9%): [1.830, 59.397] (assumes normal distribution)
  */
@State(Scope.Thread)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.SECONDS)
class LargeStreamBenchmark {

  val DefaultBufferSize = 10240
  val src = "/Users/luke.stephenson/projects/listings/results4.txt"
  val target = "/Users/luke.stephenson/projects/listings/results3.txt"

  @Benchmark
  def commonsIo() = {
    val file = new File(src)
    val out = new File(target)

    IOUtils.copy(new FileInputStream(file), new FileOutputStream(out), DefaultBufferSize)
  }

  @Benchmark
  def monixObservable() = {
    val inputStream = new FileInputStream(new File(src))
    val outputStream = new FileOutputStream(target)
    val process = Observable.fromInputStream(inputStream, DefaultBufferSize)
      .consumeWith(Consumer.foreach(bytes => outputStream.write(bytes)))

    Await.result(process.runAsync, 2.minute)
  }

  @Benchmark
  def monixObservableWithAsyncBoundary() = {
    val inputStream = new FileInputStream(new File(src))
    val outputStream = new FileOutputStream(target)
    val process = Observable.fromInputStream(inputStream, DefaultBufferSize)
      .asyncBoundary(BackPressure(1000))
      .consumeWith(Consumer.foreach(bytes => outputStream.write(bytes)))

    Await.result(process.runAsync, 2.minute)
  }

  @Benchmark
  def monixNioBenchmark() = {
    val from = java.nio.file.Paths.get(src)
    val to = java.nio.file.Paths.get(target)

    val consumer = writeAsync(to)

    val future = readAsync(from, DefaultBufferSize)
      .asyncBoundary(BackPressure(100))
      .consumeWith(consumer)
      .runAsync

    Await.result(future, 1.minute)
  }

  @Benchmark
  def fs2Stream() = {
    val file = new File(src)

    val process = fs2.io.file.readAll[IO](file.toPath, DefaultBufferSize)
      .through(fs2.io.file.writeAll(Paths.get(target)))
      .run

    process.unsafeRunSync()
  }

}
