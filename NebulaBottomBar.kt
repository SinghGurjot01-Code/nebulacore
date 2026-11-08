package com.nebula.app.design.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.nebula.app.core.navigation.TopLevelDestination
import com.nebula.app.design.theme.NebulaColors

@Composable
fun NebulaBottomBar(
    destinations: List<TopLevelDestination>,
    currentRoute: String?,
    onSelect: (String) -> Unit
) {
    Column {
        HorizontalDivider(color = NebulaColors.Border)
        NavigationBar(containerColor = NebulaColors.Surface, tonalElevation = androidx.compose.ui.unit.Dp.Hairline) {
            destinations.forEach { destination ->
                val selected = destination.route == currentRoute
                NavigationBarItem(
                    selected = selected,
                    onClick = { if (!selected) onSelect(destination.route) },
                    icon = {
                        Icon(
                            imageVector = if (selected) destination.selectedIcon else destination.icon,
                            contentDescription = destination.label
                        )
                    },
                    label = { Text(destination.label) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NebulaColors.Accent,
                        selectedTextColor = NebulaColors.Accent,
                        indicatorColor = NebulaColors.AccentSoft,
                        unselectedIconColor = NebulaColors.Secondary,
                        unselectedTextColor = NebulaColors.Secondary
                    )
                )
            }
        }
    }
}
