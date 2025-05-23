package ba.sum.fpmoz.aplikacijazaupravljanjeosobnimfinacijama

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var btnMenu: ImageButton
    private lateinit var txtWelcome: TextView
    private lateinit var progressConsumption: ProgressBar
    private lateinit var rvActivities: RecyclerView

    private lateinit var activityAdapter: ActivityAdapter
    private val activities = mutableListOf<ActivityItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navbar)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        btnMenu = findViewById(R.id.btnMenu)
        txtWelcome = findViewById(R.id.txtWelcome)
        progressConsumption = findViewById(R.id.progressConsumption)
        rvActivities = findViewById(R.id.rvActivities)

        // Postavljanje welcome teksta
        val currentUser = auth.currentUser
        val displayName = currentUser?.displayName
        val email = currentUser?.email

        val userNameToShow = when {
            !displayName.isNullOrEmpty() -> displayName
            !email.isNullOrEmpty() -> email.substringBefore("@")
            else -> "korisniče"
        }

        val formattedUserName = userNameToShow.replaceFirstChar { it.uppercaseChar() }
        txtWelcome.text = "Dobrodošao/la, $formattedUserName!"

        // Postavi kružni indikator potrošnje (primjer: 65%)
        progressConsumption.max = 100
        progressConsumption.progress = 65

        // Postavljanje RecyclerView-a i adaptera sa lambda funkcijom za brisanje
        rvActivities.layoutManager = LinearLayoutManager(this)
        activityAdapter = ActivityAdapter(activities) { activityItem ->
            deleteActivity(activityItem)
        }
        rvActivities.adapter = activityAdapter

        // Učitaj aktivnosti korisnika iz Firestore
        loadUserActivities()

        // Otvaranje drawer menija
        btnMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.dashboard -> Toast.makeText(this, "Početna", Toast.LENGTH_SHORT).show()
                R.id.income -> startActivity(Intent(this, IncomeActivity::class.java))
                R.id.expense -> startActivity(Intent(this, ExpenseActivity::class.java))
                R.id.settings -> Toast.makeText(this, "Postavke", Toast.LENGTH_SHORT).show()
                R.id.logout -> {
                    auth.signOut()
                    Toast.makeText(this, "Odjavljeni ste", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun loadUserActivities() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("activities")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                activities.clear()
                activities.addAll(documents.mapNotNull { it.toObject(ActivityItem::class.java) })
                activityAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Greška pri dohvaćanju aktivnosti", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteActivity(activityItem: ActivityItem) {
        db.collection("activities")
            .document(activityItem.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Aktivnost obrisana", Toast.LENGTH_SHORT).show()
                loadUserActivities()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Greška pri brisanju aktivnosti", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
