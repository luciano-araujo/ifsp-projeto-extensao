package br.edu.ifsp.tcc.application;

import org.junit.jupiter.api.Test;

import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApplicationTimeZoneTest {

    @Test
    void useUtc_shouldSetDefaultTimeZoneToUtc() {
        TimeZone previous = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"));
            Application.useUtc();
            assertEquals("UTC", TimeZone.getDefault().getID());
        } finally {
            TimeZone.setDefault(previous);
        }
    }
}
