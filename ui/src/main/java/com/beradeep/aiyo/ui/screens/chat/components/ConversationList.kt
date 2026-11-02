package com.beradeep.aiyo.ui.screens.chat.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.beradeep.aiyo.domain.model.Conversation
import com.beradeep.aiyo.ui.AiyoTheme
import com.beradeep.aiyo.ui.LocalTypography
import com.beradeep.aiyo.ui.basics.components.Surface
import com.beradeep.aiyo.ui.basics.components.Text
import com.beradeep.aiyo.ui.screens.chat.ConversationFilter
import com.beradeep.aiyo.ui.screens.chat.ListAutoScrollToTop

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColumnScope.ConversationList(
    conversations: List<Conversation>,
    modifier: Modifier = Modifier,
    onConversationSelected: (Conversation) -> Unit,
    selectedConversation: Conversation?,
    conversationFilter: ConversationFilter = ConversationFilter.RECENT,
    onConversationFilterSelected: (ConversationFilter) -> Unit = {}
) {
    val lazyListState = rememberLazyListState()

    ConversationFilterSelector(
        selectedFilter = conversationFilter,
        onFilterSelected = onConversationFilterSelected,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    )
    Spacer(Modifier.height(12.dp))

    ListAutoScrollToTop(0, lazyListState)

    LazyColumn(
        modifier = modifier,
        state = lazyListState
    ) {
        items(conversations, key = { it.id }) { conversation ->
            val selected = selectedConversation?.id == conversation.id
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 3.dp)
                    .clickable { onConversationSelected(conversation) },
                color = if (selected) {
                    AiyoTheme.colors.tertiary.copy(alpha = 0.12f)
                } else {
                    AiyoTheme.colors.transparent
                },
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.title,
                        style = LocalTypography.current.body2,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (selected) {
                            AiyoTheme.colors.tertiary
                        } else {
                            AiyoTheme.colors.text
                        }
                    )
                }
            }
        }
    }
}
