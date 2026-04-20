package com.example.nextstep

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.nextstep.ui.setOnClickWithSound

class MiniGamesAdapter(
    private val games: List<MiniGameConfig>,
    private val onClick: (MiniGameConfig) -> Unit
) : RecyclerView.Adapter<MiniGamesAdapter.GameViewHolder>() {

    inner class GameViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cvGameCard: CardView = itemView.findViewById(R.id.cvGameCard)
        val tvGameIcon: TextView = itemView.findViewById(R.id.tvGameIcon)
        val tvGameName: TextView = itemView.findViewById(R.id.tvGameName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mini_game_card, parent, false)
        return GameViewHolder(view)
    }

    override fun getItemCount() = games.size

    override fun onBindViewHolder(holder: GameViewHolder, position: Int) {
        val config = games[position]
        holder.tvGameIcon.text = getGameIcon(config.id)
        holder.tvGameName.text = config.name

        // Background color with safe fallback
        try {
            holder.cvGameCard.setCardBackgroundColor(Color.parseColor(config.backgroundColor))
        } catch (_: IllegalArgumentException) {
            holder.cvGameCard.setCardBackgroundColor(Color.parseColor("#3A3A5E"))
        }

        // Click with sound on the card (single source of truth)
        holder.cvGameCard.setOnClickWithSound { onClick(config) }
    }

    private fun getGameIcon(gameId: String): String = when (gameId) {
        "code_debugger" -> "💻"
        "slogan_matcher" -> "💡"
        "market_predictor" -> "📈"
        "headline_huddle" -> "📰"
        "symptom_matcher" -> "🩺"
        "resource_balancer" -> "🌳"
        "empathy_response" -> "❤️"
        "color_palette_mixer" -> "🎨"
        "lesson_plan_organizer" -> "📝"
        "threat_identifier" -> "🚨"
        "fact_checker" -> "✅"
        else -> "🎮"
    }
}
