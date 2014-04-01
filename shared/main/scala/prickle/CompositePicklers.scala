package prickle

import scala.reflect.ClassTag
import scala.util.{Success, Failure, Try}

/**
 * A `CompositePickler[A]` is used to pickle closed class hierarchies under a supertype `A`,
 * where the subclasses' precise static types are lost.
 *
 * Picklers for each concrete subclass `B` must be registered with the composite using the `concreteType[B]` method.
 *
 * CompositePicklers use a more complex serialization format than regular picklers, storing the subclass name
 * under key `CompositePickler.ClassKey` and the pickle body under `CompositePickler.ValueKey`.
 * */

case class CompositePickler[A <: AnyRef](picklers: Map[String, Pickler[_]] = Map.empty) extends Pickler[A] {


  def pickle[P](obj: A)(implicit builder: PBuilder[P]): P = {
    if (obj != null) {
      val name = obj.getClass.getName
      val p = picklers.get(name).get.asInstanceOf[Pickler[A]]
      builder.makeObject((CompositePickler.ClassKey, builder.makeString(name)), (CompositePickler.ValueKey, p.pickle(obj)))
    } else {
      builder.makeNull()
    }
  }

  def concreteType[B <: A](implicit p: Pickler[B], subtag: ClassTag[B]): CompositePickler[A] = {
    copy(picklers = this.picklers + (subtag.runtimeClass.getName -> p))
  }
}

/**
 * A `CompositeUnpickler[A]` is used to unpickle closed class hierarchies under a supertype `A`,
 * where the subclasses' precise static types are lost.
 *
 * Unpicklers for each concrete subclass `B` must be registered with the composite using the `concreteType[B]` method.
 * */
case class CompositeUnpickler[A <: AnyRef](unpicklers: Map[String, Unpickler[_]] = Map.empty) extends Unpickler[A] {

  import CompositePickler._


  def unpickle[P](pickle: P)(implicit reader: PReader[P]): Try[A] = {
    if (reader.isNull(pickle)) {
      Success(null.asInstanceOf[A])
    } else {
      for {
        className <-  reader.readObjectFieldStr(pickle, ClassKey)

        field <- reader.readObjectField(pickle, ValueKey)

        result <- unpicklers.get(className) match {
          case Some(unpickler) =>
            unpickler.asInstanceOf[Unpickler[A]].unpickle(field)
          case None =>
            Failure(new RuntimeException(s"No unpickler for '$className' in: ${unpicklers.keys.mkString(", ")}"))
        }
      } yield result
    }
  }

  def concreteType[B <: A](implicit u: Unpickler[B], subtag: ClassTag[B]): CompositeUnpickler[A] = {
    copy(unpicklers + (subtag.runtimeClass.getName -> u))
  }
}

object CompositePickler {

  val ClassKey = "#cls"
  val ValueKey = "#val"

  def apply[A <: AnyRef] = new PicklerPair[A]()

}

/** Helper for registration of Pickler[B]/Unpickler[B] pairs via `withSubtype[B]`*/
case class PicklerPair[A <: AnyRef](pickler: CompositePickler[A] = new CompositePickler[A](),
                          unpickler: CompositeUnpickler[A] = new CompositeUnpickler[A]()) {

  def concreteType[B <: A](implicit p: Pickler[B], u: Unpickler[B], tag: ClassTag[B]) = {
    copy(pickler = this.pickler.concreteType[B], unpickler = this.unpickler.concreteType[B])
  }
}