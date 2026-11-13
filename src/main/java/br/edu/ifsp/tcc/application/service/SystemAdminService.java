package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Who is a system administrator: the e-mails listed in the configuration {@code app.admin.emails}. */
@Service
public class SystemAdminService {

    private final Set<String> adminEmails;

    public SystemAdminService(@Value("${app.admin.emails:}") String adminEmails) {
        this.adminEmails = Arrays.stream(adminEmails.split(","))
                .map(e -> e.trim().toLowerCase(Locale.ROOT))
                .filter(e -> !e.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean isSystemAdmin(User user) {
        return user != null && user.getEmail() != null
                && adminEmails.contains(user.getEmail().trim().toLowerCase(Locale.ROOT));
    }

    /** 403 unless the user is a system administrator. */
    public void requireSystemAdmin(User user) {
        if (!isSystemAdmin(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Apenas o administrador do sistema pode executar esta ação.");
        }
    }
}
