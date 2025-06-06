package ba.sum.fpmoz.aplikacijazaupravljanjeosobnimfinacijama

import android.annotation.SuppressLint
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

    @SuppressLint("PrivateResource")
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

        // Firebase reference - spremamo prihode u "activities" kao i ostali troškovi
        database = FirebaseDatabase.getInstance().getReference("activities")

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
            val iznosStr = editIznos.text.toString().trim()
            val opis = editOpis.text.toString().trim()

            if (naziv.isEmpty() || iznosStr.isEmpty()) {
                Toast.makeText(this, "Popunite naziv i iznos!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amountDouble = iznosStr.toDoubleOrNull()
            if (amountDouble == null || amountDouble <= 0) {
                Toast.makeText(this, "Unesite ispravan iznos!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val id = database.push().key ?: ""
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

            val prihod = ActivityItem(
                id = id,
                userId = currentUserId,
                type = ActivityItem.TYPE_INCOME, // obično 1
                naziv = naziv,
                amount = amountDouble,  // OVDJE: saljemo kao Double, ne kao String
                opis = opis,
                datum = "",          // Ako nemate datum, možete ostaviti prazno ili postaviti trenutno vrijeme
                kategorija = ""      // Ako kategorija nije potrebna, ostavite prazno
            )

            database.child(id).setValue(prihod).addOnCompleteListener { task ->
                if (task.isSuccessful) {
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
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        database.orderByChild("userId").equalTo(currentUserId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    prihodi.clear()
                    for (child in snapshot.children) {
                        val item = child.getValue(ActivityItem::class.java)
                        if (item != null && item.type == ActivityItem.TYPE_INCOME) {
                            item.id = child.key ?: ""
                            prihodi.add(item)
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
