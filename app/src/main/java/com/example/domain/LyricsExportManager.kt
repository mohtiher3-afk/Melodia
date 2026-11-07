package com.example.domain

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.model.Song
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object LyricsExportManager {

    fun formatTimestamp(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val hundredths = (ms % 1000) / 10
        return String.format(Locale.US, "%02d:%02d.%02d", minutes, seconds, hundredths)
    }

    /**
     * Standard Synchronized Lyrics Format (.lrc)
     * Used worldwide by music players (Spotify, Apple Music, VLC, Poweramp, etc.)
     */
    fun generateLrcContent(song: Song): String {
        val sections = MusicEngine.parseLyricsIntoSections(song.lyrics)
        val sb = StringBuilder()

        sb.appendLine("[ti:${song.title}]")
        sb.appendLine("[ar:Melodia AI]")
        sb.appendLine("[al:${song.genre}]")
        sb.appendLine("[by:Melodia Studio (Nothing OS)]")
        sb.appendLine("[re:Melodia Treblo Engine]")

        sections.forEach { section ->
            // Section header timestamp
            sb.appendLine("[${formatTimestamp(section.startTimeMs)}][${section.name.uppercase()}]")

            val lines = section.text.lines()
                .map { it.trim() }
                .filter { it.isNotBlank() }

            if (lines.isNotEmpty()) {
                val durationPerLine = if (lines.size > 1) {
                    (section.endTimeMs - section.startTimeMs) / lines.size
                } else {
                    section.endTimeMs - section.startTimeMs
                }

                lines.forEachIndexed { idx, line ->
                    val lineTime = section.startTimeMs + (idx * durationPerLine)
                    sb.appendLine("[${formatTimestamp(lineTime)}]$line")
                }
            }
        }

        return sb.toString()
    }

    /**
     * Microsoft Word Document format (.doc)
     * Styled rich document opened natively by Microsoft Word, Google Docs, and WPS Office
     */
    fun generateWordDocContent(song: Song): String {
        val sections = MusicEngine.parseLyricsIntoSections(song.lyrics)
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val sectionsHtml = StringBuilder()
        sections.forEach { section ->
            sectionsHtml.appendLine("<div class='section-block'>")
            sectionsHtml.appendLine("<div class='section-title'>[${section.name.uppercase()}] <span class='timing'>(${formatTimestamp(section.startTimeMs)} - ${formatTimestamp(section.endTimeMs)})</span></div>")
            section.text.lines().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isNotBlank()) {
                    sectionsHtml.appendLine("<p class='lyric-line'>$trimmed</p>")
                }
            }
            sectionsHtml.appendLine("</div>")
        }

        return """
            <html xmlns:o='urn:schemas-microsoft-com:office:office' xmlns:w='urn:schemas-microsoft-com:office:word' xmlns='http://www.w3.org/TR/REC-html40'>
            <head>
                <meta charset='utf-8'>
                <title>${song.title} - Lyrics Document</title>
                <style>
                    body {
                        font-family: 'Segoe UI', Calibri, Arial, sans-serif;
                        padding: 36pt;
                        color: #1a1a1a;
                        background: #ffffff;
                        line-height: 1.6;
                    }
                    .header-box {
                        border-bottom: 2pt solid #d71921;
                        padding-bottom: 12pt;
                        margin-bottom: 20pt;
                    }
                    .app-badge {
                        color: #d71921;
                        font-size: 10pt;
                        font-weight: bold;
                        letter-spacing: 1.5pt;
                        text-transform: uppercase;
                    }
                    h1 {
                        color: #111111;
                        font-size: 24pt;
                        margin: 4pt 0;
                    }
                    .meta-info {
                        color: #666666;
                        font-size: 11pt;
                    }
                    .section-block {
                        margin-bottom: 16pt;
                        page-break-inside: avoid;
                    }
                    .section-title {
                        color: #d71921;
                        font-size: 12pt;
                        font-weight: bold;
                        letter-spacing: 1pt;
                        margin-bottom: 6pt;
                    }
                    .timing {
                        color: #777777;
                        font-size: 10pt;
                        font-weight: normal;
                    }
                    .lyric-line {
                        font-size: 13pt;
                        margin: 3pt 0;
                        color: #222222;
                    }
                    .footer-note {
                        margin-top: 30pt;
                        border-top: 1pt solid #e0e0e0;
                        padding-top: 10pt;
                        font-size: 9pt;
                        color: #888888;
                    }
                </style>
            </head>
            <body>
                <div class='header-box'>
                    <div class='app-badge'>Melodia Studio // Nothing OS Music AI</div>
                    <h1>${song.title}</h1>
                    <div class='meta-info'>
                        <strong>Genre:</strong> ${song.genre} &nbsp;|&nbsp;
                        <strong>Mood:</strong> ${song.mood} &nbsp;|&nbsp;
                        <strong>Language:</strong> ${song.language} &nbsp;|&nbsp;
                        <strong>Date:</strong> $dateStr
                    </div>
                </div>

                $sectionsHtml

                <div class='footer-note'>
                    Generated by Melodia AI Music Studio &nbsp;&bull;&nbsp; Lyrics & timing data exported in document format.
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    /**
     * Clean Lyrics Text (.txt)
     */
    fun generatePureLyricsText(song: Song): String {
        val sb = StringBuilder()
        sb.appendLine(song.title.uppercase())
        sb.appendLine("${song.genre.uppercase()} // ${song.mood.uppercase()}")
        sb.appendLine("==========================================")
        sb.appendLine()
        sb.appendLine(song.lyrics)
        sb.appendLine()
        sb.appendLine("==========================================")
        sb.appendLine("Melodia Studio (Nothing OS AI Music)")
        return sb.toString()
    }

    /**
     * JSON format with complete metadata, timestamps, and word timings
     */
    fun generateLyricsJson(song: Song): String {
        val sections = MusicEngine.parseLyricsIntoSections(song.lyrics)

        val rootObj = JSONObject()
        rootObj.put("title", song.title)
        rootObj.put("genre", song.genre)
        rootObj.put("mood", song.mood)
        rootObj.put("language", song.language)
        rootObj.put("prompt", song.promptText)
        rootObj.put("tags", JSONArray(song.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }))
        if (!song.audioUrl.isNullOrBlank()) {
            rootObj.put("audioUrl", song.audioUrl)
        }
        if (!song.trebloGenerationId.isNullOrBlank()) {
            rootObj.put("trebloGenerationId", song.trebloGenerationId)
        }

        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        rootObj.put("exportedAt", isoFormat.format(Date()))
        rootObj.put("software", "Melodia // Nothing OS Music Studio")

        val sectionsArray = JSONArray()
        sections.forEach { section ->
            val sectionObj = JSONObject()
            sectionObj.put("section", section.name)
            sectionObj.put("startTime", formatTimestamp(section.startTimeMs))
            sectionObj.put("endTime", formatTimestamp(section.endTimeMs))
            sectionObj.put("startTimeMs", section.startTimeMs)
            sectionObj.put("endTimeMs", section.endTimeMs)
            sectionObj.put("lyrics", section.text)
            sectionsArray.put(sectionObj)
        }
        rootObj.put("sections", sectionsArray)

        val wordTimingsArray = JSONArray()
        sections.forEach { section ->
            val words = section.text.split(Regex("\\s+")).filter { it.isNotBlank() }
            if (words.isNotEmpty()) {
                val durationPerWord = if (words.size > 1) {
                    (section.endTimeMs - section.startTimeMs) / words.size
                } else {
                    500L
                }
                words.forEachIndexed { idx, word ->
                    val wordStart = section.startTimeMs + (idx * durationPerWord)
                    val wordEnd = wordStart + durationPerWord
                    val wordObj = JSONObject()
                    wordObj.put("word", word)
                    wordObj.put("section", section.name)
                    wordObj.put("startTime", formatTimestamp(wordStart))
                    wordObj.put("startTimeMs", wordStart)
                    wordObj.put("endTime", formatTimestamp(wordEnd))
                    wordObj.put("endTimeMs", wordEnd)
                    wordTimingsArray.put(wordObj)
                }
            }
        }
        rootObj.put("wordTimings", wordTimingsArray)
        rootObj.put("fullLyrics", song.lyrics)

        return rootObj.toString(2)
    }

    fun generateLyricsTextWithTimings(song: Song): String {
        val sections = MusicEngine.parseLyricsIntoSections(song.lyrics)
        val sb = StringBuilder()

        sb.appendLine("==================================================")
        sb.appendLine("MELODIA // LYRICS & TIMING EXPORT (NOTHING OS)")
        sb.appendLine("==================================================")
        sb.appendLine("TITLE:    ${song.title}")
        sb.appendLine("GENRE:    ${song.genre.uppercase()} // ${song.mood.uppercase()}")
        sb.appendLine("LANGUAGE: ${song.language}")
        if (song.tags.isNotBlank()) {
            sb.appendLine("TAGS:     ${song.tags}")
        }
        sb.appendLine("EXPORT:   ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}")
        sb.appendLine("--------------------------------------------------")
        sb.appendLine()
        sb.appendLine("[TIMED SECTIONS & LYRICS]")
        sb.appendLine()

        sections.forEach { section ->
            val startStr = formatTimestamp(section.startTimeMs)
            val endStr = formatTimestamp(section.endTimeMs)
            sb.appendLine("[$startStr - $endStr] [${section.name.uppercase()}]")
            section.text.lines().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isNotBlank()) {
                    sb.appendLine("  $trimmed")
                }
            }
            sb.appendLine()
        }

        sb.appendLine("--------------------------------------------------")
        sb.appendLine("[FULL RAW LYRICS]")
        sb.appendLine(song.lyrics)
        sb.appendLine("==================================================")
        sb.appendLine("Generated via Melodia Studio (Treblo AI & Gemini)")

        return sb.toString()
    }

    // --- Intent Creators ---

    fun createLrcExportIntent(context: Context, song: Song): Intent {
        val safeTitle = song.title.lowercase().replace(Regex("[^a-z0-9]"), "_").take(24)
        val fileName = "$safeTitle.lrc"
        val file = File(context.cacheDir, fileName)

        val content = generateLrcContent(song)
        FileOutputStream(file).use { fos ->
            fos.write(content.toByteArray(Charsets.UTF_8))
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        return Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "${song.title} - Synchronized Lyrics (.lrc)")
            putExtra(Intent.EXTRA_TEXT, "Synchronized LRC lyrics file for \"${song.title}\".")
            type = "text/plain"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun createWordDocExportIntent(context: Context, song: Song): Intent {
        val safeTitle = song.title.lowercase().replace(Regex("[^a-z0-9]"), "_").take(24)
        val fileName = "${safeTitle}_lyrics.doc"
        val file = File(context.cacheDir, fileName)

        val content = generateWordDocContent(song)
        FileOutputStream(file).use { fos ->
            fos.write(content.toByteArray(Charsets.UTF_8))
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        return Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "${song.title} - Lyrics Document (.doc)")
            putExtra(Intent.EXTRA_TEXT, "Formatted lyrics document for \"${song.title}\".")
            type = "application/msword"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun createJsonExportIntent(context: Context, song: Song): Intent {
        val safeTitle = song.title.lowercase().replace(Regex("[^a-z0-9]"), "_").take(24)
        val fileName = "melodia_${safeTitle}_timed_lyrics.json"
        val file = File(context.cacheDir, fileName)

        val jsonContent = generateLyricsJson(song)
        FileOutputStream(file).use { fos ->
            fos.write(jsonContent.toByteArray(Charsets.UTF_8))
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        return Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "${song.title} - Lyrics & Timing JSON")
            putExtra(Intent.EXTRA_TEXT, "Exported lyrics and timing data for \"${song.title}\" via Melodia Studio.")
            type = "application/json"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun createTextExportIntent(context: Context, song: Song): Intent {
        val safeTitle = song.title.lowercase().replace(Regex("[^a-z0-9]"), "_").take(24)
        val fileName = "melodia_${safeTitle}_lyrics.txt"
        val file = File(context.cacheDir, fileName)

        val textContent = generatePureLyricsText(song)
        FileOutputStream(file).use { fos ->
            fos.write(textContent.toByteArray(Charsets.UTF_8))
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        return Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "${song.title} - Lyrics Text")
            putExtra(Intent.EXTRA_TEXT, "Lyrics text for \"${song.title}\" via Melodia Studio.")
            type = "text/plain"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
