package com.codebyramiz.breathefocus.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.codebyramiz.breathefocus.core.model.BreathingPattern
import com.codebyramiz.breathefocus.wear.ui.WearApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val quickStart = intent.getStringExtra(EXTRA_PATTERN)?.let(BreathingPattern::fromNameOrDefault)
        setContent { WearApp(quickStart = quickStart) }
    }

    companion object {
        /** Set by the tile's Start button. */
        const val EXTRA_PATTERN = "pattern"
    }
}
