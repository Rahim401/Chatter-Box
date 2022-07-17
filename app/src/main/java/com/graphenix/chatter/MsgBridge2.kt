package com.graphenix.chatter

import android.os.Handler
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.Error
import java.net.*
import kotlin.jvm.internal.Intrinsics


const val PORT = 54002
const val RETRY = 5


interface ConnCallback{
    fun handleRequest(name:String,address:String):Boolean
    fun handleConnResults(result:String)
    fun onScanning()
    fun onConnecting(user:String)
    fun onConnected(user:String)
    fun onDisconnected()
}

class MsgBridgeTcp(val userName:String){
    enum class State{
        Initialized,
        Connecting,
        Connected,
        Closed
    }

    private var trsSk:Socket? = null
    private var inStream:InputStream? = null
    private var outStream:OutputStream? = null

    private val srvSk = ServerSocket(PORT)
    private var frm2Address:InetAddress? = null
    private var toUserName:String = ""
    private var currentState = State.Initialized

    private var uiHand:Handler? = null
    private var connCbl:ConnCallback? = null

    private var onMsgReceived:((String)->Unit)? = null
    fun setOnMsgRecvListener(lis:(String)->Unit){
        onMsgReceived = lis
    }

    fun setConnCbl(cbl:ConnCallback,hand:Handler){
        connCbl = cbl
        uiHand = hand
    }

    fun isConnected() = currentState==State.Connected

    fun get2Data() = Pair(
        toUserName,
        frm2Address?.hostAddress
    )


    private var isSearching = false
    fun connect(ip:String){
        if (isSearching || currentState!=State.Initialized) return
        isSearching = true
        Thread{
            var connResult = ""
            val isIpv4 = !ip.contains(":")
            if(ip==(if(isIpv4) getWifiApIpAddress() else getInternetAddress()))
                connResult = "SameDevice"
            else try{
                val tmpSk = Socket()
                uiHand?.post { connCbl?.onScanning() }
                tmpSk.connect(InetSocketAddress(ip, PORT),5000)
                val outStm = tmpSk.getOutputStream()
                if(currentState==State.Initialized){
                    currentState = State.Connecting
                    outStm.putData(0,userName)
                    val inStm = tmpSk.getInputStream()
                    val (spc,name) = inStm.getData()
                    println("$spc $name")
                    if(spc==1){
                        uiHand?.post { connCbl?.onConnecting(name) }
                        val (conRes,_) = inStm.getData()
                        if (conRes==2){
                            currentState = State.Connected
                            trsSk = tmpSk
                            inStream = inStm
                            outStream = outStm
                            toUserName = name

                            frm2Address = trsSk?.inetAddress
                            uiHand?.post { connCbl?.onConnected(name) }
                            receiveMessage()
                        }else{
                            currentState = State.Initialized
                            connResult = "Denied"
                            tmpSk.close()
                            inStm.close()
                            outStm.close()
                        }
                    }else{
                        when(spc){
                            10->{
                                tmpSk.close()
                                inStm.close()
                                outStm.close()
                                connResult = "2Conn"
                            }
                            else-> outStm.putData(11,userName)
                        }
                        currentState = State.Initialized
                    }
                }else outStm.putData(10,userName)
            }
            catch (e:EOFException){ connResult="NoReplay"; e.printStackTrace() }
            catch(e:SocketTimeoutException){ connResult="NoReplay"; e.printStackTrace() }
            catch(e:IOException){ connResult="UnReachable"; e.printStackTrace() }
            uiHand?.post { connCbl?.handleConnResults(connResult) }

            isSearching = false
        }.start()
    }

    private var isListening = false
    fun listen(){
        if (isListening || currentState!=State.Initialized) return
        isListening = true
        srvSk.reuseAddress = true
        Thread{
            while(currentState==State.Initialized){
                try{
                    val tmpSk = srvSk.accept()
                    val outStm = tmpSk.getOutputStream()
                    if(currentState!=State.Initialized){
                        outStm.putData(10,userName)
                        continue
                    }
                    currentState = State.Connecting
                    val inStm = tmpSk.getInputStream()
                    val (spc,name) = inStm.getData()
                    println("$spc $name")
                    if(spc==0){
                        outStm.putData(1,userName)
                        val accepted = connCbl?.handleRequest(name,tmpSk.inetAddress.hostAddress ?: "0.0.0.0")?:true
                        outStm.putData(if(accepted) 2 else 3)
                        if(accepted){
                            currentState = State.Connected
                            trsSk = tmpSk
                            inStream = inStm
                            outStream = outStm
                            toUserName = name
                            frm2Address = trsSk?.inetAddress
                            uiHand?.post { connCbl?.onConnected(name) }
                            receiveMessage()

                        }else currentState = State.Initialized
                    }else{
                        when(spc){
                            10->{
                                tmpSk.close()
                                inStm.close()
                                outStm.close()
                            }
                            else-> outStm.putData(11,userName)
                        }
                        currentState = State.Initialized
                    }
                }catch (e:Exception){
                    if(currentState==State.Connecting)
                        currentState = State.Initialized
                }
            }
            isListening = false
        }.start()
    }



    fun sendMessage(msg:String){
        if(currentState!=State.Connected) return
        Thread{
            outStream?.putData(4,msg)
        }.start()
    }

    private var isReceiving = false
    private fun receiveMessage(){
        if(currentState!=State.Connected || isReceiving) return
        isReceiving = true
        Thread{
            try{
                while (currentState==State.Connected){
                    if(inStream==null) break
                    val (spc,msg) = inStream!!.getData()
                    when(spc){
                        4 -> uiHand?.post{onMsgReceived?.invoke(msg)}
                        5 -> break
                    }
                }
            }catch (e:Exception){ e.printStackTrace() }
            disconnect()
            isReceiving = false
        }.start()
    }

    fun disconnect(){
        if(currentState!=State.Connected) return
        if(!srvSk.isClosed) Thread{
            try{ outStream?.putData(5) }
            catch (e:Error){ e.printStackTrace() }
            catch (e:Exception){ e.printStackTrace() }
        }.start()
        uiHand?.post { connCbl?.onDisconnected() }
        frm2Address = null
        currentState = State.Initialized

        trsSk?.close()
        inStream?.close()
        outStream?.close()
    }

    fun close(){
        disconnect()
        srvSk.close()
        currentState = State.Closed
    }
}