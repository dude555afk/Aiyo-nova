# Aiyo Nova

Aiyo Nova is an Android AI chat app based on [Aiyo](https://github.com/beradeep/aiyo), configured to use the **Kilo AI Gateway's anonymous free-model access**.

## Kilo anonymous gateway

- No OpenRouter account or API key is required.
- Chat requests are sent to `https://ai-gateway.kilo.ai/api/v1`.
- Requests omit the `Authorization` header entirely.
- The model picker includes Kilo's `kilo-auto/free` router and models marked `:free` in Kilo's public model catalog.
- Kilo documents anonymous free-model rate limiting at 200 requests per hour per IP. Availability and model limits can change.

Anonymous access is only for free models. Paid models require a Kilo API key and are intentionally not exposed in this build.

## Privacy

Conversation history is stored locally on the device. Prompts sent for inference are processed by Kilo and the selected upstream inference provider; do not send confidential information unless you have reviewed their data-handling terms.

## Features

- Jetpack Compose interface
- Streaming chat responses
- Multiple free Kilo models
- Local conversation history
- Reasoning effort controls
- Theme and typography settings

OpenRouter-specific `:online` model routing is not used. A separate web-search tool integration is required for web search.

## Tech stack

- Kotlin and Jetpack Compose
- Clean Architecture and MVVM
- Hilt dependency injection
- Room local database
- MMKV key-value storage
- OpenAI Kotlin SDK with Kilo Gateway-compatible requests

## Build

1. Open the project in Android Studio.
2. Sync Gradle.
3. Run the `:app` configuration on Android API 24 or newer.

GitHub Actions builds the Aiyo Nova source in this repository and publishes an APK release.

## License

Apache License 2.0. See the upstream project for attribution and license details.
