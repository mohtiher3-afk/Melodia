package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Host-side unit tests for ChatHistorySummarizer XML generation.
 */
class ChatHistorySummarizerTest {

    @Test
    fun builder_generatesValidXmlSnapshot() {
        val xml = ChatHistorySummarizer.builder()
            .setOverallGoal("Test the ChatHistorySummarizer component")
            .addKnowledge(listOf(
                "Project uses Gradle 9.9.0",
                "Room migration policy protects v7+ schemas",
                "MP4 export uses EncoderInputSurface for A/V sync"
            ))
            .addChange("CREATED", "app/src/main/java/com/example/domain/ChatHistorySummarizer.kt", "New summarizer component")
            .addChange("MODIFIED", "app/src/main/java/com/example/domain/LyricVideoExporter.kt", "A/V sync fix")
            .addChange("DELETED", "app/src/main/cpp/whisper.cpp", "Obsolete C++ stub")
            .addAction("Implemented ChatHistorySummarizer with XML output")
            .addAction("Added unit tests for XML generation")
            .addPlan("Test MP4 export on physical device", ChatHistorySummarizer.PlanItem.Status.DONE)
            .addPlan("Verify Room migration for v8 schema", ChatHistorySummarizer.PlanItem.Status.TODO)
            .addPlan("Run full test suite on Android SDK", ChatHistorySummarizer.PlanItem.Status.IN_PROGRESS)
            .build()

        assertNotNull(xml)
        assertTrue("XML should contain state_snapshot root", xml.contains("<state_snapshot"))
        assertTrue("XML should contain overall_goal", xml.contains("<overall_goal>"))
        assertTrue("XML should contain key_knowledge", xml.contains("<key_knowledge>"))
        assertTrue("XML should contain file_system_state", xml.contains("<file_system_state>"))
        assertTrue("XML should contain recent_actions", xml.contains("<recent_actions>"))
        assertTrue("XML should contain current_plan", xml.contains("<current_plan>"))
    }

    @Test
    fun builder_handlesEmptyState() {
        val xml = ChatHistorySummarizer.builder().build()

        assertNotNull(xml)
        assertTrue("XML should still have state_snapshot root", xml.contains("<state_snapshot"))
        assertTrue("XML should not contain overall_goal when empty", !xml.contains("<overall_goal>"))
        assertTrue("XML should not contain key_knowledge when empty", !xml.contains("<key_knowledge>"))
        assertTrue("XML should not contain file_system_state when empty", !xml.contains("<file_system_state>"))
        assertTrue("XML should not contain recent_actions when empty", !xml.contains("<recent_actions>"))
        assertTrue("XML should not contain current_plan when empty", !xml.contains("<current_plan>"))
    }

    @Test
    fun builder_planStatusAttributesCorrect() {
        val xml = ChatHistorySummarizer.builder()
            .addPlan("Completed task", ChatHistorySummarizer.PlanItem.Status.DONE)
            .addPlan("In progress task", ChatHistorySummarizer.PlanItem.Status.IN_PROGRESS)
            .addPlan("Pending task", ChatHistorySummarizer.PlanItem.Status.TODO)
            .build()

        assertTrue("Should contain done status", xml.contains("status=\"done\""))
        assertTrue("Should contain in_progress status", xml.contains("status=\"in_progress\""))
        assertTrue("Should contain todo status", xml.contains("status=\"todo\""))
    }

    @Test
    fun builder_fileSystemStateGroupsCorrectly() {
        val xml = ChatHistorySummarizer.builder()
            .addChange("CREATED", "summarizer/FileA.kt", "Summarizer project file")
            .addChange("CREATED", "summarizer/FileB.kt", "Another summarizer file")
            .addChange("MODIFIED", "melodia/Domain.kt", "Melodia domain change")
            .addChange("DELETED", "melodia/OldFile.kt", "Removed from melodia")
            .build()

        // Created files should be in summarizer_project
        assertTrue("Created files in summarizer_project", xml.contains("<summarizer_project>"))
        assertTrue("FileA in created", xml.contains("path=\"summarizer/FileA.kt\""))
        assertTrue("FileB in created", xml.contains("path=\"summarizer/FileB.kt\""))

        // Modified/Deleted in melodia_project
        assertTrue("Modified files in melodia_project", xml.contains("<melodia_project>"))
        assertTrue("Domain.kt in modified", xml.contains("path=\"melodia/Domain.kt\""))
        assertTrue("OldFile.kt in deleted", xml.contains("path=\"melodia/OldFile.kt\""))
    }

    @Test
    fun builder_recentActionsReversed() {
        val xml = ChatHistorySummarizer.builder()
            .addAction("First action")
            .addAction("Second action")
            .addAction("Third action")
            .build()

        // Actions should appear in reverse order (most recent first)
        val firstIdx = xml.indexOf("First action")
        val secondIdx = xml.indexOf("Second action")
        val thirdIdx = xml.indexOf("Third action")

        assertTrue("Third action should appear before second", thirdIdx < secondIdx)
        assertTrue("Second action should appear before first", secondIdx < firstIdx)
    }

    @Test
    fun builder_knowledgeItemsPreserved() {
        val xml = ChatHistorySummarizer.builder()
            .addKnowledge("Fact 1")
            .addKnowledge("Fact 2")
            .build()

        assertTrue("Knowledge should be in key_knowledge", xml.contains("<key_knowledge>"))
        assertTrue("Fact 1 present", xml.contains("Fact 1"))
        assertTrue("Fact 2 present", xml.contains("Fact 2"))
        // Should be wrapped in <item> tags
        assertEquals("Two knowledge items", 2, xml.split("<item>").size - 1)
    }

    @Test
    fun changeEntry_typeUppercased() {
        val entry = ChatHistorySummarizer.ChangeEntry.create("created", "test.kt")
        assertEquals("CREATED", entry.type)

        val entry2 = ChatHistorySummarizer.ChangeEntry.create("Modified", "test.kt")
        assertEquals("MODIFIED", entry2.type)
    }

    @Test
    fun planItem_defaultStatusIsTodo() {
        val item = ChatHistorySummarizer.PlanItem(1, "Test task")
        assertEquals(ChatHistorySummarizer.PlanItem.Status.TODO, item.status)
    }

    @Test
    fun markPlanDone_updatesStatus() {
        val builder = ChatHistorySummarizer.builder()
            .addPlan("Task 1", ChatHistorySummarizer.PlanItem.Status.TODO)
            .addPlan("Task 2", ChatHistorySummarizer.PlanItem.Status.IN_PROGRESS)

        builder.markPlanDone(1)

        val xml = builder.build()
        assertTrue("Task 1 should be done", xml.contains("status=\"done\""))
        assertTrue("Task 2 should remain in_progress", xml.contains("status=\"in_progress\""))
    }
}