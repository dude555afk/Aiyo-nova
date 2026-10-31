package com.beradeep.aiyo.domain.repository

import com.beradeep.aiyo.domain.model.ThemeType
import kotlinx.coroutines.flow.Flow

interface SettingRepository {
    suspend fun getThemeType(): ThemeType
    suspend fun setThemeType(themeType: ThemeType)
    suspend fun getRequestFontSize(): Int
    suspend fun setRequestFontSize(fontSize: Int)
    fun getRequestFontSizeFlow(): Flow<Int>
    suspend fun getResponseFontSize(): Int
    suspend fun setResponseFontSize(fontSize: Int)
    fun getResponseFontSizeFlow(): Flow<Int>

    companion object {
        const val DEFAULT_REQUEST_FONT_SIZE = 16
        const val DEFAULT_RESPONSE_FONT_SIZE = 16
        const val MIN_FONT_SIZE = 12
        const val MAX_FONT_SIZE = 24
    }
}