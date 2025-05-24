package ba.sum.fpmoz.aplikacijazaupravljanjeosobnimfinacijama

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
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

    private lateinit var editNazivTrosak: TextInputEditText
    private lateinit var editIznosTrosak: TextInputEditText
    private lateinit var editOpisTrosak: TextInputEditText
    private lateinit var editDatumTrosak: TextInputEditText
    private lateinit var spinnerKategorijaTrosak: AutoCompleteTextView
    private lateinit var btnDodajTrosak: MaterialButton
    private lateinit var recyclerViewTroskovi: RecyclerView

    private val troskovi = mutableListOf<ExpenseItem>()
    private lateinit var adapter: ExpenseAdapter
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expense)

        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Niste prijavljeni!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        initUI()

        btnDodajTrosak.setBackgroundColor(Color.parseColor("#2196F3"))
        btnDodajTrosak.setTextColor(Color.WHITE)

        setupDatePicker()
        setupKategorijaSpinner()
        postaviZadaniDatum()

        adapter = ExpenseAdapter(troskovi) { expenseToDelete ->
            obrisiTrosak(expenseToDelete)
        }

        recyclerViewTroskovi.layoutManager = LinearLayoutManager(this)
        recyclerViewTroskovi.adapter = adapter

        database = FirebaseDatabase.getInstance()
            .getReference("troskovi")
            .child(currentUser.uid)

        loadTroskovi()

        btnDodajTrosak.setOnClickListener {
            dodajTrosak()
        }
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.setBackgroundColor(Color.parseColor("#2196F3"))
        toolbar.setTitleTextColor(Color.WHITE)
        toolbar.navigationIcon?.setTint(Color.WHITE)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "Troškovi"

        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun initUI() {
        editNazivTrosak = findViewById(R.id.editNazivTrosak)
        editIznosTrosak = findViewById(R.id.editIznosTrosak)
        editOpisTrosak = findViewById(R.id.editOpisTrosak)
        editDatumTrosak = findViewById(R.id.editDatumTrosak)
        spinnerKategorijaTrosak = findViewById(R.id.spinnerKategorijaTrosak)
        btnDodajTrosak = findViewById(R.id.btnDodajTrosak)
        recyclerViewTroskovi = findViewById(R.id.recyclerViewTroskovi)
    }

    private fun setupDatePicker() {
        editDatumTrosak.setOnClickListener {
            val c = Calendar.getInstance()
            val trenutniDatum = editDatumTrosak.text.toString()
            if (trenutniDatum.isNotEmpty()) {
                try {
                    val parts = trenutniDatum.split(".")
                    if (parts.size == 3) {
                        val dan = parts[0].toInt()
                        val mjesec = parts[1].toInt() - 1
                        val godina = parts[2].toInt()
                        c.set(godina, mjesec, dan)
                    }
                } catch (e: Exception) {
                    // ignoriraj grešku
                }
            }
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)

            val dpd = DatePickerDialog(this, android.R.style.Theme_Holo_Light_Dialog_MinWidth, { _, y, m, d ->
                val mjesec = m + 1
                val datumStr = String.format("%02d.%02d.%d", d, mjesec, y)
                editDatumTrosak.setText(datumStr)
            }, year, month, day)

            dpd.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dpd.setOnShowListener {
                dpd.getButton(DatePickerDialog.BUTTON_POSITIVE)?.setTextColor(Color.parseColor("#2196F3"))
                dpd.getButton(DatePickerDialog.BUTTON_NEGATIVE)?.setTextColor(Color.parseColor("#2196F3"))
            }

            dpd.show()
        }
    }

    private fun setupKategorijaSpinner() {
        val kategorije = listOf("Hrana", "Prijevoz", "Stanovanje", "Zabava", "Ostalo")
        val adapterSpinner = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, kategorije)
        spinnerKategorijaTrosak.setAdapter(adapterSpinner)
        spinnerKategorijaTrosak.keyListener = null // korisnik ne može tipkati, samo birati
        spinnerKategorijaTrosak.setOnClickListener {
            spinnerKategorijaTrosak.showDropDown()
        }
    }

    private fun postaviZadaniDatum() {
        val danas = Calendar.getInstance()
        val datumStr = String.format(
            "%02d.%02d.%d",
            danas.get(Calendar.DAY_OF_MONTH),
            danas.get(Calendar.MONTH) + 1,
            danas.get(Calendar.YEAR)
        )
        editDatumTrosak.setText(datumStr)
    }

    private fun loadTroskovi() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val listaTroskova = mutableListOf<ExpenseItem>()
                for (child in snapshot.children) {
                    val trosak = child.getValue(ExpenseItem::class.java)
                    trosak?.let { listaTroskova.add(it) }
                }
                troskovi.clear()
                troskovi.addAll(listaTroskova)
                adapter.updateData(troskovi)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ExpenseActivity, "Greška: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun dodajTrosak() {
        val naziv = editNazivTrosak.text.toString().trim()
        val iznosStr = editIznosTrosak.text.toString().trim()
        val opis = editOpisTrosak.text.toString().trim()
        val datum = editDatumTrosak.text.toString().trim()
        val kategorija = spinnerKategorijaTrosak.text.toString().trim()

        if (naziv.isEmpty() || iznosStr.isEmpty() || opis.isEmpty() || datum.isEmpty() || kategorija.isEmpty()) {
            Toast.makeText(this, "Popunite sva polja!", Toast.LENGTH_SHORT).show()
            return
        }

        val iznos = iznosStr.toDoubleOrNull()
        if (iznos == null) {
            Toast.makeText(this, "Unesite ispravan iznos!", Toast.LENGTH_SHORT).show()
            return
        }

        val id = database.push().key ?: ""
        val noviTrosak = ExpenseItem(id, naziv, iznos, opis, datum, kategorija)

        database.child(id).setValue(noviTrosak).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Trošak dodan!", Toast.LENGTH_SHORT).show()
                ocistiPolja()
            } else {
                Toast.makeText(this, "Greška prilikom dodavanja troška", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun ocistiPolja() {
        editNazivTrosak.text?.clear()
        editIznosTrosak.text?.clear()
        editOpisTrosak.text?.clear()
        postaviZadaniDatum()
        spinnerKategorijaTrosak.text.clear()
    }

    private fun obrisiTrosak(trosak: ExpenseItem) {
        database.child(trosak.id).removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Trošak obrisan", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Greška prilikom brisanja", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
