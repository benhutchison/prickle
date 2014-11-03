package prickle

import scala.collection.immutable.SortedMap
import scala.language.experimental.macros
import scala.collection.mutable
import scala.concurrent.duration.Duration
import microjson._

import java.util.Date

/** Use this object to invoke Pickling from user code */
object Pickle {

  def apply[A, P](value: A, state: PickleState = PickleState())(implicit p: Pickler[A], config: PConfig[P]): P =
    p.pickle(value, state)(config)

  def intoString[A](value: A, state: PickleState = PickleState())(implicit p: Pickler[A], config: PConfig[JsValue]): String = {
    val pickle = p.pickle(value, state)(config)
    Json.write(pickle)
  }

  def withPickler[A, P](value: A, state: PickleState = PickleState(), p: Pickler[A])(implicit config: PConfig[P]): P =
    p.pickle(value, state)(config)

  def withConfig[A, P](value: A, state: PickleState = PickleState(), config: PConfig[P])(implicit p: Pickler[A]): P =
    p.pickle(value, state)(config)

}

case class PickleState(refs: mutable.Map[Any, String] = mutable.Map.empty, var seq: Int = 0)

/** You should not need to implement this for the supported use cases:
  * - Primitives and Strings
  * - Case classes and case objects
  * - Maps, Sets and Seqs
  * - Class-hierarchies supported via composite picklers
  * */
trait  Pickler[A] {

  def pickle[P](obj: A, state: PickleState)(implicit config: PConfig[P]): P
}

/** Do not import this companion object into scope in user code.*/
object Pickler extends MaterializePicklerFallback {

  /** Rendered into English:
    * Take a `value`, whose fields have been pickled, and the current pickle `state`,
    * and do the right thing if shared object support is enabled:
    * - if its the first time we've seen this object, give it an id, add it to the pickle state,
    * and emit its' pickle normally.
    * - if we've seen it before, don't emit a full pickle; substitute a ref to the previous occurence.*/
  def resolvingSharing[P](value: Any, fieldPickles: Seq[(String, P)], state: PickleState, config: PConfig[P]): P = {
    if (config.areSharedObjectsSupported) {
      state.refs.get(value).fold {
        state.seq += 1
        state.refs += value -> state.seq.toString
        val idKey = config.prefix + "id"
        config.makeObject((idKey, config.makeString(state.seq.toString)) +: fieldPickles)
      }(
          id => config.makeObject(config.prefix + "ref", config.makeString(id))
        )
    }
    else {
      config.makeObject(fieldPickles)
    }
  }

  /** As above, but for a collection whose elements are stored in a Json Array*/
  def resolvingSharingCollection[P](coll: Any, elems: Seq[P], state: PickleState, config: PConfig[P]): P = {
    val payload = (config.prefix + "elems", config.makeArray(elems:_*))
    if (config.areSharedObjectsSupported) {
      state.refs.get(coll).fold {
        state.seq += 1
        state.refs += coll -> state.seq.toString
        val idTag = (config.prefix + "id", config.makeString(state.seq.toString))
        config.makeObjectFrom(idTag, payload)
      }(
          id => config.makeObject(config.prefix + "ref", config.makeString(id))
        )
    }
    else {
      config.makeObjectFrom(payload)
    }
  }

  implicit object BooleanPickler extends Pickler[Boolean] {
    def pickle[P](x: Boolean, state: PickleState)(implicit config: PConfig[P]): P =
      config.makeBoolean(x)
  }

  implicit object CharPickler extends Pickler[Char] {
    def pickle[P](x: Char, state: PickleState)(implicit config: PConfig[P]): P =
      config.makeString(x.toString)
  }

  implicit object BytePickler extends Pickler[Byte] {
    def pickle[P](x: Byte, state: PickleState)(implicit config: PConfig[P]): P =
      config.makeNumber(x)
  }

  implicit object ShortPickler extends Pickler[Short] {
    def pickle[P](x: Short, state: PickleState)(implicit config: PConfig[P]): P =
      config.makeNumber(x)
  }

  implicit object IntPickler extends Pickler[Int] {
    def pickle[P](x: Int, state: PickleState)(implicit config: PConfig[P]): P =
      config.makeNumber(x)
  }

  implicit object LongPickler extends Pickler[Long] {
    def pickle[P](x: Long, state: PickleState)(implicit config: PConfig[P]): P = {
      config.makeObject(Seq(
        ("l", config.makeNumber(x.toInt & 0x3fffff)),
        ("m", config.makeNumber((x >> 22).toInt & 0x3fffff)),
        ("h", config.makeNumber((x >> 44).toInt))))
    }
  }

  implicit object FloatPickler extends Pickler[Float] {
    def pickle[P](x: Float, state: PickleState)(implicit config: PConfig[P]): P =
      config.makeNumber(x)
  }

  implicit object DoublePickler extends Pickler[Double] {
    def pickle[P](x: Double, state: PickleState)(implicit config: PConfig[P]): P =
      config.makeNumber(x)
  }

  implicit object StringPickler extends Pickler[String] {
    def pickle[P](x: String, state: PickleState)(implicit config: PConfig[P]): P =
      config.makeString(x)
  }


  implicit object DurationPickler extends Pickler[Duration] {
    def pickle[P](x: Duration, state: PickleState)(implicit config: PConfig[P]): P =
      LongPickler.pickle(x.toNanos, state)
  }

  implicit object DatePickler extends Pickler[Date] {
    def pickle[P](x: Date, state: PickleState)(implicit config: PConfig[P]): P =
      config.makeNumber(x.getTime)
  }

  implicit def mapPickler[K, V](implicit kpickler: Pickler[K], vpickler: Pickler[V]) = new Pickler[Map[K, V]] {
    def pickle[P](value: Map[K, V], state: PickleState)(implicit config: PConfig[P]): P = {
      val entries: Iterable[P] = value.map(kv => {
        val (k, v) = kv
        config.makeArray(Pickle(k, state), Pickle(v, state))
      })
      resolvingSharingCollection[P](value, entries.toSeq, state, config)
    }
  }

  implicit def sortedMapPickler[K, V](implicit kpickler: Pickler[K], vpickler: Pickler[V]) = new Pickler[SortedMap[K, V]] {
    def pickle[P](value: SortedMap[K, V], state: PickleState)(implicit config: PConfig[P]): P = {
      val entries: Iterable[P] = value.map(kv => {
        val (k, v) = kv
        config.makeArray(Pickle(k, state), Pickle(v, state))
      })
      resolvingSharingCollection[P](value, entries.toSeq, state, config)
    }
  }

  implicit def seqPickler[T](implicit pickler: Pickler[T]) = new Pickler[Seq[T]] {
    def pickle[P](value: Seq[T], state: PickleState)(implicit config: PConfig[P]): P = {
      resolvingSharingCollection[P](value, value.map(e => Pickle(e, state)), state, config)
    }
  }

  implicit def iterablePickler[T](implicit pickler: Pickler[T]) = new Pickler[Iterable[T]] {
    def pickle[P](value: Iterable[T], state: PickleState)(implicit config: PConfig[P]): P = {
      resolvingSharingCollection[P](value, value.map(e => Pickle(e, state)).toSeq, state, config)
    }
  }

  implicit def setPickler[T](implicit pickler: Pickler[T]) = new Pickler[Set[T]] {
    def pickle[P](value: Set[T], state: PickleState)(implicit config: PConfig[P]): P = {
      resolvingSharingCollection[P](value, value.map(e => Pickle(e, state)).toSeq, state, config)
    }
  }

  implicit def optionPickler[T](implicit pickler: Pickler[T]): Pickler[Option[T]] = new Pickler[Option[T]] {
    def pickle[P](value: Option[T], state: PickleState)(implicit config: PConfig[P]): P = {
      resolvingSharingCollection[P](value, value.map(e => Pickle(e, state)).toSeq, state, config)
    }
  }


  implicit def toPickler[A <: AnyRef](implicit pair: PicklerPair[A]): Pickler[A] = pair.pickler
}
trait MaterializePicklerFallback {

  implicit def materializePickler[T]: Pickler[T] =
  macro PicklerMaterializersImpl.materializePickler[T]
}
