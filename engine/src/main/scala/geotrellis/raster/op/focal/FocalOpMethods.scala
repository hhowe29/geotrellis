/*
 * Copyright (c) 2014 Azavea.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.raster.op.focal

import geotrellis.raster._
import geotrellis.engine._

trait FocalOpMethods[+Repr <: RasterSource] { self: Repr =>
  def zipWithNeighbors: Op[Seq[(Op[Tile], TileNeighbors)]] =
    (self.tiles, self.rasterDefinition).map { (seq, rd) =>
      val re = rd.rasterExtent
      val tileLayout = rd.tileLayout

      val colMax = tileLayout.tileCols - 1
      val rowMax = tileLayout.tileRows - 1

      def getTile(tileCol: Int, tileRow: Int): Option[Op[Tile]] =
        if(0 <= tileCol && tileCol <= colMax &&
          0 <= tileRow && tileRow <= rowMax) {
          Some(seq(tileRow * (colMax + 1) + tileCol))
        } else { None }

      seq.zipWithIndex.map { case (tile, i) =>
        val col = i % (colMax + 1)
        val row = i / (colMax + 1)

        // get tileCols, tileRows, & list of relative neighbor coordinate tuples
        val tileSeq = Seq(
          /* North */
          getTile(col, row - 1),
          /* NorthEast */
          getTile(col + 1, row - 1),
          /* East */
          getTile(col + 1, row),
          /* SouthEast */
          getTile(col + 1, row + 1),
          /* South */
          getTile(col, row + 1),
          /* SouthWest */
          getTile(col - 1, row + 1),
          /* West */
          getTile(col - 1, row),
          /* NorthWest */
          getTile(col - 1, row - 1)
        )

        (tile, SeqTileNeighbors(tileSeq))
      }
    }

  def focal[T, That](n: Neighborhood)
                   (op: (Op[Tile], Op[Neighborhood], TileNeighbors) => FocalOperation[Tile]): RasterSource = {
    val tileOps = 
      zipWithNeighbors.map(_.map { case (t, ns) => op(t, n, ns) })

    new RasterSource(rasterDefinition, tileOps)
  }

  def focalSum(n: Neighborhood) = focal(n)(Sum(_, _, _))

  def focalMin(n: Neighborhood): RasterSource = focal(n)(Min(_, _, _))
  def focalMax(n: Neighborhood): RasterSource = focal(n)(Max(_, _, _))
  def focalMean(n: Neighborhood): RasterSource = focal(n)(Mean(_, _, _))
  def focalMedian(n: Neighborhood): RasterSource = focal(n)(Median(_, _, _))
  def focalMode(n: Neighborhood): RasterSource = focal(n)(Mode(_, _, _))
  def focalStandardDeviation(n: Neighborhood): RasterSource = focal(n)(StandardDeviation(_, _, _))

  def focalAspect: RasterSource =
    focal(Square(1)) { (r, _, nbs) => 
      Aspect(r, nbs, rasterDefinition.map(_.rasterExtent.cellSize))
    }

  def focalSlope: RasterSource =
    focal(Square(1)) { (r, _, nbs) => 
      Slope(r, nbs, rasterDefinition.map(_.rasterExtent.cellSize))
    }

  def focalHillshade: RasterSource =
    focal(Square(1)) { (r, _, nbs) =>
      Hillshade(r, nbs, rasterDefinition.map(_.rasterExtent.cellSize))
    }

  def focalHillshade(azimuth: Double, altitude: Double, zFactor: Double): RasterSource =
    focal(Square(1)) { (r, _, nbs) => 
      Hillshade(r, nbs, rasterDefinition.map(_.rasterExtent.cellSize), azimuth, altitude, zFactor)
    }

  def focalMoransI(n: Neighborhood): RasterSource =
    self.globalOp(TileMoransI(_, n))

  def focalScalarMoransI(n: Neighborhood): ValueSource[Double] =
    self.converge.mapOp(ScalarMoransI(_, n))
}
