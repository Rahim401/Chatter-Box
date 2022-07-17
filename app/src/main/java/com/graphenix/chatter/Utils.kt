package com.graphenix.chatter

import android.text.Html
import java.io.InputStream
import java.io.OutputStream
import java.net.*
import java.util.*

private const val ipv4Pattern = "(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])"
private const val ipv6Pattern = "([0-9a-f]{1,4}:){7}([0-9a-f]){1,4}"

fun String.isIpAddress() =
    matches(Regex(ipv4Pattern)) || matches(Regex(ipv6Pattern))
fun String.toHtmlText() =
    Html.fromHtml(
        replace("cl:","font color=")
        .replace("<cl>","</font>")
    )
fun DatagramPacket.flush() = data.forEachIndexed { index, _ ->
    data[index] = 0
}

fun InputStream.getData():Pair<Int,String> {
    val fst = read()
    val dataSz = read()
    if(dataSz==-1) return Pair(5,"")
    val data = ByteArray(dataSz)
    read(data)
    return Pair(fst,data.decodeToString())
}
fun OutputStream.putData(spc:Int,data:String=""){
    write(spc)
    data.encodeToByteArray().apply {
        write(size)
        write(this)
    }
}

fun getWifiApIpAddress(): String? {
    try {
        val en: Enumeration<NetworkInterface> = NetworkInterface.getNetworkInterfaces()
        while (en.hasMoreElements()) {
            val intf: NetworkInterface = en.nextElement()
            if (listOf("ap","wl").any { intf.name.contains(it) }) {
                val enumIpAddr: Enumeration<InetAddress> = intf.inetAddresses
                while (enumIpAddr.hasMoreElements()) {
                    val inetAddress: InetAddress = enumIpAddr.nextElement()
                    if (!inetAddress.isLoopbackAddress && inetAddress.address.size == 4)
                        return inetAddress.hostAddress
                }
            }
        }
    } catch (ex: SocketException) { ex.printStackTrace() }
    return null
}


fun getInternetAddress(): String? {
    try {
        val en: Enumeration<NetworkInterface> = NetworkInterface.getNetworkInterfaces()
        while (en.hasMoreElements()) {
            val intf: NetworkInterface = en.nextElement()
            val enumIpAddr: Enumeration<InetAddress> = intf.inetAddresses
            while (enumIpAddr.hasMoreElements()) {
                val inetAddress: InetAddress = enumIpAddr.nextElement()
                if (!inetAddress.isLoopbackAddress && inetAddress is Inet6Address && inetAddress.hostAddress?.contains("::")==false)
                    return inetAddress.hostAddress?.run {
                        val frm = indexOf("%")
                        if(frm==-1) return@run this
                        removeRange(indexOf("%"),length)
                    }
            }
        }
    } catch (ex: SocketException) { ex.printStackTrace() }
    return null
}