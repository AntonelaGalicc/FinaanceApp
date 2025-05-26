package ba.sum.fpmoz.aplikacijazaupravljanjeosobnimfinacijama

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*

class ExpenseActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var activityAdapter: ActivityAdapter
    private val activityList = mutableListOf<ActivityItem>()

    private lateinit var editNaziv: TextInputEditText
    private lateinit var editIznos: TextInputEditText
    private lateinit var editOpis: TextInputEditText
    private lateinit var editDatum: TextInputEditText
    private lateinit var spinnerKategorija: AutoCompleteTextView
    private lateinit var btnDodaj: MaterialButton
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expense)

        // Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbarExpense)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = "Troškovi"
            setDisplayHomeAsUpEnabled(true)
        }
        toolbar.navigationIcon?.setTint(Color.WHITE)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        // UI komponente
        editNaziv = findViewById(R.id.editNazivTrosak)
        editIznos = findViewById(R.id.editIznosTrosak)
        editOpis = findViewById(R.id.editOpisTrosak)
        editDatum = findViewById(R.id.editDatumTrosak)
        spinnerKategorija = findViewById(R.id.spinnerKategorijaTrosak)
        btnDodaj = findViewById(R.id.btnDodajTrosak)
        recyclerView = findViewById(R.id.recyclerViewTroskovi)

        recyclerView.layoutManager = LinearLayoutManager(this)

        activityAdapter = ActivityAdapter(mutableListOf()) { deleteActivity(it) }
        recyclerView.adapter = activityAdapter

        // Kategorije
        val kategorije = listOf("Hrana", "Stanarina", "Transport", "Zabava", "Ostalo")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, kategorije)
        spinnerKategorija.setAdapter(adapter)
        spinnerKategorija.setOnClickListener { spinnerKategorija.showDropDown() }

        // Onemogući unos teksta
        spinnerKategorija.keyListener = null
        spinnerKategorija.setTextColor(Color.BLACK)
        spinnerKategorija.setBackgroundColor(Color.parseColor("#954535")) // Svijetlo plava

        // Datum
        editDatum.setOnClickListener {
            val c = Calendar.getInstance()
            val dpd = DatePickerDialog(this, { _, year, month, day ->
                val formatted = String.format("%04d-%02d-%02d", year, month + 1, day)
                editDatum.setText(formatted)
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))
            dpd.show()
        }

        // Stilizacija polja za datum
        editDatum.setTextColor(Color.BLACK)
        editDatum.setBackgroundColor(Color.parseColor("#954535"))



        btnDodaj.setOnClickListener { addExpense() }

        fetchExpenses()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun addExpense() {
        val naziv = editNaziv.text.toString().trim()
        val iznosStr = editIznos.text.toString().trim()
        val opis = editOpis.text.toString().trim()
        val datum = editDatum.text.toString().trim()
        val kategorija = spinnerKategorija.text.toString().trim()

        if (naziv.isEmpty() || iznosStr.isEmpty() || datum.isEmpty() || kategorija.isEmpty()) {
            Toast.makeText(this, "Popunite sva polja!", Toast.LENGTH_SHORT).show()
            return
        }

        val iznosDouble = iznosStr.toDoubleOrNull()
        if (iznosDouble == null || iznosDouble <= 0) {
            Toast.makeText(this, "Unesite ispravan iznos!", Toast.LENGTH_SHORT).show()
            return
        }

        val id = database.child("activities").push().key ?: run {
            Toast.makeText(this, "Neuspješan pokušaj dodavanja", Toast.LENGTH_SHORT).show()
            return
        }
        val userId = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "Niste prijavljeni!", Toast.LENGTH_SHORT).show()
            return
        }

        val noviTrosak = ActivityItem(
            id = id,
            userId = userId,
            type = ActivityItem.TYPE_EXPENSE,
            naziv = naziv,
            amount = iznosDouble,
            opis = opis,
            datum = datum,
            kategorija = kategorija
        )

        database.child("activities").child(id).setValue(noviTrosak).addOnSuccessListener {
            Toast.makeText(this, "Trošak dodan!", Toast.LENGTH_SHORT).show()
            clearInputs()
            // fetchExpenses() nije striktno potreban jer ValueEventListener prati promjene,
            // ali možeš ostaviti za sigurnost:
            fetchExpenses()
        }.addOnFailureListener {
            Toast.makeText(this, "Greška pri dodavanju!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearInputs() {
        editNaziv.text?.clear()
        editIznos.text?.clear()
        editOpis.text?.clear()
        editDatum.text?.clear()
        spinnerKategorija.setText("")
    }

    private fun fetchExpenses() {
        val userId = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "Niste prijavljeni!", Toast.LENGTH_SHORT).show()
            return
        }
        database.child("activities")
            .orderByChild("userId")
            .equalTo(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.d("ExpenseActivity", "Dohvaćeno stavki: ${snapshot.childrenCount}")
                    activityList.clear()
                    for (child in snapshot.children) {
                        val item = child.getValue(ActivityItem::class.java)
                        Log.d("ExpenseActivity", "Dohvaćena stavka: $item")  // dodano za debug
                        if (item != null) {
                            Log.d("ExpenseActivity", "Tip stavke: ${item.type}, ID iz child key: ${child.key}")
                        }
                        if (item != null && item.type == ActivityItem.TYPE_EXPENSE) {
                            item.id = child.key ?: ""
                            activityList.add(item)
                            Log.d("ExpenseActivity", "Dodano u listu: ${item.naziv} sa iznosom ${item.amount}")
                        }
                    }
                    activityAdapter.updateList(activityList)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ExpenseActivity, "Greška pri dohvaćanju!", Toast.LENGTH_SHORT).show()
                    Log.e("ExpenseActivity", "Database error: ${error.message}")
                }
            })
    }


    private fun deleteActivity(activity: ActivityItem) {
        database.child("activities").child(activity.id).removeValue().addOnSuccessListener {
            Toast.makeText(this, "Trošak obrisan", Toast.LENGTH_SHORT).show()
            fetchExpenses() // Osvježi listu nakon brisanja
        }.addOnFailureListener {
            Toast.makeText(this, "Brisanje nije uspjelo", Toast.LENGTH_SHORT).show()
        }
    }
}