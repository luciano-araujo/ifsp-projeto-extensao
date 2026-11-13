package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

class SystemAdminServiceTest {

    private User user(String email) {
        User user = new User();
        user.setEmail(email);
        return user;
    }

    @Test
    void isSystemAdmin_shouldMatchConfiguredEmailsIgnoringCaseAndSpaces() {
        SystemAdminService service = new SystemAdminService(" Admin@Site.com , outro@site.com ,");

        assertTrue(service.isSystemAdmin(user("admin@site.com")));
        assertTrue(service.isSystemAdmin(user(" OUTRO@site.com ")));
        assertFalse(service.isSystemAdmin(user("ana@site.com")));
        assertFalse(service.isSystemAdmin(user(null)));
        assertFalse(service.isSystemAdmin(null));
    }

    @Test
    void emptyConfiguration_shouldHaveNoSystemAdmin() {
        SystemAdminService service = new SystemAdminService("");

        assertFalse(service.isSystemAdmin(user("")));
        assertFalse(service.isSystemAdmin(user("admin@site.com")));
    }

    @Test
    void requireSystemAdmin_shouldReturn403ForOthers() {
        SystemAdminService service = new SystemAdminService("admin@site.com");

        service.requireSystemAdmin(user("admin@site.com"));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.requireSystemAdmin(user("ana@site.com")));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }
}
