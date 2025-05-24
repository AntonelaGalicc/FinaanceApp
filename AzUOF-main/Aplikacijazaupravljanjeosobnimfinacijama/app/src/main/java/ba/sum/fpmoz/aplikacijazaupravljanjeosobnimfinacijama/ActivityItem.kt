package ba.sum.fpmoz.aplikacijazaupravljanjeosobnimfinacijama

data class ActivityItem(
    var id: String = "",
    var userId: String = "",
    var type: String = "",
    var naziv: String = "",
    var amount: Double = 0.0,   // amount sada Double
    var opis: String = "",
    var datum: String = "",
    var kategorija: String = ""
) {
    companion object {
        const val TYPE_INCOME = "income"
        const val TYPE_EXPENSE = "expense"
    }
}
