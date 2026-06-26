package com.passingguest.myapplication.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.passingguest.myapplication.data.entity.AccountRecord
import com.passingguest.myapplication.data.entity.Category
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.InputStreamReader

object ExportUtils {

    fun exportToCsv(
        context: Context,
        records: List<AccountRecord>,
        categoryMap: Map<Long, Category>
    ): Boolean {
        return try {
            val csvContent = buildString {
                appendLine("日期,类型,分类,金额,备注")
                records.forEach { record ->
                    val date = DateUtils.formatDate(record.date)
                    val type = if (record.type == "INCOME") "收入" else "支出"
                    val category = categoryMap[record.categoryId]?.name ?: "未知"
                    val amount = CurrencyUtils.formatWithoutSymbol(record.amount)
                    val note = record.note?.replace(",", "，") ?: ""
                    appendLine("$date,$type,$category,$amount,$note")
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Use MediaStore for Android 10+
                val contentResolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, "记账本_${DateUtils.getCurrentYearMonth().replace("-", "")}.csv")
                    put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }

                val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    contentResolver.openOutputStream(it)?.use { outputStream ->
                        outputStream.write(csvContent.toByteArray())
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                    contentResolver.update(it, contentValues, null, null)
                    Toast.makeText(context, "导出成功", Toast.LENGTH_SHORT).show()
                    return true
                }
            } else {
                // Legacy for Android 9 and below
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, "记账本_${DateUtils.getCurrentYearMonth().replace("-", "")}.csv")
                file.writeText(csvContent)
                Toast.makeText(context, "导出成功: ${file.absolutePath}", Toast.LENGTH_SHORT).show()
                return true
            }

            false
        } catch (e: Exception) {
            Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    data class CsvRecord(
        val date: String,
        val type: String,
        val category: String,
        val amount: Double,
        val note: String
    )

    fun parseCsv(inputStream: java.io.InputStream): List<CsvRecord> {
        val records = mutableListOf<CsvRecord>()
        try {
            val reader = BufferedReader(InputStreamReader(inputStream))
            reader.readLine() // Skip header

            reader.forEachLine { line ->
                val parts = line.split(",", limit = 5)
                if (parts.size >= 4) {
                    val date = parts[0].trim()
                    val type = parts[1].trim()
                    val category = parts[2].trim()
                    val amount = parts[3].trim().toDoubleOrNull() ?: 0.0
                    val note = parts.getOrElse(4) { "" }.trim()
                    records.add(CsvRecord(date, type, category, amount, note))
                }
            }
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return records
    }
}
