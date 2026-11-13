package br.edu.ifsp.tcc.application.it;

import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/** Alerts with the zone set far east of UTC (Pacific/Kiritimati); the cases are in {@link QaM4ZoneAlerts}. */
@Import(MailCapture.Config.class)
@TestPropertySource(properties = "app.alerts.timezone=Pacific/Kiritimati")
class QaM4AlertsZoneEastIT extends QaM4ZoneAlerts {
}
