# Aiyo Nova

Aiyo Nova is an Android AI chat app based on [Aiyo](https://github.com/beradeep/aiyo), configured to use the Kilo AI Gateway.

## Chat capabilities

- Streaming chat with the selected Kilo-compatible model.
- Image attachments sent as OpenAI-compatible vision content. The selected model must support image input.
- PDF and DOCX text extraction, plus TXT, Markdown, CSV, JSON, XML, and source-code text files.
- Multiple attachments per message, with attachment references preserved in local conversation history.
- Web search powered by DuckDuckGo HTML results, including source links appended to responses.
- Manual web-search mode through the composer search toggle, plus automatic search for freshness-sensitive questions.
- Local conversation history, reasoning-effort controls, and theme/typography settings.

Attachments are read from Android's document provider only when a message is sent. Images are sent to the selected model as image data; supported documents are extracted into text and included in the prompt. Image input availability depends on the selected model. Individual files are limited to 8 MB and up to five files can be attached to one message.

## Kilo gateway

Chat requests are sent to `https://ai-gateway.kilo.ai/api/v1`. The app omits the Authorization header for anonymous access. Kilo model availability and anonymous rate limits can change, and not every listed model supports image input.

## Web search and privacy

When search is enabled or a question appears to require fresh information, the app queries DuckDuckGo and passes a small set of result titles, snippets, and URLs to the selected model. Responses include links to the results used. Search queries are sent to DuckDuckGo; prompts and attachments are sent to Kilo and the selected upstream inference provider. Conversation history and attachment URI references are stored locally on the device. Do not attach confidential information unless you have reviewed the relevant providers' data-handling terms.

## Tech stack

- Kotlin and Jetpack Compose
- Clean Architecture and MVVM
- Hilt dependency injection
- Room local database
- MMKV key-value storage
- OpenAI Kotlin SDK with Kilo Gateway-compatible requests
- OkHttp for web search and PDFBox-Android for PDF text extraction

## Build

1. Open the project in Android Studio.
2. Sync Gradle.
3. Run the `:app` configuration on Android API 24 or newer.

GitHub Actions builds the Aiyo Nova source and publishes the release APK.

## License

Apache License 2.0. See the upstream project for attribution and license details.
