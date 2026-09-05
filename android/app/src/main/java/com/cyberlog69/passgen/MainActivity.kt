package com.cyberlog69.passgen

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.cyberlog69.passgen.ui.PassGenViewModel
import com.cyberlog69.passgen.ui.screens.HomeScreen
import com.cyberlog69.passgen.ui.theme.PassGenTheme

class MainActivity : ComponentActivity() {

    private val viewModel: PassGenViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val state by viewModel.uiState.collectAsState()

            // Dynamic Window Security Flag (Blocks screenshots and task switcher previews)
            if (state.isFlagSecureEnabled) {
                window.setFlags(
                    WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE
                )
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }

            PassGenTheme(preset = state.theme) {
                HomeScreen(viewModel = viewModel)
            }
        }
    }
}
