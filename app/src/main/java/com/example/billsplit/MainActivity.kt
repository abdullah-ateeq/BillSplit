package com.example.billsplit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.billsplit.ui.MainScreen
import com.example.billsplit.ui.home.BillSplitViewModel
import com.example.billsplit.ui.theme.BillSplitTheme

class MainActivity : ComponentActivity() {

    private val viewModel: BillSplitViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return BillSplitViewModel(application) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsState()
            BillSplitTheme(
                appTheme = uiState.settings.theme,
                dynamicColor = uiState.settings.dynamicColor
            ) {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}