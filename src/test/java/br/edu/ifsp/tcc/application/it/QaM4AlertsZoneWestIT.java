package br.edu.ifsp.tcc.application.it;

import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/** Alerts with the zone set far west of UTC (Pacific/Pago_Pago); the cases are in {@link QaM4ZoneAlerts}. */
@Import(MailCapture.Config.class)
@TestPropertySource(properties = "app.alerts.timezone=Pacific/Pago_Pago")
class QaM4AlertsZoneWestIT extends QaM4ZoneAlerts {
}
