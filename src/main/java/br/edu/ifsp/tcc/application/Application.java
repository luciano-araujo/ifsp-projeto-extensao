package br.edu.ifsp.tcc.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class Application {

	// Also in a static block so that every way of loading the class (tests, devtools restart) runs in UTC
	static {
		useUtc();
	}

	/** DTOs label LocalDateTime values with 'Z': the JVM must run in UTC whatever the machine zone is. */
	static void useUtc() {
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
	}

	public static void main(String[] args) {
		useUtc();
		SpringApplication.run(Application.class, args);
	}

}
