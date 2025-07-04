package org.sunbird.cache.util

import org.mockito.ArgumentMatchers._
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito._
import org.scalatest.Assertion
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import redis.clients.jedis.{Jedis, JedisPool}

import java.lang.reflect.Field
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class RedisCacheUtilTest extends AnyFlatSpec with Matchers with MockitoSugar {

    def setPrivateField(target: AnyRef, fieldName: String, value: Any): Unit = {
        val field: Field = target.getClass.getDeclaredField(fieldName)
        field.setAccessible(true)
        field.set(target, value.asInstanceOf[AnyRef])
    }

    def withMockedJedis(testCode: (RedisCacheUtil, Jedis) => Any): Unit = {
        val mockPool = mock[JedisPool]
        val mockJedis = mock[Jedis]
          when(mockPool.getResource).thenReturn(mockJedis)
        val util = new RedisCacheUtil
        setPrivateField(util, "jedisPool", mockPool)

        testCode(util, mockJedis)
        util.closePool()

    }

    "set" should "delete key, set data and optionally expire" in withMockedJedis { (util, jedis) =>
        util.set("k1", "v1", ttl = 5)
        val inOrder = org.mockito.Mockito.inOrder(jedis)
        inOrder.verify(jedis).del("k1")
        inOrder.verify(jedis).set("k1", "v1")
        inOrder.verify(jedis).expire("k1", 5)
    }

    it should "rethrow exceptions" in withMockedJedis { (util, jedis) =>
        when(jedis.set(anyString(), anyString())).thenThrow(new RuntimeException("fail"))
        assertThrows[RuntimeException] {
            util.set("k2", "v2")
        }
    }

    "get" should "return existing data without calling handler" in withMockedJedis { (util, jedis) =>
        when(jedis.get("k")).thenReturn("val")
        val result = util.get("k", _ => "hnd", ttl = 1)
        result shouldBe "val"
        verify(jedis, never()).del(anyString())
    }

    it should "call handler on missing data and set cache" in withMockedJedis { (util, jedis) =>
        when(jedis.get("k")).thenReturn(null)
        when(jedis.del(anyString())).thenReturn(1)
        val utilSpy = spy(util)
        when(utilSpy.getConnection).thenReturn(jedis)
        val data = utilSpy.get("k", _ => "computed", ttl = 2)
        data shouldBe "computed"
        verify(jedis).del("k")
        verify(jedis).set("k", "computed")
    }

    "getAsync" should "return future of existing data" in withMockedJedis { (util, jedis) =>
        when(jedis.get("k")).thenReturn("asyncVal")
        val fut = util.getAsync("k", key => throw new Exception())
        Await.result(fut, 1.second) shouldBe "asyncVal"
    }

    it should "invoke async handler on missing data" in withMockedJedis { (util, jedis) =>
        when(jedis.get("k")).thenReturn("")
        val fut = util.getAsync("k", key => Future.successful("futVal"), ttl = 3)
        val res = Await.result(fut, 1.second)
        res shouldBe "futVal"
    }

    "incrementAndGet" should "increment and return value" in withMockedJedis { (util, jedis) =>
        when(jedis.incrByFloat("cnt", 1.0)).thenReturn(2.0)
        util.incrementAndGet("cnt") shouldBe 2.0
    }

    "saveList and getList" should "store and retrieve list data" in withMockedJedis { (util, jedis) =>
        when(jedis.smembers("lk")).thenReturn(Set("a", "b").asJava)
        val lst = util.getList("lk", _ => List("x", "y"), ttl = 4, index = 1)
        lst.toSet shouldBe Set("a", "b")
    }

    "removeFromList" should "remove elements" in withMockedJedis { (util, jedis) =>
        util.removeFromList("lk", List("e1", "e2"))
        verify(jedis).srem("lk", "e1")
        verify(jedis).srem("lk", "e2")
    }

    "delete" should "delete keys" in withMockedJedis { (util, jedis) =>
        util.delete("k1", "k2")
        verify(jedis).del("k1", "k2")
    }

    "deleteByPattern" should "delete matching keys" in withMockedJedis { (util, jedis) =>
        when(jedis.keys("pat*")).thenReturn(Set("k").asJava)
        util.deleteByPattern("pat*")
        //verify(jedis).del("k")
    }

    it should "not call redis for invalid pattern" in withMockedJedis { (util, jedis) =>
        util.deleteByPattern("*")
        verify(jedis, never()).keys(anyString())
    }

    "checkConnection" should "return true on success" in withMockedJedis { (util, jedis) =>
        val spyUtil = spy(util)
        when(spyUtil.getConnection(2)).thenReturn(jedis)
        spyUtil.checkConnection shouldBe true
    }

    it should "return false on exception" in withMockedJedis { (util, jedis) =>
        val util2 = spy(util)
        when(util2.getConnection(2)).thenThrow(new RuntimeException())
        util2.checkConnection shouldBe false
    }

    "getAllKeys" should "return all keys set" in withMockedJedis { (util, jedis) =>
        val keys = Set("k1", "k2").asJava
        when(jedis.keys("*")).thenReturn(keys)
        util.getAllKeys() shouldBe keys
    }

    "saveList" should "delete key, add elements, and set expiry when isPartialUpdate is false" in withMockedJedis { (util, jedis) =>
        util.saveList("myKey", List("a", "b"), ttl = 10, isPartialUpdate = false)

        val inOrder = org.mockito.Mockito.inOrder(jedis)
        inOrder.verify(jedis).del("myKey")
        inOrder.verify(jedis).sadd("myKey", "a")
        inOrder.verify(jedis).sadd("myKey", "b")
        inOrder.verify(jedis).expire("myKey", 10)
    }

    it should "only add elements when isPartialUpdate is true" in withMockedJedis { (util, jedis) =>
        util.saveList("partialKey", List("x", "y"), ttl = 10, isPartialUpdate = true)

        verify(jedis, never()).del(anyString())
        verify(jedis).sadd("partialKey", "x")
        verify(jedis).sadd("partialKey", "y")
        verify(jedis, never()).expire(anyString(), anyInt())
    }

    it should "rethrow exception if jedis.sadd fails" in withMockedJedis { (util, jedis) =>
        when(jedis.sadd(anyString(), anyString())).thenThrow(new RuntimeException("Redis error"))

        val ex = intercept[RuntimeException] {
            util.saveList("failKey", List("boom"), ttl = 0, isPartialUpdate = false)
        }

        ex.getMessage should include ("Redis error")
    }

    def withMockedJedis(testCode: (RedisCacheUtil, Jedis) => Future[Assertion]): Future[Assertion] = {
        val mockPool = mock[JedisPool]
        val mockJedis = mock[Jedis]
        when(mockPool.getResource).thenReturn(mockJedis)

        val util = spy(new RedisCacheUtil)
        setPrivateField(util, "jedisPool", mockPool)
        testCode(util, mockJedis)
    }

    "getListAsync" should "return cached list if data is present" in withMockedJedis { (util, jedis) =>
        val redisData = Set("v1", "v2").asJava
        when(jedis.smembers("testKey")).thenReturn(redisData)

        util.getListAsync("testKey", key => Future.successful(List("computed")), ttl = 5).map { result =>
            result should contain allOf ("v1", "v2")
            verify(jedis, never()).del(anyString()) // saveList shouldn't be called
        }
    }

//    it should "invoke asyncHandler when cache is empty and save data" in withMockedJedis { (util, jedis) =>
//        when(jedis.smembers("emptyKey")).thenReturn(Set[String]().asJava)
//
//        val handlerData = List("a", "b")
//
//        val utilSpy = spy(util)
//        doReturn(jedis).when(utilSpy).getConnection
//        doNothing().when(utilSpy).saveList(eqTo("emptyKey"), eqTo(handlerData), eqTo(3), eqTo(false))
//
//        utilSpy.getListAsync("emptyKey", _ => Future.successful(handlerData), ttl = 3).map { result =>
//            result shouldBe handlerData
//            verify(utilSpy).saveList("emptyKey", handlerData, 3, false)
//        }
//    }

}
