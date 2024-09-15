package com.example.nois

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.Bundle
import android.widget.SeekBar
import android.widget.TextView
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat

class MainActivity : AppCompatActivity() {

    private lateinit var audioTrack: AudioTrack
    private var isPlaying = false
    private lateinit var toggleButton: ToggleButton
    private lateinit var volumeSeekBar: SeekBar
    private lateinit var volumeLabel: TextView
    private val sampleRate = 44100
    private val bufferSize = AudioTrack.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_FLOAT
    )
    private var volume = 0.5f

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "NoisGeneratorChannel"
        const val ACTION_STOP = "com.example.nois.STOP"
    }

    private val stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_STOP) {
                stopNoise()
                cancelNotification()
                toggleButton.isChecked = false
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toggleButton = findViewById(R.id.toggleButton)
        volumeSeekBar = findViewById(R.id.volumeSeekBar)
        volumeLabel = findViewById(R.id.volumeLabel)

        toggleButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                startNoise()
                showNotification()
            } else {
                stopNoise()
                cancelNotification()
            }
        }

        volumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                volume = progress / 100f
                volumeLabel.text = "Volume: ${progress}%"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        initializeAudioTrack()
        createNotificationChannel()
        registerReceiver(stopReceiver, IntentFilter(ACTION_STOP))
    }

    private fun initializeAudioTrack() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        val audioFormat = AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(audioAttributes)
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }

    private fun startNoise() {
        if (!isPlaying) {
            isPlaying = true
            Thread {
                val buffer = FloatArray(bufferSize / 4)
                var lastValue = 0f
                audioTrack.play()

                while (isPlaying) {
                    for (i in buffer.indices) {
                        val white = (Math.random() * 2 - 1).toFloat()
                        lastValue = ((lastValue + (0.02f * white)) / 1.02f).coerceIn(-1f, 1f)
                        buffer[i] = lastValue * volume
                    }
                    audioTrack.write(buffer, 0, buffer.size, AudioTrack.WRITE_BLOCKING)
                }
            }.start()
        }
    }

    private fun stopNoise() {
        isPlaying = false
        audioTrack.stop()
        audioTrack.flush()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Noise Generator"
            val descriptionText = "Noise Generator Notification Channel"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification() {
        val stopIntent = Intent(ACTION_STOP)
        val stopPendingIntent: PendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Nois")
            .setContentText("Noise is playing")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(true)
            .addAction(R.drawable.ic_close, "Stop", stopPendingIntent)

        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    private fun cancelNotification() {
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopNoise()
        audioTrack.release()
        cancelNotification()
        unregisterReceiver(stopReceiver)
    }
}