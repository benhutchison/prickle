package prickle

import scala.util.{Success, Failure, Try}
import scala.language.experimental.macros

/** Use this object to invoke Unpickling from user code */
object Unpickle {

  def apply[A](implicit u: Unpickler[A]) = UnpickledCurry[A](u)
}
case class UnpickledCurry[A](u: Unpickler[A]) {
  def from[P](p: P)(implicit reader: PReader[P]): Try[A] = u.unpickle(p)
}

/** You should not need to implement this for the supported use cases:
  * - Primitives and Strings
  * - Case classes and case objects
  * - Maps, Sets and Seqs
  * - Class-hierarchies supported via composite picklers
  * */
trait Unpickler[A] {

  def unpickle[P](pickle: P)(implicit reader: PReader[P]): Try[A]
}

/** Do not import this companion object into scope in user code.*/
object Unpickler extends MaterializeUnpicklerFallback {

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
    def unpickle[P](pickle: P)(implicit reader: PReader[P]) = {
      if (reader.isNull(pickle))
        Success(null)
      else
        reader.readString(pickle)
    }
  }

  implicit def mapUnpickler[K, V](implicit ku: Unpickler[K], vu: Unpickler[V]) =  new Unpickler[Map[K, V]] {
    def unpickle[P](pickle: P)(implicit reader: PReader[P]): Try[Map[K, V]] = {

      val KeyIndex = 0
      val ValueIndex = 1
      for {
        len <- reader.readArrayLength(pickle)
        kvs <- Try {
          (0 until len).toList.map(index => for {
            entryPickle <- reader.readArrayElem(pickle, index)
            kp <- reader.readArrayElem(entryPickle, KeyIndex)
            k <- ku.unpickle(kp)
            vp <- reader.readArrayElem(entryPickle, ValueIndex)
            v <- vu.unpickle(vp)
          } yield k -> v).map(_.get)
        }
      } yield
        kvs.foldLeft(Map.empty[K, V])((m, kv) => m.updated(kv._1, kv._2))
    }
  }

  implicit def toUnpickler[A <: AnyRef](implicit pair: PicklerPair[A]): Unpickler[A] = pair.unpickler
}
trait MaterializeUnpicklerFallback {

  implicit def materializeUnpickler[T]: Unpickler[T] =
  macro PicklerMaterializersImpl.materializeUnpickler[T]
}

