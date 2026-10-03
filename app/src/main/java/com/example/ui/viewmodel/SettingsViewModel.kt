package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.WarXApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserAgentPreset(
    val name: String,
    val description: String,
    val value: String
)

class SettingsViewModel : ViewModel() {

    val presets = listOf(
        UserAgentPreset(
            name = "Android Chrome (Default)",
            description = "Google Pixel 8 Pro / Chrome Mobile",
            value = "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro Build/UQ1A.240205.004) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.6261.119 Mobile Safari/537.36"
        ),
        UserAgentPreset(
            name = "Windows Desktop Chrome",
            description = "Google Chrome 124 Windows 10/11",
            value = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
        ),
        UserAgentPreset(
            name = "macOS Safari",
            description = "Apple Safari 17 on macOS Sonoma",
            value = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Safari/605.1.15"
        ),
        UserAgentPreset(
            name = "iPhone iOS Safari",
            description = "iPhone 15 Pro / iOS 17 Safari Mobile",
            value = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Mobile/15E148 Safari/604.1"
        )
    )

    private val _selectedPreset = MutableStateFlow(presets.first())
    val selectedPreset: StateFlow<UserAgentPreset> = _selectedPreset.asStateFlow()

    fun selectPreset(preset: UserAgentPreset) {
        _selectedPreset.value = preset
        WarXApp.instance.updateExtractionEngineUserAgent(preset.value)
    }
}
