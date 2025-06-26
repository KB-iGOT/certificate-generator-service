package org.sunbird.cache.platform;

import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.mockito.MockitoSugar
import com.typesafe.config.{Config, ConfigFactory}


class PlatformTest extends AnyFunSuite with MockitoSugar {

    test("getString returns value when key exists") {
        val value = "hello"
        val key = "test.string"
        val config = ConfigFactory.parseString(s"$key = $value")
        val result = config.getString(key)
        assert(result == value)
    }

    test("getString returns default when key does not exist") {
        val key = "missing.string"
        val default = "default"
        assert(Platform.getString(key, default) == default)
    }

    test("getInteger returns value when key exists") {
        val key = "test.int"
        val value = 42
        val config = ConfigFactory.parseString(s"$key = $value")
        assert(config.getInt(key) == value)
    }

    test("getInteger returns default when key does not exist") {
        val key = "missing.int"
        val default = 99
        assert(Platform.getInteger(key, default) == default)
    }

    test("getBoolean returns value when key exists") {
        val key = "test.bool"
        val config = ConfigFactory.parseString(s"$key = true")
        assert(config.getBoolean(key))
    }

    test("getBoolean returns default when key does not exist") {
        val key = "missing.bool"
        val default = false
        assert(Platform.getBoolean(key, default) == default)
    }

    test("getStringList returns value when key exists") {
        val key = "test.list"
        val config = ConfigFactory.parseString(s"""$key = ["a", "b", "c"]""")
        val list = config.getStringList(key)
        assert(list.size() == 3)
        assert(list.get(0) == "a")
    }

    test("getStringList returns default when key does not exist") {
        val key = "missing.list"
        val default = java.util.Arrays.asList("x", "y")
        assert(Platform.getStringList(key, default) == default)
    }

    test("getLong returns value when key exists") {
        val key = "test.long"
        val value = 123456789L
        val config = ConfigFactory.parseString(s"$key = $value")
        assert(config.getLong(key) == value)
    }

    test("getLong returns default when key does not exist") {
        val key = "missing.long"
        val default = 987654321L
        assert(Platform.getLong(key, default) == default)
    }

    test("getDouble returns value when key exists") {
        val key = "test.double"
        val value = 3.14
        val config = ConfigFactory.parseString(s"$key = $value")
        assert(config.getDouble(key) == value)
    }

    test("getDouble returns default when key does not exist") {
        val key = "missing.double"
        val default = 2.71
        assert(Platform.getDouble(key, default) == default)
    }
}
