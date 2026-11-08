package com.nebula.app.design.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nebula.app.R
import com.nebula.app.design.theme.NebulaColors

/** The Nebula controller logo (res/drawable/nebula_logo.xml), tinted. [size] is the height of the mark. */
@Composable
fun NebulaMark(modifier: Modifier = Modifier, size: Dp = 28.dp, color: Color = NebulaColors.Accent) {
    Icon(
        painter = painterResource(R.drawable.nebula_logo),
        contentDescription = null,
        tint = color,
        modifier = modifier.height(size).width(size * 1.617f)
    )
}

@Composable
fun NebulaWordmark(modifier: Modifier = Modifier, markSize: Dp = 28.dp) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        NebulaMark(size = markSize)
        Spacer(Modifier.width(10.dp))
        Text("Nebula", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}
