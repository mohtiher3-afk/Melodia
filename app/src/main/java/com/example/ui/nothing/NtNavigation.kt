package com.example.ui.nothing

/** The three top-level destinations exposed by the Nothing-style bottom navigation. */
enum class NtDestination(val navigationIndex: Int, val testTag: String) {
    PROJECT(0, "nav_project"),
    LIBRARY(1, "nav_library"),
    SETTINGS(2, "nav_settings");

    companion object {
        fun fromIndex(index: Int): NtDestination = entries.firstOrNull { it.navigationIndex == index } ?: PROJECT
    }
}

/** The five ordered screens that make up the active project workflow. */
enum class ProjectStep(val testTag: String) {
    STUDIO("project_step_1"),
    LYRICS("project_step_2"),
    VISUALS("project_step_3"),
    SYNC("project_step_4"),
    EXPORT("project_step_5");

    val index: Int get() = ordinal

    companion object {
        fun fromIndex(index: Int): ProjectStep = entries.getOrNull(index) ?: STUDIO
    }
}
