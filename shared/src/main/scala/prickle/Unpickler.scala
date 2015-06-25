package prickle

import scala.collection.generic.CanBuildFrom
import scala.collection.immutable.SortedMap
import scala.util.{Success, Failure, Try}
import collection.mutable
import scala.concurrent.duration.Duration
import scala.language.experimental.macros
import microjson._

import java.util.Date
import java.util.UUID

/** Use this object to invoke Unpickling from user code */
object Unpickle {

  def apply[A](implicit u: Unpickler[A]) = UnpickledCurry[A](u)
}
case class UnpickledCurry[A](u: Unpickler[A]) {

  def from[P](p: P, state: mutable.Map[String, Any] = mutable.Map.empty)
             (implicit config: PConfig[P]): Try[A] = {
    u.unpickle(p, state)
  }

  def fromString(json: String, state: mutable.Map[String, Any] = mutable.Map.empty)
             (implicit config: PConfig[JsValue]): Try[A] = {
    Try(Json.read(json)).flatMap(jsValue =>
      u.unpickle(jsValue, state)(config))
  }
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

  /** In english: ensure that the state is updated to include the object being unpickled (`value`),
    * if it has an id.*/
  def resolvingSharing[P](value: Any, pickle: P, state: mutable.Map[String, Any], config: PConfig[P]): Unit = {
    if (config.areSharedObjectsSupported)
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

  implicit object UnitUnpickler extends Unpickler[Unit] {
    def unpickle[P](pickle: P, state: mutable.Map[String, Any])(implicit config: PConfig[P]) = Try(())
  }

  implicit object DurationUnpickler extends Unpickler[Duration] {
    def unpickle[P](pickle: P, state: mutable.Map[String, Any])(implicit config: PConfig[P]): Try[Duration] =
      LongUnpickler.unpickle(pickle, state).map(f => Duration.fromNanos(f))
  }

  implicit object DateUnpickler extends Unpickler[Date] {
    def unpickle[P](pickle: P, state: mutable.Map[String, Any])(implicit config: PConfig[P]) =
      config.readNumber(pickle).map(_.toLong).map(new Date(_))
  }

  implicit object UUIDUnpickler extends Unpickler[UUID] {
    def unpickle[P](pickle: P, state: mutable.Map[String, Any])(implicit config: PConfig[P]) =
      config.readString(pickle).flatMap(s => Try(UUID.fromString(s)))
  }

  implicit def mapUnpickler[K, V](implicit ku: Unpickler[K], vu: Unpickler[V]) =  new Unpickler[Map[K, V]] {
    def unpickle[P](pickle: P, state: mutable.Map[String, Any])(implicit config: PConfig[P]): Try[Map[K, V]] = {

      val result = unpickleMap[K, V, Map[K, V], P](Map.empty, pickle, state)
      result.foreach(Unpickler.resolvingSharing(_, pickle, state, config))
      result
    }
  }

  implicit def sortedMapUnpickler[K, V](implicit ku: Unpickler[K], vu: Unpickler[V], ord: Ordering[K]) =  new Unpickler[SortedMap[K, V]] {
    def unpickle[P](pickle: P, state: mutable.Map[String, Any])(implicit config: PConfig[P]): Try[SortedMap[K, V]] = {

      val result = unpickleMap[K, V, SortedMap[K, V], P](SortedMap.empty, pickle, state)
      result.foreach(Unpickler.resolvingSharing(_, pickle, state, config))
      result
    }
  }

  private def unpickleMap[K, V, M <: Map[K, V], P](empty: M, pickle: P,
                                                   state: mutable.Map[String, Any])(
                                                   implicit config: PConfig[P],
                                                   ku: Unpickler[K],
                                                   vu: Unpickler[V],
                                                   cbf: CanBuildFrom[Nothing, (K, V), M]): Try[M] = {
    import config._
    readObjectField(pickle, prefix + "ref").transform(
      (p: P) => {
        readString(p).flatMap(ref => Try(state(ref).asInstanceOf[M]))
      },
      _ => readObjectField(pickle, prefix + "elems").flatMap(p => {
        val KeyIndex = 0
        val ValueIndex = 1
        for {
          len <- readArrayLength(p)
          kvs <- Try {
            (0 until len).toVector.map(index => for {
              entryPickle <- readArrayElem(p, index)
              kp <- readArrayElem(entryPickle, KeyIndex)
              k <- ku.unpickle(kp, state)
              vp <- readArrayElem(entryPickle, ValueIndex)
              v <- vu.unpickle(vp, state)
            } yield k -> v).map(_.get)
          }
        } yield {
          val builder = cbf()
          kvs.foreach(kv => builder+= kv._1 -> kv._2)
          builder.result()
        }
      })
    )
  }

  private def unpickleSeqish[T, S[_], P](pickle: P, state: mutable.Map[String, Any])
                               (implicit config: PConfig[P],
                                u: Unpickler[T],
                                cbf: CanBuildFrom[Nothing, T, S[T]]): Try[S[T]] = {

    import config._
    readObjectField(pickle, prefix + "ref").transform(
      (p: P) => {
        readString(p).flatMap(ref => Try(state(ref).asInstanceOf[S[T]]))
      },
      _ => readObjectField(pickle, prefix + "elems").flatMap(p => {
        readArrayLength(p).flatMap(len => {
          val builder = cbf()
          (0 until len).foreach(index =>
            builder += u.unpickle(readArrayElem(p, index).get, state).get)
          val result = builder.result()
          Unpickler.resolvingSharing(result, pickle, state, config)
          Try(result)
        })
      }
    ))
  }

  implicit def listUnpickler[T](implicit unpickler: Unpickler[T]): Unpickler[List[T]]  =  new Unpickler[List[T]] {
    def unpickle[P](pickle: P, state: mutable.Map[String, Any])(implicit config: PConfig[P]): Try[List[T]] = {
      unpickleSeqish[T, List, P](pickle, state)
    }
  }

  implicit def immutableSeqUnpickler[T](implicit unpickler: Unpickler[T]) =  new Unpickler[collection.immutable.Seq[T]] {
    def unpickle[P](pickle: P, state: mutable.Map[String, Any])(implicit config: PConfig[P]): Try[collection.immutable.Seq[T]] = {
      unpickleSeqish[T, collection.immutable.Seq, P](pickle, state)
    }
  }

  implicit def seqUnpickler[T](implicit unpickler: Unpickler[T]) =  new Unpickler[Seq[T]] {
    def unpickle[P](pickle: P, state: mutable.Map[String, Any])(implicit config: PConfig[P]): Try[Seq[T]] = {
      unpickleSeqish[T, Seq, P](pickle, state)
    }
  }

 implicit def iterableUnpickler[T](implicit unpickler: Unpickler[T]) =  new Unpickler[Iterable[T]] {
    def unpickle[P](pickle: P, state: mutable.Map[String, Any])(implicit config: PConfig[P]): Try[Iterable[T]] = {
      unpickleSeqish[T, Iterable, P](pickle, state)
    }
  }

  implicit def setUnpickler[T](implicit unpickler: Unpickler[T]) =  new Unpickler[Set[T]] {
    def unpickle[P](pickle: P, state: mutable.Map[String, Any])(implicit config: PConfig[P]): Try[Set[T]] = {
      unpickleSeqish[T, Set, P](pickle, state)
    }
  }

  implicit def optionUnpickler[T](implicit unpickler: Unpickler[T]): Unpickler[Option[T]] = new Unpickler[Option[T]] {
    def unpickle[P](pickle: P, state: mutable.Map[String, Any])(implicit config: PConfig[P]): Try[Option[T]] = {
      unpickleSeqish[T, List, P](pickle, state).map(_.headOption)
    }
  }

  implicit def toUnpickler[A <: AnyRef](implicit pair: PicklerPair[A]): Unpickler[A] = pair.unpickler
}
trait MaterializeUnpicklerFallback {

  implicit def materializeUnpickler[T]: Unpickler[T] =
    macro PicklerMaterializersImpl.materializeUnpickler[T]
}

