package prickle

case class Person(name: String)
case class PersonalDetails(person: Person, starsign: String, age: Int, isFunny: Boolean, height: Double, parent: Person, ref: AnObject.type, favoriteLawnmower: ModelNumber)
case class ModelNumber(series: Char, model: Short, variant: Byte, barcode: Long, fuelConsumption: Float)
case object AnObject

trait EdiblePlant
abstract class Fruit extends EdiblePlant
trait Vegetable extends EdiblePlant
case object Carrot extends Vegetable
case class Apple(wormCount: Int) extends Fruit
case class Lime(isSour: Boolean) extends Fruit

abstract class TestData[P](builder: PBuilder[P]) {

  import builder._

  val apple: Fruit = Apple(2)
  val carrot: EdiblePlant = Carrot

  implicit val fruitPickler = CompositePickler[Fruit].concreteType[Apple].concreteType[Lime]
  implicit val plantPickler = CompositePickler[EdiblePlant].concreteType[Carrot.type].concreteType[Apple].concreteType[Lime]

  val lawnmowerModel = ModelNumber('V', 2000, 42, 1234567890l, -0.04f)
  val parent = Person("Keith")
  val ben = Person("Ben")
  val benDetails = PersonalDetails(ben, null, 40, false, 175.6667, parent, AnObject, lawnmowerModel)


  val expectedBenDetailsPickle: P = makeObjectFrom(
    "#id" -> makeString("1"),
    "person" -> makeObjectFrom(
      "#id" -> makeString("2"),
      "name" -> makeString("Ben")
    ),
    "starsign" -> makeNull,
    "age" -> makeNumber(40.0),
    "isFunny" -> makeBoolean(false),
    "height" -> makeNumber(175.6667),
    "ref" -> makeObjectFrom("#scalaObj" -> makeString("prickle.AnObject")),
    "parent" -> makeObjectFrom(
      "#id" -> makeString("3"),
      "name" -> makeString("Keith")
    ),
    "favoriteLawnmower" -> makeObjectFrom(
      "#id" -> makeString("4"),
      "series" -> makeString("V"),
      "barcode" -> makeObjectFrom("l" -> makeNumber(1442514.0), "m" -> makeNumber(294.0), "h" -> makeNumber(0.0)),
      "model" -> makeNumber(2000.0),
      "variant" -> makeNumber(42.0),
      "fuelConsumption" -> makeNumber(-0.03999999910593033))
  )

  val appleCls: P = makeString("prickle.Apple")
  val carrotCls: P = makeString("prickle.Carrot$")
  val carrotObj: P = makeString("prickle.Carrot")

  val expectedApplePickle: P = makeObjectFrom("#cls" -> appleCls, "#val" -> makeObjectFrom(
    "#id" -> makeString("1"), "wormCount" -> makeNumber(2.0)))
  val expectedCarrotPickle: P = makeObjectFrom("#cls" -> carrotCls, "#val" -> makeObjectFrom("#scalaObj" -> carrotObj))

  val expectedMapPickle: P = makeArray(
    makeArray(
      makeObjectFrom("#id" -> makeString("1"), "name" -> makeString("Ben")),
      makeObjectFrom("#cls" -> appleCls, "#val" -> makeObjectFrom("#id" -> makeString("2"), "wormCount" -> makeNumber(2.0)))),
    makeArray(
      makeObjectFrom("#id" -> makeString("3"), "name" -> makeString("Keith")),
      makeObjectFrom("#cls" -> carrotCls, "#val" -> makeObjectFrom("#scalaObj" -> carrotObj)))
  )


  def expectedBenDetailsAcyclicPickle: P = stripIdTags(expectedBenDetailsPickle)

  def stripIdTags(pickle: P): P
//  = pickle match {
//    case PObject(fields) => PObject(fields - "#id")
//    case PArray(elems) => PArray(elems.map(stripIdTags))
//    case _ => pickle
//  }

  def addField(pickle: P, field: (String, P)): P

  def areEqual(p1: P, p2: P): Boolean

  val brotherDetails = PersonalDetails(Person("Oliver"), null, 37, false, 175.6667, parent, AnObject, lawnmowerModel)
  val brothers = (benDetails, brotherDetails)
}
