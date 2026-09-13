package com.qianrenni.reading.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * 用系统浏览器（或拨号、邮件等对应应用）打开链接。
 *
 * 会加上 `FLAG_ACTIVITY_NEW_TASK`，避免非 Activity 上下文调用时崩溃。
 *
 * @return 是否成功唤起；无可用应用或链接非法时返回 false（调用方应提示用户而不是崩溃）。
 */
fun openWithSystem(context: Context, url: String): Boolean {
    val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
    val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
