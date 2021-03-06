package ch.epfl.sweng.GyroDraw.utils;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class TestUsersUnitTest {

    @Test
    public void testRecognizesTestUser() {
        assertThat(TestUsers.isTestUser("123456789"), is(true));
    }

    @Test
    public void testIgnoresRealUser() {
        assertThat(TestUsers.isTestUser("PICASSO"), is(false));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIsTestUserWithNullId() {
        TestUsers.isTestUser(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIsTestUserWithEmptyId() {
        TestUsers.isTestUser("");
    }

}