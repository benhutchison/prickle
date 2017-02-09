package prickle

object TestVectorData {
  val impl: AbstractTypes = ImplTypes

  trait AbstractTypes {
    type Vector
    def Vector(x: Float): Vector

    implicit def getVectorPickler: Pickler[Vector]
    implicit def getVectorUnpickler: Unpickler[Vector]
  }

  object ImplTypes extends AbstractTypes {
    case class MyVector(x: Float)
    type Vector = MyVector
    override def Vector(x: Float): Vector = MyVector(x)

    override def getVectorPickler: Pickler[Vector] = Pickler.materializePickler[Vector]
    override def getVectorUnpickler: Unpickler[Vector] = Unpickler.materializeUnpickler[Vector]
  }
}
