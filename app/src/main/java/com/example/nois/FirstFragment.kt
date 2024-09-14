package com.example.nois

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Bundle
import android.widget.SeekBar
import android.widget.TextView
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.sqrt

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
    private var volume = 0.5f // Default volume

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toggleButton = findViewById(R.id.toggleButton)
        volumeSeekBar = findViewById(R.id.volumeSeekBar)
        volumeLabel = findViewById(R.id.volumeLabel)

        toggleButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                startNoise()
            } else {
                stopNoise()
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

        if (!initializeAudioTrack()) {
            // Handle initialization failure
            toggleButton.isEnabled = false
        }
    }

    private fun initializeAudioTrack(): Boolean {
        return try {
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

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
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
                        buffer[i] = lastValue * volume // Apply volume
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

    override fun onDestroy() {
        super.onDestroy()
        audioTrack.release()
    }
}