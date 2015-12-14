package geotrellis.spark.io.accumulo

import geotrellis.raster.Tile
import geotrellis.spark.io.accumulo.spacetime.{SpaceTimeAccumuloRDDReader, SpaceTimeAccumuloRDDWriter}
import geotrellis.spark.io.index.ZCurveKeyIndexMethod
import geotrellis.spark.testfiles.TestFiles
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro.codecs._

class AccumuloSpaceTimeAlternativeSpec
  extends PersistenceSpec[SpaceTimeKey, Tile]
          with TestSparkContext
          with TestEnvironment with TestFiles
          with CoordinateSpaceTimeTests
          with LayerUpdateSpaceTimeTileTests
          with LayerReindexSpaceTimeTileTests {
  type Container = RasterRDD[SpaceTimeKey]

  override val layerId = LayerId(name, 1)
  implicit val instance = MockAccumuloInstance()

  lazy val reader = new AccumuloLayerReader[SpaceTimeKey, Tile, RasterRDD[SpaceTimeKey]] (
    AccumuloAttributeStore(instance.connector),
    new SpaceTimeAccumuloRDDReader[Tile](instance))

  lazy val writer =
    new AccumuloLayerWriter[SpaceTimeKey, Tile, RasterRDD[SpaceTimeKey]](
      attributeStore = AccumuloAttributeStore(instance.connector),
      rddWriter = new SpaceTimeAccumuloRDDWriter[Tile](instance, SocketWriteStrategy()),
      keyIndexMethod = ZCurveKeyIndexMethod.byYear,
      table = "tiles")

  lazy val updater = new AccumuloLayerUpdater[SpaceTimeKey, Tile, RasterRDD[SpaceTimeKey]] (
    AccumuloAttributeStore(instance.connector),
    new SpaceTimeAccumuloRDDWriter[Tile](instance, SocketWriteStrategy()))

  lazy val deleter = new AccumuloLayerDeleter(AccumuloAttributeStore(instance.connector), instance.connector)
  lazy val copier  = AccumuloLayerCopier[SpaceTimeKey, Tile, RasterRDD](instance, reader, writer)
  lazy val mover   = AccumuloLayerMover(AccumuloAttributeStore(instance.connector), copier, deleter)
  lazy val reindexer = AccumuloLayerReindexer[SpaceTimeKey, Tile, RasterRDD](instance, "tiles", ZCurveKeyIndexMethod.byPattern("YMM"), SocketWriteStrategy())

  lazy val tiles  = AccumuloTileReader[SpaceTimeKey, Tile](instance)
  lazy val sample = CoordinateSpaceTime
}
