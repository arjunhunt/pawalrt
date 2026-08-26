package com.example.pawalert

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.pawalert.ui.navigation.PawAlertNavGraph
import com.example.pawalert.ui.navigation.Screen
import com.example.pawalert.ui.theme.PawAlertTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            enableEdgeToEdge()
        } catch (_: Throwable) {}

        setContent {
            PawAlertTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val startDestination = try {
                        val auth = FirebaseAuth.getInstance()
                        if (auth.currentUser != null) Screen.Feed.route else Screen.Auth.route
                    } catch (_: Throwable) {
                        Screen.Auth.route
                    }

                    PawAlertNavGraph(
                        navController = navController,
                        startDestination = startDestination
                    )
                }
            }
        }
    }
}
