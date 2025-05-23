package ba.sum.fpmoz.aplikacijazaupravljanjeosobnimfinacijama

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.DecimalFormat
import java.util.*

class ExpenseActivity : AppCompatActivity() {

    private lateinit var editNazivTrosak: TextInputEditText
    private lateinit var editIznosTrosak: TextInputEditText
    private lateinit var editOpisTrosak: TextInputEditText
    private lateinit var editDatumTrosak: TextInputEditText
    private lateinit var spinnerKategorijaTrosak: AutoCompleteTextView
    private lateinit var btnDodajTrosak: MaterialButton
    private lateinit var containerTroskovi: LinearLayout

    private val troskovi = mutableListOf<Trosak>()
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

        // Postavljanje boje gumba
        btnDodajTrosak.setBackgroundColor(Color.parseColor("#2196F3"))
        btnDodajTrosak.setTextColor(Color.WHITE)

        setupDatePicker()
        setupKategorijaSpinner()

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
        containerTroskovi = findViewById(R.id.containerTroskovi)
    }

    private fun setupDatePicker() {
        editDatumTrosak.setOnClickListener {
            val c = Calendar.getInstance()

            // Pokušaj učitati već uneseni datum da se postavi kao početni
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
                    // ignoriraj, koristi današnji datum
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

            // Postavi boju na plavu #2196F3
            dpd.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dpd.setOnShowListener {
                // Promjena boje dugmadi u pickeru
                dpd.getButton(DatePickerDialog.BUTTON_POSITIVE)?.setTextColor(Color.parseColor("#2196F3"))
                dpd.getButton(DatePickerDialog.BUTTON_NEGATIVE)?.setTextColor(Color.parseColor("#2196F3"))
            }

            dpd.show()
        }
    }

    private fun setupKategorijaSpinner() {
        val kategorije = listOf("Hrana", "Prijevoz", "Stanovanje", "Zabava", "Ostalo")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, kategorije)
        spinnerKategorijaTrosak.setAdapter(adapter)

        spinnerKategorijaTrosak.keyListener = null
        spinnerKategorijaTrosak.setOnClickListener {
            spinnerKategorijaTrosak.showDropDown()
        }
    }

    private fun loadTroskovi() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                troskovi.clear()
                for (child in snapshot.children) {
                    val trosak = child.getValue(Trosak::class.java)
                    trosak?.let { troskovi.add(it) }
                }
                prikaziTroskove()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ExpenseActivity, "Greška: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun prikaziTroskove() {
        containerTroskovi.removeAllViews()
        val inflater = LayoutInflater.from(this)

        troskovi.forEach { trosak ->
            val cardView = inflater.inflate(R.layout.item_trosak, containerTroskovi, false)

            val textNaziv = cardView.findViewById<TextView>(R.id.textNaziv)
            val textIznos = cardView.findViewById<TextView>(R.id.textIznos)
            val textOpis = cardView.findViewById<TextView>(R.id.textOpis)
            val textDatum = cardView.findViewById<TextView>(R.id.textDatum)
            val textKategorija = cardView.findViewById<TextView>(R.id.textKategorija)
            val btnObrisi = cardView.findViewById<ImageButton>(R.id.btnObrisi)

            textNaziv.text = trosak.naziv

            val df = DecimalFormat("#.00")
            val iznosDouble = trosak.iznos.toDoubleOrNull() ?: 0.0
            textIznos.text = "${df.format(iznosDouble)} KM"

            textOpis.text = trosak.opis
            textDatum.text = "Datum: ${trosak.datum}"
            textKategorija.text = "Kategorija: ${trosak.kategorija}"

            btnObrisi.setOnClickListener {
                database.child(trosak.id).removeValue().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Trošak obrisan", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Greška prilikom brisanja", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            containerTroskovi.addView(cardView)
        }
    }

    private fun dodajTrosak() {
        val naziv = editNazivTrosak.text.toString().trim()
        val iznos = editIznosTrosak.text.toString().trim()
        val opis = editOpisTrosak.text.toString().trim()
        val datum = editDatumTrosak.text.toString().trim()
        val kategorija = spinnerKategorijaTrosak.text.toString().trim()

        if (naziv.isEmpty() || iznos.isEmpty() || opis.isEmpty() || datum.isEmpty() || kategorija.isEmpty()) {
            Toast.makeText(this, "Popunite sva polja!", Toast.LENGTH_SHORT).show()
            return
        }

        val iznosDouble = iznos.toDoubleOrNull()
        if (iznosDouble == null) {
            Toast.makeText(this, "Unesite ispravan iznos!", Toast.LENGTH_SHORT).show()
            return
        }

        val id = database.push().key ?: ""
        val trosak = Trosak(id, naziv, iznosDouble.toString(), opis, datum, kategorija)

        database.child(id).setValue(trosak).addOnCompleteListener { task ->
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
        editDatumTrosak.text?.clear()
        spinnerKategorijaTrosak.text.clear()
    }
}

data class Trosak(
    val id: String = "",
    val naziv: String = "",
    val iznos: String = "",
    val opis: String = "",
    val datum: String = "",
    val kategorija: String = ""
)
