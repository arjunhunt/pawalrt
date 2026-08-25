package com.example.pawalert.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.pawalert.ui.auth.AuthScreen
import com.example.pawalert.ui.detail.DetailScreen
import com.example.pawalert.ui.feed.FeedScreen
import com.example.pawalert.ui.report.ReportScreen

@Composable
fun PawAlertNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Feed.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(route = Screen.Feed.route) {
            FeedScreen(
                onNavigateToReport = {
                    navController.navigate(Screen.Report.route)
                },
                onNavigateToDetail = { reportId ->
                    navController.navigate(Screen.Detail.createRoute(reportId))
                },
                onNavigateToAuth = {
                    navController.navigate(Screen.Auth.route)
                }
            )
        }

        composable(route = Screen.Report.route) {
            ReportScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onReportSubmitted = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.Detail.route,
            arguments = listOf(
                navArgument("reportId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val reportId = backStackEntry.arguments?.getString("reportId").orEmpty()
            DetailScreen(
                reportId = reportId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = Screen.Auth.route) {
            AuthScreen(
                onAuthSuccess = {
                    navController.navigate(Screen.Feed.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    }
                }
            )
        }
    }
}
