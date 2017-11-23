package benchmarks

import java.io.File
import java.nio.file.Paths

import cats.effect.IO

object CompressTest {
  val DefaultBufferSize = 1
  val src = "/Users/luke.stephenson/projects/listings/results4.txt"
  val target = "/Users/luke.stephenson/projects/listings/results3.txt"

  def main(args: Array[String]): Unit = {
    val file = new File(src)

    val start = System.currentTimeMillis()

    def process = fs2.io.file.readAll[IO](file.toPath, DefaultBufferSize)
      .through(fs2.io.file.writeAll(Paths.get(target)))
      .run

    process.unsafeRunSync()

    val end = System.currentTimeMillis()

    println(s"took ${end - start}ms")
  }
}
