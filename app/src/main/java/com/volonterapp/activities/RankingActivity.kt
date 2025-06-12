package com.volonterapp.activities

import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.volonterapp.R

data class AppUser(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val brojsati: Int = 0
)

class RankingActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RankingAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ranking)

        setupViews()
        loadUserData()
    }

    private fun setupViews() {
        recyclerView = findViewById(R.id.recyclerViewRanking)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = RankingAdapter()
        recyclerView.adapter = adapter
    }

    private fun loadUserData() {
        // Ovdje možeš dodati svoje korisničke podatke
        // Možeš ih učitati iz baze podataka, API-ja, ili bilo kojeg drugog izvora
        val users = getSampleUsers()

        // Sortiranje korisnika po broju sati (od najvećeg prema najmangijem)
        val sortedUsers = users.sortedByDescending { it.brojsati }

        adapter.updateUsers(sortedUsers)
    }

    private fun getSampleUsers(): List<AppUser> {
        // Primjer korisnika - zamijeni s pravim podacima
        return listOf(
            AppUser("1", "Ana Anić", "ana@example.com", 125),
            AppUser("2", "Marko Marković", "marko@example.com", 98),
            AppUser("3", "Petra Petrić", "petra@example.com", 156),
            AppUser("4", "Ivan Ivanović", "ivan@example.com", 87),
            AppUser("5", "Sara Sarić", "sara@example.com", 134),
            AppUser("6", "Luka Lukić", "luka@example.com", 76),
            AppUser("7", "Mia Mikić", "mia@example.com", 189),
            AppUser("8", "Tomislav Tomić", "tomo@example.com", 112)
        )
    }
}

class RankingAdapter : RecyclerView.Adapter<RankingAdapter.RankingViewHolder>() {

    private var users: List<AppUser> = emptyList()

    fun updateUsers(newUsers: List<AppUser>) {
        users = newUsers
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RankingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ranking, parent, false)
        return RankingViewHolder(view)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onBindViewHolder(holder: RankingViewHolder, position: Int) {
        holder.bind(users[position], position + 1)
    }

    override fun getItemCount(): Int = users.size

    class RankingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvPosition: TextView = itemView.findViewById(R.id.tvPosition)
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvEmail: TextView = itemView.findViewById(R.id.tvEmail)
        private val tvHours: TextView = itemView.findViewById(R.id.tvHours)

        @RequiresApi(Build.VERSION_CODES.M)
        fun bind(user: AppUser, position: Int) {
            tvPosition.text = "$position."
            tvName.text = user.name
            tvEmail.text = user.email
            tvHours.text = "${user.brojsati} sati"

            // Promjena boje za prva tri mjesta
            when (position) {
                1 -> {
                    tvPosition.setTextColor(itemView.context.getColor(android.R.color.holo_orange_dark))
                    tvPosition.text = "🥇 1."
                }
                2 -> {
                    tvPosition.setTextColor(itemView.context.getColor(android.R.color.darker_gray))
                    tvPosition.text = "🥈 2."
                }
                3 -> {
                    tvPosition.setTextColor(itemView.context.getColor(android.R.color.holo_orange_light))
                    tvPosition.text = "🥉 3."
                }
            }
        }
    }
}