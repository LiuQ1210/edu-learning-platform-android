package com.github.learningplatform.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.net.UnknownHostException

/**
 * [CacheStore] 的单元测试。
 *
 * 为什么用假 DAO 而不是 Room 的 in-memory 数据库：
 * 那需要 Robolectric，而它依赖一个 100MB+ 的 nativeruntime 包，本机网络拉不下来。
 * 缓存的核心逻辑（TTL 判定、回源、旧数据兜底、坏数据清理）**全在 CacheStore 里**，
 * 与 SQL 实现无关，所以用内存 Map 实现的假 DAO 就能覆盖。
 *
 * 真正依赖 SQL 语义的部分（比如 `LIKE` 前缀匹配是否如预期）不在这里覆盖，
 * 那属于 instrumented test 的范围。
 */
class CacheStoreTest {

    @Serializable
    data class Item(val id: Long = 0, val title: String = "")

    @Serializable
    data class Payload(val total: Long = 0, val items: List<Item> = emptyList())

    /** 内存实现的假 DAO，只实现 [CacheStore] 用到的方法 */
    private class FakeCacheDao : CacheDao {
        private val map = linkedMapOf<String, CacheEntry>()

        override suspend fun get(key: String): String? = map[key]?.value

        override fun observe(key: String): Flow<CacheEntry?> =
            MutableStateFlow(map[key]).map { it }

        override suspend fun getEntry(key: String): CacheEntry? = map[key]

        override suspend fun removeByPrefix(prefix: String) {
            map.keys.filter { it.startsWith(prefix) }.forEach { map.remove(it) }
        }

        override suspend fun put(entry: CacheEntry) {
            map[entry.key] = entry
        }

        override suspend fun remove(key: String) {
            map.remove(key)
        }

        override suspend fun removeOlderThan(before: Long) {
            map.entries.filter { it.value.updatedAt < before }.forEach { map.remove(it.key) }
        }

        override suspend fun clear() = map.clear()
    }

    private lateinit var dao: FakeCacheDao
    private lateinit var store: CacheStore

    @Before
    fun setUp() {
        dao = FakeCacheDao()
        store = CacheStore(dao)
    }

    /** 把某个键的写入时间往前拨，用来模拟「已过期」而不必真的 sleep */
    private suspend fun ageKey(key: String, deltaMillis: Long) {
        val e = dao.getEntry(key)!!
        dao.put(e.copy(updatedAt = e.updatedAt - deltaMillis))
    }

    // ---------------------------------------------------------------- 基本读写

    @Test
    fun `写入列表后能读回`() = runTest {
        store.putList("k", listOf(Item(1, "a"), Item(2, "b")))
        val got = store.getList<Item>("k", ttlMillis = 60_000)
        assertEquals(2, got?.size)
        assertEquals("a", got?.first()?.title)
    }

    @Test
    fun `写入对象后能读回`() = runTest {
        store.putObject("p", Payload(total = 7, items = listOf(Item(1, "x"))))
        val got = store.getObject<Payload>("p", ttlMillis = 60_000)
        assertEquals(7L, got?.total)
        assertEquals(1, got?.items?.size)
    }

    @Test
    fun `读不存在的键返回 null`() = runTest {
        assertNull(store.getList<Item>("nope", ttlMillis = 60_000))
        assertNull(store.getObject<Payload>("nope", ttlMillis = 60_000))
    }

    // ---------------------------------------------------------------- TTL

    @Test
    fun `未过期时命中缓存`() = runTest {
        store.putList("k", listOf(Item(1, "fresh")))
        assertNotNull(store.getList<Item>("k", ttlMillis = 60_000))
    }

    @Test
    fun `ttl 为 0 表示不判过期`() = runTest {
        store.putList("k", listOf(Item(1, "x")))
        ageKey("k", 100 * 24 * 60 * 60 * 1000L) // 拨老 100 天
        assertNotNull("传 0 应永不判过期", store.getList<Item>("k", ttlMillis = 0))
    }

    @Test
    fun `超过 ttl 后视为过期`() = runTest {
        store.putList("k", listOf(Item(1, "old")))
        ageKey("k", 2 * 60 * 60 * 1000L) // 拨老 2 小时
        assertNull(
            "已过期就不该被 get 返回",
            store.getList<Item>("k", ttlMillis = 60_000)
        )
    }

    @Test
    fun `过期的缓存仍能被 getStale 读到`() = runTest {
        store.putList("k", listOf(Item(1, "stale")))
        ageKey("k", 2 * 60 * 60 * 1000L)

        assertNull(store.getList<Item>("k", ttlMillis = 60_000))
        // getStale 不看新鲜度 —— 这是断网兜底的基础
        assertEquals("stale", store.getStaleList<Item>("k")?.first()?.title)
    }

    // ---------------------------------------------------------------- 回源与兜底

    @Test
    fun `回源成功时写入缓存并在下次命中`() = runTest {
        val first = store.cachedList<Item>("k", ttlMillis = 60_000) {
            listOf(Item(9, "from-network"))
        }
        assertEquals("from-network", first.first().title)

        var fetchCalled = false
        val second = store.cachedList<Item>("k", ttlMillis = 60_000) {
            fetchCalled = true
            listOf(Item(1, "should-not-be-used"))
        }
        assertEquals("from-network", second.first().title)
        assertTrue("缓存命中时不应再回源", !fetchCalled)
    }

    /**
     * 本项目缓存最关键的一条：**回源失败时返回旧缓存，而不是抛异常**。
     * 弱网/断网下用户看到略旧内容，好过看到错误页。
     */
    @Test
    fun `回源失败时退回过期缓存而不抛异常`() = runTest {
        store.putList("k", listOf(Item(1, "cached-before")))
        ageKey("k", 2 * 60 * 60 * 1000L)

        val result = store.cachedList<Item>("k", ttlMillis = 60_000) {
            throw UnknownHostException("模拟断网")
        }
        assertEquals("cached-before", result.first().title)
    }

    @Test
    fun `没有缓存且回源失败时把异常抛给上层`() = runTest {
        try {
            store.cachedList<Item>("empty", ttlMillis = 60_000) {
                throw UnknownHostException("模拟断网")
            }
            fail("没有缓存兜底时应抛出异常，让上层显示错误态")
        } catch (e: UnknownHostException) {
            assertEquals("模拟断网", e.message)
        }
    }

    @Test
    fun `对象版 cachedObject 同样具备兜底`() = runTest {
        store.putObject("p", Payload(total = 3))
        ageKey("p", 2 * 60 * 60 * 1000L)

        val result = store.cachedObject<Payload>("p", ttlMillis = 60_000) {
            throw UnknownHostException("模拟断网")
        }
        assertEquals(3L, result.total)
    }

    // ---------------------------------------------------------------- 失效

    @Test
    fun `按模块前缀清理只影响该模块`() = runTest {
        store.putList(CacheKeys.page("course_list", "sort" to 1), listOf(Item(1, "a")))
        store.putList(CacheKeys.page("course_list", "sort" to 2), listOf(Item(2, "b")))
        store.putList(CacheKeys.page("other", "sort" to 1), listOf(Item(3, "c")))

        store.invalidateModule("course_list")

        assertNull(store.getList<Item>(CacheKeys.page("course_list", "sort" to 1), 0))
        assertNull(store.getList<Item>(CacheKeys.page("course_list", "sort" to 2), 0))
        assertNotNull(
            "不该误清其他模块",
            store.getList<Item>(CacheKeys.page("other", "sort" to 1), 0)
        )
    }

    @Test
    fun `clearAll 清空全部`() = runTest {
        store.putList(CacheKeys.page("m", "a" to 1), listOf(Item(1, "x")))
        store.putObject(CacheKeys.detail("m", 5), Item(5, "y"))

        store.clearAll()

        assertNull(store.getList<Item>(CacheKeys.page("m", "a" to 1), 0))
        assertNull(store.getObject<Item>(CacheKeys.detail("m", 5), 0))
    }

    @Test
    fun `prune 清掉超龄条目`() = runTest {
        store.putList("old", listOf(Item(1, "x")))
        store.putList("new", listOf(Item(2, "y")))
        // 拨老到超过 CacheTtl.MAX_AGE（8 天）
        ageKey("old", CacheTtl.MAX_AGE + 60_000)

        store.prune()

        assertNull("超龄条目应被清掉", store.getList<Item>("old", 0))
        assertNotNull("未超龄的应保留", store.getList<Item>("new", 0))
    }

    // ---------------------------------------------------------------- 坏数据

    /**
     * 模型结构变化后旧缓存会解不出来。这时必须**删掉该键**，
     * 否则每次读都失败，等于这块缓存永久失效。
     */
    @Test
    fun `解码失败会清除该键`() = runTest {
        dao.put(CacheEntry(key = "bad", value = "{ 这不是合法 JSON }"))

        assertNull(store.getList<Item>("bad", ttlMillis = 60_000))
        assertNull("解码失败后该键应被删除", dao.getEntry("bad"))
    }

    @Test
    fun `字段缺失但可容忍时仍能解出`() = runTest {
        // AppJson 配了 coerceInputValues，缺字段会走默认值而不是抛异常
        dao.put(CacheEntry(key = "partial", value = """{"id":5}"""))
        val got = store.getObject<Item>("partial", ttlMillis = 60_000)
        assertEquals(5L, got?.id)
        assertEquals("", got?.title)
    }

    // ---------------------------------------------------------------- 键约定

    @Test
    fun `缓存键把参数都带上了`() {
        val a = CacheKeys.page("course_list", "sort" to 1, "categoryId" to 10)
        val b = CacheKeys.page("course_list", "sort" to 2, "categoryId" to 10)
        val c = CacheKeys.page("course_list", "sort" to 1, "categoryId" to 11)
        assertEquals("三个键必须互不相同，否则不同请求会互相覆盖", 3, setOf(a, b, c).size)
    }

    @Test
    fun `空参数写成 all 而不是空串`() {
        val key = CacheKeys.page("m", "categoryId" to null)
        assertTrue("空值应落成 all，便于肉眼排查：$key", key.endsWith("categoryId=all"))
    }

    @Test
    fun `各模块键都带统一前缀`() {
        val keys = listOf(
            CacheKeys.categoryTree(2),
            CacheKeys.page("course_list", "sort" to 1),
            CacheKeys.detail("course", 1001),
            CacheKeys.config("ad_slots", "app_config")
        )
        keys.forEach { k ->
            assertTrue("键必须以版本前缀开头，便于整批失效：$k", k.startsWith("v1:"))
        }
    }
}
