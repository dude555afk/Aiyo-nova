package com.beradeep.aiyo.ui.screens.chat.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beradeep.aiyo.domain.model.Reason
import com.beradeep.aiyo.ui.AiyoTheme
import com.beradeep.aiyo.ui.R
import com.beradeep.aiyo.ui.basics.components.Text
import com.beradeep.aiyo.ui.basics.components.Tooltip
import com.beradeep.aiyo.ui.basics.components.TooltipBox
import com.beradeep.aiyo.ui.basics.components.rememberTooltipPositionProvider
import com.beradeep.aiyo.ui.basics.components.rememberTooltipState
import kotlinx.coroutines.launch

@Composable
fun RowScope.ChatInputTextField(
    value: String,
    reasonEffort: Reason,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onStopRequest: () -> Unit,
    onWebSearch: () -> Unit,
    onReason: (Reason) -> Unit,
    onAttach: () -> Unit,
    isWebSearchEnabled: Boolean,
    isLoadingOrStreamingResponse: Boolean,
    fontSize: Int
) {
    val tooltipState = rememberTooltipState(isPersistent = true)
    val scope = rememberCoroutineScope()
    val inputTextStyle = TextStyle(
        color = AiyoTheme.colors.text,
        fontSize = fontSize.sp,
        lineHeight = (fontSize + 8).sp
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = AiyoTheme.colors.surface,
        shape = RoundedCornerShape(28.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            AiyoTheme.colors.outline.copy(alpha = 0.72f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 7.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 9.dp),
                textStyle = inputTextStyle,
                cursorBrush = SolidColor(AiyoTheme.colors.tertiary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                maxLines = 6,
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isBlank()) {
                            Text(
                                text = "Message Aiyo...",
                                style = inputTextStyle.copy(
                                    color = AiyoTheme.colors.textSecondary
                                )
                            )
                        }
                        innerTextField()
                    }
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onAttach,
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Attach image or file",
                        tint = AiyoTheme.colors.text
                    )
                }

                ComposerToggle(
                    label = "Search",
                    selected = isWebSearchEnabled,
                    onClick = onWebSearch
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Language,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                        tint = if (isWebSearchEnabled) AiyoTheme.colors.tertiary else AiyoTheme.colors.textSecondary
                    )
                }

                TooltipBox(
                    state = tooltipState,
                    positionProvider = rememberTooltipPositionProvider(12.dp),
                    tooltip = {
                        Tooltip(shape = RoundedCornerShape(16.dp)) {
                            Column {
                                Reason.entries.forEach { effort ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onReason(effort)
                                                tooltipState.dismiss()
                                            }
                                            .padding(horizontal = 18.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = effort.name,
                                            color = if (effort == reasonEffort) AiyoTheme.colors.tertiary else AiyoTheme.colors.text
                                        )
                                    }
                                }
                            }
                        }
                    }
                ) {
                    ComposerToggle(
                        label = if (reasonEffort == Reason.None) "Think" else "Think · ${reasonEffort.name}",
                        selected = reasonEffort != Reason.None,
                        onClick = { scope.launch { tooltipState.show() } }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.cognition_24px),
                            contentDescription = null,
                            modifier = Modifier.size(17.dp),
                            tint = if (reasonEffort == Reason.None) AiyoTheme.colors.textSecondary else AiyoTheme.colors.tertiary
                        )
                    }
                }

                androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))

                FilledIconButton(
                    onClick = if (isLoadingOrStreamingResponse) onStopRequest else onSend,
                    enabled = isLoadingOrStreamingResponse || value.isNotBlank(),
                    modifier = Modifier.size(42.dp),
                    shape = CircleShape
                ) {
                    AnimatedContent(targetState = isLoadingOrStreamingResponse, label = "composer-send-state") { loading ->
                        if (loading) {
                            Icon(
                                imageVector = Icons.Rounded.Stop,
                                contentDescription = "Stop response"
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Send,
                                contentDescription = "Send message"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ComposerToggle(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(
                if (selected) AiyoTheme.colors.tertiary.copy(alpha = 0.13f) else AiyoTheme.colors.transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        androidx.compose.foundation.layout.Spacer(Modifier.size(6.dp))
        Text(
            text = label,
            color = if (selected) AiyoTheme.colors.tertiary else AiyoTheme.colors.textSecondary,
            style = AiyoTheme.typography.body3
        )
    }
}
