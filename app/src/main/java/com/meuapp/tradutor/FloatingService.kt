package com.meuapp.tradutor

import android.app.*
import android.content.*
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.*
import android.view.*
import android.widget.*
import androidx.core.app.NotificationCompat
import kotlin.concurrent.thread

class FloatingService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var bubbleView: TextView
    private var chatView: ScrollView? = null
    private var isChatOpen = false
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val textFromMenu = intent?.getStringExtra("SELECTED_TEXT")
        if (!textFromMenu.isNullOrEmpty()) openChat(textFromMenu)
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        bubbleView = TextView(this).apply {
            text = "IA"
            setTextColor(Color.WHITE)
            textSize = 18f
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#2563EB"))
                setStroke(4, Color.WHITE)
            }
            width = 140; height = 140
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = 50; y = 300 }

        var initialX = 0; var initialY = 0; var initialTouchX = 0f; var initialTouchY = 0f

        bubbleView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x; initialY = params.y
                    initialTouchX = event.rawX; initialTouchY = event.rawY; true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(bubbleView, params); true
                }
                MotionEvent.ACTION_UP -> {
                    if (Math.abs(event.rawX - initialTouchX) < 10 && Math.abs(event.rawY - initialTouchY) < 10) {
                        if (isChatOpen) closeChat() else openChat("")
                    }
                    true
                }
                else -> false
            }
        }
        windowManager.addView(bubbleView, params)
    }

    private fun closeChat() {
        chatView?.let { windowManager.removeView(it) }
        chatView = null; isChatOpen = false
    }

    private fun openChat(prefillText: String) {
        if (isChatOpen) closeChat()

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 36, 36, 36)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#0F172A"))
                cornerRadius = 32f
                setStroke(3, Color.parseColor("#3B82F6"))
            }
        }

        val title = TextView(this).apply {
            text = "Chat Tradutor Natural"
            setTextColor(Color.WHITE); textSize = 17f; setPadding(0, 0, 0, 16)
        }

        val inputReceived = EditText(this).apply {
            hint = "Cole o texto recebido aqui..."
            setHintTextColor(Color.GRAY); setTextColor(Color.WHITE); setText(prefillText)
        }

        val txtTranslated = TextView(this).apply {
            text = if (prefillText.isNotEmpty()) "Traduzindo..." else "A tradução natural aparecerá aqui."
            setTextColor(Color.parseColor("#93C5FD")); textSize = 15f; setPadding(0, 16, 0, 16)
        }

        val btnTranslateIn = Button(this).apply {
            text = "Traduzir Recebido"
            setOnClickListener {
                val txt = inputReceived.text.toString().trim()
                if (txt.isNotEmpty()) {
                    txtTranslated.text = "Traduzindo..."
                    thread {
                        val res = GroqTranslator.translate(this@FloatingService, txt, false)
                        mainHandler.post { txtTranslated.text = res }
                    }
                }
            }
        }

        val divider = TextView(this).apply {
            text = "─── Responder no seu idioma ───"
            setTextColor(Color.LTGRAY); gravity = Gravity.CENTER; setPadding(0, 20, 0, 10)
        }

        val inputReply = EditText(this).apply {
            hint = "Digite sua resposta natural..."
            setHintTextColor(Color.GRAY); setTextColor(Color.WHITE)
        }

        val txtReplyTranslated = TextView(this).apply {
            text = ""
            setTextColor(Color.parseColor("#6EE7B7")); textSize = 15f; setPadding(0, 12, 0, 12)
        }

        val btnTranslateOut = Button(this).apply {
            text = "Traduzir Resposta"
            setOnClickListener {
                val reply = inputReply.text.toString().trim()
                val orig = inputReceived.text.toString().trim()
                if (reply.isNotEmpty()) {
                    txtReplyTranslated.text = "Gerando resposta natural..."
                    thread {
                        val res = GroqTranslator.translate(this@FloatingService, reply, true, orig)
                        mainHandler.post { txtReplyTranslated.text = res }
                    }
                }
            }
        }

        val btnCopy = Button(this).apply {
            text = "Copiar Resposta"
            setOnClickListener {
                val toCopy = txtReplyTranslated.text.toString()
                if (toCopy.isNotEmpty()) {
                    val cb = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cb.setPrimaryClip(ClipData.newPlainText("Traducao", toCopy))
                    Toast.makeText(this@FloatingService, "Copiado!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val btnClose = Button(this).apply {
            text = "Fechar Janela"
            setOnClickListener { closeChat() }
        }

        container.addView(title); container.addView(inputReceived); container.addView(btnTranslateIn)
        container.addView(txtTranslated); container.addView(divider); container.addView(inputReply)
        container.addView(btnTranslateOut); container.addView(txtReplyTranslated)
        container.addView(btnCopy); container.addView(btnClose)

        val scroll = ScrollView(this).apply { addView(container) }
        chatView = scroll

        val chatParams = WindowManager.LayoutParams(
            900, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.CENTER }

        windowManager.addView(chatView, chatParams)
        isChatOpen = true

        if (prefillText.isNotEmpty()) {
            thread {
                val res = GroqTranslator.translate(this@FloatingService, prefillText, false)
                mainHandler.post { txtTranslated.text = res }
            }
        }
    }

    private fun startForegroundNotification() {
        val channelId = "tradutor_flutuante_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Tradutor Flutuante", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Tradutor IA Ativo")
            .setContentText("Toque na bolinha azul na tela para traduzir.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::bubbleView.isInitialized) windowManager.removeView(bubbleView)
        closeChat()
    }
}
