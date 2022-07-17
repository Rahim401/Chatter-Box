package com.graphenix.chatter

import android.content.DialogInterface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import java.net.InetAddress
import java.net.Socket
import java.net.SocketAddress
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    private lateinit var titleVw:TextView
    var userName = "User${Random(System.currentTimeMillis()).nextInt(0,100)}"
    val brg = MsgBridgeTcp(userName)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_main)
        titleVw = findViewById(R.id.title)
        brg.setConnCbl(
            object:ConnCallback{
                override fun handleRequest(name: String, address: String): Boolean {
                    var result:Boolean? = null
                    runOnUiThread {
                        AlertDialog.Builder(this@MainActivity).apply {
                            setTitle("Connection Request")
                            setMessage("Do you want to connect with $name from Ip address $address")
                            setCancelable(false)
                            setPositiveButton("Accept") { _, _ ->
                                result = true
                            }
                            setNegativeButton("Decline") { _, _ ->
                                result = false
                            }
                        }.show()
                    }
                    while (result==null) Thread.sleep(500)
                    return result!!
                }

                override fun handleConnResults(result: String) {
                    if(result.isNotEmpty()) Toast.makeText(this@MainActivity,when(result){
                        "NoReplay" -> "No Replay on this Ip"
                        "UnReachable" -> "Ip not in the network"
                        "Timeout" -> "Can't connect,Check network strength"
                        "2Conn" -> "Given Chatter already connected"
                        "Denied" -> "Chatter denied to connect"
                        "SameDevice" -> "Can't connect to same device"
                        else -> "Error Occurred"
                    },Toast.LENGTH_SHORT).show()
                }

                override fun onScanning() =
                    Toast.makeText(this@MainActivity,"Reaching the Ip address",Toast.LENGTH_SHORT).show()

                override fun onConnecting(user: String)  =
                    Toast.makeText(this@MainActivity,"Connecting to $user",Toast.LENGTH_SHORT).show()

                override fun onConnected(user: String){
                    Toast.makeText(this@MainActivity,"Connected to $user",Toast.LENGTH_SHORT).show()
                    updateFrag()
                }

                override fun onDisconnected(){
                    Toast.makeText(this@MainActivity,"Disconnected",Toast.LENGTH_SHORT).show()
                    titleVw.postDelayed({
                        updateFrag()
                    },1000)
                }
            },
            Handler(mainLooper)
        )
        updateFrag()
    }
    fun updateFrag(){
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.Container, if(brg.isConnected()) ChatFrag() else SearchFrag())
            .commitAllowingStateLoss()
    }

    override fun onBackPressed() {
        if(!brg.isConnected()) super.onBackPressed()
        brg.disconnect()
    }

    override fun onDestroy() {
        brg.close()
        super.onDestroy()
    }
}