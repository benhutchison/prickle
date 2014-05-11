package prickle

import scala.util.{Success, Failure, Try}
import collection.mutable
import scala.language.experimental.macros

/** Use this object to invoke Unpickling from user code */
object Unpickle {

  def apply[A](implicit u: Unpickler[A]) = UnpickledCurry[A](u)
}
case class UnpickledCurry[A](u: Unpickler[A]) {

  def from[P](p: P, state: mutable.Map[String, Any] = mutable.Map.empty)(implicit config: PConfig[P]): Try[A] = u.unpickle(p, state)
}

/** You should not need to implement this for the supported use cases:
  * - Primitives and Strings
  * - Case classes and case objects
  * - Maps, Sets and Seqs
  * - Class-hierarchies supported via composite picklers
  * */
trait Unpickler[A] {

  def unpickle[P](pickle: P, state: mutable.Map[String, Any])(implicit config: PConfig[P]): Try[A]
}

/** Do not import this companion object into scope in user code.*/
object Unpickler extends MaterializeUnpicklerFallback {

  private[prickle] def resolvingSharing[P](value: Any, pickle: P, state: mutable.Map[String, Any], config: PConfig[P]): Unit = {
    if (config.isCyclesSupported)
      config.readObjectField(pickle, config.prefix + "id").flatMap(
        field => config.readString(field)).foreach(
          id => state += (id -> value))
  }

  implicit object BooleanUnpickler extends Unpickler[Boolean] {
    def unpickle[P](pickle: P, state: mutable.Map[String, Any])(implicit config: PConfig[P]) = config.readBoolean(pickle)
  }

  implicit object CharUnpickler extends Unpickler[Char] {
    def unpickle[P](pickle: P, state: mutable.Map[String, Any])(implicit config: PConfig[P]) = config.readString(pickle).flatMap(s => Try(s.charAt(0)))
  }

  implicit object ByteUnpickler extends Unpickler[Byte] {
    def unpickle[P](pickle: P, state: mutable.Map[String, Any])(implicit config: PConfig[P]) = config.readNumber(pickle).map(_.toByte)
  }

  implicit object ShortUnpickler extends Unpickler[Short] {
    def unpickle[P](pickle: P, state: mutable.Map[String, Any])(implicit config: PConfig[P]) = config.readNumber(pickle).map(_.toShort)
  }

  implicit object IntUnpickler extends Unpickler[Int] {
    def unpickle[P](pickle: P, state: mutable.Map[String, Any])(implicit config: PConfig[P]) = config.readNumber(pickle).map(_.toInt)
  }

  implicit object LongUnpickler extends Unpickler[Long] {
    def unpickle[P](pickle: P, state: mutable.Map[String, Any])(implicit config: PConfig[P]) = {
      for {
        l <- config.readObjectFieldNum(pickle, "l")
        m <- config.readObjectFieldNum(pickle, "m")
        h <- config.readObjectFieldNum(pickle, "h")
      } yield ((h.toLong << 44) | (m.toLong << 22) | l.toLong)
    }
  }

  implicit object FloatUnpickler extends Unpickler[Float] {
    def unpickle[P](pickle: P, state: mutable.Map[String, Any])(implicit config: PConfig[P]) = config.readNumber(pickle).map(_.toFloat)
  }

  implicit object DoubleUnpickler extends Unpickler[Double] {
    def unpickle[P](pickle: P, state: mutable.Map[String, Any])(implicit config: PConfig[P]) = config.readNumber(pickle)
  }

  implicit object StringUnpickler extends Unpickler[String] {
    def unpickle[P](pickle: P, state: mutable.Map[String, Any])(implicit config: PConfig[P]) = {
      if (config.isNull(pickle))
        Success(null)
      else
        config.readString(pickle)
    }
  }

  implicit def mapUnpickler[K, V](implicit ku: Unpickler[K], vu: Unpickler[V]) =  new Unpickler[Map[K, V]] {
    def unpickle[P](pickle: P, state: mutable.Map[String, Any])(implicit config: PConfig[P]): Try[Map[K, V]] = {

      val KeyIndex = 0
      val ValueIndex = 1
      for {
        len <- config.readArrayLength(pickle)
        kvs <- Try {
          (0 until len).toList.map(index => for {
            entryPickle <- config.readArrayElem(pickle, index)
            kp <- config.readArrayElem(entryPickle, KeyIndex)
            k <- ku.unpickle(kp, state)
            vp <- config.readArrayElem(entryPickle, ValueIndex)
            v <- vu.unpickle(vp, state)
          } yield k -> v).map(_.get)
        }
      } yield
        kvs.foldLeft(Map.empty[K, V])((m, kv) => m.updated(kv._1, kv._2))
    }
  }

  //TODO support shared objects for all of these cases
  implicit def seqUnpickler[T](implicit unpickler: Unpickler[T]) =  new Unpickler[Seq[T]] {
    def unpickle[P](pickle: P, state: mutable.Map[String, Any])(implicit config: PConfig[P]): Try[Seq[T]] = {
      import config._
      readArrayLength(pickle).flatMap(len =>
        Try((0 until len).map(index => unpickler.unpickle(readArrayElem(pickle, index).get, state).get))
      )
    }
  }

  implicit def setUnpickler[T](implicit unpickler: Unpickler[T]) =  new Unpickler[Set[T]] {
    def unpickle[P](pickle: P, state: mutable.Map[String, Any])(implicit config: PConfig[P]): Try[Set[T]] = {
      import config._
      readArrayLength(pickle).flatMap(len =>
        Try((0 until len).map(index => unpickler.unpickle(readArrayElem(pickle, index).get, state).get).toSet)
      )
    }
  }

  implicit def optionUnpickler[T](implicit unpickler: Unpickler[T]): Unpickler[Option[T]] = new Unpickler[Option[T]] {
    def unpickle[P](pickle: P, state: mutable.Map[String, Any])(implicit config: PConfig[P]): Try[Option[T]] = {
      import config._
      readArrayLength(pickle).flatMap(len =>
        if (len == 1)
          readArrayElem(pickle, 0).flatMap(p => unpickler.unpickle(p, state).map(t => Some(t)))
        else
          Try(None)
      )
    }
  }

  implicit def toUnpickler[A <: AnyRef](implicit pair: PicklerPair[A]): Unpickler[A] = pair.unpickler
}
trait MaterializeUnpicklerFallback {

  implicit def materializeUnpickler[T]: Unpickler[T] =
    macro PicklerMaterializersImpl.materializeUnpickler[T]
}

