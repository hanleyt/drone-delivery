object DummyFoodRepository : FoodRepository {
    override suspend fun getFoodNames(): List<String> {
        return listOf(
            "Food1",
            "Food2",
            "Food3",
            "Food4"
        )
    }

    override suspend fun getFoodSummary(name: String): String? {
        return when (name) {
            "Food1" -> "Summary1"
            "Food2" -> "Summary2"
            "Food3" -> "Summary3"
            "Food4" -> "Summary4"
            else -> null
        }
    }
}
