package com.manoogianmedia.studiorack.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

internal enum class StudioButtonKind { Primary, Secondary, Danger }

@Composable
internal fun StudioButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    kind: StudioButtonKind = StudioButtonKind.Primary,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(50)
    val borderColor = when (kind) {
        StudioButtonKind.Primary -> Color(0xB8FF9D1E)
        StudioButtonKind.Secondary -> Color(0x29FFFFFF)
        StudioButtonKind.Danger -> Color(0xB8E55757)
    }.copy(alpha = if (enabled) 1f else 0.35f)
    val background = when (kind) {
        StudioButtonKind.Primary -> Brush.linearGradient(listOf(Color(0xFFFF9D1E), Color(0xFFFFD16A)))
        StudioButtonKind.Secondary -> Brush.linearGradient(listOf(Color(0x1FFFFFFF), Color(0x0D42D9FF)))
        StudioButtonKind.Danger -> Brush.linearGradient(listOf(Color(0xFFE55757), Color(0xFFA82F3A)))
    }
    Box(
        modifier
            .defaultMinSize(minHeight = 44.dp)
            .clip(shape)
            .background(background, shape)
            .border(BorderStroke(1.dp, borderColor), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center,
        content = content,
    )
}
