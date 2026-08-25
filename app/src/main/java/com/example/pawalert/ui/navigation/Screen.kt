package com.example.pawalert.ui.navigation

sealed class Screen(val route: String) {
    data object Feed : Screen("feed")
    data object Report : Screen("report")
    data object Auth : Screen("auth")
    data object Detail : Screen("detail/{reportId}") {
        fun createRoute(reportId: String): String = "detail/$reportId"
    }
}
