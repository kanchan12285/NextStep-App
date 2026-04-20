package com.example.nextstep

import android.media.MediaPlayer
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class ResultsActivity : BaseActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var rv: RecyclerView
    private lateinit var adapter: RankingAdapter
    private var roomListener: ListenerRegistration? = null

    // Audio
    private var clickPlayer: MediaPlayer? = null
    private var themePlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_results)

        // Recycler setup
        rv = findViewById(R.id.rvRanking)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = RankingAdapter()
        rv.adapter = adapter

        // Audio setup
        clickPlayer = MediaPlayer.create(this, R.raw.button_click)
        themePlayer = MediaPlayer.create(this, R.raw.app_theme).apply { isLooping = true }

        // Optional UI buttons if present in the layout
        findViewById<Button?>(R.id.btnPlayAgain)?.setOnClickListener {
            playClick()
            finish() // or startActivity(Intent(this, PlayOnlineActivity::class.java))
        }

        // Listen to final ranking
        val roomId = intent.getStringExtra("ROOM_ID") ?: return
        roomListener = db.collection("rooms").document(roomId)
            .addSnapshotListener { doc, _ ->
                val ranks = doc?.get("finalRanking") as? List<Map<String, Any>> ?: return@addSnapshotListener
                val items = ranks.mapIndexed { index, m ->
                    RankRow(
                        place = index + 1,
                        name = m["name"]?.toString() ?: "Player",
                        score = (m["score"] as? Number)?.toInt() ?: 0
                    )
                }
                adapter.submit(items)
                findViewById<TextView?>(R.id.tvResultsTitle)?.text = "Final Ranking (${items.size})"
            }
    }

    private fun playClick() {
        clickPlayer?.let { mp ->
            if (mp.isPlaying) mp.seekTo(0)
            mp.start()
        }
    }

    override fun onResume() {
        super.onResume()
        if (themePlayer?.isPlaying != true) themePlayer?.start()
    }

    override fun onPause() {
        super.onPause()
        if (themePlayer?.isPlaying == true) themePlayer?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        roomListener?.remove()
        clickPlayer?.release(); clickPlayer = null
        themePlayer?.release(); themePlayer = null
    }
}

/* ---------- Simple adapter and model ---------- */

data class RankRow(val place: Int, val name: String, val score: Int)

class RankingAdapter : RecyclerView.Adapter<RankingAdapter.VH>() {

    private val rows = mutableListOf<RankRow>()

    fun submit(newRows: List<RankRow>) {
        rows.clear()
        rows.addAll(newRows)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
        val v = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_rank_row, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(rows[position])

    override fun getItemCount(): Int = rows.size

    class VH(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val tvPlace: TextView = itemView.findViewById(R.id.tvPlace)
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvScore: TextView = itemView.findViewById(R.id.tvScore)
        fun bind(row: RankRow) {
            tvPlace.text = row.place.toString()
            tvName.text = row.name
            tvScore.text = row.score.toString()
        }
    }
}
