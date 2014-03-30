package prickle

import scala.util.{Success, Failure, Try}
import scala.language.experimental.macros

trait Unpickler[A] {

  def unpickle[P](pickle: P)(implicit reader: PReader[P]): Try[A]
}

trait MaterializeUnpicklerFallback {

  implicit def materializeUnpickler[T]: Unpickler[T] =
    macro PicklerMaterializersImpl.materializeUnpickler[T]
}
object Unpickler extends MaterializeUnpicklerFallback {

  def to[A](implicit u: Unpickler[A]) = UnpickledCurry[A](u)

  implicit object BooleanUnpickler extends Unpickler[Boolean] {
    def unpickle[P](pickle: P)(implicit reader: PReader[P]) = reader.readBoolean(pickle)
  }

  implicit object CharUnpickler extends Unpickler[Char] {
    def unpickle[P](pickle: P)(implicit reader: PReader[P]) = reader.readString(pickle).flatMap(s => Try(s.charAt(0)))
  }

  implicit object ByteUnpickler extends Unpickler[Byte] {
    def unpickle[P](pickle: P)(implicit reader: PReader[P]) = reader.readNumber(pickle).map(_.toByte)
  }

  implicit object ShortUnpickler extends Unpickler[Short] {
    def unpickle[P](pickle: P)(implicit reader: PReader[P]) = reader.readNumber(pickle).map(_.toShort)
  }

  implicit object IntUnpickler extends Unpickler[Int] {
    def unpickle[P](pickle: P)(implicit reader: PReader[P]) = reader.readNumber(pickle).map(_.toInt)
  }

  implicit object LongUnpickler extends Unpickler[Long] {
    def unpickle[P](pickle: P)(implicit reader: PReader[P]) = {
      for {
        l <- reader.readObjectFieldNum(pickle, "l")
        m <- reader.readObjectFieldNum(pickle, "m")
        h <- reader.readObjectFieldNum(pickle, "h")
      } yield ((h.toLong << 44) | (m.toLong << 22) | l.toLong)
    }
  }

  implicit object FloatUnpickler extends Unpickler[Float] {
    def unpickle[P](pickle: P)(implicit reader: PReader[P]) = reader.readNumber(pickle).map(_.toFloat)
  }

  implicit object DoubleUnpickler extends Unpickler[Double] {
    def unpickle[P](pickle: P)(implicit reader: PReader[P]) = reader.readNumber(pickle)
  }

  implicit object StringUnpickler extends Unpickler[String] {
    def unpickle[P](pickle: P)(implicit reader: PReader[P]) = reader.readString(pickle)
  }
}

case class UnpickledCurry[A](u: Unpickler[A]) {
  def unpickle[P](p: P)(implicit reader: PReader[P]): Try[A] = u.unpickle(p)
}
