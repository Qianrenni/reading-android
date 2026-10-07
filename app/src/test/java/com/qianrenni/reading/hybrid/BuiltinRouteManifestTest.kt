package com.qianrenni.reading.hybrid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * 内置路由清单（`assets/hybrid/builtin.json`）解析测试。
 *
 * 清单是随 APK 发布的兜底路由，格式写错会导致整条路由不可用，所以既覆盖解析规则，
 * 也校验真正打进包里的那份 JSON 能被解析（解析不出来时容器只会如实报「未注册」）。
 */
class BuiltinRouteManifestTest {

    @Test
    fun `解析出全部字段并标记来源为内置`() {
        val specs = BuiltinRouteManifest.parse(
            """
            [
              {"route":"hybrid-demo-h5","engine":"h5","entry":"https://example.com/app","appKey":"guga-builtin","versionCode":3}
            ]
            """.trimIndent()
        )

        assertEquals(1, specs.size)
        val spec = specs.first()
        assertEquals("hybrid-demo-h5", spec.route)
        assertEquals(EngineType.H5, spec.engine)
        assertEquals("https://example.com/app", spec.entry)
        assertEquals("guga-builtin", spec.appKey)
        assertEquals(3, spec.versionCode)
        assertEquals(HybridSource.BUILTIN, spec.source)
    }

    @Test
    fun `引擎名大小写不敏感`() {
        val specs = BuiltinRouteManifest.parse(
            """[{"route":"r","engine":"RN","entry":"HybridDemo"}]"""
        )
        assertEquals(EngineType.RN, specs.single().engine)
    }

    @Test
    fun `未知引擎与缺字段的条目被跳过而不是整体失败`() {
        val specs = BuiltinRouteManifest.parse(
            """
            [
              {"route":"ok","engine":"h5","entry":"index.html"},
              {"route":"bad-engine","engine":"flutter","entry":"index.html"},
              {"route":"","engine":"h5","entry":"index.html"},
              {"route":"bad-entry","engine":"h5","entry":""}
            ]
            """.trimIndent()
        )
        assertEquals(listOf("ok"), specs.map { it.route })
    }

    @Test
    fun `格式非法时返回空列表`() {
        assertTrue(BuiltinRouteManifest.parse("{ not json").isEmpty())
        assertTrue(BuiltinRouteManifest.parse("").isEmpty())
    }

    @Test
    fun `随包发布的内置清单可被解析`() {
        val asset = File("src/main/assets/hybrid/builtin.json")
        assumeTrue("内置清单不在预期路径，跳过（由打包流程保证）", asset.exists())

        val specs = BuiltinRouteManifest.parse(asset.readText())
        assertTrue("内置清单应至少有一条兜底路由", specs.isNotEmpty())
        assertTrue(specs.all { it.route.isNotBlank() && it.entry.isNotBlank() })
    }
}
