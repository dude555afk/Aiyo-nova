package com.beradeep.aiyo.data.repository

import android.content.Context
import com.beradeep.aiyo.data.local.kv.KVStore
import com.beradeep.aiyo.domain.model.ThemeType
import com.beradeep.aiyo.domain.repository.SettingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class SettingRepositoryImpl(context: Context) : SettingRepository {

    private val kvStore by lazy { KVStore.getInstance(context) }
    private val requestFontSizeFlow =
        MutableStateFlow(getFontSize(KEY_REQUEST_FONT_SIZE, SettingRepository.DEFAULT_REQUEST_FONT_SIZE))
    private val responseFontSizeFlow =
        MutableStateFlow(getFontSize(KEY_RESPONSE_FONT_SIZE, SettingRepository.DEFAULT_RESPONSE_FONT_SIZE))

    override suspend fun getThemeType(): ThemeType {
        return kvStore.getString(KEY_THEME_TYPE)?.let { ThemeType.valueOf(it) } ?: ThemeType.System
    }

    override suspend fun setThemeType(themeType: ThemeType) {
        kvStore.putString(KEY_THEME_TYPE, themeType.name)
    }

    override suspend fun getRequestFontSize(): Int {
        return getFontSize(KEY_REQUEST_FONT_SIZE, SettingRepository.DEFAULT_REQUEST_FONT_SIZE)
    }

    override suspend fun setRequestFontSize(fontSize: Int) {
        val boundedFontSize = fontSize.coerceIn(
            SettingRepository.MIN_FONT_SIZE,
            SettingRepository.MAX_FONT_SIZE
        )
        kvStore.putLong(KEY_REQUEST_FONT_SIZE, boundedFontSize.toLong())
        requestFontSizeFlow.value = boundedFontSize
    }

    override fun getRequestFontSizeFlow(): Flow<Int> {
        return requestFontSizeFlow
    }

    override suspend fun getResponseFontSize(): Int {
        return getFontSize(KEY_RESPONSE_FONT_SIZE, SettingRepository.DEFAULT_RESPONSE_FONT_SIZE)
    }

    override suspend fun setResponseFontSize(fontSize: Int) {
        val boundedFontSize = fontSize.coerceIn(
            SettingRepository.MIN_FONT_SIZE,
            SettingRepository.MAX_FONT_SIZE
        )
        kvStore.putLong(KEY_RESPONSE_FONT_SIZE, boundedFontSize.toLong())
        responseFontSizeFlow.value = boundedFontSize
    }

    override fun getResponseFontSizeFlow(): Flow<Int> {
        return responseFontSizeFlow
    }

    private fun getFontSize(key: String, defaultValue: Int): Int {
        return kvStore.getLong(key, defaultValue.toLong())
            .toInt()
            .coerceIn(SettingRepository.MIN_FONT_SIZE, SettingRepository.MAX_FONT_SIZE)
    }

    companion object {
        const val KEY_THEME_TYPE = "theme_type"
        const val KEY_REQUEST_FONT_SIZE = "request_font_size"
        const val KEY_RESPONSE_FONT_SIZE = "response_font_size"
    }
}