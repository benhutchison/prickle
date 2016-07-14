#Prickle

Prickle is a library for easily pickling (serializing) object graphs between Scala and Scala.js. 

It is based upon scala-js-pickling, but adds several improvements & refinements:

* [Pickling to/from Strings](#pickling-to-string-by-default)
* [Better support for class hierarchies / sum types](#support-for-class-hierarchies-and-sum-types)
* [Support for shared objects and cycles in the serialized object graph](#support-for-shared-objects)
* [Unpickling a value of type T yields a Try[T]](#unpickling-yields-a-try)
* 100% identical scala code between JVM and JS; no platform specific dependecy
* [Can handle Static Reference Data in the pickled object graph](#supporting-static-reference-data) 

Currently, prickle supports automatic, reflection-free recursive pickling of
* Case classes
* Case objects
* Iterables, Seqs, Sets, Maps and SortedMaps
* Dates and Durations
* Primitives

##Getting Prickle

Scala.jvm 2.11
`"com.github.benhutchison" %% "prickle" % "1.1.11"`

Scala.js 0.6+ on 2.11
`"com.github.benhutchison" %%% "prickle" % "1.1.11"`

To use, import the package, but do not import on the Pickler & Unpickler objects
```scala
import prickle._

//!Don't do this. Not Necessary
import Pickler._
```

Prickle depends upon [microjson](https://github.com/benhutchison/MicroJson) in its default pickle configuration.

Prickle is open source under the Apache 2 license.

##[Runnable Example](https://github.com/benhutchison/prickle/blob/master/example/src/main/scala/Example.scala)

To run:
```sbt
> example/run
```
The first example demonstrates: 
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
## Changelog

| Version | When   | Changes |
| --------| -------| --------|
| 1.0.3   | Oct 14 | SortedMap support. Duration Support. 2.10.x binary added |
| 1.1.0   | Nov 14 | Collection picklers support shared objects properly. Iterable support. |
| 1.1.1   | Jan 15 | Example showing how to handle static reference data in a pickled object graph. |
| 1.1.2   | Jan 15 | Fix #9 Double serialization bug |
| 1.1.3   | Feb 15 | 2.10.x support dropped, scala-js-0.6.0 support added |
| 1.1.4   | Mar 15 | List and immutable Seq support, upgrade to Scala.js 0.6.1|
| 1.1.5   | Apr 15 | Performance improvement: use object identity for equality check during unpickle, using mutable Builder during Map unpickle |
| 1.1.6   | Jun 15 | Performance: use builder for Seq/Iterable collections. UUID support. Upgrade scalajs 0.6.3 |
| 1.1.7   | Jun 15 | Performance: microjson 1.3 removed some silly inefficiency. Support Unit picklers |
| 1.1.8   | Jul 15 | Support Vector picklers |
| 1.1.9   | Aug 15 | Expose some Pickler/Unpickler helper methods for use by custom picklers |
| 1.1.10  | Nov 15 | Fix #28: account for unstable Set iteration order |
| 1.1.11  | Jul 16 | Fix incorrect utest runtime dependency, freshen libraries |


##Pickling to String by Default 

Prickle expects you probably want to pickle to and from a json String, so this is
the default. Because Strings are defined in both Scala and Scala.js core lib, there
is no need to depend upon a platform-specific json dependency.

Call `prickle.Pickle.intoString()` to pickle your object. The static type of the passed object
will be used to search for Pickler typeclasses in implicit scope. If none are found, and the object is 
a case-class or case-object, a macro will *materialize* a pickler using compile-time reflection
to analyze the fields of the object.

When Unpickling with `prickle.Unpickle[T].fromString()`, you must tell prickle what type to unpickle into, since it's unable to determine
this from the String parameter.

```
val p = Person("Ben", "Hutchison")

val s: String = Pickle.intoString(p)

val tryPerson = Unpickle[Person].fromString(s)
```

Under the hood, prikle converts objects to/from a json model (`microjson.JsValue`)
based on the [microjson library](https://github.com/benhutchison/MicroJson). 
MicroJson then renders or parses the JSON object graph to a flat String.


##Support for Class Hierarchies and Sum Types

It's common to have a hierarchy of classes where the concrete type of a value is not known statically.
In some contexts these are called [Sum Types](http://en.wikipedia.org/wiki/Tagged_union).

Prickle supports these via CompositePicklers. These are not automically derived by a macro, 
but must be configured by the programmer, and assigned to an implicit val. 

Example: How to creates a PicklerPair[Fruit], that handles two cases of fruit,
`Apple`s and `Lemon`s:
```scala
import prickle._ 

implicit val fruitPickler = CompositePickler[Fruit].concreteType[Apple].concreteType[Lemon]

val fruit1: Fruit = new Apple(true)

val jsonString = Pickle.intoString(apple)
```

The pickle and unpickle operations can be specified together, yielding a `PicklerPair[A]`, that knows how to pickle/unpickle values of type `A`,
and all specified concrete subclasses. There are background implicit conversions in the `Pickler` and `Unpickler` that
can auto-unpack `PicklerPairs` into their two parts.

(Note also the detail that `fruit1` is declared to have super-type `Fruit`. Problems would result if this was omitted, as the val `fruit1` would then have inferred subtype `Apple`. In that case, the compiler will prefer to auto-generate a `Pickler[Apple]` via macro rather than use the `Pickler[Fruit]`.)

### Improved Type-Safety vs [scala-js-pickling](https://github.com/scala-js/scala-js-pickling)

CompositePicklers play a similar role to the PicklerRegistry used in scala-js-pickling, but are safer.
In Prickle, missing Picklers will normally result in a compile-time error, as an implicit not found. 
(The exception is unregistered concrete subclasses of a CompositePickler.) 
However, in Scala-js-pickling, the discovery of missing un/picklers occurs at runtime when un/pickling is attempted.

![Composite Picklers Vs Singleton Registry](/docs/CompositePicklersVsRegistry.png?raw=true "Composite Picklers Vs Singleton Registry")


##Picklers, Unpicklers, Macros and Formats

Like scala-pickling and scala-js-pickling, prickle uses implicit `Pickler[T]` and `Unpickler[T]` type-classes to transform values of type T. 

For case classes and case objects, these type classes will be automatically *materialized* via an implicit macro, 
if they aren't found in implicit scope. 
This is recursive, so picklers & unpicklers for each field will also be resolved and possibly materialized.

If there is a non-case class you wish to pickle that's unsupported out-of-the-box, 
you can define your own and put them in implicit scope. See `prickle.Pickler` and `prickle.Unpickler` for examples.

Prickle isn't limited to pickling to Strings - any json-like format can be used.
You will need to implement`prickle.PReader` and `prickle.PBuilder` for your format,
and pass a custom PConfig when un/pickling.

```scala
import prickle._

implicit val myConfig: PConfig[Array[Byte]] = ??? //..your defn goes here

val p = Person("Ben", "Hutchison")

val bytes: Array[Byte] = Pickle(p)

val tryPerson = Unpickle[Person].from(bytes)
```


### Unpickling yields a Try
 
It's tempting to think of Pickling and Unpickling as symmetrical, inverse operations 
 (eg `T => String`, `String => T`) but there's a difference: Unpickling is
 far more likely to fail. 
 
This stems from the nature of the operations. Pickling transforms a structured, well typed object graph into flattened, stringly-typed data. 
Unpickling takes a weakly-typed string and re-constructs the typed object-graph from it. Most arbitrary strings won't re-construct valid
object graphs, so the unpickle operation attempts to move from a higher entropy state to a lower entropy state.

Prickle acknowledges the possibility of failure by returning a Try[T] when attempting to unpickle 
([An extended talk about the philosophy guiding this design, with supa-crunch audio](https://www.youtube.com/watch?v=ujpHtodp6OQ)) 

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

##Supporting Static Reference Data

The second [AdvancedLookupExample](https://github.com/benhutchison/prickle/blob/master/example/src/main/scala/Example.scala) 
shows how prickle can be extended to handle *reference data* in the pickled
object graph. Here, *reference data* denotes pre-existing values presumed to exist the static environment on both
the pickle and unpickle sides. When an object graph refers to such values, typically it is not
desirable to pickle the actual data, but just a reference to it, and then to re-enstate the
reference on the other side by looking it up from an Id. 

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

##Limitations

* Prickle focuses on 2-way data transfer from object graph to pickled form and back. The pickled layout (ie the json) should be considered an implementation detail (issues #17 #21).
* Objects must be unpickled with the corresponding type of pickler as they were pickled with (issue #15).

##Troubleshooting

If you escape "Implicit Parameter Not Found" errors when you first use prickle on a non-trivial problem,
you're very lucky! For the rest of you, here's some tips for diagnosing such problems:

- Firstly, be patient: Prickle pushes more errors to compile-time, so you're doing the debugging early. 
- Typically, the errors result from a missing type-class for some type in your object graph. The goal therefore is to find which one and why.
- Divide and conquer: break up big chains of implicit dependencies into simpler pieces, get them working, then combine.
- Don't always take the compiler errors literally - the root cause often lies elsewhere to the sympton. Especially when implicit materialization is involved.
- You can manually invoke materialisation, to test if its working OK, eg like this 
```scala
implicit val personPickler: Pickler[Person] = Pickler.materializePickler[Person]
``` 
- This compiler option can help diagnose implicit problems (in `build.sbt` form): `scalacOptions ++= Seq("-Xlog-implicits")`
- Be aware that Picklers and Unpicklers are [invariant](http://en.wikipedia.org/wiki/Covariance_and_contravariance_(computer_science)), which can lead to puzzling errors:
```scala
import prickle._
trait Fruit
class Lemon extends Fruit
implicit val fruitPickler: Pickler[Fruit] = ???
val l = new Lemon()

//won't compile, because we don't have a Pickler of *Lemons*
//Pickle(l)

//Acribing type Fruit (up-casting) compiles OK
Pickle(l: Fruit)
```
 
##Contributors
    
Prickle is written and maintained by Ben Hutchison.

Credit & thanks for prior work to Sebastien Doeraene for scala-js-pickling, Li Haoyi for microjson

Contributors: @xeno-by, @antonkulaga, @ddispaltro, @mysticfall

YourKit is kindly supporting this open source project with its full-featured [Java Profiler](http://www.yourkit.com/java/profiler/index.jsp).
