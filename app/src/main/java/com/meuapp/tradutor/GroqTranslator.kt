package com.meuapp.tradutor

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object GroqTranslator {
    fun getPrefs(context: Context) = context.getSharedPreferences("tradutor_prefs", Context.MODE_PRIVATE)

    fun translate(context: Context, text: String, isReply: Boolean, originalContext: String = ""): String {
        val prefs = getPrefs(context)
        val apiKey = prefs.getString("api_key", "")?.trim() ?: ""
        if (apiKey.isEmpty()) return "Erro: Cole sua chave gsk_... na tela inicial do app!"

        val myLang = prefs.getString("my_lang", "Português (Brasil)") ?: "Português (Brasil)"
        val targetLang = prefs.getString("target_lang", "Inglês") ?: "Inglês"

        val systemPrompt = if (!isReply) {
            "Você é um tradutor especialista em linguagem natural, casual e cotidiana. Traduza o texto recebido para $myLang. Mantenha o tom original, gírias e naturalidade para não parecer um robô. Retorne APENAS a tradução direta, sem aspas nem explicações."
        } else {
            "Você é um tradutor especialista em conversas naturais e casuais. O usuário recebeu a mensagem: '$originalContext'. Agora ele quer responder dizendo: '$text'. Traduza essa resposta para $targetLang de forma super natural, fluida e casual, combinando com o tom da conversa. Retorne APENAS a tradução final, sem aspas nem explicações."
        }

        return try {
            val host = "https:/" + "/api.groq." + "com/openai/v1/chat/completions"
            val conn = (URL(host).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", "Bearer $apiKey")
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 15000
                readTimeout = 15000
            }

            val messages = JSONArray().apply {
                put(JSONObject().put("role", "system").put("content", systemPrompt))
                put(JSONObject().put("role", "user").put("content", text))
            }

            val body = JSONObject().apply {
                put("model", "llama-3.3-70b-versatile")
                put("messages", messages)
                put("temperature", 0.6)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val responseText = stream.bufferedReader().use { it.readText() }

            if (code in 200..299) {
                val json = JSONObject(responseText)
                json.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content").trim()
            } else {
                "Erro API ($code): Verifique sua chave."
            }
        } catch (e: Exception) {
            "Erro de conexão: ${e.message}"
        }
    }
}
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
