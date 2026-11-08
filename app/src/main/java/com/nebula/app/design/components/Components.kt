package com.nebula.app.design.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nebula.app.design.theme.NebulaColors
import com.nebula.app.domain.models.Game
import com.nebula.app.domain.models.GameAvailability

// ---------------------------------------------------------------- Cards

@Composable
fun NebulaCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val border = BorderStroke(1.dp, NebulaColors.Border)
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            color = NebulaColors.Surface,
            border = border
        ) { Column(content = content) }
    } else {
        Surface(
            modifier = modifier,
            shape = shape,
            color = NebulaColors.Surface,
            border = border
        ) { Column(content = content) }
    }
}

// ---------------------------------------------------------------- Buttons

enum class ButtonKind { Primary, Accent, Secondary, Danger }

@Composable
fun NebulaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null
) {
    val container = when (kind) {
        ButtonKind.Primary -> NebulaColors.Ink
        ButtonKind.Accent -> NebulaColors.Accent
        ButtonKind.Secondary -> NebulaColors.Surface
        ButtonKind.Danger -> NebulaColors.Surface
    }
    val content = when (kind) {
        ButtonKind.Primary, ButtonKind.Accent -> Color.White
        ButtonKind.Secondary -> NebulaColors.Ink
        ButtonKind.Danger -> NebulaColors.Error
    }
    val border = when (kind) {
        ButtonKind.Secondary -> BorderStroke(1.dp, NebulaColors.Border)
        ButtonKind.Danger -> BorderStroke(1.dp, NebulaColors.Error.copy(alpha = 0.4f))
        else -> null
    }
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp),
        enabled = enabled && !loading,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = if (loading) container else NebulaColors.Subtle,
            disabledContentColor = if (loading) content else NebulaColors.Secondary
        ),
        border = border,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = content,
                strokeWidth = 2.dp
            )
        } else {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ---------------------------------------------------------------- Inputs

@Composable
fun NebulaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    supportingText: String? = null,
    leadingIcon: ImageVector? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    isPassword: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    onImeAction: (() -> Unit)? = null
) {
    val focusManager = LocalFocusManager.current
    var passwordVisible by remember { mutableStateOf(false) }
    val hint = error ?: supportingText

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        enabled = enabled,
        readOnly = readOnly,
        isError = error != null,
        singleLine = singleLine,
        minLines = minLines,
        shape = RoundedCornerShape(14.dp),
        leadingIcon = if (leadingIcon != null) {
            { Icon(leadingIcon, contentDescription = null) }
        } else null,
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                    )
                }
            }
        } else null,
        supportingText = if (hint != null) {
            { Text(hint) }
        } else null,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(
            onNext = { focusManager.moveFocus(FocusDirection.Down) },
            onDone = {
                focusManager.clearFocus()
                onImeAction?.invoke()
            },
            onSearch = {
                focusManager.clearFocus()
                onImeAction?.invoke()
            },
            onSend = {
                onImeAction?.invoke()
            }
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NebulaColors.Accent,
            unfocusedBorderColor = NebulaColors.Border,
            errorBorderColor = NebulaColors.Error,
            focusedContainerColor = NebulaColors.Surface,
            unfocusedContainerColor = NebulaColors.Surface,
            disabledContainerColor = NebulaColors.Subtle,
            focusedLabelColor = NebulaColors.Accent,
            cursorColor = NebulaColors.Accent
        )
    )
}

// ---------------------------------------------------------------- Chips and banners

enum class ChipTone { Neutral, Accent, Success, Warning, Danger }

@Composable
fun StatusChip(text: String, tone: ChipTone = ChipTone.Neutral, modifier: Modifier = Modifier) {
    val (bg, fg) = when (tone) {
        ChipTone.Neutral -> NebulaColors.Subtle to NebulaColors.Secondary
        ChipTone.Accent -> NebulaColors.AccentSoft to NebulaColors.AccentPressed
        ChipTone.Success -> NebulaColors.SuccessSoft to NebulaColors.Success
        ChipTone.Warning -> NebulaColors.WarningSoft to NebulaColors.Warning
        ChipTone.Danger -> NebulaColors.ErrorSoft to NebulaColors.Error
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, color = fg, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

@Composable
fun DemoBadge(modifier: Modifier = Modifier, text: String = "Simulated demo") {
    StatusChip(text = text, tone = ChipTone.Accent, modifier = modifier)
}

@Composable
fun InfoBanner(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tone: ChipTone = ChipTone.Accent
) {
    val (bg, fg) = when (tone) {
        ChipTone.Warning -> NebulaColors.WarningSoft to NebulaColors.Warning
        ChipTone.Danger -> NebulaColors.ErrorSoft to NebulaColors.Error
        ChipTone.Success -> NebulaColors.SuccessSoft to NebulaColors.Success
        else -> NebulaColors.AccentSoft to NebulaColors.AccentPressed
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
        Text(text, color = fg, style = MaterialTheme.typography.bodyMedium)
    }
}

// ---------------------------------------------------------------- Headers

@Composable
fun NebulaTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        } else {
            Spacer(Modifier.width(12.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        actions()
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(actionLabel, color = NebulaColors.Accent, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun SettingsRow(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val rowModifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier
    Row(
        modifier = rowModifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(NebulaColors.Subtle),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = NebulaColors.Secondary)
            }
        }
        if (trailing != null) {
            trailing()
        } else if (onClick != null) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = NebulaColors.Secondary
            )
        }
    }
}

@Composable
fun RowDivider() {
    HorizontalDivider(color = NebulaColors.Border, modifier = Modifier.padding(start = 68.dp))
}

// ---------------------------------------------------------------- States

@Composable
fun ShimmerBox(modifier: Modifier = Modifier, shape: RoundedCornerShape = RoundedCornerShape(16.dp)) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "shimmerAlpha"
    )
    Box(
        modifier = modifier
            .alpha(alpha)
            .clip(shape)
            .background(NebulaColors.Subtle)
    )
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = NebulaColors.Accent)
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(NebulaColors.Subtle),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = NebulaColors.Secondary, modifier = Modifier.size(30.dp))
        }
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = NebulaColors.Secondary,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.size(6.dp))
            NebulaButton(text = actionLabel, onClick = onAction, kind = ButtonKind.Secondary)
        }
    }
}

@Composable
fun ErrorState(
    message: String,
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    EmptyState(
        icon = Icons.Filled.CloudOff,
        title = "Something went wrong",
        message = message,
        modifier = modifier,
        actionLabel = if (onRetry != null) "Try again" else null,
        onAction = onRetry
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissText: String = "Cancel",
    destructive: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NebulaColors.Surface,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium, color = NebulaColors.Secondary) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    confirmText,
                    fontWeight = FontWeight.SemiBold,
                    color = if (destructive) NebulaColors.Error else NebulaColors.Accent
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismissText, color = NebulaColors.Secondary) }
        }
    )
}

// ---------------------------------------------------------------- Games

/** Deterministic dark gradient so a missing image still looks intentional. */
private fun fallbackBrush(seed: String): Brush {
    val hue = (seed.hashCode().mod(360)).toFloat()
    return Brush.linearGradient(
        listOf(Color.hsv(hue, 0.35f, 0.42f), Color.hsv((hue + 40f) % 360f, 0.45f, 0.18f))
    )
}

@Composable
fun GameImage(url: String, title: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(fallbackBrush(title))) {
        AsyncImage(
            model = url,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

fun availabilityTone(availability: GameAvailability): ChipTone = when (availability) {
    GameAvailability.Available -> ChipTone.Success
    GameAvailability.HighDemand -> ChipTone.Warning
    GameAvailability.ComingSoon -> ChipTone.Neutral
}

@Composable
fun GameTile(
    game: Game,
    isFavourite: Boolean,
    onClick: () -> Unit,
    onToggleFavourite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(16.dp))
        ) {
            GameImage(game.coverUrl, game.title, Modifier.fillMaxSize())
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.55f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.55f)
                        )
                    )
            )
            IconButton(
                onClick = onToggleFavourite,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.9f))
            ) {
                Icon(
                    imageVector = if (isFavourite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (isFavourite) "Remove from favourites" else "Add to favourites",
                    tint = if (isFavourite) NebulaColors.Accent else NebulaColors.Ink,
                    modifier = Modifier.size(20.dp)
                )
            }
            if (game.availability != GameAvailability.Available) {
                StatusChip(
                    text = game.availability.label,
                    tone = availabilityTone(game.availability),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                )
            }
        }
        Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
            Text(
                game.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                game.studio,
                style = MaterialTheme.typography.bodySmall,
                color = NebulaColors.Secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun GameHeroCard(game: Game, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        GameImage(game.heroUrl, game.title, Modifier.fillMaxSize())
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.35f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.7f)
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text(game.title, color = Color.White, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(game.genre + " - " + game.studio, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
