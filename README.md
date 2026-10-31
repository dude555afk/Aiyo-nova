# Aiyo Nova

Aiyo is a privacy-respecting, high-performance AI chat application for Android that uses **Kilo.ai's anonymous gateway** instead of requiring your own API key.

## Key Features

* **No API Key Required** - Uses Kilo.ai's free anonymous gateway, no OpenRouter key needed
* **Multi-Model Support** - Access hundreds of models via Kilo.ai gateway
* **Privacy First** - No data collection; all chat histories stored locally on device
* **Modern UI** - Built with Jetpack Compose and a custom design system
* **Streaming Responses** - Real-time streaming of AI responses
* **Web Search** - Native web search support
* **Reasoning Effort** - Control model reasoning levels (None, Low, Medium, High)

## Tech Stack

* Kotlin, Jetpack Compose
* Clean Architecture + MVVM
* Hilt (Dependency Injection)
* Room (Local Database)
* MMKV (Key-Value Storage)
* OpenAI Kotlin SDK (configured for Kilo.ai gateway)

## Getting Started

1. Clone the repo
2. Open in Android Studio Ladybug or newer
3. Sync Gradle and run on device/emulator (API 24+)
4. Start chatting - no API key setup needed!

## Architecture

The app follows Clean Architecture with four modules:

* `:app` - Entry point, Hilt DI, navigation
* `:ui` - Compose screens, ViewModels, design system
* `:domain` - Business logic, repository interfaces, models
* `:data` - Repository implementations, Room DB, networking

## License

Apache License 2.0