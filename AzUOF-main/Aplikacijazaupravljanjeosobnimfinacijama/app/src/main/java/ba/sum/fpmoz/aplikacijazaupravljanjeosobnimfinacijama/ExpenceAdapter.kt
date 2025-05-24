package ba.sum.fpmoz.aplikacijazaupravljanjeosobnimfinacijama

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import java.text.DecimalFormat

class ExpenseAdapter(
    private val troskovi: MutableList<ExpenseItem>,
    private val onDeleteClick: (ExpenseItem) -> Unit
) : RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    inner class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textNaziv: TextView = itemView.findViewById(R.id.textNaziv)
        val textIznos: TextView = itemView.findViewById(R.id.textIznos)
        val textOpis: TextView = itemView.findViewById(R.id.textOpis)
        val textDatum: TextView = itemView.findViewById(R.id.textDatum)
        val textKategorija: TextView = itemView.findViewById(R.id.textKategorija)
        val btnObrisi: ImageButton = itemView.findViewById(R.id.btnObrisi)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_trosak, parent, false)
        return ExpenseViewHolder(view)
    }

    override fun getItemCount(): Int = troskovi.size

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val trosak = troskovi[position]

        holder.textNaziv.text = trosak.naziv

        val df = DecimalFormat("#.00")
        holder.textIznos.text = "${df.format(trosak.iznos)} KM"

        holder.textOpis.text = trosak.opis
        holder.textDatum.text = "Datum: ${trosak.datum}"
        holder.textKategorija.text = "Kategorija: ${trosak.kategorija}"

        holder.btnObrisi.setOnClickListener {
            onDeleteClick(trosak)
        }
    }

    fun updateData(newList: List<ExpenseItem>) {
        troskovi.clear()
        troskovi.addAll(newList)
        notifyDataSetChanged()
    }
}
