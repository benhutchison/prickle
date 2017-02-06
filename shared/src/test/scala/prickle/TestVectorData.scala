package prickle

object TestVectorData {
  val EngineInstance: ModelTypes = jmeModelTypes

  trait ModelTypes {
    type Vector

    trait ConstructVector {
      def apply(): Vector

      def apply(x: Float, y: Float): Vector
    }

    implicit val Vector: ConstructVector

    def getVectorPickler: Pickler[Vector]

    implicit val vectorPickler: Pickler[Vector] = getVectorPickler

    def getVectorUnpickler: Unpickler[Vector]

    implicit val vectorUnpickler: Unpickler[Vector] = getVectorUnpickler
  }

  object jmeModelTypes extends ModelTypes {
    type Vector = Impl.Vector

    override def getVectorPickler: Pickler[Vector] = Pickler.materializePickler[Vector]

    override def getVectorUnpickler: Unpickler[Vector] = Unpickler.materializeUnpickler[Vector]

    object Vector extends ConstructVector {
      def apply() = Impl.Vector(0, 0)

      def apply(x: Float, y: Float) = Impl.Vector(x, y)
    }

  }

  object Impl {
    case class Vector(x: Float, y: Float)
  }

}
