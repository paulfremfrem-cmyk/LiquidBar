// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright 2026 btm_m
package btm.m.liquidglass

enum class NavigationStyle(val preferenceValue: String, val displayName: String) {
    LIQUID_GLASS("liquid_glass", "iOS Liquid Glass (Verre)"),
    HYPER_OS("hyper_os", "HyperOS Docké"),
    HYPER_OS_FLOATING("hyper_os_floating", "HyperOS Capsule Flottante"),
    HARMONY_OS_FLOATING("harmony_os_floating", "HarmonyOS Capsule");

    companion object {
        const val PREFERENCE_KEY = "nav_style"
        const val DEFAULT_VALUE = "hyper_os_floating"

        @JvmStatic
        fun fromPreference(value: String?): NavigationStyle =
            entries.firstOrNull { it.preferenceValue == value } ?: HYPER_OS_FLOATING
    }
}

enum class AppColorMode(val preferenceValue: String, val displayName: String) {
    SYSTEM("system", "Système"),
    LIGHT("light", "Clair"),
    DARK("dark", "Sombre");

    companion object {
        const val DEFAULT_VALUE = "system"

        @JvmStatic
        fun fromPreference(value: String?): AppColorMode =
            entries.firstOrNull { it.preferenceValue == value } ?: SYSTEM
    }
}

enum class LabelMode(val preferenceValue: String, val displayName: String) {
    ICON_AND_TEXT("icon_and_text", "Icônes et texte"),
    ICON_ONLY("icon_only", "Icônes seulement"),
    TEXT_ONLY("text_only", "Texte seulement");

    companion object {
        const val DEFAULT_VALUE = "icon_and_text"

        @JvmStatic
        fun fromPreference(value: String?): LabelMode =
            entries.firstOrNull { it.preferenceValue == value } ?: ICON_AND_TEXT
    }
}
