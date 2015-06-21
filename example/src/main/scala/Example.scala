import prickle._

import scala.collection.mutable
import scala.reflect.ClassTag
import scala.util.{Success, Failure, Try}

sealed trait Fruit
case class Apple(isJuicy: Boolean) extends Fruit
case class Lemon(sourness: Double) extends Fruit
case class FruitSalad(components: Seq[Fruit]) extends Fruit
case object TheDurian extends Fruit

object Example extends App {

  println("\n1. No preparation is needed to pickle or unpickle values whose static type is exactly known:")

  val apples = Seq(Apple(true), Apple(false))
  val pickledApples = Pickle.intoString(apples)
  val rehydratedApples = Unpickle[Seq[Apple]].fromString(pickledApples)


  println(s"A bunch of Apples: ${apples}")
  println(s"Pickled apples: ${pickledApples}")
  println(s"Rehydrated apples: ${rehydratedApples}\n")


  println("2. To pickle a class hierarchy (aka 'Sum Type'), create a CompositePickler and enumerate the concrete types")

  //implict defs/vals should have an explicitly declared type to work properly
  implicit val fruitPickler: PicklerPair[Fruit] = CompositePickler[Fruit].
    concreteType[Apple].concreteType[Lemon].concreteType[FruitSalad].concreteType[TheDurian.type]

  val sourLemon = Lemon(sourness = 100.0)
  //fruitSalad's concrete type has been forgotten, replaced by more general supertype
  val fruitSalad: Fruit = FruitSalad(Seq(Apple(true), sourLemon, sourLemon, TheDurian))

  val fruitPickles = Pickle.intoString(fruitSalad)

  val rehydratedSalad = Unpickle[Fruit].fromString(fruitPickles)

  println(s"Notice how the fruit salad has multiple references to the same object 'sourLemon':\n${fruitSalad}")

  println(s"In the JSON, 2nd and subsequent occurences of an object are replaced by refs:\n${fruitPickles}")

  println(s"The rehydrated object graph doesnt contain duplicated lemons:\n${rehydratedSalad}\n")

}

trait Static {
  def id: Int
}
class Banana(val id: Int, condition: String) extends Fruit with Static

class Mandarine(val id: Int, seedCount: Int) extends Fruit with Static

object AdvancedLookupExample extends App {

  val bananas = Map(
    1 -> new Banana(1, "A bit green"),
    2 -> new Banana(2, "Perfect yellow"))

  val mandarines = Map(
    1 -> new Mandarine(1, 0),
    2 -> new Mandarine(2, 8))

  println("Sometime, you *don't* want to pickle objects in the graph because they already exist " +
    "in the environment where unpickling takes place (think 'reference data'). Rather, look them up by ID " +
    "at unpickle time.")

  println("This is a good use case for a custom pickler implementation, which can be fairly general " +
    "and un/pickle any statically looked-up object")

  val fruits = Seq(Apple(true), bananas(2), mandarines(1))

  implicit def staticPickler[S <: Static]: Pickler[S] = new Pickler[S] {
    //each instance of S is pickled to it's ID
    def pickle[P](value: S, state: PickleState)(implicit config: PConfig[P]): P = {
      config.makeNumber(value.id)
    }
  }

  //we need a mapping from runtime class name to the associated lookup table
  val lookupsForClass = Map(
    "Banana" -> bananas,
    "Mandarine" -> mandarines
  )
  implicit def staticUnpickler[S <: Static](implicit tag: ClassTag[S]): Unpickler[S] = new Unpickler[S] {
    //each instance of S is unpickled by lookup from it's Class+ID
    def unpickle[P](pickle: P, state: mutable.Map[String, Any])(implicit config: PConfig[P]) = {
      val id = config.readNumber(pickle).map(_.toInt)

      //this downcast reflects the multiple types of static data that this unpickler handles
      //it isn't required if you need only a single type of reference data (ie just `Unpickler[Banana]`)
      val lookup = lookupsForClass(tag.runtimeClass.getName).asInstanceOf[Map[Int, S]]

      id.map(lookup(_))
    }
  }

  implicit val fruitPickler: PicklerPair[Fruit] = CompositePickler[Fruit].
    concreteType[Apple].concreteType[Banana].concreteType[Mandarine]

//  implicit val fruitPickler2: PicklerPair[Fruit] = CompositePickler[Fruit].
//    concreteType[Apple].concreteType[Banana].concreteType[Mandarine]

  val fruitPickles = Pickle.intoString(fruits)

  println(s"In the pickled json, the banana and mandarine reference data" +
    s" is replaced by just their IDs: $fruitPickles")

  val Success(Seq(apple, banana, mandarine)) =
    Unpickle[Seq[Fruit]].fromString(fruitPickles)

  println("After unpickling, the apple is a new object")
  assert(!(apple eq fruits(0)))
  println("..but the banana and mandarine are the same static reference objects")
  assert(banana eq fruits(1))
  assert(mandarine eq fruits(2))
}