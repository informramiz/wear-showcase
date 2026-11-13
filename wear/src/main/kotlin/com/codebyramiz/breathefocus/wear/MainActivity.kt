package com.codebyramiz.breathefocus.wear

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.util.Consumer
import com.codebyramiz.breathefocus.core.model.BreathingPattern
import com.codebyramiz.breathefocus.wear.ui.WearApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** Pending tile request; cleared once the UI has acted on it. */
    private var quickStart by mutableStateOf<BreathingPattern?>(null)

    // The activity is singleTask, so a tile tap while the app is open arrives here, not in onCreate.
    private val newIntentListener = Consumer<Intent> { intent ->
        intent.quickStartPattern()?.let { quickStart = it }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Only a fresh launch reads the intent; after recreation the back stack is already restored.
        if (savedInstanceState == null) quickStart = intent.quickStartPattern()
        addOnNewIntentListener(newIntentListener)
        setContent { WearApp(quickStart = quickStart, onQuickStartHandled = { quickStart = null }) }
    }

    override fun onDestroy() {
        removeOnNewIntentListener(newIntentListener)
        super.onDestroy()
    }

    private fun Intent.quickStartPattern(): BreathingPattern? =
        getStringExtra(EXTRA_PATTERN)?.let(BreathingPattern::fromNameOrDefault)

    companion object {
        /** Set by the tile's Start button. */
        const val EXTRA_PATTERN = "pattern"
    }
}
