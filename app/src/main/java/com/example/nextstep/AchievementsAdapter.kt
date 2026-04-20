package com.example.nextstep

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AchievementsAdapter(
    private val achievementsList: List<Map<String, Any>>
) : RecyclerView.Adapter<AchievementsAdapter.AchievementViewHolder>() {

    inner class AchievementViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvAchievementName: TextView = itemView.findViewById(R.id.tvAchievementName)
        val tvAchievementDesc: TextView = itemView.findViewById(R.id.tvAchievementDesc)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AchievementViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_achievement, parent, false)
        return AchievementViewHolder(view)
    }

    override fun onBindViewHolder(holder: AchievementViewHolder, position: Int) {
        val item = achievementsList[position]
        holder.tvAchievementName.text = item["name"] as? String ?: "Achievement"
        holder.tvAchievementDesc.text = item["description"] as? String ?: ""
    }

    override fun getItemCount(): Int = achievementsList.size
}
