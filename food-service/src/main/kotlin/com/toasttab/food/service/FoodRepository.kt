interface FoodRepository {
    suspend fun getFoodNames(): List<String>
    suspend fun getFoodSummary(name: String): String?
}
