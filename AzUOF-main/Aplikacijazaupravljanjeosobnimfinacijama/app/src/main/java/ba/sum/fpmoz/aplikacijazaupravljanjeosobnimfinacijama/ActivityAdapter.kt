package ba.sum.fpmoz.aplikacijazaupravljanjeosobnimfinacijama

import android.graphics.Color
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

    companion object {
        private const val TYPE_EXPENSE = 0
        private const val TYPE_INCOME = 1
    }

    inner class ActivityViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtNaziv: TextView = view.findViewById(R.id.textNaziv)
        val txtZnak: TextView = view.findViewById(R.id.textZnak)
        val txtIznos: TextView = view.findViewById(R.id.textIznos)
        val txtOpis: TextView = view.findViewById(R.id.textOpis)
        val txtDatum: TextView? = view.findViewById(R.id.textDatum)       // Nullable jer prihod kartica nema datuma
        val txtKategorija: TextView? = view.findViewById(R.id.textKategorija) // Nullable isto
        val btnObrisi: ImageButton = view.findViewById(R.id.btnObrisi)
    }

    override fun getItemViewType(position: Int): Int {
        // Vrati tip kao int za adapter (0 za trošak, 1 za prihod)
        return if (activities[position].type == ActivityItem.TYPE_EXPENSE) TYPE_EXPENSE else TYPE_INCOME
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val layout = if (viewType == TYPE_EXPENSE) {
            R.layout.item_trosak
        } else {
            R.layout.prihod_kartica
        }
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return ActivityViewHolder(view)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        val item = activities[position]

        // Naziv prikazi ili ako nema naziv, onda opiši
        holder.txtNaziv.text = if (item.naziv.isNotEmpty()) item.naziv else item.opis
        holder.txtOpis.text = if (item.opis.isNotEmpty()) item.opis else "-"

        if (item.type == ActivityItem.TYPE_EXPENSE) {
            // Trošak - crvena boja i znak minus
            holder.txtZnak.text = "-"
            holder.txtZnak.setTextColor(Color.parseColor("#FF5252"))
            holder.txtIznos.setTextColor(Color.parseColor("#FF5252"))

            holder.txtDatum?.visibility = View.VISIBLE
            holder.txtDatum?.text = item.datum

            holder.txtKategorija?.visibility = View.VISIBLE
            holder.txtKategorija?.text = item.kategorija

        } else {
            // Prihod - zelena boja i znak plus
            holder.txtZnak.text = "+"
            holder.txtZnak.setTextColor(Color.parseColor("#4CAF50"))
            holder.txtIznos.setTextColor(Color.parseColor("#4CAF50"))

            // Sakrij datum i kategoriju jer ih prihod kartica nema
            holder.txtDatum?.visibility = View.GONE
            holder.txtKategorija?.visibility = View.GONE
        }

        // Prikaz iznosa s dvije decimale i oznakom KM
        holder.txtIznos.text = String.format("%.2f KM", kotlin.math.abs(item.amount))

        // Delete button poziv callback funkcije
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
