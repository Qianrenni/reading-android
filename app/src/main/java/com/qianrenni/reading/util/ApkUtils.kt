package com.qianrenni.reading.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import com.qianrenni.reading.data.repository.ApkInfo
import com.qianrenni.reading.data.repository.ApkReader
import com.qianrenni.reading.data.repository.AppVersion
import java.io.File

private const val TAG = "ApkUtils"
private const val APK_MIME_TYPE = "application/vnd.android.package-archive"

/** 通过 PackageManager 读取 APK 文件内声明的包名与版本号（不执行安装）。 */
class AndroidApkReader(private val context: Context) : ApkReader {

    override fun read(apkFile: File): ApkInfo? {
        val info = context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, 0) ?: return null
        return ApkInfo(
            packageName = info.packageName.orEmpty(),
            versionCode = PackageInfoCompat.getLongVersionCode(info),
            versionName = info.versionName.orEmpty()
        )
    }
}

/** 读取当前已安装应用自身的版本号。 */
fun installedAppVersion(context: Context): AppVersion {
    return try {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        AppVersion(
            versionCode = PackageInfoCompat.getLongVersionCode(info),
            versionName = info.versionName.orEmpty()
        )
    } catch (e: Exception) {
        Log.e(TAG, "installedAppVersion: ${e.message}")
        AppVersion(versionCode = 0L, versionName = "")
    }
}

/**
 * APK 安装器：通过 FileProvider 暴露缓存中的安装包并拉起系统安装界面。
 */
object ApkInstaller {

    /**
     * 拉起系统安装界面。
     *
     * @return true 表示已成功发起安装；false 表示失败（文件不存在，或缺少“安装未知应用”授权，
     * 此时会尝试跳转到对应的授权设置页）。
     */
    fun install(context: Context, apkFile: File): Boolean {
        if (!apkFile.exists() || apkFile.length() <= 0) {
            Log.e(TAG, "install: 安装包不存在 ${apkFile.absolutePath}")
            return false
        }
        if (!canRequestInstall(context)) {
            openUnknownSourcesSettings(context)
            return false
        }
        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, APK_MIME_TYPE)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "install: ${e.message}")
            false
        }
    }

    /** Android 8.0+ 需要用户显式授予“安装未知应用”权限。 */
    fun canRequestInstall(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
        return context.packageManager.canRequestPackageInstalls()
    }

    /** 跳转到本应用的“安装未知应用”授权页，便于用户手动开启。 */
    private fun openUnknownSourcesSettings(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        try {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "openUnknownSourcesSettings: ${e.message}")
        }
    }
}
