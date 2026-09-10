package com.qianrenni.reading.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.qianrenni.reading.navigation.Bookshelf
import com.qianrenni.reading.navigation.History
import com.qianrenni.reading.navigation.Home
import com.qianrenni.reading.navigation.Navigator
import com.qianrenni.reading.navigation.Profile

data class BottomNavItem(
    val label: String,
    val navKey: NavKey
)

@Composable
fun BottomNavigationBar(navigator: Navigator) {
    val items = listOf(
        BottomNavItem("书城", Home),
        BottomNavItem("书架", Bookshelf),
        BottomNavItem("历史", History),
        BottomNavItem("我的", Profile)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            BottomNavTextItem(
                label = item.label,
                selected = navigator.currentState == item.navKey,
                onClick = { navigator.navigate(item.navKey) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** 纯文字导航项，可复用于其它横向标签栏。 */
@Composable
fun BottomNavTextItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = label,
        modifier = modifier
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(vertical = 12.dp),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyMedium,
        color = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
    )
}
