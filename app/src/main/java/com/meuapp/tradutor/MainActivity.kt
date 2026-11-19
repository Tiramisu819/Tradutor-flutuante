package com.meuapp.tradutor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = GroqTranslator.getPrefs(this)
        val idiomas = arrayOf("Português (Brasil)", "Inglês", "Espanhol", "Japonês", "Coreano", "Francês", "Alemão")

        val scroll = ScrollView(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 80, 50, 60)
        }

        val titulo = TextView(this).apply {
            text = "Tradutor Flutuante IA"
            textSize = 24f
            setPadding(0, 0, 0, 30)
        }

        val lblKey = TextView(this).apply { text = "Chave API do Groq (gsk_...):" }
        val inputKey = EditText(this).apply {
            hint = "Cole sua chave gsk_... aqui"
            setText(prefs.getString("api_key", ""))
        }

        val lblMyLang = TextView(this).apply {
            text = "Seu Idioma (para traduzir o que você lê):"
            setPadding(0, 25, 0, 5)
        }
        val spinMyLang = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, idiomas)
            setSelection(idiomas.indexOf(prefs.getString("my_lang", "Português (Brasil)")).coerceAtLeast(0))
        }

        val lblTargetLang = TextView(this).apply {
            text = "Idioma da Outra Pessoa (para suas respostas):"
            setPadding(0, 25, 0, 5)
        }
        val spinTargetLang = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, idiomas)
            setSelection(idiomas.indexOf(prefs.getString("target_lang", "Inglês")).coerceAtLeast(0))
        }

        val btnSalvar = Button(this).apply {
            text = "Salvar Configurações"
            setOnClickListener {
                prefs.edit()
                    .putString("api_key", inputKey.text.toString().trim())
                    .putString("my_lang", spinMyLang.selectedItem.toString())
                    .putString("target_lang", spinTargetLang.selectedItem.toString())
                    .apply()
                Toast.makeText(this@MainActivity, "Configurações salvas!", Toast.LENGTH_SHORT).show()
            }
        }

        val btnAtivar = Button(this).apply {
            text = "Ativar Bolinha Flutuante"
            setOnClickListener {
                prefs.edit()
                    .putString("api_key", inputKey.text.toString().trim())
                    .putString("my_lang", spinMyLang.selectedItem.toString())
                    .putString("target_lang", spinTargetLang.selectedItem.toString())
                    .apply()

                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                    startActivity(intent)
                    Toast.makeText(this@MainActivity, "Libere a sobreposição e volte ao app", Toast.LENGTH_LONG).show()
                } else {
                    startService(Intent(this@MainActivity, FloatingService::class.java))
                    Toast.makeText(this@MainActivity, "Bolinha ativada!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val btnDesativar = Button(this).apply {
            text = "Desativar Bolinha"
            setOnClickListener { stopService(Intent(this@MainActivity, FloatingService::class.java)) }
        }

        layout.addView(titulo); layout.addView(lblKey); layout.addView(inputKey)
        layout.addView(lblMyLang); layout.addView(spinMyLang)
        layout.addView(lblTargetLang); layout.addView(spinTargetLang)
        layout.addView(btnSalvar); layout.addView(btnAtivar); layout.addView(btnDesativar)
        scroll.addView(layout)
        setContentView(scroll)
    }
}
