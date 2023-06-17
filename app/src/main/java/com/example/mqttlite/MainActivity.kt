package com.example.mqttlite

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*

class MainActivity : AppCompatActivity() {

    // This is for the list view that will show the messages
    lateinit var listView: ListView
    var list: ArrayList<String> = ArrayList()
    lateinit var arrayAdapter: ArrayAdapter<String>

    // on below is the graph view
    lateinit var lineGraphView: GraphView
    var latestX: Double = 0.0
    lateinit var series: LineGraphSeries<DataPoint>

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // on below line we are initializing
        // our variable with their ids.
        lineGraphView = findViewById(R.id.idGraphView)

        // on below line we are adding data to our graph view.
        series = LineGraphSeries(
            arrayOf(
                // on below line we are adding
                // each point on our x and y axis.
                DataPoint(latestX++, 30.0),
                DataPoint(latestX++, 29.0)
            )
        )

        // on below line adding animation
        lineGraphView.animate()

        // on below line we are setting scrollable
        // for point graph view
        lineGraphView.viewport.isScrollable = false

        // on below line we are setting scalable.
        lineGraphView.viewport.isScalable = true

        // on below line we are setting scalable y
        lineGraphView.viewport.setScalableY(true)

        // on below line we are setting scrollable y
        lineGraphView.viewport.setScrollableY(false)

        lineGraphView.viewport.setMaxX(20.0)

        lineGraphView.viewport.setMinX(0.0)

        // on below line we are setting color for series. I need #FF0000 (RED)
        series.color = getColor(R.color.white)

        // on below line we are adding
        // data series to our graph view.
        lineGraphView.addSeries(series)

        supportActionBar?.hide()

        // on below line we are initializing our list view, array list and array adapter.
        listView = findViewById(R.id.lista)
        arrayAdapter = ArrayAdapter(this, R.layout.list_item, R.id.text1, list)
        // on below line we are adding our array adapter to our list view.
        list.add("Listing test")
        arrayAdapter.notifyDataSetChanged()
        listView.adapter = arrayAdapter

        // on below line we are creating a mqtt client to check the connection status
        mqttClient = MqttAndroidClient(null, null, null)

        // this is the connect button
        val connectBtn: Button = findViewById(R.id.connect)
        connectBtn.setOnClickListener{
            connect(this)
        }

        // this is the publish teste button
        val publishBtn: Button = findViewById(R.id.publish)
        publishBtn.setOnClickListener{
            publish("dripdriplite","teste",1,false)
        }

        // this is the disconnect button
        val disconnectBtn: Button = findViewById(R.id.disconnect)
        disconnectBtn.setOnClickListener{
            disconnect()
        }

        // this is the publish on button
        val onBtn: Button = findViewById(R.id.on)
        onBtn.setOnClickListener{
            if(mqttClient.isConnected){
                publish("dripdriplite","on",1,true)
            }else{
                Toast.makeText(this, "No conection bruh \uD83E\uDD28", Toast.LENGTH_SHORT).show()
            }
        }

        // this is the publish off button
        val offBtn: Button = findViewById(R.id.off)
        offBtn.setOnClickListener{
            if(mqttClient.isConnected){
                publish("dripdriplite","off",1,true)
            }else{
                Toast.makeText(this, "No conection bruh \uD83E\uDD28", Toast.LENGTH_SHORT).show()
            }
        }

        // this is would be a settings button
        val settingsBtn: Button = findViewById(R.id.subscribe)
        settingsBtn.setOnClickListener{

        }

    }

    private lateinit var mqttClient: MqttAndroidClient
    // TAG
    companion object {
        const val TAG = "AndroidMqttClient"
    }

    // this function is for new messages to be added to the list view
    fun newMessage(message: String){
        val regex = Regex("""\d+(\.\d+)?""") // matches any decimal number

        val matchResult = regex.find(message)
        val numberString = matchResult?.value // the matched number string, e.g. "238.56"

        if(!numberString.isNullOrEmpty()){
            val number = numberString.toDouble() // parse the number string to a Double or return null
            // Create a new data point with x = current X value + 1 and y = new Y value.
            val newDataPoint = DataPoint(latestX++, number)
            // Append the new data point to the series.
            series.appendData(newDataPoint, false, 20)
        }
        listView = findViewById(R.id.lista)
        arrayAdapter = ArrayAdapter(this, R.layout.list_item, R.id.text1, list)
        list.add(1, message)
        arrayAdapter.notifyDataSetChanged()
        listView.adapter = arrayAdapter
    }

    // this is the function to connect to broker
    fun connect(context: Context) {
        // this is your broker ip (maintain the port 1883 as it is the standard for MQTT) unless you using ngrok or something else
        val serverURI = "tcp://192.168.1.144:1883"

        // quick check to see if the client is already connected
        if (mqttClient.isConnected) {
            Toast.makeText(this, "Already Connected?? \uD83D\uDE11", Toast.LENGTH_SHORT).show()
            return
        }

        // this is the client id, it must be unique for each client
        mqttClient = MqttAndroidClient(context, serverURI, "kotlin_client514")

        // this is the callback for when a message is received
        mqttClient.setCallback(object : MqttCallback {
            override fun messageArrived(topic: String?, message: MqttMessage?) {
                Log.d(TAG, "Receive message: ${message.toString()} from topic: $topic")
                newMessage(message.toString())
            }

            override fun connectionLost(cause: Throwable?) {
                Log.d(TAG, "Connection lost ${cause.toString()}")
                Toast.makeText(
                    this@MainActivity,
                    "Connection died for no reason \uD83D\uDE1E",
                    Toast.LENGTH_SHORT
                ).show()
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {

            }
        })

        // this is the connection options
        val options = MqttConnectOptions()
        options.userName = "USERNAME"
        options.password = "PASSWORD".toCharArray()
        options.setWill("dripdriplite", "DEDDD".toByteArray(), 0, false)

        try {
            mqttClient.connect(options, null, object : IMqttActionListener {
                // this function is executed when the connection is successful
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Connection success")
                    Toast.makeText(
                        this@MainActivity,
                        "Bro got the connection successfully \uD83D\uDC4D",
                        Toast.LENGTH_SHORT
                    ).show()
                    mqttClient.subscribe("dripdriplite", 0)
                }
                // this function is executed when the connection fails
                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d(TAG, "Connection failure")
                    Toast.makeText(
                        this@MainActivity,
                        "Bro did not get the connection \uD83D\uDE2B",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }

    }

    // this is the function to publish messages (very straightfoward, im sure you can figure it out). Too tired to explain
    fun publish(topic: String, msg: String, qos: Int = 1, retained: Boolean = false) {
        if(!mqttClient.isConnected){
            Toast.makeText(this, "No conection bruh \uD83E\uDD28", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val message = MqttMessage()
            message.payload = msg.toByteArray()
            message.qos = qos
            message.isRetained = retained
            mqttClient.publish(topic, message, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "$msg published to $topic")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d(TAG, "Failed to publish $msg to $topic")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    // Another one pretty much straightfoward. You are smart enough to figure it out. Just a disconnect function
    fun disconnect() {
        if(!mqttClient.isConnected){
            Toast.makeText(this, "No conection bruh \uD83E\uDD28", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            mqttClient.disconnect(null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Disconnected")
                    Toast.makeText(this@MainActivity, "Bro killed the connection \uD83D\uDD2A", Toast.LENGTH_SHORT).show()
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d(TAG, "Failed to disconnect")
                    Toast.makeText(this@MainActivity, "Bro can't run \uD83D\uDE28 Failed to Disconnect", Toast.LENGTH_SHORT).show()
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    // this is the function to subscribe to a topic. Mostly useless since Im doing it in the connect function anyways. But you can use it if you want
    fun subscribe(topic: String, qos: Int = 1) {
        if(!mqttClient.isConnected){
            Toast.makeText(this, "No conection bruh \uD83E\uDD28", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            mqttClient.subscribe(topic, qos, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Subscribed to $topic")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d(TAG, "Failed to subscribe $topic")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }


}