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
import ba.sum.fpmoz.aplikacijazaupravljanjeosobnimfinacijama.ActivityItem

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

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Prihodi"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val upArrow = ContextCompat.getDrawable(this, androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        upArrow?.setColorFilter(ContextCompat.getColor(this, android.R.color.white), PorterDuff.Mode.SRC_ATOP)
        supportActionBar?.setHomeAsUpIndicator(upArrow)

        editNaziv = findViewById(R.id.editNaziv)
        editIznos = findViewById(R.id.editIznos)
        editOpis = findViewById(R.id.editOpis)
        btnDodaj = findViewById(R.id.btnDodaj)
        recyclerView = findViewById(R.id.recyclerView)

        database = FirebaseDatabase.getInstance().getReference("activities")

        adapter = ActivityAdapter(prihodi) { prihod -> deleteIncome(prihod) }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        ucitajPrihodeIzFirebase()

        btnDodaj.setOnClickListener {
            dodajNoviPrihod()
        }
    }

    private fun dodajNoviPrihod() {
        val naziv = editNaziv.text.toString().trim()
        val iznosStr = editIznos.text.toString().trim()
        val opis = editOpis.text.toString().trim()

        if (naziv.isEmpty() || iznosStr.isEmpty()) {
            Toast.makeText(this, "Popunite naziv i iznos!", Toast.LENGTH_SHORT).show()
            return
        }

        val amountDouble = iznosStr.toDoubleOrNull()
        if (amountDouble == null || amountDouble <= 0) {
            Toast.makeText(this, "Unesite ispravan iznos!", Toast.LENGTH_SHORT).show()
            return
        }

        val id = database.push().key ?: ""
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        val prihod = ActivityItem(
            id = id,
            userId = currentUserId,
            type = ActivityItem.TYPE_INCOME,
            naziv = naziv,
            amount = amountDouble,
            opis = opis,
            datum = "",
            kategorija = ""
        )

        database.child(id).setValue(prihod).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Prihod dodan!", Toast.LENGTH_SHORT).show()
                clearInputs()
            } else {
                Toast.makeText(this, "Greška prilikom dodavanja prihoda", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun clearInputs() {
        editNaziv.text?.clear()
        editIznos.text?.clear()
        editOpis.text?.clear()
    }

    private fun deleteIncome(item: ActivityItem) {
        database.child(item.id).removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Prihod obrisan!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Greška prilikom brisanja prihoda", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun ucitajPrihodeIzFirebase() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        database.orderByChild("userId").equalTo(currentUserId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    prihodi.clear()
                    for (childSnapshot in snapshot.children) {
                        val item = childSnapshot.getValue(ActivityItem::class.java)
                        if (item != null && item.type == ActivityItem.TYPE_INCOME) {
                            prihodi.add(item)
                        }
                    }
                    adapter.updateList(prihodi)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@IncomeActivity, "Greška prilikom učitavanja prihoda", Toast.LENGTH_SHORT).show()
                }
            })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
