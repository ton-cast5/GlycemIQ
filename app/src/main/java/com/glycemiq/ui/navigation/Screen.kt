package com.glycemiq.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Home : Screen("home", "Inicio", Icons.Filled.Home, Icons.Outlined.Home)
    data object Glucose : Screen("glucose", "Glucosa", Icons.Filled.Add, Icons.Outlined.Add)
    data object Medications : Screen("medications", "Medicamentos", Icons.Filled.Medication, Icons.Outlined.Medication)
    data object Charts : Screen("charts", "Gráficas", Icons.Filled.ShowChart, Icons.Outlined.ShowChart)
    data object Report : Screen("report", "Reporte", Icons.Filled.Assessment, Icons.Outlined.Assessment)

    companion object {
        val bottomNavItems = listOf(Home, Glucose, Medications, Charts, Report)
    }
}
