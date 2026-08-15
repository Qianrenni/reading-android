package com.qianrenni.reading

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.qianrenni.reading.data.store.AuthStore
import com.qianrenni.reading.ui.theme.ReadingTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AuthStore.init(this)

            ReadingTheme {
                AppNavigation(this)
            }
        }
    }
}