package com.codebyramiz.breathefocus.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.codebyramiz.breathefocus.mobile.ui.HomeRoute
import com.codebyramiz.breathefocus.mobile.ui.theme.BreatheMobileTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { BreatheMobileTheme { HomeRoute() } }
    }
}
