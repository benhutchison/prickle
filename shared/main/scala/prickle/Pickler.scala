package prickle

import scala.language.experimental.macros
import scala.collection.mutable
import scala.reflect.ClassTag


/** Use this object to invoke Pickling from user code */
object Pickle {

  def apply[A, P](value: A, state: PickleState = PickleState())(implicit p: Pickler[A], config: PConfig[P]): P =
    p.pickle(value, state)(config)

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

  private[prickle] def resolvingSharing[P](value: Any, fieldPickles: Seq[(String, P)], state: PickleState, config: PConfig[P]): P = {
    if (config.isCyclesSupported) {
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

  implicit def mapPickler[K, V](implicit kpickler: Pickler[K], vpickler: Pickler[V]) = new Pickler[Map[K, V]] {
    def pickle[P](value: Map[K, V], state: PickleState)(implicit config: PConfig[P]): P = {
      val entries = value.map(kv => {
        val (k, v) = kv
        config.makeArray(Pickle(k, state), Pickle(v, state))
      })
      config.makeArray(entries.toSeq: _*)
    }
  }

  implicit def seqPickler[T](implicit pickler: Pickler[T]) = new Pickler[Seq[T]] {
    def pickle[P](value: Seq[T], state: PickleState)(implicit config: PConfig[P]): P = {
      config.makeArray(value.map(e => Pickle(e, state)): _*)
    }
  }

  implicit def setPickler[T](implicit pickler: Pickler[T]) = new Pickler[Set[T]] {
    def pickle[P](value: Set[T], state: PickleState)(implicit config: PConfig[P]): P = {
      config.makeArray(value.map(e => Pickle(e, state)).toSeq: _*)
    }
  }

  implicit def optionPickler[T](implicit pickler: Pickler[T]): Pickler[Option[T]] = new Pickler[Option[T]] {
    def pickle[P](value: Option[T], state: PickleState)(implicit config: PConfig[P]): P = {
      config.makeArray(value.map(e => Pickle(e, state)).toSeq: _*)
    }
  }


  implicit def toPickler[A <: AnyRef](implicit pair: PicklerPair[A]): Pickler[A] = pair.pickler
}
trait MaterializePicklerFallback {

  implicit def materializePickler[T]: Pickler[T] =
  macro PicklerMaterializersImpl.materializePickler[T]
}
