package ba.sum.fpmoz.aplikacijazaupravljanjeosobnimfinacijama

data class ActivityItem(
    var id: String = "",
    var userId: String = "",
    var type: String = "",
    var description: String = "",
    var amount: Double = 0.0,
    var details: String = ""
) {
    // Empty constructor je potreban za Firebase
    constructor() : this("", "", "", "", 0.0, "")
}

