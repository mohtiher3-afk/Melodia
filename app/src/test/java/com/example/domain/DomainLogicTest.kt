package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.RandomAccessFile

/**
 * Host-side unit tests for the pure logic in the domain layer.
 * These guard the parsing/normalisation rules the UI depends on.
 */
class DomainLogicTest {

    // --- MusicEngine.parseLyricsIntoSections ---------------------------------

    @Test
    fun parseLyricsIntoSections_readsNamesTextAndTimings() {
        val lyrics = """
            [Intro]
            Hello world
            [Chorus]
            Sing it loud
            Sing it proud
        """.trimIndent()

        val sections = MusicEngine.parseLyricsIntoSections(lyrics)

        assertEquals(2, sections.size)
        assertEquals("Intro", sections[0].name)
        assertEquals("Hello world", sections[0].text)
        assertEquals(0L, sections[0].startTimeMs)
        assertEquals(25_000L, sections[0].endTimeMs)

        assertEquals("Chorus", sections[1].name)
        assertEquals("Sing it loud\nSing it proud", sections[1].text)
        assertEquals(25_000L, sections[1].startTimeMs)
        assertEquals(50_000L, sections[1].endTimeMs)
    }

    @Test
    fun parseLyricsIntoSections_ignoresTextBeforeFirstTag() {
        val lyrics = "stray line\n[Verse 1]\nReal lyric"

        val sections = MusicEngine.parseLyricsIntoSections(lyrics)

        assertEquals(1, sections.size)
        assertEquals("Verse 1", sections[0].name)
        assertEquals("Real lyric", sections[0].text)
    }

    @Test
    fun parseLyricsIntoSections_returnsEmptyForPlainText() {
        assertTrue(MusicEngine.parseLyricsIntoSections("just words").isEmpty())
        assertTrue(MusicEngine.parseLyricsIntoSections("").isEmpty())
    }

    // --- LocalWhisperEngine.normalizeArabicText ------------------------------

    @Test
    fun normalizeArabicText_stripsDiacriticsAndNormalizesLetters() {
        assertEquals("احمد", LocalWhisperEngine.normalizeArabicText("أَحْمَد"))
        assertEquals("مدرسه", LocalWhisperEngine.normalizeArabicText("مدرسة"))
        assertEquals("علي", LocalWhisperEngine.normalizeArabicText("على"))
        assertEquals("ايمان", LocalWhisperEngine.normalizeArabicText("إيمان"))
    }

    @Test
    fun normalizeArabicText_removesPunctuationAndTrims() {
        assertEquals("كلمات الاغنيه", LocalWhisperEngine.normalizeArabicText("  كلمات، الأغنية!  "))
        assertEquals("hello world", LocalWhisperEngine.normalizeArabicText("Hello, World!"))
    }

    // --- LocalAudioImporter.isWithinTranscriptionLimit -----------------------

    @Test
    fun isWithinTranscriptionLimit_acceptsSmallNonEmptyFile() {
        val file = File.createTempFile("melodia-small", ".mp3")
        try {
            file.writeBytes(ByteArray(1024))
            assertTrue(LocalAudioImporter.isWithinTranscriptionLimit(file))
        } finally {
            file.delete()
        }
    }

    @Test
    fun isWithinTranscriptionLimit_rejectsEmptyAndMissingFiles() {
        val empty = File.createTempFile("melodia-empty", ".mp3")
        try {
            assertFalse(LocalAudioImporter.isWithinTranscriptionLimit(empty))
            assertFalse(LocalAudioImporter.isWithinTranscriptionLimit(File("does-not-exist-${System.nanoTime()}.mp3")))
        } finally {
            empty.delete()
        }
    }

    @Test
    fun isWithinTranscriptionLimit_rejectsFilesOverInlineLimit() {
        val big = File.createTempFile("melodia-big", ".mp3")
        try {
            RandomAccessFile(big, "rw").use { it.setLength(15L * 1024L * 1024L) }
            assertFalse(LocalAudioImporter.isWithinTranscriptionLimit(big))
        } finally {
            big.delete()
        }
    }

    // --- framePresentationTimeNs (lyric video exporter) ---------------------

    @Test
    fun framePresentationTimeNs_frame0_isZero() {
        assertEquals(0L, framePresentationTimeNs(0, 30))
        assertEquals(0L, framePresentationTimeNs(0, 24))
    }

    @Test
    fun framePresentationTimeNs_exactSecondBoundaries() {
        assertEquals(1_000_000_000L, framePresentationTimeNs(30, 30))
        assertEquals(2_000_000_000L, framePresentationTimeNs(60, 30))
        assertEquals(1_000_000_000L, framePresentationTimeNs(24, 24))
    }

    @Test
    fun framePresentationTimeNs_isStrictlyIncreasing() {
        val times = (0 until 120).map { framePresentationTimeNs(it, 30) }
        for (i in 1 until times.size) {
            assertTrue("frame $i must be after frame ${i - 1}", times[i] > times[i - 1])
        }
    }

    @Test
    fun framePresentationTimeNs_keepsSubMicrosecondPrecision() {
        assertEquals(41_666_666L, framePresentationTimeNs(1, 24))
    }

    @Test(expected = IllegalArgumentException::class)
    fun framePresentationTimeNs_rejectsNonPositiveFps() {
        framePresentationTimeNs(0, 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun framePresentationTimeNs_rejectsNegativeFrameIndex() {
        framePresentationTimeNs(-1, 30)
    }
}
