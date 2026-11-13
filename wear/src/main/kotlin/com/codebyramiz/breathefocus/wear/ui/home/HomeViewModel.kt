package com.codebyramiz.breathefocus.wear.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codebyramiz.breathefocus.core.data.SettingsRepository
import com.codebyramiz.breathefocus.core.model.BreathingPattern
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class HomeViewModel @Inject constructor(settings: SettingsRepository) : ViewModel() {
    /** Pattern the phone (or a previous run) picked: what Home pre-selects. */
    val defaultPattern: StateFlow<BreathingPattern> =
        settings.defaultPattern.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BreathingPattern.DEFAULT)
}
