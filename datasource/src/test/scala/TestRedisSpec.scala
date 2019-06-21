import java.time.{LocalDateTime, ZoneId}

import akka.util.Timeout
import org.scalatest.{FunSpec, Matchers}
import com.redis.RedisClient
import org.scalatest.concurrent.ScalaFutures
import com.eztier.datasource.redis.session.implicits.Transactors._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class TestRedisSpec extends FunSpec with ScalaFutures with Matchers {

  implicit class WrapLocalDateTime(dt: LocalDateTime) {
    val timeToEpochMilli = dt.atZone(ZoneId.of("America/New_York")).toInstant.getEpochSecond * 1000.0
  }

  describe("Redis Suite") {
    implicit val timeout = Timeout(5 seconds)
    val client: RedisClient = implicitly[RedisClient]
    val setname = "hl7:patient:last:time"
    val pointInTime = LocalDateTime.of(2015, 5, 13, 13, 25, 34)

    val snapShotTimes = (2 to 5).map(a => (a,  pointInTime.plusHours(a).timeToEpochMilli)).toMap


    it("Should echo back") {
      client.echo("Foo Bar")
        .futureValue should equal ("Foo Bar")
    }

    it("Should add records") {
      import com.eztier.datasource.common.implicits.ExecutionContext._
      implicit val timeout = Timeout(5 seconds)

      val f = Future.sequence(Seq(
        client.zadd(setname, snapShotTimes(5), "patient:d"),
        client.zadd(setname, snapShotTimes(4), "patient:c"),
        client.zadd(setname, snapShotTimes(3), "patient:b"),
        client.zadd(setname, snapShotTimes(2), "patient:a")
      ))

      whenReady(f) {
        a => a.sum should be (4)
      }

    }

    // zrangebyscore hl7:patient:last:time 1559922666000 +inf withscores (include start, use "(" to exclude)
    it("Should fetch records within > min and <= max") {
      /*
      val f = client
        .zrangeByScoreWithScores(setname, snapShotTimes(3), false, snapShotTimes(4), true, None)

      val r = Await.result(f, 5 seconds)
      */
      
      client
        .zrangeByScoreWithScores(setname, snapShotTimes(3) - 1000, false, snapShotTimes(4), true, None)
        .futureValue should equal (List(
          ("patient:b", snapShotTimes(3)),
          ("patient:c", snapShotTimes(4))
        ))

      client
        .zrangeByScoreWithScores(setname, snapShotTimes(3), false, snapShotTimes(4), true, None)
        .futureValue should equal (List(
        ("patient:c", snapShotTimes(4))
      ))

    }

  }
}