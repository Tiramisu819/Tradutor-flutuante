package com.seu.pacote // Mantenha o package original do seu projeto

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object GroqTranslator {

    private const val API_URL = "https://api.groq.com/openai/v1/chat/completions"
    // Modelo atualizado (llama-3.3-70b-versatile foi desativado e gera erro 404)
    private const val MODEL = "openai/gpt-oss-120b" 

    suspend fun translate(
        apiKey: String,
        systemPrompt: String,
        userText: String
    ): String = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isEmpty()) {
            return@withContext "Erro: Chave API não configurada."
        }

        try {
            val url = URL(API_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Authorization", "Bearer $cleanKey")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.doOutput = true

            val messagesArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userText)
                })
            }

            val payload = JSONObject().apply {
                put("model", MODEL)
                put("messages", messagesArray)
                put("temperature", 0.3)
            }

            OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = JSONObject(responseText)
                val choices = jsonResponse.getJSONArray("choices")
                if (choices.length() > 0) {
                    return@withContext choices.getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                        .trim()
                }
                return@withContext "Erro: Resposta vazia da IA."
            } else {
                val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                val errorMsg = try {
                    JSONObject(errorStream).getJSONObject("error").getString("message")
                } catch (e: Exception) {
                    "Erro HTTP $responseCode"
                }
                return@withContext "Erro API ($responseCode): $errorMsg"
            }
        } catch (e: Exception) {
            return@withContext "Erro de conexão: ${e.localizedMessage}"
        }
    }
}
