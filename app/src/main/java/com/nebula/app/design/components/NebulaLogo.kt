package com.nebula.app.design.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nebula.app.design.theme.NebulaColors

@Composable
fun NebulaMark(modifier: Modifier = Modifier, size: Dp = 28.dp, color: Color = NebulaColors.Accent) {
    Canvas(modifier = modifier.size(size)) {
        val radius = this.size.minDimension * 0.34f
        val stroke = this.size.minDimension * 0.13f
        drawCircle(color = color, radius = radius, style = Stroke(width = stroke), center = Offset(this.size.width / 2f, this.size.height / 2f))
        drawCircle(color = color, radius = this.size.minDimension * 0.12f, center = Offset(this.size.width / 2f, this.size.height / 2f))
    }
}

@Composable
fun NebulaWordmark(modifier: Modifier = Modifier, markSize: Dp = 28.dp) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        NebulaMark(size = markSize)
        Spacer(Modifier.width(8.dp))
        Text("Nebula", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}
