package ba.sum.fpmoz.aplikacijazaupravljanjeosobnimfinacijama

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ActivityAdapter(
    private val activities: MutableList<ActivityItem>,
    private val onDeleteClick: (ActivityItem) -> Unit
) : RecyclerView.Adapter<ActivityAdapter.ActivityViewHolder>() {

    inner class ActivityViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtNaziv: TextView = view.findViewById(R.id.textNaziv)
        val txtZnak: TextView = view.findViewById(R.id.textZnak)
        val txtIznos: TextView = view.findViewById(R.id.textIznos)
        val txtOpis: TextView = view.findViewById(R.id.textOpis)
        val btnObrisi: ImageButton = view.findViewById(R.id.btnObrisi)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.prihod_kartica, parent, false)
        return ActivityViewHolder(view)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        val item = activities[position]

        // Postavi opis kao naziv aktivnosti
        holder.txtNaziv.text = item.description

        // Postavi znak i boju ovisno o iznosu
        if (item.amount < 0) {
            holder.txtZnak.text = "-"
            holder.txtZnak.setTextColor(android.graphics.Color.parseColor("#FF5252")) // crvena
            holder.txtIznos.setTextColor(android.graphics.Color.parseColor("#FF5252"))
        } else {
            holder.txtZnak.text = "+"
            holder.txtZnak.setTextColor(android.graphics.Color.parseColor("#4CAF50")) // zelena
            holder.txtIznos.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
        }

        // Iznos sa 2 decimale i bez znaka, jer je znak u posebnom TextViewu
        holder.txtIznos.text = String.format("%.2f KM", kotlin.math.abs(item.amount))

        // Detalji (može biti datum, dodatni opis itd.)
        holder.txtOpis.text = item.details

        // Klik na gumb za brisanje
        holder.btnObrisi.setOnClickListener {
            onDeleteClick(item)
        }
    }

    override fun getItemCount(): Int = activities.size

    fun updateList(newList: List<ActivityItem>) {
        activities.clear()
        activities.addAll(newList)
        notifyDataSetChanged()
    }
}
