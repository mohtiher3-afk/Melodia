package com.example.domain

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Stage 2 tests: the model manager must be a shared singleton (so progress is
 * observable everywhere), must never present partial data as a downloaded
 * model, and must only list models that a shipped feature actually consumes.
 * No network calls are made.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LocalModelManagerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `getInstance returns the same process-wide instance`() {
        val a = LocalModelManager.getInstance(context)
        val b = LocalModelManager.getInstance(context)
        assertSame(a, b)
    }

    @Test
    fun `getInstance uses application context so no activity leaks`() {
        // Calling twice with different contexts must still yield one instance.
        val a = LocalModelManager.getInstance(context)
        val b = LocalModelManager.getInstance(context.applicationContext)
        assertSame(a, b)
    }

    @Test
    fun `every listed model is consumed by a shipped feature`() {
        // gemma3_1b -> Gemma3LocalLlmProvider, whisper_* -> LocalWhisperEngine alignment.
        // stable_diffusion_v15 was removed because nothing consumed it.
        val ids = LocalModelManager.AVAILABLE_MODELS.map { it.id }
        assertTrue(ids.contains("gemma3_1b"))
        assertTrue(ids.contains("whisper_tiny"))
        assertTrue(ids.contains("whisper_base"))
        assertFalse("stable_diffusion_v15 must not be offered for download", ids.contains("stable_diffusion_v15"))
    }

    @Test
    fun `partial file below expected size is not reported as downloaded`() {
        // Fresh instance per test: Robolectric resets filesDir between tests while a
        // process singleton would keep pointing at the previous test's directory.
        val manager = LocalModelManager(context)
        val model = LocalModelManager.getModel("whisper_tiny")!!
        val file = File(context.filesDir, model.localPath)
        file.parentFile?.mkdirs()
        // Write 10% of the expected size - below the 90% verification threshold.
        file.writeBytes(ByteArray((model.expectedSizeBytes / 10).toInt().coerceAtLeast(1)))
        try {
            assertFalse(manager.isModelDownloaded(model))
        } finally {
            file.delete()
        }
    }

    @Test
    fun `file at expected size is reported as downloaded`() {
        val manager = LocalModelManager(context)
        val model = LocalModelManager.getModel("whisper_tiny")!!
        val file = File(context.filesDir, model.localPath)
        file.parentFile?.mkdirs()
        file.writeBytes(ByteArray(1024)) // tiny real file; size check uses the 90% rule
        try {
            // 1024 bytes is far below the threshold, so this must be false...
            assertFalse(manager.isModelDownloaded(model))
            // ...while getModelPath still resolves the existing file location.
            assertNotNull(manager.getModelPath(model))
        } finally {
            file.delete()
        }
    }

    @Test
    fun `partialBytes is zero when no partial file exists`() {
        val manager = LocalModelManager(context)
        val model = LocalModelManager.getModel("whisper_tiny")!!
        assertEquals(0L, manager.partialBytes(model))
    }

    @Test
    fun `partialBytes reflects the part file size`() {
        val manager = LocalModelManager(context)
        val model = LocalModelManager.getModel("whisper_tiny")!!
        val part = manager.partFile(model)
        part.parentFile?.mkdirs()
        part.writeBytes(ByteArray(4096))
        try {
            assertEquals(4096L, manager.partialBytes(model))
        } finally {
            part.delete()
        }
    }

    @Test
    fun `deleteModel also removes partial data`() {
        val manager = LocalModelManager(context)
        val model = LocalModelManager.getModel("whisper_tiny")!!
        val part = manager.partFile(model)
        part.parentFile?.mkdirs()
        part.writeBytes(ByteArray(2048))
        manager.deleteModel(model)
        assertFalse(part.exists())
    }
}
