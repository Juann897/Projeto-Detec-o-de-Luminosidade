package com.example.projeto3bim

import android.graphics.Color
import android.os.Bundle
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager

    private var sensorLuz: Sensor? = null
    private var sensorProximidade: Sensor? = null

    private lateinit var layoutPrincipal: LinearLayout
    private lateinit var textoLeitura: TextView
    private lateinit var valorLux: TextView
    private lateinit var statusLeitura: TextView
    private lateinit var botaoLeitura: Button

    private var proximidadeJob: Job? = null
    private var ultimoLux = 0f

    private val _leituraAtiva = MutableStateFlow(true)
    val leituraAtiva = _leituraAtiva.asStateFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        layoutPrincipal = findViewById(R.id.layoutPrincipal)
        textoLeitura = findViewById(R.id.textoLeitura)
        valorLux = findViewById(R.id.valorLux)
        statusLeitura = findViewById(R.id.statusLeitura)
        botaoLeitura = findViewById(R.id.botaoLeitura)

        sensorManager =
            getSystemService(SENSOR_SERVICE) as SensorManager

        sensorLuz =
            sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        sensorProximidade =
            sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        // BOTÃO
        botaoLeitura.setOnClickListener {

            if (leituraAtiva.value) {
                pausarLeitura()
            } else {
                iniciarLeitura()
            }
        }

        // OBSERVAR O STATEFLOW
        lifecycleScope.launch {

            leituraAtiva.collectLatest { ativa ->

                if (ativa) {
                    statusLeitura.text = "Leitura ativa"
                    botaoLeitura.text = "Pausar leitura"
                } else {
                    statusLeitura.text = "Leitura pausada"
                    botaoLeitura.text = "Iniciar leitura"
                }
            }
        }
    }

    // INTERPRETA O LUX

    private fun interpretarLux(lux: Float) {

        when {
            lux < 10 -> {
                aplicarTemaEscuro()
                alterarBrilho(0.2f)
            }

            lux < 100 -> {
                aplicarTemaEscuro()
                alterarBrilho(0.4f)
            }

            lux < 500 -> {
                aplicarTemaSepia()
                alterarBrilho(0.6f)
            }

            else -> {
                aplicarTemaClaro()
                alterarBrilho(0.9f)
            }
        }
    }

    // ALTERA BRILHO

    private fun alterarBrilho(brilho: Float) {

        val parametros = window.attributes

        parametros.screenBrightness = brilho

        window.attributes = parametros
    }

    // TEMA ESCURO

    private fun aplicarTemaEscuro() {

        layoutPrincipal.setBackgroundColor(Color.BLACK)

        textoLeitura.setTextColor(Color.WHITE)
    }

    // TEMA SÉPIA

    private fun aplicarTemaSepia() {

        layoutPrincipal.setBackgroundColor(
            Color.rgb(244, 236, 210)
        )

        textoLeitura.setTextColor(
            Color.rgb(80, 50, 30)
        )
    }

    // TEMA CLARO

    private fun aplicarTemaClaro() {

        layoutPrincipal.setBackgroundColor(Color.WHITE)

        textoLeitura.setTextColor(Color.BLACK)
    }

    // RECEBER DADOS DOS SENSORES

    override fun onSensorChanged(event: SensorEvent?) {

        if (event == null) return

        // SENSOR DE LUZ

        if (event.sensor.type == Sensor.TYPE_LIGHT) {

            val lux = event.values[0]

            ultimoLux = lux

            valorLux.text =
                "Luminosidade: %.2f lux".format(lux)

            if (leituraAtiva.value) {
                interpretarLux(lux)
            }
        }

        // SENSOR DE PROXIMIDADE

        if (event.sensor.type == Sensor.TYPE_PROXIMITY) {

            val distancia = event.values[0]

            if (distancia < event.sensor.maximumRange) {
                iniciarContagemProximidade()
            } else {
                cancelarContagemProximidade()
            }
        }
    }

    // CONTAGEM DE 3 SEGUNDOS

    private fun iniciarContagemProximidade() {

        if (proximidadeJob?.isActive == true) {
            return
        }

        proximidadeJob = lifecycleScope.launch {

            delay(3000)

            pausarLeitura()

            alterarBrilho(0.1f)
        }
    }

    // CANCELAR CONTAGEM

    private fun cancelarContagemProximidade() {

        proximidadeJob?.cancel()

        proximidadeJob = null
    }

    // PAUSAR

    private fun pausarLeitura() {

        _leituraAtiva.value = false
    }

    // INICIAR

    private fun iniciarLeitura() {

        _leituraAtiva.value = true

        interpretarLux(ultimoLux)
    }

    // CICLO DE VIDA

    override fun onResume() {
        super.onResume()

        sensorLuz?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }

        sensorProximidade?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    override fun onPause() {
        super.onPause()

        sensorManager.unregisterListener(this)

        cancelarContagemProximidade()
    }

    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int
    ) {
    }
}