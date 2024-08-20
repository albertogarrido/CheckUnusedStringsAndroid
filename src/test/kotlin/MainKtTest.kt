import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MainKtTest {

    @Test
    fun `anyOf all true gives true`() {
        val bool1 = true
        val bool2 = true
        val bool3 = true
        assertTrue(anyOf(bool1, bool2, bool3))
    }

    @Test
    fun noneOf() {
    }
}