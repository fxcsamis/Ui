package com.arkitak.lite

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Minimal Gemini REST client (no extra libraries). Call from a background thread. */
object GeminiClient {

    /** [code] is the HTTP status (0 for local problems). */
    class GeminiException(message: String, val code: Int = 0) : Exception(message) {
        /** True when trying a different model could help (model retired, overloaded, or out of quota). */
        val worthTryingAnotherModel: Boolean
            get() = code == 404 || code == 503 || code == 429 ||
                (message ?: "").contains("no longer available", ignoreCase = true)
    }

    data class Turn(val role: String, val text: String) // role: "user" or "model"

    const val SYSTEM_PROMPT =
        "Your name is Olivia. You are a friendly voice assistant on the user's phone. " +
        "If someone asks who you are, say you are Olivia. " +
        "Reply in the same language the user spoke (Bangla, English, or a mix). " +
        "Your answer is read aloud, so keep it short, 1 to 3 sentences, in plain spoken text. " +
        "Do not use markdown, lists, symbols or emojis."

    private const val BASE = "https://generativelanguage.googleapis.com/v1beta"

    /**
     * Tries [preferred] first. If Google says that model is retired, busy or out of quota, it asks Google which
     * models this key can use, tries the newest Flash models, and reports the one that worked through [onModelChanged].
     */
    fun askWithFallback(
        apiKey: String,
        preferred: String,
        history: List<Turn>,
        onModelChanged: (String) -> Unit
    ): String {
        val candidates = arrayListOf(preferred)
        val tried = HashSet<String>()
        var discovered = false
        var lastError: GeminiException? = null
        var i = 0
        while (i < candidates.size && tried.size < 5) {
            val model = candidates[i++]
            if (!tried.add(model)) continue
            try {
                val answer = ask(apiKey, model, history)
                if (model != preferred) onModelChanged(model)
                return answer
            } catch (e: GeminiException) {
                lastError = e
                if (!e.worthTryingAnotherModel) throw e
                // Google's error text often names the replacement model ("...use models/gemini-3.x-flash"): try it next.
                val hinted = Regex("use\\s+models/([A-Za-z0-9._-]+)").find(e.message ?: "")?.groupValues?.get(1)
                if (hinted != null && hinted !in tried) candidates.add(i, hinted)
                if (!discovered) {
                    discovered = true
                    try { candidates.addAll(discoverFlashModels(apiKey)) } catch (_: Exception) {}
                }
            }
        }
        throw lastError ?: GeminiException("No Gemini model is available for this key")
    }

    /** Newest stable "gemini-N-flash" models that support generateContent, newest first. */
    private fun discoverFlashModels(apiKey: String): List<String> {
        val conn = URL("$BASE/models?pageSize=200").openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 15000
            conn.readTimeout = 20000
            conn.setRequestProperty("x-goog-api-key", apiKey)
            if (conn.responseCode !in 200..299) return emptyList()
            val text = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val models = JSONObject(text).optJSONArray("models") ?: return emptyList()
            val pattern = Regex("^gemini-(\\d+(?:\\.\\d+)?)-flash$")
            val found = ArrayList<Pair<Double, String>>()
            for (i in 0 until models.length()) {
                val m = models.getJSONObject(i)
                val name = m.optString("name").removePrefix("models/")
                val methods = m.optJSONArray("supportedGenerationMethods") ?: continue
                var ok = false
                for (j in 0 until methods.length()) if (methods.optString(j) == "generateContent") ok = true
                val match = pattern.find(name) ?: continue
                if (ok) found.add((match.groupValues[1].toDoubleOrNull() ?: 0.0) to name)
            }
            return found.sortedByDescending { it.first }.map { it.second }
        } finally {
            conn.disconnect()
        }
    }

    fun ask(apiKey: String, model: String, history: List<Turn>): String {
        val url = URL("$BASE/models/$model:generateContent")

        val contents = JSONArray()
        for (turn in history) {
            contents.put(
                JSONObject()
                    .put("role", turn.role)
                    .put("parts", JSONArray().put(JSONObject().put("text", turn.text)))
            )
        }
        val generation = JSONObject().put("temperature", 0.7).put("maxOutputTokens", 1024)
        // Only Gemini 2.5 Flash understands thinkingBudget; newer models manage thinking themselves.
        if (model.contains("2.5") && model.contains("flash")) {
            generation.put("thinkingConfig", JSONObject().put("thinkingBudget", 0))
        }
        val body = JSONObject()
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", SYSTEM_PROMPT))))
            .put("contents", contents)
            .put("generationConfig", generation)

        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 15000
            conn.readTimeout = 30000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("x-goog-api-key", apiKey)
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""

            if (code !in 200..299) {
                val message = try {
                    JSONObject(text).getJSONObject("error").getString("message")
                } catch (e: JSONException) {
                    "HTTP $code"
                }
                throw GeminiException(message, code)
            }

            try {
                val parts = JSONObject(text)
                    .getJSONArray("candidates").getJSONObject(0)
                    .getJSONObject("content").getJSONArray("parts")
                val sb = StringBuilder()
                for (i in 0 until parts.length()) sb.append(parts.getJSONObject(i).optString("text"))
                val answer = sb.toString().trim()
                if (answer.isEmpty()) throw GeminiException("Empty answer")
                return answer
            } catch (e: JSONException) {
                throw GeminiException("Gemini gave no answer for that")
            }
        } finally {
            conn.disconnect()
        }
    }
}
