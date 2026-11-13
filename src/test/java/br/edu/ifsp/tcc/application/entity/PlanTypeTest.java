package br.edu.ifsp.tcc.application.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PlanTypeTest {

    private static final long MB = 1024L * 1024;

    @Test
    void gratuito_shouldHaveTheApprovedLimits() {
        PlanType p = PlanType.GRATUITO;
        assertEquals("Gratuito", p.getDisplayName());
        assertEquals(0, new BigDecimal("0").compareTo(p.getMonthlyPrice()));
        assertEquals(1, p.getMaxUsers());
        assertEquals(3, p.getMaxActiveProjects());
        assertEquals(1, p.getMaxCourses());
        assertFalse(p.isAutomaticSummary());
        assertEquals(100 * MB, p.getStorageBytes());
    }

    @Test
    void paidPlans_shouldHaveTheApprovedLimitsAndPrices() {
        assertEquals(0, new BigDecimal("49").compareTo(PlanType.SOLO.getMonthlyPrice()));
        assertEquals(10, PlanType.SOLO.getMaxActiveProjects());
        assertEquals(5, PlanType.SOLO.getMaxCourses());
        assertFalse(PlanType.SOLO.isAutomaticSummary());
        assertEquals(1024 * MB, PlanType.SOLO.getStorageBytes());

        assertEquals(0, new BigDecimal("99").compareTo(PlanType.PRO.getMonthlyPrice()));
        assertEquals(1, PlanType.PRO.getMaxUsers());
        assertNull(PlanType.PRO.getMaxActiveProjects());
        assertNull(PlanType.PRO.getMaxCourses());
        assertTrue(PlanType.PRO.isAutomaticSummary());
        assertEquals(5 * 1024 * MB, PlanType.PRO.getStorageBytes());

        assertEquals("Agência", PlanType.AGENCIA.getDisplayName());
        assertEquals(0, new BigDecimal("249").compareTo(PlanType.AGENCIA.getMonthlyPrice()));
        assertEquals(5, PlanType.AGENCIA.getMaxUsers());
        assertNull(PlanType.AGENCIA.getMaxActiveProjects());
        assertTrue(PlanType.AGENCIA.isAutomaticSummary());
        assertEquals(20 * 1024 * MB, PlanType.AGENCIA.getStorageBytes());
    }
}
