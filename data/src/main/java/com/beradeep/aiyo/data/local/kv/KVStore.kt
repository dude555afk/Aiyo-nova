package com.beradeep.aiyo.data.local.kv

import android.content.Context
import com.tencent.mmkv.MMKV

class KVStore(context: Context) {
    private val mmkv by lazy { MMKV.defaultMMKV() }
    private val mmkvEC by lazy {
        MMKV.mmkvWithID("encrypted_mmkv", MMKV.SINGLE_PROCESS_MODE, "aiyo")
    }

    init {
        MMKV.initialize(context)
    }

    fun putString(key: String, value: String) {
        mmkv.encode(key, value)
    }

    fun getString(key: String): String? = mmkv.getString(key, null)

    fun putLong(key: String, value: Long) {
        mmkv.encode(key, value)
    }

    fun getLong(key: String, defaultValue: Long): Long = mmkv.getLong(key, defaultValue)

    fun putInt(key: String, value: Int) {
        mmkv.encode(key, value)
    }

    fun getInt(key: String, defaultValue: Int): Int = mmkv.getInt(key, defaultValue)

    fun putBoolean(key: String, value: Boolean) {
        mmkv.encode(key, value)
    }

    fun getBoolean(key: String, defaultValue: Boolean): Boolean = mmkv.getBoolean(key, defaultValue)

    fun putEncryptedString(key: String, value: String) {
        mmkvEC.encode(key, value)
    }

    fun getEncryptedString(key: String): String? = mmkvEC.getString(key, null)

    companion object {
        @Volatile
        private var instance: KVStore? = null

        fun getInstance(context: Context): KVStore {
            return instance ?: synchronized(this) {
                instance ?: KVStore(context).also { instance = it }
            }
        }
    }
}