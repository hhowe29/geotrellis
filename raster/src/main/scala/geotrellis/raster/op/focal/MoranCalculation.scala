package geotrellis.raster.op.focal

import geotrellis.raster._
import geotrellis.raster.stats.{Statistics, FastMapHistogram}

/**
 * Calculates spatial autocorrelation of cells based on the similarity to
 * neighboring values.
 *
 * The statistic for each focus in the resulting raster is such that the more
 * positive the number, the greater the similarity between the focus value and
 * it's neighboring values, and the more negative the number, the more dissimilar
 * the focus value is with it's neighboring values.
 *
 * @note                  This operation requires that the whole raster be passed in;
 *                        it does not work over tiles.
 *
 * @note                  Since mean and standard deviation are based off of an
 *                        Int based Histogram, those values will come from rounded values
 *                        of a double typed Tile (TypeFloat, TypeDouble).
 */
object TileMoransICalculation {
  def apply(tile: Tile, n: Neighborhood): FocalCalculation[Tile] with Initialization = {
    new CursorCalculation[Tile] with DoubleArrayTileResult {
      var mean = 0.0
      var `stddev^2` = 0.0

      override def init(r: Tile) = {
        super.init(r)
        val h = FastMapHistogram.fromTile(r)
        val Statistics(m, _, _, s, _, _) = h.generateStatistics
        mean = m
        `stddev^2` = s * s
      }

      def calc(r: Tile, cursor: Cursor) = {
        var z = 0.0
        var w = 0
        var base = 0.0

        cursor.allCells.foreach { (x, y) =>
          if(x == cursor.col && y == cursor.row) {
            base = r.getDouble(x, y) - mean
          } else {
            z += r.getDouble(x, y) - mean
            w += 1
          }
        }

        tile.setDouble(cursor.col, cursor.row, (base / `stddev^2` * z) / w)
      }
    }
  }
}

/**
 * Calculates global spatial autocorrelation of a raster based on the similarity to
 * neighboring values.
 *
 * The resulting statistic is such that the more positive the number, the greater
 * the similarity of values in the raster, and the more negative the number,
 * the more dissimilar the raster values are.
 *
 * @note                  This operation requires that the whole raster be passed in;
 *                        it does not work over tiles.
 *
 * @note                  Since mean and standard deviation are based off of an
 *                        Int based Histogram, those values will come from rounded values
 *                        of a double typed Tile (TypeFloat, TypeDouble).
 */
object ScalarMoransICalculation {
  def apply(tile: Tile, n: Neighborhood): FocalCalculation[Double] with Initialization = {
    new CursorCalculation[Double] with Initialization {
      var mean: Double = 0
      var `stddev^2`: Double = 0

      var count: Double = 0.0
      var ws: Int = 0

      def init(r: Tile) = {
        val h = FastMapHistogram.fromTile(r)
        val Statistics(m, _, _, s, _, _) = h.generateStatistics()
        mean = m
        `stddev^2` = s * s
      }

      def calc(r: Tile, cursor: Cursor) = {
        val base = r.getDouble(cursor.col, cursor.row) - mean
        var z = -base

        cursor.allCells.foreach { (x, y) => z += r.getDouble(x, y) - mean; ws += 1}

        count += base / `stddev^2` * z
        ws -= 1 // subtract one to account for focus
      }

      def result = count / ws
    }
  }
}
