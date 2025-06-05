package ba.sum.fpmoz.aplikacijazaupravljanjeosobnimfinacijama

import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class Ishodi : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var pieChart: PieChart
    private lateinit var barChartTop5: BarChart
    private lateinit var txtIncome: TextView
    private lateinit var txtExpense: TextView
    private lateinit var rvTopExpenses: RecyclerView
    private lateinit var database: DatabaseReference

    private val expensesByCategory = mutableMapOf<String, Float>()
    private val topExpenses = mutableListOf<ActivityItem>()
    private val userId: String by lazy { FirebaseAuth.getInstance().currentUser?.uid ?: "" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ishodi)

        // Prikaz ispod status bara
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        // Inicijalizacija UI elemenata
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Izvješće"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(android.R.drawable.ic_media_previous)

        pieChart = findViewById(R.id.pieChartReport)
        barChartTop5 = findViewById(R.id.barChartTop5)
        txtIncome = findViewById(R.id.tvUkupniPrihodi)
        txtExpense = findViewById(R.id.tvUkupniRashodi)
        rvTopExpenses = findViewById(R.id.rvTop5Troskova)

        rvTopExpenses.layoutManager = LinearLayoutManager(this)
        rvTopExpenses.adapter = ActivityAdapter(topExpenses) { }

        database = FirebaseDatabase.getInstance().getReference("activities")

        // Učitavanje podataka
        loadReportData()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun loadReportData() {
        database.orderByChild("userId").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var totalIncome = 0f
                    var totalExpense = 0f
                    expensesByCategory.clear()
                    topExpenses.clear()

                    for (child in snapshot.children) {
                        val item = child.getValue(ActivityItem::class.java) ?: continue
                        if (item.type == ActivityItem.TYPE_INCOME) {
                            totalIncome += item.amount.toFloat()
                        } else if (item.type == ActivityItem.TYPE_EXPENSE) {
                            totalExpense += item.amount.toFloat()
                            val category = item.kategorija.ifEmpty { "Ostalo" }
                            expensesByCategory[category] =
                                expensesByCategory.getOrDefault(category, 0f) + item.amount.toFloat()

                            topExpenses.add(item)
                        }
                    }

                    topExpenses.sortByDescending { it.amount }
                    if (topExpenses.size > 5) {
                        topExpenses.subList(5, topExpenses.size).clear()
                    }

                    rvTopExpenses.adapter?.notifyDataSetChanged()

                    showPieChart()
                    showBarChartTop5()

                    txtIncome.text = "Ukupni prihodi: %.2f KM".format(totalIncome)
                    txtExpense.text = "Ukupni troškovi: %.2f KM".format(totalExpense)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@Ishodi, "Greška: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showPieChart() {
        val entries = expensesByCategory.map { PieEntry(it.value, it.key) }

        val dataSet = PieDataSet(entries, "Kategorije")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        dataSet.valueTextColor = Color.BLACK
        dataSet.valueTextSize = 14f

        pieChart.data = PieData(dataSet)
        pieChart.setUsePercentValues(false)
        pieChart.description = Description().apply { text = "" }
        pieChart.setDrawEntryLabels(true)
        pieChart.legend.isEnabled = true
        pieChart.invalidate()
    }

    private fun showBarChartTop5() {
        val entries = topExpenses.mapIndexed { index, item ->
            BarEntry(index.toFloat(), item.amount.toFloat())
        }

        val labels = topExpenses.map { it.kategorija.ifEmpty { "Ostalo" } }

        val dataSet = BarDataSet(entries, "Top 5 troškova")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        dataSet.valueTextColor = Color.BLACK
        dataSet.valueTextSize = 14f

        val barData = BarData(dataSet)
        barChartTop5.data = barData
        barChartTop5.description.isEnabled = false
        barChartTop5.legend.isEnabled = false

        val xAxis = barChartTop5.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(false)
        xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
        xAxis.textColor = Color.BLACK

        barChartTop5.axisLeft.textColor = Color.BLACK
        barChartTop5.axisRight.isEnabled = false

        barChartTop5.invalidate()
    }
}
