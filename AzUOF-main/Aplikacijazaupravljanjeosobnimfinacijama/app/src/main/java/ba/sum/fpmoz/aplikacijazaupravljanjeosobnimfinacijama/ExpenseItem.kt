package ba.sum.fpmoz.aplikacijazaupravljanjeosobnimfinacijama

data class ExpenseItem(
    val id: String = "",
    val naziv: String = "",
    val iznos: Double = 0.0,
    val opis: String = "",
    val datum: String = "",
    val kategorija: String = ""
)

