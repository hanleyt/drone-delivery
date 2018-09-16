import kotlinx.coroutines.experimental.delay
import java.time.Duration

val Number.ms get() = Duration.ofMillis(this.toLong())

class DelayedFoodRepository(val base: FoodRepository, val delayMs: Duration = 4000.ms) : FoodRepository {
    private suspend fun intercept(method: String, args: Array<Any?>): FoodRepository {
        delay(delayMs.toMillis())
        return base
    }

    override suspend fun getFoodNames(): List<String> = intercept("getFoodNames", arrayOf()).getFoodNames()
    override suspend fun getFoodSummary(name: String): String? =
            intercept("getFoodSummary", arrayOf(name)).getFoodSummary(name)
}
