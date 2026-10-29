package com.mealory.app.util

import android.database.sqlite.SQLiteFullException
import android.system.ErrnoException
import android.system.OsConstants
import java.io.FileNotFoundException

fun Throwable.toUserFacingMessage(fallback: String): String {
    val causes = generateSequence(this as Throwable?) { it.cause }.toList()
    return when {
        causes.any { it is SQLiteFullException || it is ErrnoException && it.errno == OsConstants.ENOSPC } ->
            "设备存储空间不足。请清理一些空间后再试，当前记录不会被删除。"
        causes.any { it is OutOfMemoryError } ->
            "这张照片尺寸过大，设备无法处理。请选择较小的照片后再试。"
        causes.any { it is SecurityException || it is FileNotFoundException } ->
            "无法继续访问所选文件，请重新选择后再试。"
        causes.any { it.message?.contains("damaged image", ignoreCase = true) == true } ->
            "照片已损坏或格式不受支持，请换一张照片。"
        else -> message?.takeIf(String::isNotBlank)?.let { "$fallback：$it" } ?: fallback
    }
}
