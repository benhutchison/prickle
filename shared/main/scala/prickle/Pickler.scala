package prickle

import scala.language.experimental.macros


/** Use this object to invoke Pickling from user code */
object Pickle {

  def apply[A, P](value: A)(implicit p: Pickler[A], builder: PBuilder[P]): P = p.pickle(value)
}

/** You should not need to implement this for the supported use cases:
  * - Primitives and Strings
  * - Case classes and case objects
  * - Maps, Sets and Seqs
  * - Class-hierarchies supported via composite picklers
  * */
trait Pickler[A] {

  def pickle[P](obj: A)(implicit builder: PBuilder[P]): P
}

/** Do not import this companion object into scope in user code.*/
object Pickler extends MaterializePicklerFallback {

  implicit object BooleanPickler extends Pickler[Boolean] {
    def pickle[P](x: Boolean)(implicit builder: PBuilder[P]): P = builder.makeBoolean(x)
  }

  implicit object CharPickler extends Pickler[Char] {
    def pickle[P](x: Char)(implicit builder: PBuilder[P]): P = builder.makeString(x.toString)
  }

  implicit object BytePickler extends Pickler[Byte] {
    def pickle[P](x: Byte)(implicit builder: PBuilder[P]): P = builder.makeNumber(x)
  }

  implicit object ShortPickler extends Pickler[Short] {
    def pickle[P](x: Short)(implicit builder: PBuilder[P]): P = builder.makeNumber(x)
  }

  implicit object IntPickler extends Pickler[Int] {
    def pickle[P](x: Int)(implicit builder: PBuilder[P]): P = builder.makeNumber(x)
  }

  implicit object LongPickler extends Pickler[Long] {
    def pickle[P](x: Long)(implicit builder: PBuilder[P]): P = {
      builder.makeObject(
          ("l", builder.makeNumber(x.toInt & 0x3fffff)),
          ("m", builder.makeNumber((x >> 22).toInt & 0x3fffff)),
          ("h", builder.makeNumber((x >> 44).toInt)))
    }
  }

  implicit object FloatPickler extends Pickler[Float] {
    def pickle[P](x: Float)(implicit builder: PBuilder[P]): P = builder.makeNumber(x)
  }

  implicit object DoublePickler extends Pickler[Double] {
    def pickle[P](x: Double)(implicit builder: PBuilder[P]): P = builder.makeNumber(x)
  }

  implicit object StringPickler extends Pickler[String] {
    def pickle[P](x: String)(implicit builder: PBuilder[P]): P = builder.makeString(x)
  }

  implicit def mapPickler[K, V](implicit kp: Pickler[K], vp: Pickler[V]) = new Pickler[Map[K, V]] {
    def pickle[P](value: Map[K, V])(implicit builder: PBuilder[P]): P = {
      builder.makeArray(value.map(kv => {
        val (k, v) = kv
        builder.makeArray(kp.pickle(k), vp.pickle(v))
      }).toSeq: _*)
    }
  }

  implicit def toPickler[A <: AnyRef](implicit pair: PicklerPair[A]): Pickler[A] = pair.pickler
}
trait MaterializePicklerFallback {

  implicit def materializePickler[T]: Pickler[T] =
  macro PicklerMaterializersImpl.materializePickler[T]
}
