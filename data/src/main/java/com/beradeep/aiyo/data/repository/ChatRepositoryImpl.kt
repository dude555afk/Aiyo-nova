package com.beradeep.aiyo.data.repository

import android.content.Context
import android.net.Uri
import android.text.Html
import android.util.Base64
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ContentPart
import com.aallam.openai.api.chat.Effort
import com.aallam.openai.api.chat.ImagePart
import com.aallam.openai.api.chat.StreamOptions
import com.aallam.openai.api.chat.TextPart
import com.aallam.openai.api.model.ModelId
import com.beradeep.aiyo.data.local.db.dao.ConversationDao
import com.beradeep.aiyo.data.local.db.dao.MessageDao
import com.beradeep.aiyo.data.remote.DataApiClient
import com.beradeep.aiyo.data.toChatRole
import com.beradeep.aiyo.data.toDomain
import com.beradeep.aiyo.data.toEntity
import com.beradeep.aiyo.domain.model.Conversation
import com.beradeep.aiyo.domain.model.Message
import com.beradeep.aiyo.domain.model.MessageAttachment
import com.beradeep.aiyo.domain.model.Model
import com.beradeep.aiyo.domain.model.Reason
import com.beradeep.aiyo.domain.repository.ChatRepository
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

private data class WebSearchResult(
    val title: String,
    val url: String,
    val snippet: String
)

class ChatRepositoryImpl(
    private val context: Context,
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val apiClient: DataApiClient
) : ChatRepository {
    private val openAi get() = apiClient.openAI
    private val searchClient = OkHttpClient.Builder()
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    override suspend fun getCompletion(model: Model, message: Message): Result<String?> = openAi?.let { client ->
        runCatching {
            val apiMessage = withContext(Dispatchers.IO) { message.toApiMessage() }
            client.chatCompletion(
                ChatCompletionRequest(
                    model = ModelId(model.id),
                    messages = listOf(apiMessage)
                )
            ).choices.first().message.content
        }
    } ?: Result.failure(IllegalStateException("OpenAI client not initialized"))

    override suspend fun getChatCompletion(model: Model, messages: List<Message>): Result<String?> = openAi?.let { client ->
        runCatching {
            val apiMessages = withContext(Dispatchers.IO) { messages.map { it.toApiMessage() } }
            client.chatCompletion(
                ChatCompletionRequest(
                    model = ModelId(model.id),
                    messages = apiMessages
                )
            ).choices.first().message.content
        }
    } ?: Result.failure(IllegalStateException("OpenAI client not initialized"))

    override suspend fun getChatCompletionFlow(
        model: Model,
        messages: List<Message>,
        reason: Reason,
        webSearch: Boolean
    ): Result<Flow<String>> = openAi?.let { client ->
        runCatching {
            val latestUserMessage = messages.lastOrNull { it.role.name == "User" }
            val searchResults = if (webSearch && latestUserMessage != null) {
                runCatching {
                    withContext(Dispatchers.IO) { searchWeb(latestUserMessage.content) }
                }.getOrDefault(emptyList())
            } else {
                emptyList()
            }

            val apiMessages = withContext(Dispatchers.IO) { messages.map { it.toApiMessage() } }
            val searchPrompt = if (webSearch) {
                buildSearchPrompt(searchResults)
            } else {
                null
            }
            val requestMessages = if (searchPrompt == null) {
                apiMessages
            } else {
                listOf(ChatMessage.System(searchPrompt)) + apiMessages
            }

            client.chatCompletions(
                ChatCompletionRequest(
                    model = ModelId(model.id),
                    messages = requestMessages,
                    streamOptions = StreamOptions(true),
                    reasoningEffort = reason.effort?.let { Effort(it) }
                )
            ).mapNotNull { chunk ->
                chunk.choices.firstOrNull()?.delta?.content
            }.catch { error ->
                emit("Error: " + (error.message ?: "The request failed."))
            }.onCompletion { cause ->
                if (cause == null && webSearch) {
                    emit(buildSourcesFooter(searchResults))
                }
            }.flowOn(Dispatchers.IO)
        }
    } ?: Result.failure(IllegalStateException("OpenAI client not initialized"))

    private fun buildSearchPrompt(results: List<WebSearchResult>): String {
        if (results.isEmpty()) {
            return "Live web search was requested, but DuckDuckGo returned no usable results. Do not invent current facts or citations. State that live sources were unavailable and answer cautiously."
        }
        val sourceContext = results.mapIndexed { index, result ->
            "[" + (index + 1) + "] " + result.title +
                "\nURL: " + result.url +
                "\nSnippet: " + result.snippet
        }.joinToString("\n\n")
        return "Answer using the live web search results below. Cite supported factual claims inline using the exact bracketed source numbers, such as [1]. Do not invent sources or claim a source supports something it does not. If the results are insufficient, say so.\n\n" + sourceContext
    }

    private fun buildSourcesFooter(results: List<WebSearchResult>): String {
        if (results.isEmpty()) {
            return "\n\n> Live search did not return usable sources for this request."
        }
        val lines = results.mapIndexed { index, result ->
            "[" + (index + 1) + "] [" + result.title.replace("]", "") + "](" + result.url + ")"
        }
        return "\n\n**Sources**\n" + lines.joinToString("\n")
    }

    private fun searchWeb(query: String): List<WebSearchResult> {
        val encodedQuery = URLEncoder.encode(query.take(400), StandardCharsets.UTF_8.name())
        val request = Request.Builder()
            .url("https://html.duckduckgo.com/html/?q=" + encodedQuery)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 16) AppleWebKit/537.36 Chrome/131.0 Mobile Safari/537.36")
            .header("Accept", "text/html")
            .header("Accept-Language", "en-IN,en;q=0.9")
            .build()

        val html = searchClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("DuckDuckGo search returned HTTP " + response.code)
            }
            response.body?.string().orEmpty()
        }

        val linkMatches = Regex(
            """<a(?=[^>]*class=["'][^"']*result__a[^"']*["'])[^>]*href=["']([^"']+)["'][^>]*>(.*?)</a>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        ).findAll(html).toList()
        val snippets = Regex(
            """<(?:a|div)[^>]*class=["'][^"']*result__snippet[^"']*["'][^>]*>(.*?)</(?:a|div)>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        ).findAll(html).map { htmlText(it.groupValues[1]) }.toList()

        return linkMatches.mapIndexedNotNull { index, match ->
            val rawHref = match.groupValues[1]
            val href = if (rawHref.startsWith("//")) "https:" + rawHref else rawHref
            val parsed = Uri.parse(href)
            val targetUrl = parsed.getQueryParameter("uddg") ?: href
            if (!targetUrl.startsWith("https://") && !targetUrl.startsWith("http://")) return@mapIndexedNotNull null
            val title = htmlText(match.groupValues[2]).take(180)
            if (title.isBlank()) return@mapNotNull null
            WebSearchResult(
                title = title,
                url = targetUrl,
                snippet = snippets.getOrNull(index).orEmpty().take(600)
            )
        }.distinctBy { it.url }.take(5)
    }

    private fun htmlText(value: String): String {
        val withoutTags = value.replace(Regex("<[^>]+>"), " ")
        return Html.fromHtml(withoutTags, Html.FROM_HTML_MODE_COMPACT)
            .toString()
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun Message.toApiMessage(): ChatMessage {
        if (attachments.isEmpty()) {
            return ChatMessage(role = role.toChatRole(), content = content)
        }

        val parts = mutableListOf<ContentPart>()
        if (content.isNotBlank()) parts.add(TextPart(content))
        attachments.forEach { attachment ->
            val bytes = readAttachment(attachment)
            if (attachment.mimeType.startsWith("image/")) {
                if (attachment.mimeType !in setOf("image/jpeg", "image/png", "image/webp", "image/gif")) {
                    throw IllegalArgumentException("Unsupported image type: " + attachment.mimeType + ". Use JPG, PNG, WEBP, or GIF.")
                }
                val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
                parts.add(ImagePart("data:" + attachment.mimeType + ";base64," + encoded, "auto"))
            } else {
                val extractedText = extractDocumentText(attachment, bytes)
                parts.add(TextPart(
                    "\n\n--- Attachment: " + attachment.name + " ---\n" +
                        extractedText.take(MAX_EXTRACTED_CHARACTERS) +
                        "\n--- End attachment ---"
                ))
            }
        }
        if (parts.isEmpty()) parts.add(TextPart("Please analyze the attached content."))
        return ChatMessage(role = role.toChatRole(), content = parts)
    }

    private fun readAttachment(attachment: MessageAttachment): ByteArray {
        val uri = Uri.parse(attachment.uri)
        val stream = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Could not open attachment: " + attachment.name)
        val maxBytes = if (attachment.mimeType.startsWith("image/")) MAX_IMAGE_BYTES else MAX_FILE_BYTES
        return stream.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            var total = 0
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                total += count
                if (total > maxBytes) {
                    throw IOException("Attachment is too large: " + attachment.name + ". Maximum size is " + (maxBytes / 1024 / 1024) + " MB.")
                }
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        }
    }

    private fun extractDocumentText(attachment: MessageAttachment, bytes: ByteArray): String {
        val extension = attachment.name.substringAfterLast('.', "").lowercase()
        return when {
            attachment.mimeType == "application/pdf" || extension == "pdf" -> extractPdfText(bytes)
            attachment.mimeType.contains("wordprocessingml") || extension == "docx" -> extractDocxText(bytes)
            attachment.mimeType.startsWith("text/") ||
                attachment.mimeType.contains("json") ||
                attachment.mimeType.contains("xml") ||
                extension in TEXT_EXTENSIONS -> String(bytes, StandardCharsets.UTF_8)
            else -> throw IllegalArgumentException(
                "Unsupported file type for " + attachment.name +
                    ". Supported documents: PDF, DOCX, TXT, MD, CSV, JSON, XML, and source-code text files."
            )
        }
    }

    private fun extractPdfText(bytes: ByteArray): String {
        synchronized(pdfLock) {
            if (!pdfInitialized) {
                PDFBoxResourceLoader.init(context)
                pdfInitialized = true
            }
        }
        PDDocument.load(ByteArrayInputStream(bytes)).use { document ->
            return PDFTextStripper().getText(document)
        }
    }

    private fun extractDocxText(bytes: ByteArray): String {
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == "word/document.xml") {
                    val xml = zip.readBytes().toString(StandardCharsets.UTF_8)
                    val withBreaks = xml.replace(Regex("</w:p>"), "\n")
                    return Html.fromHtml(
                        withBreaks.replace(Regex("<[^>]+>"), " "),
                        Html.FROM_HTML_MODE_COMPACT
                    ).toString().trim()
                }
                entry = zip.nextEntry
            }
        }
        throw IOException("Could not read text from DOCX attachment: " + "document.xml was missing.")
    }

    override suspend fun getConversations(): List<Conversation> =
        conversationDao.getAllConversations().map { it.toDomain() }

    override fun getConversationsFlow(): Flow<List<Conversation>> {
        return conversationDao.getAllConversationsFlow()
            .map { flow -> flow.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun createConversation(title: String, model: Model, isStarred: Boolean): Conversation {
        val timeStamp = System.currentTimeMillis()
        val conversation = Conversation(
            id = UUID.randomUUID(),
            title = title,
            selectedModel = model,
            isStarred = isStarred,
            createdAt = timeStamp,
            lastUpdatedAt = timeStamp
        )
        conversationDao.upsertConversation(conversation.toEntity())
        return conversation
    }

    override suspend fun updateConversation(conversation: Conversation) {
        conversationDao.upsertConversation(conversation.toEntity())
    }

    override suspend fun deleteConversation(id: UUID) {
        conversationDao.deleteConversation(id)
        messageDao.deleteMessagesByConversation(id)
    }

    override suspend fun getMessages(conversationId: UUID) =
        messageDao.getMessagesForConversation(conversationId).map { it.toDomain() }

    override suspend fun insertMessage(message: Message, conversationId: UUID) {
        messageDao.insertMessage(message.toEntity(conversationId))
    }

    override suspend fun insertMessages(messages: List<Message>, conversationId: UUID) {
        messageDao.insertMessages(messages.map { it.toEntity(conversationId) })
    }

    override suspend fun deleteMessages(conversationId: UUID) {
        messageDao.deleteMessagesByConversation(conversationId)
    }

    companion object {
        private const val MAX_IMAGE_BYTES = 8 * 1024 * 1024
        private const val MAX_FILE_BYTES = 8 * 1024 * 1024
        private const val MAX_EXTRACTED_CHARACTERS = 80_000
        private val TEXT_EXTENSIONS = setOf(
            "txt", "md", "markdown", "csv", "tsv", "json", "xml", "html", "htm",
            "yaml", "yml", "log", "kt", "java", "py", "js", "ts", "css", "sql",
            "gradle", "properties", "sh", "ps1", "ini", "toml", "c", "cpp", "h", "rs"
        )
        private val pdfLock = Any()
        @Volatile private var pdfInitialized = false
    }
}
