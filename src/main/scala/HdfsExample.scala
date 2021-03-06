import com.couchbase.client.java.document.JsonDocument
import com.couchbase.client.java.query.Query
import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.sources.EqualTo
import org.apache.spark.{SparkContext, SparkConf}
import com.couchbase.spark.sql._
import com.couchbase.spark._

object HdfsExample {

  def main(args: Array[String]): Unit = {

    // Configure Spark
    val cfg = new SparkConf()
      .setAppName("keyValueExample")
      .setMaster("local[*]")
      .set("com.couchbase.bucket.travel-sample", "")

    // Generate The Context
    val sc = new SparkContext(cfg)

    // Spark SQL Setup
    val sql = new SQLContext(sc)

    // Write Data into HDFS
    // ! do once to load and then comment out ... !
    /*sc.couchbaseQuery(Query.simple("SELECT `travel-sample`.* from `travel-sample` WHERE type = 'landmark'"))
      .map(_.value.toString)
      .saveAsTextFile("hdfs://127.0.0.1:9000/landmarks")*/

    // Load Data from HDFS and Join with Data in Couchbase

    // Load Landmarks from HDFS
    val landmarks = sql.jsonFile("hdfs://127.0.0.1:9000/landmarks/*")
    landmarks.registerTempTable("landmarks")

    // Load Airports from Couchbase
    val airports = sql.n1ql(filter = EqualTo("type", "airport"))

    // find all landmarks in the same city as the given FAA code
    val toFind = "SFO" // try SFO or LAX

    airports
      .join(landmarks, airports("city") === landmarks("city"))
      .select(airports("faa"), landmarks("name"), landmarks("url"))
      .where(airports("faa") === toFind and landmarks("url").isNotNull)
      .orderBy(landmarks("name").asc)
      .show(20)

  }
}
