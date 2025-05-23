package ba.sum.fpmoz.aplikacijazaupravljanjeosobnimfinacijama

data class ActivityItem(
    val id: String = "",
    val type: String = "",          // "Prihod" ili "Rashod"
    val description: String = "",   // Ovo će biti Naziv
    val amount: String = "",        // Iznos
    val details: String = "",       // Opis (dodaj ovo polje za opis)
    val userId: String = ""
)

