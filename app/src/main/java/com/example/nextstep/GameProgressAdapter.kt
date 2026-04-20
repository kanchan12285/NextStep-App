package com.example.nextstep

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class GameProgressAdapter(
    private val progressList: List<Map<String, Any>>
) : RecyclerView.Adapter<GameProgressAdapter.ProgressViewHolder>() {

    inner class ProgressViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvGameName: TextView = itemView.findViewById(R.id.tvGameName)
        val tvScore: TextView = itemView.findViewById(R.id.tvScore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProgressViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_game_progress, parent, false)
        return ProgressViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProgressViewHolder, position: Int) {
        val item = progressList[position]
        holder.tvGameName.text = item["gameName"] as? String ?: "Unknown Game"
        holder.tvScore.text = (item["score"] as? Long ?: 0).toString()
    }

    override fun getItemCount(): Int = progressList.size
}
