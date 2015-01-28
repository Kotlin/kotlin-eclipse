import org.junit.Test
import org.junit.Assert
import org.junit.Before
import org.junit.After

public class SomeKt {
	Before fun setUp() {
	}
	
	After fun tearDown() {
	}
	
	Test fun okTest() {
		Assert.assertEquals(0, 0)
	}
	
	Test fun failTest() {
		Assert.assertEquals(0, 1)
	}
}