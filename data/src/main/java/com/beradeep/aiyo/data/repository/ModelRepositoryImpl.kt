package com.beradeep.aiyo.data.repository

import android.content.Context
import com.beradeep.aiyo.data.local.kv.KVStore
import com.beradeep.aiyo.data.local.kv.entity.ModelEntity
import com.beradeep.aiyo.data.remote.DataApiClient
import com.beradeep.aiyo.data.toDomain
import com.beradeep.aiyo.data.toEntity
import com.beradeep.aiyo.domain.model.Model
import com.beradeep.aiyo.domain.repository.ModelRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class ModelRepositoryImpl(context: Context, val apiClient: DataApiClient) : ModelRepository {
    private val kvStore by lazy { KVStore.getInstance(context) }
    private val json = Json { ignoreUnknownKeys = true }
    private val httpClient = OkHttpClient()
    private val openAi get() = apiClient.openAI

    override suspend fun getModels(): Result<List<Model>> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("${DataApiClient.BASE_URL}/models")
                .get()
                .build()

            val availableFreeModels = httpClient.newCall(request).execute().use { response ->
                check(response.isSuccessful) {
                    "Kilo model catalog request failed: HTTP ${response.code}"
                }
                val payload = response.body?.string()
                    ?: error("Kilo returned an empty model catalog")
                val data = JSONObject(payload).optJSONArray("data")
                    ?: error("Kilo returned an invalid model catalog")

                buildList {
                    for (index in 0 until data.length()) {
                        val item = data.optJSONObject(index) ?: continue
                        val id = item.optString("id").takeIf(String::isNotBlank) ?: continue
                        // Anonymous Kilo access is restricted to free model IDs.
                        if (id.endsWith(":free")) {
                            add(
                                Model(
                                    id = id,
                                    ownedBy = item.optString("owned_by").takeIf(String::isNotBlank)
                                )
                            )
                        }
                    }
                }
            }

            // Kilo's virtual free router tracks changing free-model availability.
            (listOf(Model.defaultModel) + availableFreeModels)
                .distinctBy(Model::id)
        }
    }

    override fun getDefaultModel(): Model {
        val modelJson = kvStore.getString(KEY_DEFAULT_MODEL)
        return modelJson?.let {
            try {
                json.decodeFromString<ModelEntity>(it).toDomain()
            } catch (_: Throwable) {
                Model.defaultModel
            }
        } ?: Model.defaultModel
    }

    override fun setDefaultModel(model: Model) {
        val modelJson = json.encodeToString(model.toEntity())
        kvStore.putString(KEY_DEFAULT_MODEL, modelJson)
    }

    companion object {
        private const val KEY_DEFAULT_MODEL = "default_model"
    }
}
