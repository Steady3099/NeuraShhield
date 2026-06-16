package com.attentionmanager.domain.usecase

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build

class AppCategoryResolver(private val context: Context) {
    fun labelFor(packageName: String): String =
        runCatching {
            val info = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(info).toString()
        }.getOrElse { packageName.substringAfterLast('.') }

    fun categoryFor(packageName: String, title: String, body: String): String {
        val text = "$title $body"
        if (text.contains("newsletter", ignoreCase = true) || text.contains("unsubscribe", ignoreCase = true)) {
            return "promotional emails"
        }
        if (text.contains("liked", ignoreCase = true) || text.contains("reacted", ignoreCase = true)) {
            return "${labelFor(packageName)} reactions"
        }
        val category = runCatching {
            val info = context.packageManager.getApplicationInfo(packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) info.category else ApplicationInfo.CATEGORY_UNDEFINED
        }.getOrDefault(ApplicationInfo.CATEGORY_UNDEFINED)

        return when (category) {
            ApplicationInfo.CATEGORY_NEWS -> "news updates"
            ApplicationInfo.CATEGORY_SOCIAL -> "${labelFor(packageName)} updates"
            ApplicationInfo.CATEGORY_PRODUCTIVITY -> "productivity updates"
            ApplicationInfo.CATEGORY_AUDIO,
            ApplicationInfo.CATEGORY_VIDEO,
            ApplicationInfo.CATEGORY_IMAGE -> "media updates"
            else -> "${labelFor(packageName)} notifications"
        }
    }
}
