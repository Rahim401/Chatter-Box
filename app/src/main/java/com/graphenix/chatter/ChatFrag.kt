package com.graphenix.chatter

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ChatFrag: Fragment() {
    private lateinit var msgBoxVw:EditText
    private lateinit var sndBtnVw:Button
    private lateinit var userTxVw:TextView
    private lateinit var ipTxVw:TextView

    private lateinit var msgAdapter: MsgAdapter
    private lateinit var msgLstVw:RecyclerView
    val blueColor = "#3091E6"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.chat_frag,container,false)

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val brg = (activity as MainActivity).brg
        val useData = brg.get2Data()

        userTxVw = view.findViewById(R.id.userName)
        userTxVw.text = useData.first.ifEmpty { "NoName" }

        ipTxVw = view.findViewById(R.id.ipShower)
        ipTxVw.text = useData.second ?: "0.0.0.0"


        msgBoxVw = view.findViewById(R.id.msgBox)
        sndBtnVw = view.findViewById(R.id.sndBtn)
        sndBtnVw.setOnClickListener {
            val msg = msgBoxVw.text.toString()
            if(msg.isNotBlank()) {
                msgAdapter.addMsgS(Message(msg,true))
                brg.sendMessage(msg)
                msgBoxVw.setText("")
                msgLstVw.scrollToPosition(msgAdapter.itemCount-1)
            }
        }

        msgAdapter = MsgAdapter()
        msgLstVw = view.findViewById(R.id.msgLst)
        msgLstVw.layoutManager = LinearLayoutManager(requireContext())
        msgLstVw.adapter = msgAdapter
        println("Attached")


//        msgAdapter.addMsgS(
//            Message("Hi",true),
//            Message("Hello bro",false),
//            Message("How are you?",false),
//            Message("Fine Man"),
//            Message("What about you?")
//        )

        brg.setOnMsgRecvListener{
            msgAdapter.addMsgS(Message(it,false))
        }
    }



}