package com.aurelle.music.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.aurelle.music.ui.navigation.TopLevelDestination
import com.aurelle.music.ui.theme.AurelleGold
import com.aurelle.music.ui.theme.AurelleGoldLight
import com.aurelle.music.ui.theme.AurelleNavBar
import com.aurelle.music.ui.components.pressScale

/** Dock flutuante de navegação (68dp) com item selecionado animado em formato de pílula. */
@Composable
fun AurelleBottomBar(
    selected: TopLevelDestination,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(34.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .shadow(8.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(shape)
            .background(AurelleNavBar)
            .border(1.5.dp, AurelleGold.copy(alpha = 0.2f), shape)
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .pressScale(scaleOnPress = 0.98f, durationMillis = 100),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TopLevelDestination.entries.forEach { destination ->
            NavItem(
                destination = destination,
                selected = destination == selected,
                onClick = { onSelect(destination) },
            )
        }
    }
}

@Composable
private fun RowScope.NavItem(
    destination: TopLevelDestination,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val pillColor by animateColorAsState(
        targetValue = if (selected) AurelleGold.copy(alpha = 0.25f) else Color.Transparent,
        animationSpec = androidx.compose.animation.core.tween(200),
        label = "navPill",
    )
    val tint by animateColorAsState(
        targetValue = if (selected) AurelleGoldLight else AurelleGold.copy(alpha = 0.70f),
        animationSpec = androidx.compose.animation.core.tween(200),
        label = "navTint",
    )

    Row(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(RoundedCornerShape(26.dp))
            .background(pillColor)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .pressScale(scaleOnPress = 0.95f, durationMillis = 100),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
            contentDescription = stringResource(destination.label),
            tint = tint,
            modifier = Modifier.size(28.dp),
        )
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut() + shrinkHorizontally(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(destination.label),
                    style = MaterialTheme.typography.labelLarge,
                    color = tint,
                    maxLines = 1,
                )
            }
        }
    }
}
