package prickle

import scala.reflect.ClassTag
import scala.util.{Success, Failure, Try}
import collection.mutable

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


  def pickle[P](obj: A, state: PickleState)(implicit config: PConfig[P]): P = {
    if (obj != null) {
      val name = obj.getClass.getName
      val concretePickler = picklers.get(name).get.asInstanceOf[Pickler[A]]
      config.makeObject(Seq(
        (CompositePickler.classKey, config.makeString(name)),
        (CompositePickler.valueKey, Pickle(obj, state)(concretePickler, config))))
    } else {
      config.makeNull
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

  def unpickle[P](pickle: P, state: mutable.Map[String, Any])(implicit config: PConfig[P]): Try[A] = {
    if (config.isNull(pickle)) {
      Success(null.asInstanceOf[A])
    } else {
      for {
        className <-  config.readObjectFieldStr(pickle, classKey)

        field <- config.readObjectField(pickle, valueKey)

        result <- unpicklers.get(className) match {
          case Some(unpickler) =>
            unpickler.asInstanceOf[Unpickler[A]].unpickle(field, state)(config)
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

  def classKey(implicit config: PConfig[_]) = s"${config.prefix}cls"
  def valueKey(implicit config: PConfig[_]) = s"${config.prefix}val"

  def apply[A <: AnyRef] = new PicklerPair[A]()

}

/** Helper for registration of Pickler[B]/Unpickler[B] pairs via `withSubtype[B]`*/
case class PicklerPair[A <: AnyRef](pickler: CompositePickler[A] = new CompositePickler[A](),
                          unpickler: CompositeUnpickler[A] = new CompositeUnpickler[A]()) {

  def concreteType[B <: A](implicit p: Pickler[B], u: Unpickler[B], tag: ClassTag[B]) = {
    copy(pickler = this.pickler.concreteType[B], unpickler = this.unpickler.concreteType[B])
  }
}