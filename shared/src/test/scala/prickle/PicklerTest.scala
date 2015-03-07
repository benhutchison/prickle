package prickle

import java.util.Date

import utest._
import scala.collection.immutable.SortedMap
import scala.util.Success


import microjson._


object PickleTests extends TestSuite {
  val testData = new TestData()
  import testData._

  def tests = TestSuite {
    "with cyclic config"- {
      implicit val config = JsConfig()
      import config._

      "caseclass" - {
        "encoding" - {
          val actual = Pickle(benDetails)
          val expected = expectedBenDetailsPickle
          assert(expected == actual)
        }
        "unpickling" - {
          val actual = Unpickle[PersonalDetails].from(expectedBenDetailsPickle)
          assert(Success(benDetails) == actual)
        }
        "toleratesextradata" - {
          val extra: JsValue = new JsObject(
            expectedBenDetailsPickle.asInstanceOf[JsObject].value + ("foo" -> config.makeString("bar")))

          val actual = Unpickle[PersonalDetails].from(extra).get
          assert(benDetails == actual)
        }
      }
      "generic" - {
        val initial: (Person, Person) = (ben, parent)
        val p = Pickle(initial)
        val unpickled = Unpickle[(Person, Person)].from(p).get
        assert(initial == unpickled)
      }
      "compositepicklers" - {
        "apple" - {
          "pickle" - {
            val applePickle = Pickle(apple)
            assert(expectedApplePickle == applePickle)
            val plantPickle = Pickle(apple: EdiblePlant)
            assert(plantPickle == applePickle)
          }
          "unpickle" - {
            val unpickleFruit = Unpickle[Fruit].from(expectedApplePickle).get
            assert(apple == unpickleFruit)
            val unpicklePlant = Unpickle[EdiblePlant].from(expectedApplePickle).get
            assert(apple == unpicklePlant)
          }
        }
        "carrot" - {
          "pickle" - {
            val carrotPickle = Pickle(carrot)
            assert(carrotPickle == expectedCarrotPickle)
          }
          "unpickle" - {
            val unpickle = Unpickle[EdiblePlant].from(expectedCarrotPickle).get
            assert(carrot == unpickle)
          }
        }
        "null" - {
          val pickle: JsValue = config.makeNull

          "pickle" - {
            assert(Pickle(null: EdiblePlant) == pickle)
          }
          "unpickle" - {
            val unpickle = Unpickle[EdiblePlant].from(pickle).get
            assert(null == unpickle)
          }
        }
      }
      "maps" - {
        val favoriteFoods = Map(ben -> apple, parent -> carrot)
        val unpickle = Unpickle[Map[Person, EdiblePlant]].from(Pickle(favoriteFoods)).get
        assert(unpickle == favoriteFoods)
      }
      "sorted maps" - {
        val map = SortedMap[Int, String](1 -> "Sydney", 2 -> "Inverloch")
        assert(Unpickle[SortedMap[Int, String]].fromString(Pickle.intoString(map)).get == map)
      }
      "dates" - {
        val date = new Date()
        assert(Unpickle[Date].fromString(Pickle.intoString(date)).get == date)
      }
      "seqs" - {
        val elem = Seq("One")
        val seq = Seq(elem, elem)
        val unpickle = Unpickle[Seq[Seq[String]]].from(Pickle(seq)).get
        Predef.assert(unpickle == seq)
        "shared object support" - {
          val e1 = unpickle(0)
          val e2 = unpickle(1)
          assert(e1 eq e2)
        }
      }
      "immutable seq" - {
        val it = collection.immutable.Seq(1, 2, 3)
        val unpickle = Unpickle[collection.immutable.Seq[Int]].from(Pickle(it)).get
        Predef.assert(unpickle == it)
      }
      "iterable" - {
        val it = Iterable(1, 2, 3)
        val unpickle = Unpickle[Iterable[Int]].from(Pickle(it)).get
        Predef.assert(unpickle == it)
      }

      "lists" - {
        val list = List(1, 2)
        val unpickle = Unpickle[List[Int]].from(Pickle(list)).get
        assert(unpickle == list)
        "shared object support" - {
          val list = Unpickle[List[List[Int]]].from(Pickle(List(List(1), List(1)))).get
          val e1 = list(0)
          val e2 = list(1)
          assert(e1 eq e2)
        }
      }

      "sets" - {
        val set = Set(1, 2)
        val unpickle = Unpickle[Set[Int]].from(Pickle(set)).get
        assert(unpickle == set)
        "shared object support" - {
          val seq = Unpickle[Seq[Set[Int]]].from(Pickle(Seq(Set(1), Set(1)))).get
          val e1 = seq(0)
          val e2 = seq(1)
          assert(e1 eq e2)
        }
      }
      "tuple shared object support" - {
        assert(brothers._1.parent eq brothers._2.parent)

        val pickle = Pickle(brothers)
        val afterPickling = Unpickle[(PersonalDetails, PersonalDetails)].from(pickle).get

        val p1 = afterPickling._1.parent
        val p2 = afterPickling._2.parent
        assert(p1 eq p2)
      }
      "case_class_with_doubly_nested_parametric_field"-{
        Unpickle[DoublyNested].from(Pickle(DoublyNested(None)))
      }
//Currently, this test cannot be easily written in a Jvm/JS-portable way
//due to differences in how Numbers are rendered into json strings
//      "intoString"-{
//        val s = Pickle.intoString(benDetails)
//        assert(expectedBenDetailsString == s)
//      }
      "fromString"-{
        val actual = Unpickle[PersonalDetails].fromString(expectedBenDetailsString)
        assert(Success(benDetails) == actual)
      }
  }
    "with non-shared config"-{
      implicit val acyclicConfig = JsConfig(areSharedObjectsSupported = false)

      "over shared structure"-{

        assert(brothers._1.parent eq brothers._2.parent)

        val pickleState = PickleState()
        val pickle = Pickle(brothers, pickleState)

        val unpickleState = collection.mutable.Map.empty[String, Any]
        val afterPickling = Unpickle[(PersonalDetails, PersonalDetails)].from(pickle, unpickleState).get

        val p1 = afterPickling._1.parent
        val p2 = afterPickling._2.parent

        "shared structures are duplicated"-{assert(!(p1 eq p2))}

        "data is preserved"-{
          assert(afterPickling._1 == benDetails)
          assert(afterPickling._2 == brotherDetails)
        }

        "no id tags added to pickle"-{assert(pickle == stripIdTags(pickle))}

        "no state during pickle"-{assert(pickleState.refs.isEmpty)}
        "no state during unpickle"-{assert(unpickleState.isEmpty)}
      }
    }
  }
}

case class DoublyNested(field:  Option[(Fruit, Int)])