#Prickle

Prickle is a library for easily pickling (serializing) object graphs between Scala and Scala.js. 

It is based upon scala-js-pickling, but adds several improvements & refinements:

* [Better support for class hierarchies / sum types](#support-for-class-hierarchies-and-sum-types)
* [Support for shared objects and cycles in the serialized object graph](#support-for-shared-objects)
* [Pickles to/from Strings](#pickling-to-string)
* [Unpickling a value of type T yields a Try[T]](#unpickling-yields-a-try)
* 100% identical scala code between JVM and JS; no platform specific dependecy 

Currently, prickle supports automatic, reflection-free recursive pickling of
* Case classes
* Case objects
* Seqs, Sets and Maps
* Primitives

##Getting Prickle

Scala.jvm 2.11+
`"com.github.benhutchison" %% "prickle" % "1.0"`

Scala.js 0.5+
`"com.github.benhutchison" %%% "prickle" % "1.0"`

Prickle depends upon [microjson](https://github.com/benhutchison/MicroJson) in its default pickle configuration.

Prickle is open source under the Apache 2 license.

##[Runnable Example](https://github.com/benhutchison/prickle/blob/master/example/src/main/scala/Example.scala)

To run:
```sbt
> example/run
```
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

Like scala-pickling and scala-js-pickling, prickle uses implicit `Pickler[T]` and `Unpickler[T]` type-classes to transform values of type T. 
For case classes, these type classes will be automatically generated via an implicit macro, if they are not already available in implicit scope. This is a recursive process, so that picklers & unpicklers for each field of type T will also be resolved and possibly generated.


### Unpickling yields a Try
 
It's tempting to think of Pickling and Unpickling as symmetrical, inverse operations 
 (eg `T => String`, `String => T`) but there is one key difference in practice: Unpickling is
 far more likely to fail. 
 
This stems from the nature of the operations. Pickling transforms a structured, well typed object graph into flattened, stringly-typed data. 
Unpickling takes a weakly-typed string and re-constructs the typed object-graph from it. Most arbitrary strings won't re-construct valid
object graphs, so the unpickle operation attempts to move from a higher entropy state to a lower entropy state.

Prickle acknowledges the possibility of failure by returning a Try[T] when attempting to unpickle 
([An extended talk about the philosophy guiding this design, with supa-crunch audio](https://www.youtube.com/watch?v=ujpHtodp6OQ)) 

##Support for Class Hierarchies and Sum Types

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

What is meant by "shared" here are objects that are referenced more than once in an object graph.
To avoid duplicating such objects when pickling, prickle's algorithm remembers what objects it has
pickled so far, and introduce references to already pickled state when it re-encounters them.
On the unpickle side, prickle tracks an IDs associated with each unpickled object and resolved references
to IDs it has already encountered in the stream.

Shared objects brings a memory and pickled-data overhead, since a mapping between objects and IDs must be maintained during pickling
and unpickling. It can be turned off in the PConfig by setting `isCyclesSupported = false`.

Note on terminology: sometimes object graphs with shared objects are described as having *circular* or cyclic references,
but there is a subtle difference. Circular references implies there is a path through the graph that returns
to the originating object. Shared references is a weaker condition, that simply implies there are 
two different paths to the same object.  The former cannot result from the use of purely immutable data, but shared objects
certainly can- and does- often.

##Controlling Pickling via PConfig

The pickle and unpickle operations take an implicit PConfig ("pickle-config") which specifies:

- The type/format of the pickled data. Default: String
- Whether support for shared/cyclic objects is required. Default: true.
- The prefix String to prepend to *internal keys* in the Json objects used by prickle itself. Default: #

These *internal keys* are
- id: identifies an object that may be shared
- ref: refers to the id of an earlier object in the stream
- scalaObj: refers to the name of a scala object
- cls: the concrete class of a pickled object
- val: identifies the fields of a pickled object

##Pickling to String 

The default pickle format is JsValue from the [microjson library](https://github.com/benhutchison/MicroJson). 
`JsValue` serves as a waypoint in structured JSON to/from a `String`.
Because un/pickling to String is so common, methods existing to go straight to and
from Strings:
```
val p = Person("Ben", "Hutchison")

val s: String = Pickle.toString(p)

Unpickle[Person].fromString(s): Try[A]
```

##Troubleshooting

It's likely you will hit "Implicit Parameter Not Found" errors when you first use prickle on a non-trivial problem.
Here's some tips for diagnosing such problems:

- Firstly, Prickle pushes more errors to compile-time, so be patient
- TODO

##Authors

Prickle is written and maintained by Ben Hutchison.

Credits to Sebastien Doeraene for scala-js-pickling, Li Haoyi for microjson,
and Eugene Burmako for helping fix Prickle's macro.
