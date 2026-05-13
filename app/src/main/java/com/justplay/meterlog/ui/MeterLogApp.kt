package com.justplay.meterlog.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.justplay.meterlog.data.MeterType
import com.justplay.meterlog.ui.screens.AuthScreen
import com.justplay.meterlog.ui.screens.BuildingDetailScreen
import com.justplay.meterlog.ui.screens.BuildingFormScreen
import com.justplay.meterlog.ui.screens.BuildingListScreen
import com.justplay.meterlog.ui.screens.BuildingReadingScreen
import com.justplay.meterlog.ui.screens.MeterDetailScreen
import com.justplay.meterlog.ui.screens.MeterEditScreen
import com.justplay.meterlog.ui.viewmodel.AuthViewModel

@Composable
fun MeterLogApp(authViewModel: AuthViewModel = viewModel()) {
    val uid by authViewModel.uid.collectAsStateWithLifecycle()
    if (uid == null) {
        AuthScreen(authViewModel = authViewModel)
        return
    }

    val currentUid = uid ?: return
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "buildings") {
        composable("buildings") {
            BuildingListScreen(
                uid = currentUid,
                onAddBuilding = { navController.navigate("building-form") },
                onOpenBuilding = { navController.navigate("buildings/$it") },
                onSignOut = authViewModel::signOut
            )
        }
        composable("building-form") {
            BuildingFormScreen(
                uid = currentUid,
                onBack = navController::popBackStack,
                onSaved = navController::popBackStack
            )
        }
        composable(
            route = "buildings/{buildingId}",
            arguments = listOf(navArgument("buildingId") { type = NavType.StringType })
        ) { entry ->
            val buildingId = entry.arguments?.getString("buildingId").orEmpty()
            BuildingDetailScreen(
                uid = currentUid,
                buildingId = buildingId,
                onBack = navController::popBackStack,
                onOpenMeter = { meterId -> navController.navigate("buildings/$buildingId/meters/$meterId") },
                onBatchReading = { meterType -> navController.navigate("buildings/$buildingId/readings/new/${meterType.name}") }
            )
        }
        composable(
            route = "buildings/{buildingId}/meters/{meterId}",
            arguments = listOf(
                navArgument("buildingId") { type = NavType.StringType },
                navArgument("meterId") { type = NavType.StringType }
            )
        ) { entry ->
            val buildingId = entry.arguments?.getString("buildingId").orEmpty()
            val meterId = entry.arguments?.getString("meterId").orEmpty()
            MeterDetailScreen(
                uid = currentUid,
                buildingId = buildingId,
                meterId = meterId,
                onBack = navController::popBackStack,
                onEditMeter = { navController.navigate("buildings/$buildingId/meters/$meterId/edit") }
            )
        }
        composable(
            route = "buildings/{buildingId}/meters/{meterId}/edit",
            arguments = listOf(
                navArgument("buildingId") { type = NavType.StringType },
                navArgument("meterId") { type = NavType.StringType }
            )
        ) { entry ->
            val buildingId = entry.arguments?.getString("buildingId").orEmpty()
            val meterId = entry.arguments?.getString("meterId").orEmpty()
            MeterEditScreen(
                uid = currentUid,
                buildingId = buildingId,
                meterId = meterId,
                onBack = navController::popBackStack,
                onSaved = navController::popBackStack
            )
        }
        composable(
            route = "buildings/{buildingId}/readings/new/{meterType}",
            arguments = listOf(
                navArgument("buildingId") { type = NavType.StringType },
                navArgument("meterType") { type = NavType.StringType }
            )
        ) { entry ->
            val buildingId = entry.arguments?.getString("buildingId").orEmpty()
            val meterType = MeterType.fromName(entry.arguments?.getString("meterType"))
            BuildingReadingScreen(
                uid = currentUid,
                buildingId = buildingId,
                meterType = meterType,
                onBack = navController::popBackStack,
                onSaved = navController::popBackStack
            )
        }
    }
}
