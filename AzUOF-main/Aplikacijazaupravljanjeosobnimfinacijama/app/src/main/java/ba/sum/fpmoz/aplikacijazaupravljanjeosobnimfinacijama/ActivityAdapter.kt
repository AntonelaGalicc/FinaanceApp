package ba.sum.fpmoz.aplikacijazaupravljanjeosobnimfinacijama

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ActivityAdapter(
    private val activities: List<ActivityItem>,
    private val onDeleteClick: (ActivityItem) -> Unit
) : RecyclerView.Adapter<ActivityAdapter.ActivityViewHolder>() {

    inner class ActivityViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtNaziv: TextView = view.findViewById(R.id.textNaziv)
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

        // Postavi tekstove iz polja ActivityItem
        holder.txtNaziv.text = item.description             // koristimo description kao naziv
        holder.txtIznos.text = "${item.amount} KM"           // amount za iznos
        holder.txtOpis.text = item.type                       // type koristimo kao dodatni opis (ili promijeni prema potrebi)

        holder.btnObrisi.setOnClickListener {
            onDeleteClick(item)
        }
    }

    override fun getItemCount(): Int = activities.size
}
