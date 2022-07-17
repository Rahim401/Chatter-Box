package com.graphenix.chatter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


data class Message(val msg:String,val byUs:Boolean=true,val msgAt:Long=System.currentTimeMillis())
class MsgAdapter: RecyclerView.Adapter<MsgAdapter.ViewHolder>() {
    private val msgList = ArrayList<Message>(10)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.msg_layout,parent,false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val msg = msgList[position]
        val txVw = if(msg.byUs) holder.ourMsgVw else holder.theirMsgVw
        txVw.visibility = View.VISIBLE
        txVw.text = msg.msg
    }

    override fun getItemCount(): Int = msgList.size

    fun addMsgS(vararg msgS:Message){
        msgS.forEach {
            msgList.add(it)
            notifyItemChanged(msgList.size-1)
        }
    }


    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ourMsgVw = itemView.findViewById<TextView>(R.id.ourMsg)
        val theirMsgVw = itemView.findViewById<TextView>(R.id.theirMsg)
    }
}