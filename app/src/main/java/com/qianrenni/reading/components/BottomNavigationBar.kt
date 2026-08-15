package com.qianrenni.reading.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import com.qianrenni.reading.navigation.Bookshelf
import com.qianrenni.reading.navigation.History
import com.qianrenni.reading.navigation.Home
import com.qianrenni.reading.navigation.Navigator
import com.qianrenni.reading.navigation.Profile

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val navKey: NavKey
)

@Composable
fun BottomNavigationBar(navigator: Navigator) {
    val items = listOf(
        BottomNavItem("书城", Icons.Default.Home, Home),
        BottomNavItem("书架", Icons.AutoMirrored.Filled.LibraryBooks, Bookshelf),
        BottomNavItem("历史", Icons.Default.History, History),
        BottomNavItem("我的", Icons.Default.Person, Profile)
    )

    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = navigator.currentState == item.navKey,
                onClick = {
                    navigator.navigate(item.navKey)
                }
            )
        }
    }
}
