package com.example.domain

import java.io.StringWriter

/**
 * Generates an XML state snapshot summarizing a development session's progress, knowledge, and plans.
 *
 * The output follows the required schema:
 * ```xml
 * <state_snapshot>
 *   <overall_goal>...</overall_goal>
 *   <key_knowledge>...</key_knowledge>
 *   <file_system_state>...</file_system_state>
 *   <recent_actions>...</recent_actions>
 *   <current_plan>...</current_plan>
 * </state_snapshot>
 * ```
 */
object ChatHistorySummarizer {

    data class ChangeEntry(val type: String, val path: String, val description: String? = null) {
        companion object {
            fun create(type: String, path: String, description: String? = null) =
                ChangeEntry(type.uppercase(), path, description)
        }
    }

    data class PlanItem(
        val id: Int,
        val description: String,
        val status: Status = Status.TODO,
    ) {
        enum class Status { DONE, IN_PROGRESS, TODO }
    }

    class SessionBuilder {
        private val _overallGoal = StringBuilder()
        private val _knowledgePoints = mutableListOf<String>()
        private val _changes = mutableListOf<ChangeEntry>()
        private val _actions = mutableListOf<String>()
        private val _planItems = mutableListOf<PlanItem>()
        private var _nextPlanId = 1

        fun setOverallGoal(goal: String) = apply {
            _overallGoal.appendLine(goal)
        }

        fun addKnowledge(points: List<String>) = apply {
            _knowledgePoints.addAll(points)
        }

        fun addKnowledge(point: String) = apply {
            _knowledgePoints.add(point)
        }

        fun addChange(type: String, path: String, description: String? = null) = apply {
            _changes.add(ChangeEntry.create(type, path, description))
        }

        fun addAction(action: String) = apply {
            _actions.add(action)
        }

        fun addPlan(description: String, status: PlanItem.Status = PlanItem.Status.TODO) = apply {
            _planItems.add(PlanItem(_nextPlanId++, description, status))
        }

        fun markPlanDone(id: Int) = apply {
            _planItems.find { it.id == id }?.let { item ->
                _planItems[_planItems.indexOf(item)] = item.copy(status = PlanItem.Status.DONE)
            }
        }

        fun markPlanInProgress(id: Int) = apply {
            _planItems.find { it.id == id }?.let { item ->
                _planItems[_planItems.indexOf(item)] = item.copy(status = PlanItem.Status.IN_PROGRESS)
            }
        }

        fun build(): String {
            val writer = StringWriter()
            val xml = XmlBuilder(writer)

            xml.startDocument()
            xml.startElement("state_snapshot")

            if (_overallGoal.isNotEmpty()) {
                xml.startElement("overall_goal")
                xml.text(_overallGoal.toString().trim())
                xml.endElement("overall_goal")
            }

            if (_knowledgePoints.isNotEmpty()) {
                xml.startElement("key_knowledge")
                _knowledgePoints.forEach { point ->
                    xml.startElement("item")
                    xml.text(point)
                    xml.endElement("item")
                }
                xml.endElement("key_knowledge")
            }

            if (_changes.isNotEmpty()) {
                xml.startElement("file_system_state")
                val created = _changes.filter { it.type == "CREATED" }
                val modified = _changes.filter { it.type == "MODIFIED" }
                val deleted = _changes.filter { it.type == "DELETED" }

                if (created.isNotEmpty()) {
                    xml.startElement("summarizer_project")
                    created.forEach { change ->
                        xml.startElement("created")
                        xml.attribute("path", change.path)
                        change.description?.let { desc ->
                            xml.attribute("description", desc)
                        }
                        xml.endElement("created")
                    }
                    xml.endElement("summarizer_project")
                }

                if (modified.isNotEmpty() || deleted.isNotEmpty()) {
                    xml.startElement("melodia_project")
                    modified.forEach { change ->
                        xml.startElement("modified")
                        xml.attribute("path", change.path)
                        if (change.description != null) {
                            xml.attribute("description", change.description)
                        }
                        xml.endElement("modified")
                    }
                    deleted.forEach { change ->
                        xml.startElement("deleted")
                        xml.attribute("path", change.path)
                        if (change.description != null) {
                            xml.attribute("description", change.description)
                        }
                        xml.endElement("deleted")
                    }
                    xml.endElement("melodia_project")
                }

                xml.endElement("file_system_state")
            }

            if (_actions.isNotEmpty()) {
                xml.startElement("recent_actions")
                _actions.reversed().forEach { action ->
                    xml.startElement("action")
                    xml.text(action)
                    xml.endElement("action")
                }
                xml.endElement("recent_actions")
            }

            if (_planItems.isNotEmpty()) {
                xml.startElement("current_plan")
                _planItems.forEach { item ->
                    xml.startElement("item")
                    xml.attribute("id", item.id.toString())
                    xml.attribute("status", item.status.name.lowercase())
                    xml.text(item.description)
                    xml.endElement("item")
                }
                xml.endElement("current_plan")
            }

            xml.endElement("state_snapshot")
            xml.endDocument()
            return writer.toString()
        }
    }

    fun builder(): SessionBuilder = SessionBuilder()

    private class XmlBuilder(private val writer: StringWriter) {
        private var _inElement = false

        fun startDocument() {
            writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
        }

        fun endDocument() {
        }

        fun startElement(name: String) {
            if (_inElement) {
                writer.append(">")
                _inElement = false
            }
            writer.append("<").append(name)
            _inElement = true
        }

        fun attribute(name: String, value: String) {
            writer.append(" ").append(name).append("=\"").append(escapeXml(value)).append("\"")
        }

        fun text(value: String) {
            if (_inElement) {
                writer.append(">")
                _inElement = false
            }
            writer.append(escapeXml(value))
        }

        fun endElement(name: String) {
            if (_inElement) {
                writer.append(" />")
                _inElement = false
            } else {
                writer.append("</").append(name).append(">")
            }
        }

        private fun escapeXml(value: String): String {
            return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;")
        }
    }
}