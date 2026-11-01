package com.beradeep.aiyo.ui.screens.chat

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.window.PopupPositionProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.beradeep.aiyo.domain.model.MessageAttachment
import com.beradeep.aiyo.ui.AiyoTheme
import com.beradeep.aiyo.ui.LocalTypography
import com.beradeep.aiyo.ui.R
import com.beradeep.aiyo.ui.basics.components.Button
import com.beradeep.aiyo.ui.basics.components.ButtonVariant
import com.beradeep.aiyo.ui.basics.components.HorizontalDivider
import com.beradeep.aiyo.ui.basics.components.Icon
import com.beradeep.aiyo.ui.basics.components.IconButton
import com.beradeep.aiyo.ui.basics.components.IconButtonVariant
import com.beradeep.aiyo.ui.basics.components.Scaffold
import com.beradeep.aiyo.ui.basics.components.Text
import com.beradeep.aiyo.ui.basics.components.Tooltip
import com.beradeep.aiyo.ui.basics.components.TooltipBox
import com.beradeep.aiyo.ui.basics.components.rememberTooltipState
import com.beradeep.aiyo.ui.basics.components.topbar.TopBar
import com.beradeep.aiyo.ui.screens.chat.components.ChatInputTextField
import com.beradeep.aiyo.ui.screens.chat.components.ChatOptions
import com.beradeep.aiyo.ui.screens.chat.components.ConversationList
import com.beradeep.aiyo.ui.screens.chat.components.DeleteConversationDialog
import com.beradeep.aiyo.ui.screens.chat.components.DotsTyping
import com.beradeep.aiyo.ui.screens.chat.components.MessageBubble
import com.beradeep.aiyo.ui.screens.chat.components.ModelSelectorChip
import com.beradeep.aiyo.ui.screens.chat.components.RenameConversationDialog
import com.beradeep.aiyo.ui.screens.components.ModelSelectionSheet
import com.mikepenz.markdown.model.State
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(viewModel: ChatViewModel, onNavigateToSettings: () -> Unit) {
    val chatUiState by viewModel.uiState.collectAsStateWithLifecycle()
    ChatScreen(
        uiState = chatUiState,
        onUiEvent = viewModel::onUiEvent,
        onNavigateToSettings = onNavigateToSettings
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChatScreen(
    modifier: Modifier = Modifier,
    uiState: ChatUiState,
    onUiEvent: (ChatUiEvent) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val attachmentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        val attachments = uris.map { uri ->
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            MessageAttachment(
                uri = uri.toString(),
                name = attachmentDisplayName(context, uri),
                mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
            )
        }
        if (attachments.isNotEmpty()) {
            onUiEvent(ChatUiEvent.OnAttachmentsSelected(attachments))
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxWidth(0.8f),
                drawerContainerColor = AiyoTheme.colors.surface,
                drawerContentColor = AiyoTheme.colors.onSurface
            ) {
                Column(
                    Modifier.systemBarsPadding()
                ) {
                    Button(
                        modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clip(CircleShape),
                        onClick = {
                            onUiEvent(ChatUiEvent.OnNewChat)
                            coroutineScope.launch { drawerState.close() }
                        },
                        variant = ButtonVariant.PrimaryElevated
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Chat)
                            Spacer(Modifier.width(12.dp))
                            Text(text = "New Chat")
                        }
                    }

                    ConversationList(
                        conversations = uiState.conversations,
                        modifier = Modifier.weight(1f),
                        selectedConversation = uiState.selectedConversation,
                        onConversationSelected = { conversation ->
                            onUiEvent(ChatUiEvent.OnConversationSelected(conversation))
                            coroutineScope.launch { drawerState.close() }
                        },
                        conversationFilter = uiState.conversationFilter,
                        onConversationFilterSelected = { filter ->
                            onUiEvent(ChatUiEvent.OnConversationFilterSelected(filter))
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        color = AiyoTheme.colors.disabled
                    )

                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                coroutineScope.launch {
                                    drawerState.close()
                                    onNavigateToSettings()
                                }
                            }
                    ) {
                        Spacer(Modifier.height(24.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Settings", style = LocalTypography.current.h4)
                            Icon(Icons.Filled.Settings)
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    ) {
        var showRenameConversationDialog by remember { mutableStateOf(false) }
        var showDeleteConversationDialog by remember { mutableStateOf(false) }
        var showModelSheet by remember { mutableStateOf(false) }

        Scaffold(
            modifier = modifier,
            topBar = {
                TopBar {
                    Row(
                        modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    drawerState.open()
                                }
                            },
                            enabled = drawerState.isClosed || drawerState.isAnimationRunning,
                            variant = IconButtonVariant.PrimaryGhost
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Menu,
                                contentDescription = "Open Menu"
                            )
                        }
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            ModelSelectorChip(
                                model = uiState.selectedModel,
                                onClick = { showModelSheet = true }
                            )
                        }
                        uiState.selectedConversation?.let { conversation ->
                            ChatOptions(
                                conversation = conversation,
                                onClickDelete = { showDeleteConversationDialog = true },
                                onClickRename = { showRenameConversationDialog = true },
                                onClickStar = { onUiEvent(ChatUiEvent.OnUpdateConversation(it)) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            val focusManager = LocalFocusManager.current

            Box(
                Modifier.fillMaxSize()
            ) {
                Column(
                    modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .consumeWindowInsets(WindowInsets.systemBars)
                        .imePadding()
                        .padding(8.dp)
                        .pointerInput(Unit) {
                            detectTapGestures {
                                focusManager.clearFocus()
                            }
                        },
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    val lazyListState = rememberLazyListState()
                    ListAutoScrollToBottom(2, lazyListState)

                    var longPressX by remember { mutableIntStateOf(0) }
                    var longPressY by remember { mutableIntStateOf(0) }
                    val positionProvider = tooltipPositionProvider(longPressX, longPressY)
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        state = lazyListState
                    ) {
                        items(
                            items = uiState.messages,
                            key = { it.id }
                        ) { msg ->
                            Spacer(modifier = Modifier.height(8.dp))
                            val tooltipState = rememberTooltipState(isPersistent = true)
                            TooltipBox(
                                state = tooltipState,
                                positionProvider = positionProvider,
                                tooltip = {
                                    val clipboard = LocalClipboard.current
                                    Tooltip(
                                        caretSize = DpSize(0.dp, 0.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Button(
                                            variant = ButtonVariant.PrimaryGhost,
                                            onClick = {
                                                clipboard.nativeClipboard.setPrimaryClip(
                                                    ClipData.newPlainText(
                                                        "Copied from Aiyo",
                                                        msg.content ?: ""
                                                    )
                                                )
                                                tooltipState.dismiss()
                                            }
                                        ) {
                                            Row {
                                                Icon(imageVector = Icons.Rounded.ContentCopy)
                                                Spacer(Modifier.width(16.dp))
                                                Text(text = "Copy")
                                            }
                                        }
                                    }
                                }
                            ) {
                                val scope = rememberCoroutineScope()
                                Box(
                                    Modifier
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onLongPress = { offset ->
                                                    scope.launch {
                                                        longPressX = offset.x.fastRoundToInt()
                                                        longPressY = offset.y.fastRoundToInt()
                                                        tooltipState.show()
                                                    }
                                                }
                                            )
                                        }
                                ) {
                                    Column {
                                        if (msg.attachments.isNotEmpty()) {
                                            Text(
                                                text = msg.attachments.joinToString(
                                                    separator = " · ",
                                                    prefix = "📎 "
                                                ) { it.name },
                                                color = AiyoTheme.colors.tertiary
                                            )
                                        }
                                        MessageBubble(
                                            content = msg.content.toString(),
                                            isUser = msg.isUser,
                                            markdownState = msg.markdownState,
                                            fontSize =
                                            if (msg.isUser) {
                                                uiState.requestFontSize
                                            } else {
                                                uiState.responseFontSize
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        if (uiState.isLoadingResponse) {
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                DotsTyping()
                            }
                        }
                        uiState.streamingResponse?.let { content ->
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                MessageBubble(
                                    content = content,
                                    isUser = false,
                                    markdownState = State.Loading(),
                                    fontSize = uiState.responseFontSize
                                )
                            }
                        }
                    }

                    if (uiState.pendingAttachments.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            uiState.pendingAttachments.forEach { attachment ->
                                Row(
                                    modifier = Modifier
                                        .padding(end = 6.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(AiyoTheme.colors.surface)
                                        .padding(start = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = attachment.name, color = AiyoTheme.colors.onSurface)
                                    IconButton(
                                        onClick = {
                                            onUiEvent(ChatUiEvent.OnAttachmentRemoved(attachment.uri))
                                        },
                                        variant = IconButtonVariant.PrimaryGhost
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Remove attachment"
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { attachmentPicker.launch(arrayOf("*/*")) },
                            variant = IconButtonVariant.PrimaryGhost
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Attach image or file"
                            )
                        }
                        ChatInputTextField(
                            value = uiState.inputText,
                            onValueChange = { onUiEvent(ChatUiEvent.OnInputTextEdit(it)) },
                            onSend = { onUiEvent(ChatUiEvent.OnMessageSend) },
                            onWebSearch = { onUiEvent(ChatUiEvent.OnWebSearchTapped) },
                            isWebSearchEnabled = uiState.isWebSearchEnabled,
                            isLoadingOrStreamingResponse =
                            uiState.isLoadingResponse || uiState.isStreamingResponse,
                            onReason = { onUiEvent(ChatUiEvent.OnReason(it)) },
                            reasonEffort = uiState.reasoningEffort,
                            onStopRequest = { onUiEvent(ChatUiEvent.OnStopRequest) },
                            fontSize = uiState.requestFontSize
                        )
                    }
                }

                val isKeyBoardVisible = WindowInsets.isImeVisible
                if (uiState.messages.isEmpty() && !isKeyBoardVisible) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Image(
                            modifier = Modifier.size(240.dp),
                            painter = painterResource(id = R.drawable.aiyo_icon),
                            contentDescription = "Aiyo Icon"
                        )
                        Text(
                            text = "Hello there! How can I assist you today?",
                            style = AiyoTheme.typography.body1,
                            color = AiyoTheme.colors.tertiary,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }
            }
        }

        ModelSelectionSheet(
            isVisible = showModelSheet,
            models = uiState.models,
            selectedModel = uiState.selectedModel,
            onModelSelected = { model ->
                onUiEvent(ChatUiEvent.OnModelSelected(model))
                showModelSheet = false
            },
            isFetchingModels = uiState.isFetchingModels,
            fetchModels = { onUiEvent(ChatUiEvent.OnFetchModels) },
            onDismiss = { showModelSheet = false }
        )

        if (showRenameConversationDialog && uiState.selectedConversation != null) {
            RenameConversationDialog(
                initial = uiState.selectedConversation.title,
                onSave = { newTitle ->
                    onUiEvent(
                        ChatUiEvent.OnUpdateConversation(
                            uiState.selectedConversation.copy(title = newTitle)
                        )
                    )
                    showRenameConversationDialog = false
                },
                onDismiss = { showRenameConversationDialog = false }
            )
        }

        if (showDeleteConversationDialog && uiState.selectedConversation != null) {
            DeleteConversationDialog(
                onDelete = {
                    onUiEvent(ChatUiEvent.OnDeleteConversation(uiState.selectedConversation))
                    showDeleteConversationDialog = false
                },
                onDismiss = { showDeleteConversationDialog = false }
            )
        }
    }
}

@Composable
fun tooltipPositionProvider(x: Int, y: Int): PopupPositionProvider {
    return remember(x, y) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset {
                val x1 = anchorBounds.left + x
                val y1 = anchorBounds.top + y
                return IntOffset(x1, y1)
            }
        }
    }
}

private fun attachmentDisplayName(context: Context, uri: Uri): String {
    val fromProvider = runCatching {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        ).use { cursor ->
            if (cursor != null && cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull()

    return fromProvider?.takeIf { it.isNotBlank() }
        ?: uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
        ?: "Attachment"
}
