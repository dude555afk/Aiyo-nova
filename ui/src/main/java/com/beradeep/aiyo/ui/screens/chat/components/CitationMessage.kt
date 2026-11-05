package com.beradeep.aiyo.ui.screens.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text as MaterialText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.beradeep.aiyo.ui.AiyoTheme
import com.beradeep.aiyo.ui.LocalTypography
import com.mikepenz.markdown.model.State
import com.mikepenz.markdown.model.rememberMarkdownState

@Composable
fun CitationMessage(
    content: String,
    markdownState: State,
    fontSize: Int
) {
    val presentation = remember(content) { presentCitations(content) }
    val parsedMarkdown = rememberMarkdownState(content = presentation.markdown)
    val parsedState by parsedMarkdown.state.collectAsStateWithLifecycle()
    val visibleMarkdownState = if (presentation.markdown == content) markdownState else parsedState

    Column(modifier = Modifier.fillMaxWidth()) {
        MessageBubble(
            content = presentation.markdown,
            isUser = false,
            markdownState = visibleMarkdownState,
            fontSize = fontSize
        )

        if (presentation.sources.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                MaterialText(
                    text = "Sources",
                    style = LocalTypography.current.label1,
                    color = AiyoTheme.colors.textSecondary,
                    modifier = Modifier.padding(top = 2.dp, bottom = 1.dp)
                )
                presentation.sources.forEach { source ->
                    SourceCard(source)
                }
            }
        }
    }
}

@Composable
private fun SourceCard(source: CitationSource) {
    val uriHandler = LocalUriHandler.current
    val shape = RoundedCornerShape(15.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AiyoTheme.colors.surface)
            .border(
                width = 1.dp,
                color = AiyoTheme.colors.disabled.copy(alpha = 0.45f),
                shape = shape
            )
            .clickable {
                runCatching { uriHandler.openUri(source.url) }
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(AiyoTheme.colors.tertiary.copy(alpha = 0.13f)),
            contentAlignment = Alignment.Center
        ) {
            MaterialText(
                text = source.domain.firstOrNull()?.uppercase() ?: "↗",
                style = LocalTypography.current.body1,
                color = AiyoTheme.colors.tertiary
            )
        }
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            MaterialText(
                text = source.title,
                style = LocalTypography.current.body3,
                color = AiyoTheme.colors.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            MaterialText(
                text = source.domain,
                style = LocalTypography.current.label1,
                color = AiyoTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(10.dp))
        MaterialText(
            text = "[" + source.number + "]",
            style = LocalTypography.current.label1,
            color = AiyoTheme.colors.textSecondary
        )
    }
}
