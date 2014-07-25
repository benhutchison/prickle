#Prickle

Prickle is a library for serializing (pickling) object graphs between Scala and Scala.js. It is based upon scala-js-pickling, but adds three key improvements:

* Better support for class hierarchies
* Support for sharing and cycles in the serialized object graph
* Pickles to/from Strings, so no platform-specific JSON dependencies are required

Currently, prickle supports automatic, reflection-free recursive pickling of
* Case classes
* Case objects
* Seqs, Sets and Maps
* Primitives


##[Runnable Example](https://github.com/benhutchison/prickle/blob/master/example/src/main/scala/Example.scala)

To run:
```sbt
> example/run```

Demonstrates: 
- Basic pickling of values whose static types is the same as their runtime class.
- Using CompositePicklers to pickle class hierarchies, i.e. values whose static type is more general than their runtime class. 
- Support for shared objects in the pickled graph

```scala
import prickle._

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
```

##Basic Concepts

Like scala-pickling and scala-js-pickling, prickle uses implicit Pickler[T] and Unpickler[T] type-classes to transform values of type T. For case classes, these type classes will be automatically generated via an implicit macro, if they are not already available in implicit scope. This is a recursive process, so that picklers & unpicklers for each field of type T will also be resolved and possibly generated.



##Support for Class Hierarchies / Sum-types

It's common to have a hierarchy of classes where the concrete type of a value is not known statically.
In some contexts these are called [Sum Types](http://en.wikipedia.org/wiki/Tagged_union).

Prickle supports these via CompositePicklers. These are not automically derived by a macro, 
but must be configured by the programmer, and assigned to an implicit val. 

The pickle and unpickle operations can be specified together, yielding a `PicklerPair[A]`, that knows how to pickle/unpickle values of type `A`,
and all specified concrete subclasses. There are background implicit conversions in the `Pickler` and `Unpickler` that
can auto-unpack `PicklerPairs` into their two parts.

For example, the code below creates a PicklerPair[Fruit], that handles two cases of fruit,
`Apple`s and `Lemon`s:
`CompositePickler[Fruit].concreteType[Apple].concreteType[Lemon]`

### Improved Type-Safety vs [scala-js-pickling](https://github.com/scala-js/scala-js-pickling)

CompositePicklers play a similar role to the PicklerRegistry used in scala-js-pickling, but are safer.
In Prickle, missing Picklers will normally result in a compile-time error, as an implicit not found. 
(The exception is unregistered concrete subclasses of a CompositePickler.) 
However, in Scala-js-pickling, the discovery of missing un/picklers occurs at runtime when un/pickling is attempted.

![Composite Picklers Vs Singleton Registry](/docs/CompositePicklersVsRegistry.png?raw=true "Composite Picklers Vs Singleton Registry")

##Support for Shared objects

##Pickling to/from String
