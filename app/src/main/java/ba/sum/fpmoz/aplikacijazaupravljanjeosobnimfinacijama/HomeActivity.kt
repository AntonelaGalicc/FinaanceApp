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
import ba.sum.fpmoz.aplikacijazaupravljanjeosobnimfinacijama.ExpenseActivity
import ba.sum.fpmoz.aplikacijazaupravljanjeosobnimfinacijama.Ishodi
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class HomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var btnMenu: ImageButton
    private lateinit var txtWelcome: TextView
    private lateinit var progressConsumption: ProgressBar
    private lateinit var txtBalance: TextView
    private lateinit var rvActivities: RecyclerView
    private lateinit var pieChart: PieChart

    private lateinit var activityAdapter: ActivityAdapter
    private val activities = mutableListOf<ActivityItem>()

    private lateinit var database: DatabaseReference
    private var balanceListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navbar)

        auth = FirebaseAuth.getInstance()

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        btnMenu = findViewById(R.id.btnMenu)
        txtWelcome = findViewById(R.id.txtWelcome)
        progressConsumption = findViewById(R.id.progressConsumption)
        txtBalance = findViewById(R.id.txtBalance)
        rvActivities = findViewById(R.id.rvActivities)
        pieChart = findViewById(R.id.pieChart) // moraš dodati PieChart u layout

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Niste prijavljeni!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val userNameToShow = currentUser.displayName?.takeIf { it.isNotEmpty() }
            ?: currentUser.email?.substringBefore("@")
            ?: "Korisniče"
        txtWelcome.text = "Pozdrav, ${userNameToShow.replaceFirstChar { it.uppercaseChar() }}!"

        progressConsumption.max = 100
        progressConsumption.progress = 65

        rvActivities.layoutManager = LinearLayoutManager(this)
        activityAdapter = ActivityAdapter(activities) { activityItem ->
            deleteActivity(activityItem)
        }
        rvActivities.adapter = activityAdapter

        database = FirebaseDatabase.getInstance().getReference("activities")

        loadUserActivities()
        loadUserBalance()

        btnMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.dashboard -> Toast.makeText(this, "Početna", Toast.LENGTH_SHORT).show()
                R.id.income -> startActivity(Intent(this, IncomeActivity::class.java))
                R.id.expense -> startActivity(Intent(this, ExpenseActivity::class.java))
                R.id.ishodi -> startActivity(Intent(this, Ishodi::class.java))
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

        database.orderByChild("userId").equalTo(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    activities.clear()
                    for (child in snapshot.children) {
                        val activityItem = child.getValue(ActivityItem::class.java)
                        if (activityItem != null) {
                            if (activityItem.id.isEmpty()) {
                                activityItem.id = child.key ?: ""
                            }
                            activities.add(activityItem)
                        }
                    }
                    activityAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@HomeActivity, "Greška pri učitavanju aktivnosti: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun deleteActivity(activityItem: ActivityItem) {
        database.child(activityItem.id).removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Aktivnost obrisana", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Greška pri brisanju aktivnosti", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadUserBalance() {
        val userId = auth.currentUser?.uid ?: return

        balanceListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var totalIncome = 0f
                var totalExpense = 0f

                for (child in snapshot.children) {
                    val activity = child.getValue(ActivityItem::class.java)
                    if (activity != null) {
                        when (activity.type) {
                            ActivityItem.TYPE_INCOME -> totalIncome += activity.amount.toFloat()
                            ActivityItem.TYPE_EXPENSE -> totalExpense += activity.amount.toFloat()
                        }
                    }
                }

                val saldo = totalIncome - totalExpense
                txtBalance.text = "Ukupno: %.2f KM".format(saldo)

                showPieChart(totalIncome, totalExpense, saldo)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@HomeActivity, "Greška pri učitavanju stanja: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }

        database.orderByChild("userId").equalTo(userId)
            .addValueEventListener(balanceListener as ValueEventListener)
    }

    private fun showPieChart(income: Float, expense: Float, saldo: Float) {
        val entries = ArrayList<PieEntry>()
        if (income > 0) entries.add(PieEntry(income, "Prihodi"))
        if (expense > 0) entries.add(PieEntry(expense, "Troškovi"))

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = listOf(
            android.graphics.Color.rgb(76, 175, 80), // Zeleno
            android.graphics.Color.rgb(244, 67, 54)  // Crveno
        )
        dataSet.valueTextSize = 14f
        dataSet.valueTextColor = android.graphics.Color.WHITE

        val data = PieData(dataSet)

        pieChart.data = data
        pieChart.centerText = "Stanje na računu\n%.2f KM".format(saldo)
        pieChart.setCenterTextSize(16f)
        pieChart.setUsePercentValues(false)
        pieChart.description = Description().apply { text = "" }
        pieChart.setDrawEntryLabels(true)
        pieChart.legend.isEnabled = true
        pieChart.invalidate()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val userId = auth.currentUser?.uid
        if (userId != null && balanceListener != null) {
            database.orderByChild("userId").equalTo(userId)
                .removeEventListener(balanceListener!!)
        }
    }
}
