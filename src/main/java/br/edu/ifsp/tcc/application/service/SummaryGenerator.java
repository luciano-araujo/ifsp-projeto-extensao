package br.edu.ifsp.tcc.application.service;

/** Turns the plain text of a script into a short summary (implemented by the Python summary service). */
public interface SummaryGenerator {

    /**
     * @throws org.springframework.web.server.ResponseStatusException 503 when the summary cannot be produced
     */
    String summarize(String plainText);
}
