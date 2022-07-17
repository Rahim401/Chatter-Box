package com.graphenix.chatter

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.SpannedString
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.text.buildSpannedString
import androidx.fragment.app.Fragment

class SearchFrag: Fragment() {
    private lateinit var ipBoxVw:EditText
    private lateinit var goBtnVw:Button
    private lateinit var ipv4TxVw:TextView
    private lateinit var ipv6TxVw:TextView
    private lateinit var userTxVw:TextView
    private lateinit var clMg:ClipboardManager
    val blueColor = "#3091E6"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.search_frag,container,false)

    private lateinit var ip4Text:String
    private lateinit var ip6Text:String
    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        clMg = activity?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        (activity as MainActivity).brg.listen()

        ipBoxVw = view.findViewById(R.id.ipBox)
        goBtnVw = view.findViewById(R.id.goBtn)
        goBtnVw.setOnClickListener {
            ipBoxVw.text.toString().apply {
                if(isIpAddress()){
                    (activity as MainActivity).brg.connect(this)
                }else
                    Toast.makeText(activity,"Invalid ip address",Toast.LENGTH_SHORT).show()
            }
        }

        ipv4TxVw = view.findViewById(R.id.ipv4Shower)
        ip4Text = ipv4TxVw.text.toString()
        ipv4TxVw.setOnLongClickListener {
            clMg.setPrimaryClip(ClipData.newPlainText("LocalIp",lip ?: "No Network"))
            Toast.makeText(activity,"Local Ip Copied",Toast.LENGTH_SHORT).show()
            true
        }

        ipv6TxVw = view.findViewById(R.id.ipv6Shower)
        ip6Text = ipv6TxVw.text.toString()
        ipv6TxVw.setOnLongClickListener {
            clMg.setPrimaryClip(ClipData.newPlainText("InternetIp",iip ?: "No Internet"))
            Toast.makeText(activity,"Internet Ip Copied",Toast.LENGTH_SHORT).show()
            true
        }
        autoUpdateIp()

        userTxVw = view.findViewById(R.id.userName)
        userTxVw.text = "Your userId is <cl:#0389FD>${(activity as MainActivity).userName}<cl>, This will be the name displayed to the other users.".toHtmlText()
    }

    private var lip:String? = null
    private var iip:String? = null
    @SuppressLint("SetTextI18n")
    private fun updateIpAddress(){
        lip = getWifiApIpAddress()
        ipv4TxVw.text = "<cl:$blueColor>$ip4Text<cl><cl:${if(lip==null) "red" else "#00cf00"}>${lip ?: "No Network"}<cl>".toHtmlText()
        iip = getInternetAddress()
        ipv6TxVw.text = "<cl:$blueColor>$ip6Text<cl><cl:${if(iip==null) "red" else "#00cf00"}>${iip ?: "No Internet"}<cl>".toHtmlText()
    }

    private fun autoUpdateIp(){
        updateIpAddress()
        view?.postDelayed(
            {autoUpdateIp()},5000
        )
    }
}