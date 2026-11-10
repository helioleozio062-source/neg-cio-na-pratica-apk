package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

object WebExportHelper {

    fun exportAndShareIndexHtml(context: Context) {
        try {
            // 1. Read index.html from assets
            val assetStream = try {
                context.assets.open("index.html")
            } catch (e: Exception) {
                null
            }

            val htmlBytes = assetStream?.use { it.readBytes() } ?: fallbackHtml().toByteArray(Charsets.UTF_8)

            // 2. Save to app's external files directory
            val exportDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir, "web")
            if (!exportDir.exists()) exportDir.mkdirs()

            val targetFile = File(exportDir, "index.html")
            targetFile.writeBytes(htmlBytes)

            // 3. Obtain content URI via FileProvider
            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                targetFile
            )

            // 4. Launch Intent to share/save to device / WhatsApp / Drive / Email
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/html"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "index.html - Negócio na Prática (Portal Web)")
                putExtra(Intent.EXTRA_TEXT, "Ficheiro index.html do portal web autónomo de Negócio na Prática (Moeda: Metical MT). Pode abri-lo em qualquer navegador de computador ou hospedar online.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(sendIntent, "Baixar / Partilhar index.html para PC")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)

            Toast.makeText(context, "Ficheiro index.html preparado! Guarde nos Downloads ou envie para o PC.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Erro ao exportar index.html: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun exportAndShareCsv(context: Context, filename: String, csvContent: String) {
        try {
            val exportDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir, "relatorios")
            if (!exportDir.exists()) exportDir.mkdirs()

            val targetFile = File(exportDir, filename)
            targetFile.writeText(csvContent, Charsets.UTF_8)

            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                targetFile
            )

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "Planilha: $filename")
                putExtra(Intent.EXTRA_TEXT, "Planilha exportada de Negócio na Prática: $filename")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(sendIntent, "Abrir / Partilhar Planilha CSV")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            Toast.makeText(context, "Planilha $filename pronta para abrir ou enviar!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Erro ao exportar planilha: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun fallbackHtml(): String {
        return """<!DOCTYPE html><html lang="pt"><head><meta charset="UTF-8"><title>Negócio na Prática</title></head><body><h1>Negócio na Prática - Portal Web (Metical MT)</h1></body></html>"""
    }
}
