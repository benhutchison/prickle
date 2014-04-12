package prickle

import utest._
import scala.reflect.ClassTag
import scala.util.Success


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


object PickleTests extends TestSuite{
  val tests = TestSuite {

    val apple: Fruit = Apple(2)
    val carrot: EdiblePlant = Carrot

    implicit val fruitPickle = CompositePickler[Fruit].concreteType[Apple].concreteType[Lime]
    implicit val plantPickle = CompositePickler[EdiblePlant].concreteType[Carrot.type].concreteType[Apple].concreteType[Lime]

    val lawnmowerModel = ModelNumber('V', 2000, 42, 1234567890l, -0.04f)
    val parent = Person("Keith")
    val ben = Person("Ben")
    val benDetails = PersonalDetails(ben, null, 40, false, 175.6667, parent, AnObject, lawnmowerModel)

    "caseclass"-{

      val expectedEncoding = PObject(Map(
        "#id" -> PString("1"),
        "person" -> PObject(Map(
          "#id" -> PString("2"),
          "name" -> PString("Ben")
        )),
        "starsign" -> PNull,
        "age" -> PNumber(40.0),
        "isFunny" -> PBoolean(false),
        "height" -> PNumber(175.6667),
        "ref" -> PObject(Map("#scalaObj" -> PString("prickle.AnObject"))),
        "favoriteLawnmower" -> PNull,
        "parent" -> PObject(Map(
          "#id" -> PString("3"),
          "name" -> PString("Keith")
        )),
        "favoriteLawnmower" -> PObject(Map(
          "#id" -> PString("4"),
          "series" -> PString("V"),
          "barcode" -> PObject(Map("l" -> PNumber(1442514.0), "m" -> PNumber(294.0), "h" -> PNumber(0.0))),
          "model" -> PNumber(2000.0),
          "variant" -> PNumber(42.0),
          "fuelConsumption" -> PNumber(-0.03999999910593033)))
      ))

      "encoding"-{
        val actual: PFormat = Pickle(benDetails)

        assert(expectedEncoding == actual)
      }
      "unpickling"-{
        val actual = Unpickle[PersonalDetails].from(expectedEncoding: PFormat)

        assert(Success(benDetails) == actual)
      }
      "toleratesextradata"-{
        val extra: PFormat = expectedEncoding.copy(fields = expectedEncoding.fields +
          ("foo" -> PString("bar")) +
          ("ref" -> PObject(Map("#scalaObj" -> PString("prickle.AnObject"), "foo2" -> PString("bar2")))))

        val actual = Unpickle[PersonalDetails].from(extra)
        assert(benDetails == actual.get)
      }
    }

    "compositepicklers"-{

      "apple"-{
        val pickle: PFormat = PObject(Map("#cls" -> PString("prickle.Apple"), "#val" -> PObject(Map(
          "#id" -> PString("1"), "wormCount" -> PNumber(2.0)))))

        "pickle"-{
          val applePickle = Pickle(apple)
          assert(applePickle == pickle)
           assert(Pickle(apple: EdiblePlant) == pickle)
        }
        "unpickle"-{
           assert(Success(apple) == Unpickle[Fruit].from(pickle))
           assert(Success(apple) == Unpickle[EdiblePlant].from(pickle))
        }
      }
      "carrot"-{
        val pickle: PFormat = PObject(Map("#cls" -> PString("prickle.Carrot$"), "#val" -> PObject(Map("#scalaObj" -> PString("prickle.Carrot")))))

        "pickle"-{
          assert(Pickle(carrot) == pickle)
        }
        "unpickle"-{
          assert(Success(carrot) == Unpickle[EdiblePlant].from(pickle))
        }
      }
      "null"-{
        val pickle: PFormat = PNull

        "pickle"-{
           assert(Pickle(null: EdiblePlant) == pickle)
        }
        "unpickle"-{
           assert(Success(null) == Unpickle[EdiblePlant].from(pickle))
        }
      }
    }
    "maps"-{
      val favoriteFoods = Map(ben -> apple, parent -> carrot)

      val pickle: PFormat = PArray(List(
        PArray(List(
          PObject(Map("#id" -> PString("1"), "name" -> PString("Ben"))),
          PObject(Map("#cls" -> PString("prickle.Apple"), "#val" -> PObject(Map("#id" -> PString("2"), "wormCount" -> PNumber(2.0))))))),
      PArray(List(
        PObject(Map("#id" -> PString("3"), "name" -> PString("Keith"))),
        PObject(Map("#cls" -> PString("prickle.Carrot$"), "#val" -> PObject(Map("#scalaObj" -> PString("prickle.Carrot")))))))
      ))

      "pickle"-{
        val favoritePickles = Pickle(favoriteFoods)
        assert(favoritePickles == pickle)}

      "unpickle"-{assert(Unpickle[Map[Person, EdiblePlant]].from(pickle) == Success(favoriteFoods))}
    }
  }
}