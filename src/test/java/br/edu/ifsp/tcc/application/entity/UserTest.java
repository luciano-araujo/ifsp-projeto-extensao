package br.edu.ifsp.tcc.application.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void settersAndGetters_shouldWorkCorrectly() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setPassword("secret");
        user.setName("Test User");

        assertEquals(1L, user.getId());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("secret", user.getPassword());
        assertEquals("Test User", user.getName());
    }
}
