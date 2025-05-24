package ba.sum.fpmoz.aplikacijazaupravljanjeosobnimfinacijama

import android.graphics.PorterDuff
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class IncomeActivity : AppCompatActivity() {

    private lateinit var editNaziv: TextInputEditText
    private lateinit var editIznos: TextInputEditText
    private lateinit var editOpis: TextInputEditText
    private lateinit var btnDodaj: MaterialButton
    private lateinit var recyclerView: RecyclerView

    private val prihodi = mutableListOf<ActivityItem>()
    private lateinit var database: DatabaseReference
    private lateinit var adapter: ActivityAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_income)

        // Toolbar setup
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Prihodi"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val upArrow = ContextCompat.getDrawable(this, androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        upArrow?.setColorFilter(ContextCompat.getColor(this, android.R.color.white), PorterDuff.Mode.SRC_ATOP)
        supportActionBar?.setHomeAsUpIndicator(upArrow)

        // Bind UI elements
        editNaziv = findViewById(R.id.editNaziv)
        editIznos = findViewById(R.id.editIznos)
        editOpis = findViewById(R.id.editOpis)
        btnDodaj = findViewById(R.id.btnDodaj)
        recyclerView = findViewById(R.id.recyclerView)

        // Firebase reference
        database = FirebaseDatabase.getInstance().getReference("prihodi")

        // Initialize adapter with delete callback
        adapter = ActivityAdapter(prihodi) { prihod ->
            database.child(prihod.id).removeValue().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val index = prihodi.indexOfFirst { it.id == prihod.id }
                    if (index != -1) {
                        prihodi.removeAt(index)
                        adapter.notifyItemRemoved(index)
                    }
                    Toast.makeText(this, "Prihod obrisan", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Greška prilikom brisanja", Toast.LENGTH_SHORT).show()
                }
            }
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Load income from Firebase
        ucitajPrihodeIzFirebase()

        // Add new income on button click
        btnDodaj.setOnClickListener {
            val naziv = editNaziv.text.toString().trim()
            val iznos = editIznos.text.toString().trim()
            val opis = editOpis.text.toString().trim()

            if (naziv.isEmpty() || iznos.isEmpty() || opis.isEmpty()) {
                Toast.makeText(this, "Popunite sva polja!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amountDouble = iznos.toDoubleOrNull()
            if (amountDouble == null) {
                Toast.makeText(this, "Neispravan iznos!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val id = database.push().key ?: ""
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

            // amount sada ide kao Double
            val prihod = ActivityItem(
                id = id,
                userId = currentUserId,
                type = "Prihod",
                description = naziv,
                amount = amountDouble,
                details = opis
            )

            database.child(id).setValue(prihod).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Dodaj u "aktivnosti" isto sa userId i amount kao Double
                    val aktivnostiRef = FirebaseDatabase.getInstance().getReference("aktivnosti")
                    val activityId = aktivnostiRef.push().key ?: ""
                    val activityItem = mapOf(
                        "id" to activityId,
                        "userId" to currentUserId,
                        "type" to "Prihod",
                        "description" to naziv,
                        "amount" to amountDouble,
                        "details" to opis
                    )
                    aktivnostiRef.child(activityId).setValue(activityItem)

                    Toast.makeText(this, "Prihod dodan!", Toast.LENGTH_SHORT).show()
                    editNaziv.text?.clear()
                    editIznos.text?.clear()
                    editOpis.text?.clear()
                } else {
                    Toast.makeText(this, "Greška prilikom dodavanja prihoda", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun ucitajPrihodeIzFirebase() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                prihodi.clear()
                for (child in snapshot.children) {
                    val prihodRaw = child.value as? Map<*, *>
                    if (prihodRaw != null) {
                        val amountAny = prihodRaw["amount"]
                        val amountDouble = when (amountAny) {
                            is Double -> amountAny
                            is Long -> amountAny.toDouble()
                            is String -> amountAny.toDoubleOrNull() ?: 0.0
                            else -> 0.0
                        }
                        val prihod = ActivityItem(
                            id = prihodRaw["id"] as? String ?: "",
                            userId = prihodRaw["userId"] as? String ?: "",
                            type = prihodRaw["type"] as? String ?: "",
                            description = prihodRaw["description"] as? String ?: "",
                            amount = amountDouble,
                            details = prihodRaw["details"] as? String ?: ""
                        )
                        prihodi.add(prihod)
                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@IncomeActivity, "Greška: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

}
