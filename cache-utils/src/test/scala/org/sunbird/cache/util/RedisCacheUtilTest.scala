package org.sunbird.cache.util



//package org.sunbird.cache.util
//
//import org.mockito.ArgumentMatchers.{any, anyInt, eq => eqTo}
//import org.mockito.MockitoSugar
//import org.scalatest.BeforeAndAfterAll
//import org.scalatest.concurrent.ScalaFutures
//import org.scalatest.flatspec.AsyncFlatSpec
//import org.scalatest.matchers.must.Matchers
//import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
//
//import scala.concurrent.{ExecutionContext, Future}
//
//class RedisCacheUtilTest extends AsyncFlatSpec
//  with Matchers
//  with BeforeAndAfterAll
//  with MockitoSugar
//  with ScalaFutures {
//
//    var cacheUtil: RedisCacheUtil = _
//
//    override def beforeAll(): Unit = {
//        cacheUtil = mock[RedisCacheUtil]
//        super.beforeAll()
//    }
//
//    "get with valid key" should "return string data for given key" in {
//        when(cacheUtil.get(eqTo("kptest-103"), any[Function1[String, String]](), anyInt())).thenReturn("kptest-value-03")
//        val result = cacheUtil.get("kptest-103", _ => "default", 0)
//        result shouldEqual "kptest-value-03"
//    }
//
//
//    "getAsync with key not having data in cache" should "return Future[String] from handler" in {
//        when(cacheUtil.getAsync(
//            eqTo("kptest-113"),
//            any[Function1[String, Future[String]]](),
//            anyInt()
//        )(any[ExecutionContext]()))
//          .thenReturn(Future.successful("sample-data-handler"))
//
//        val future = cacheUtil.getAsync("kptest-113", _ => Future("sample-data-handler"), 2)(ExecutionContext.global)
//        future.map { result =>
//            result shouldEqual "sample-data-handler"
//        }
//    }
//
//
//    "getAsync with key having data in cache" should "return Future[String] from cache" in {
//        when(cacheUtil.getAsync(
//            eqTo("kptest-114"),
//            any[Function1[String, Future[String]]](),
//            anyInt()
//        )(any[ExecutionContext]()))
//          .thenReturn(Future.successful("sample-cache-data"))
//
//        val future = cacheUtil.getAsync("kptest-114", _ => Future("sample-data-handler"), 2)(ExecutionContext.global)
//        future.map { result =>
//            result shouldEqual "sample-cache-data"
//        }
//    }
//
//
//    "getListAsync with key not having data in cache" should "return Future[List[String]] from handler" in {
//        val handlerData = List("sample-handler-data1", "sample-handler-data2")
//
//        when(cacheUtil.getListAsync(
//            eqTo("kptest-115"),
//            any[Function1[String, Future[List[String]]]](),
//            anyInt()
//        )(any[ExecutionContext]()))
//          .thenReturn(Future.successful(handlerData))
//
//        val future = cacheUtil.getListAsync("kptest-115", _ => Future(handlerData), 2)(ExecutionContext.global)
//        future.map { result =>
//            result should contain theSameElementsAs handlerData
//        }
//    }
//
//
//    "getListAsync with key having data in cache" should "return Future[List[String]] from cache" in {
//        val cacheData = List("sample-cache-data1", "sample-cache-data2")
//
//        when(cacheUtil.getListAsync(
//            eqTo("kptest-116"),
//            any[Function1[String, Future[List[String]]]](),
//            anyInt()
//        )(any[ExecutionContext]()))
//          .thenReturn(Future.successful(cacheData))
//
//        val future = cacheUtil.getListAsync("kptest-116", _ => Future(List("sample-handler-data1")), 2)(ExecutionContext.global)
//        future.map { result =>
//            result should contain theSameElementsAs cacheData
//        }
//    }
//
//    "incrementAndGet with valid key" should "return incremented value" in {
//        when(cacheUtil.incrementAndGet(eqTo("key-01"))).thenReturn(2.0)
//        val result = cacheUtil.incrementAndGet("key-01")
//        result shouldEqual 2.0
//    }
//
//    "saveList with valid key" should "save list without error" in {
//        noException should be thrownBy cacheUtil.saveList("list-key-01", List("v1", "v2"), 10)
//    }
//
//    "addToList with valid key" should "append list without error" in {
//        noException should be thrownBy cacheUtil.addToList("list-key-02", List("v1", "v2"))
//    }
//
//    "getList with default handler" should "return list without error" in {
//        when(cacheUtil.getList(eqTo("list-key-03"), any[Function1[String, List[String]]](), anyInt(), anyInt()))
//          .thenReturn(List("v1"))
//        val result = cacheUtil.getList("list-key-03", _ => List("v1"), 0, 0)
//        result should contain("v1")
//    }
//
//    "removeFromList with values" should "not throw exception" in {
//        noException should be thrownBy cacheUtil.removeFromList("list-key-04", List("v1"))
//    }
//
//    "delete keys" should "not throw exception" in {
//        noException should be thrownBy cacheUtil.delete("key-01", "key-02")
//    }
//
//    "deleteByPattern with specific pattern" should "not throw exception" in {
//        noException should be thrownBy cacheUtil.deleteByPattern("abc*")
//    }
//
//    "checkConnection should" should "return boolean" in {
//        when(cacheUtil.checkConnection).thenReturn(true)
//        cacheUtil.checkConnection shouldBe true
//    }
//
//    "resetConnection should" should "not throw exception" in {
//        noException should be thrownBy cacheUtil.resetConnection()
//    }
//
//    "closePool should" should "not throw exception" in {
//        noException should be thrownBy cacheUtil.closePool()
//    }
//
//    "getAllKeys should" should "return set of keys" in {
//        when(cacheUtil.getAllKeys()).thenReturn(new java.util.HashSet[String]())
//        cacheUtil.getAllKeys().size() shouldEqual 0
//    }
//}
