package br.edu.ifsp.tcc.application.entity;

import java.math.BigDecimal;

/** Plans of a team with their limits. A null limit means unlimited. */
public enum PlanType {

    GRATUITO("Gratuito", "0.00", 1, 3, 1, false, 100L * 1024 * 1024),
    SOLO("Solo", "49.00", 1, 10, 5, false, 1024L * 1024 * 1024),
    PRO("Pro", "99.00", 1, null, null, true, 5L * 1024 * 1024 * 1024),
    AGENCIA("Agência", "249.00", 5, null, null, true, 20L * 1024 * 1024 * 1024);

    private final String displayName;
    private final BigDecimal monthlyPrice;
    private final Integer maxUsers;
    private final Integer maxActiveProjects;
    private final Integer maxCourses;
    private final boolean automaticSummary;
    private final long storageBytes;

    PlanType(String displayName, String monthlyPrice, Integer maxUsers, Integer maxActiveProjects,
             Integer maxCourses, boolean automaticSummary, long storageBytes) {
        this.displayName = displayName;
        this.monthlyPrice = new BigDecimal(monthlyPrice);
        this.maxUsers = maxUsers;
        this.maxActiveProjects = maxActiveProjects;
        this.maxCourses = maxCourses;
        this.automaticSummary = automaticSummary;
        this.storageBytes = storageBytes;
    }

    public String getDisplayName() { return displayName; }
    public BigDecimal getMonthlyPrice() { return monthlyPrice; }
    public Integer getMaxUsers() { return maxUsers; }
    public Integer getMaxActiveProjects() { return maxActiveProjects; }
    public Integer getMaxCourses() { return maxCourses; }
    public boolean isAutomaticSummary() { return automaticSummary; }
    public long getStorageBytes() { return storageBytes; }
}
