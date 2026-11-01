package com.beradeep.aiyo.ui.screens.chat

import android.util.Log
import androidx.collection.mutableIntObjectMapOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beradeep.aiyo.domain.ApiClient
import com.beradeep.aiyo.domain.model.Conversation
import com.beradeep.aiyo.domain.model.Message
import com.beradeep.aiyo.domain.model.MessageAttachment
import com.beradeep.aiyo.domain.model.Model
import com.beradeep.aiyo.domain.model.Role
import com.beradeep.aiyo.domain.repository.ChatRepository
import com.beradeep.aiyo.domain.repository.ModelRepository
import com.beradeep.aiyo.domain.repository.SettingRepository
import com.mikepenz.markdown.model.parseMarkdownFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import java.util.UUID

open class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val modelRepository: ModelRepository,
    private val settingRepository: SettingRepository,
    private val apiClient: ApiClient
) : ViewModel() {
    private val messages = mutableStateListOf<UiMessage>()
    private val conversations = mutableStateListOf<Conversation>()

    private val _uiState = MutableStateFlow(ChatUiState.Default)

    private val messagesFlow = snapshotFlow { messages.toList() }

    private var defaultModel = Model.defaultModel

    val uiState =
        combine(
            _uiState,
            messagesFlow,
            chatRepository.getConversationsFlow()
        ) { state, messages, conversations ->
            state.copy(
                messages = messages,
                conversations = when (_uiState.value.conversationFilter) {
                    ConversationFilter.RECENT -> conversations
                    ConversationFilter.STARRED -> conversations.filter { it.isStarred }
                }
            )
        }.onStart {
            observeFontSizes()
            loadDefaultModel()
            fetchModels()
        }.stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            ChatUiState.Default
        )

    private val preloadJobs = mutableIntObjectMapOf<Job>()
    private var responseJob: Job? = null
    private var isObservingFontSizes = false

    fun onUiEvent(chatUiEvent: ChatUiEvent) {
        when (chatUiEvent) {
            is ChatUiEvent.OnInputTextEdit -> editInputText(chatUiEvent.str)
            is ChatUiEvent.OnMessageSend -> viewModelScope.launch { sendMessage() }
            is ChatUiEvent.OnPreloadMarkdownRequest -> preloadMarkdownForIndex(chatUiEvent.index)
            is ChatUiEvent.OnCancelPreloadMarkdownJobs -> cancelExistingPreloadJobs()
            is ChatUiEvent.OnFetchModels -> viewModelScope.launch { fetchModels() }
            is ChatUiEvent.OnModelSelected -> viewModelScope.launch { selectModel(chatUiEvent.model) }
            is ChatUiEvent.OnConversationSelected -> viewModelScope.launch {
                selectConversation(chatUiEvent.conversation)
            }

            is ChatUiEvent.OnNewChat -> newChat()
            ChatUiEvent.OnWebSearchTapped -> _uiState.update {
                it.copy(isWebSearchEnabled = !it.isWebSearchEnabled)
            }

            is ChatUiEvent.OnAttachmentsSelected -> addAttachments(chatUiEvent.attachments)
            is ChatUiEvent.OnAttachmentRemoved -> removeAttachment(chatUiEvent.uri)

            is ChatUiEvent.OnReason -> _uiState.update {
                it.copy(reasoningEffort = chatUiEvent.reason)
            }

            is ChatUiEvent.OnDeleteConversation -> viewModelScope.launch {
                deleteConversation(chatUiEvent.conversation)
            }

            is ChatUiEvent.OnUpdateConversation -> viewModelScope.launch {
                updateConversation(chatUiEvent.conversation)
            }

            is ChatUiEvent.OnConversationFilterSelected -> _uiState.update {
                it.copy(conversationFilter = chatUiEvent.filter)
            }

            ChatUiEvent.OnStopRequest -> viewModelScope.launch { stopRequest() }
        }
    }

    private suspend fun stopRequest() {
        responseJob?.cancelAndJoin()
        responseJob = null
        val message = Message(UUID.randomUUID(), Role.System, "_Request interrupted by user._")
        messages.add(message.toUiMessage())
        saveMessage(message)
        preloadMarkdownForIndex(messages.lastIndex)
        _uiState.update { it.copy(isLoadingResponse = false, streamingResponse = null) }
    }

    private suspend fun updateConversation(conversation: Conversation) {
        chatRepository.updateConversation(conversation)
        _uiState.update { it.copy(selectedConversation = conversation) }
    }

    private fun editInputText(string: String) {
        _uiState.update { it.copy(inputText = string) }
    }

    private fun observeFontSizes() {
        if (isObservingFontSizes) return
        isObservingFontSizes = true

        viewModelScope.launch {
            settingRepository.getRequestFontSizeFlow().collect { fontSize ->
                _uiState.update { it.copy(requestFontSize = fontSize) }
            }
        }
        viewModelScope.launch {
            settingRepository.getResponseFontSizeFlow().collect { fontSize ->
                _uiState.update { it.copy(responseFontSize = fontSize) }
            }
        }
    }

    private fun loadDefaultModel() {
        viewModelScope.launch {
            defaultModel = modelRepository.getDefaultModel()
            _uiState.update { it.copy(selectedModel = defaultModel) }
        }
    }

    private suspend fun sendMessage() {
        val state = uiState.value
        val text = state.inputText.trim()
        val attachments = state.pendingAttachments
        if ((text.isEmpty() && attachments.isEmpty()) || state.isLoadingResponse) return

        if (state.selectedConversation == null) {
            createNewConversation()
        }

        val displayContent = buildString {
            append(text.ifBlank { "Please analyze the attached file(s)." })
            if (attachments.isNotEmpty()) {
                append("\n\n")
                append(attachments.joinToString("\n") { "📎 " + it.name })
            }
        }
        val message = Message(UUID.randomUUID(), Role.User, displayContent, attachments)
        messages.add(message.toUiMessage())
        _uiState.update {
            it.copy(
                inputText = "",
                pendingAttachments = emptyList(),
                isLoadingResponse = true
            )
        }
        saveMessage(message)

        responseJob = viewModelScope.launch {
            val model = uiState.value.selectedModel
            val useWebSearch = state.isWebSearchEnabled || shouldAutoSearchWeb(text)
            val result = chatRepository.getChatCompletionFlow(
                model = model,
                messages = messages.map(UiMessage::toMessage),
                reason = state.reasoningEffort,
                webSearch = useWebSearch
            )

            result
                .onSuccess { chunkFlow ->
                    val response = StringBuilder("")
                    chunkFlow
                        .onCompletion {
                            _uiState.update { it.copy(streamingResponse = null) }
                            val assistantMessage = Message(
                                UUID.randomUUID(),
                                Role.Assistant,
                                response.toString()
                            )
                            messages.add(assistantMessage.toUiMessage())
                            saveMessage(assistantMessage)
                            preloadMarkdownForIndex(messages.lastIndex)
                        }
                        .collect { chunk ->
                            response.append(chunk)
                            if (response.isNotBlank()) {
                                _uiState.update {
                                    it.copy(
                                        streamingResponse = response.toString(),
                                        isLoadingResponse = false
                                    )
                                }
                            }
                        }
                }.onFailure { error ->
                    val content = "_" + (error.message ?: "Oops. An unknown error occurred.") + "_"
                    val errorMessage = Message(UUID.randomUUID(), Role.System, content)
                    messages.add(errorMessage.toUiMessage())
                    saveMessage(errorMessage)
                    preloadMarkdownForIndex(messages.lastIndex)
                }
            _uiState.update { it.copy(streamingResponse = null, isLoadingResponse = false) }
            aiUpdateConversationTitle()
        }
        responseJob?.join()
        responseJob = null
    }

    private fun addAttachments(selected: List<MessageAttachment>) {
        _uiState.update { state ->
            val merged = (state.pendingAttachments + selected)
                .distinctBy { it.uri }
                .take(MAX_ATTACHMENTS)
            state.copy(
                pendingAttachments = merged,
                inputText = if (state.inputText.isBlank() && merged.isNotEmpty()) {
                    "Please analyze the attached file(s)."
                } else {
                    state.inputText
                }
            )
        }
    }

    private fun removeAttachment(uri: String) {
        _uiState.update { state ->
            val remaining = state.pendingAttachments.filterNot { it.uri == uri }
            val defaultPrompt = "Please analyze the attached file(s)."
            state.copy(
                pendingAttachments = remaining,
                inputText = if (remaining.isEmpty() && state.inputText == defaultPrompt) "" else state.inputText
            )
        }
    }

    private suspend fun aiUpdateConversationTitle() {
        if (messages.size == 2) {
            val prompt =
                Message(
                    id = UUID.randomUUID(),
                    role = Role.User,
                    content = "Set a title for the conversation in 3-4 words."
                )
            val summaryTitle =
                chatRepository.getChatCompletion(
                    model = uiState.value.selectedModel,
                    messages = messages.map(UiMessage::toMessage) + prompt
                )
            summaryTitle
                .onSuccess { title ->
                    uiState.value.selectedConversation?.let { conversation ->
                        title?.let {
                            chatRepository.updateConversation(
                                conversation.copy(title = title.trim('"'))
                            )
                        }
                    }
                }
        }
    }

    private suspend fun saveMessage(message: Message) {
        uiState.value.selectedConversation?.let { conversation ->
            chatRepository.insertMessage(message, conversation.id)
        }
    }

    private fun getConversationById(conversationId: String): Conversation? {
        return conversations.find { it.id.toString() == conversationId }
    }

    private fun fetchModels() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isFetchingModels = true) }
            modelRepository
                .getModels()
                .onSuccess { models ->
                    _uiState.update { it.copy(models = models) }
                    val currentSelected = _uiState.value.selectedModel
                    val newSelected = if (!models.contains(currentSelected)) {
                        models.firstOrNull() ?: currentSelected
                    } else {
                        currentSelected
                    }
                    modelRepository.setDefaultModel(newSelected)
                    loadDefaultModel()
                }
            _uiState.update { it.copy(isFetchingModels = false) }
        }
    }

    private suspend fun selectModel(model: Model) {
        _uiState.update { it.copy(selectedModel = model) }
        uiState.value.selectedConversation?.let {
            updateConversation(it.copy(selectedModel = model))
        }
    }

    private fun newChat() {
        _uiState.update { it.copy(selectedConversation = null, selectedModel = defaultModel, pendingAttachments = emptyList()) }
        messages.clear()
    }

    private suspend fun createNewConversation(title: String? = null) {
        cancelExistingPreloadJobs()

        val title = title ?: "Untitled: ${DateFormat.getDateTimeInstance().format(Date())}"
        val conversation =
            chatRepository.createConversation(title = title, model = uiState.value.selectedModel)
        selectConversation(conversation)
    }

    private suspend fun selectConversation(conversation: Conversation) {
        cancelExistingPreloadJobs()

        _uiState.update {
            it.copy(
                selectedConversation = conversation,
                selectedModel = conversation.selectedModel,
                pendingAttachments = emptyList()
            )
        }
        messages.clear()
        messages.addAll(chatRepository.getMessages(conversation.id).map { it.toUiMessage() })

        for (i in messages.indices) {
            preloadMarkdownForIndex(i)
        }
    }

    private suspend fun deleteConversation(conversation: Conversation) {
        chatRepository.deleteMessages(conversation.id)
        chatRepository.deleteConversation(conversation.id)
        messages.clear()
    }

    private fun parseMarkdownForIndex(index: Int): Job? {
        val message = messages.getOrNull(index) ?: return null
        if (message.content.isNullOrBlank() || message.isUser) return null

        return viewModelScope.launch(Dispatchers.Default) {
            try {
                parseMarkdownFlow(message.content).distinctUntilChanged().collectLatest { state ->
                    val currentMessage = messages.getOrNull(index)
                    if (currentMessage?.id == message.id && currentMessage.markdownState != state) {
                        messages[index] = currentMessage.copy(markdownState = state)
                    }
                }
            } catch (e: Exception) {
                Log.w("ChatViewModel", "Error parsing markdown for index $index", e)
            }
        }
    }

    private fun preloadMarkdownForIndex(index: Int) {
        if (preloadJobs[index] == null) {
            val preloadJob = parseMarkdownForIndex(index)
            preloadJob?.let { preloadJobs[index] = it }
        }
    }

    private fun cancelExistingPreloadJobs() {
        preloadJobs.forEach { _, job -> job.cancel() }
        preloadJobs.clear()
    }
}

private fun shouldAutoSearchWeb(query: String): Boolean {
    val lower = query.lowercase()
    val freshnessSignals = listOf(
        "latest", "current", "today", "right now", "this week", "this month",
        "yesterday", "tomorrow", "recent", "news", "price", "cost", "weather",
        "forecast", "score", "standings", "schedule", "release date", "new version",
        "latest version", "updated", "update"
    )
    return freshnessSignals.any { lower.contains(it) } ||
        Regex("\\b20[2-9][0-9]\\b").containsMatchIn(query)
}

private const val MAX_ATTACHMENTS = 5
