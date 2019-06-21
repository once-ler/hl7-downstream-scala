import org.scalatest.{FunSpec, Matchers}
import com.redis.RedisClient
import org.scalatest.concurrent.ScalaFutures

import com.eztier.datasource.redis.session.implicits.Transactors._

class TestRedisSpec extends FunSpec with ScalaFutures with Matchers {
  describe("Redis Suite") {
    val client: RedisClient = implicitly[RedisClient]
    val setname = "hl7:patient:last:time"

    it("Should echo back") {
      client.echo("Foo Bar")
        .futureValue should equal ("Foo Bar")
    }

    it("Should add records") {
      client.zadd(setname, 10, "patient:a")
      client.zadd(setname, 22, "patient:b")
      client.zadd(setname, 12.5, "patient:c")
      client.zadd(setname, 14, "patient:d")
    }

    // zrangebyscore hl7:patient:last:time 1559922666000 +inf withscores (include start, use "(" to exclude)
    it("Should fetch records within range") {

    }

  }
}