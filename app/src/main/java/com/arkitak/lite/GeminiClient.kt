package com.arkitak.lite

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Minimal Gemini REST client (no extra libraries). Call [ask] from a background thread. */
object GeminiClient {

    class GeminiException(message: String) : Exception(message)

    data class Turn(val role: String, val text: String) // role: "user" or "model"

    const val SYSTEM_PROMPT =
        "You are ARKITAK, a friendly voice assistant on the user's phone. " +
        "Reply in the same language the user spoke (Bangla, English, or a mix). " +
        "Your answer is read aloud, so keep it short, 1 to 3 sentences, in plain spoken text. " +
        "Do not use markdown, lists, symbols or emojis."

    fun ask(apiKey: String, model: String, history: List<Turn>): String {
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent")

        val contents = JSONArray()
        for (turn in history) {
            contents.put(
                JSONObject()
                    .put("role", turn.role)
                    .put("parts", JSONArray().put(JSONObject().put("text", turn.text)))
            )
        }
        val generation = JSONObject().put("temperature", 0.7).put("maxOutputTokens", 1024)
        // Gemini 2.5 Flash can skip "thinking" for much faster voice replies.
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
                throw GeminiException(message)
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
