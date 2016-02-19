package geotrellis.spark.io

import geotrellis.spark.render.RenderedImages
import org.apache.spark.rdd.RDD


package object s3 {
  private[s3]
  def makePath(chunks: String*) =
    chunks.filter(_.nonEmpty).mkString("/")

  implicit class S3RDDExtensions[K](rdd: RenderedImages[K]) extends SaveToS3Methods[K](rdd)
}
