package de.wps.radvis.backend.netzfehler.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.context.properties.ConfigurationPropertiesBindException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.data.util.Pair;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import de.wps.radvis.backend.netzfehler.domain.valueObject.AnpassungswunschKategorie;
import lombok.Getter;

@ExtendWith(OutputCaptureExtension.class)
class AnpassungswuenscheConfigurationPropertiesTest {

	private TestLogAppender logAppender;

	@BeforeEach
	void setUpLogAppender() {
		logAppender = new TestLogAppender();
		logAppender.start();
		Logger logger = (Logger) LoggerFactory.getLogger(AnpassungswuenscheConfigurationProperties.class);
		logger.addAppender(logAppender);
	}

	@Test
	void sollteNichtVorhandeneEmailsLoggen() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(UserConfigurations.of(TestConfig.class));

		contextRunner.withPropertyValues(
			"radvis.anpassungswuensche.distanzZuFahrradrouteInMetern=20",
			"radvis.anpassungswuensche.emailProKategorie.DLM=dlm@dlm.de",
			"radvis.anpassungswuensche.emailProKategorie.TOUBIZ=",
			"radvis.anpassungswuensche.emailProKategorie.RADVIS=radvis@wps.de",
			"radvis.anpassungswuensche.emailProKategorie.OSM=osm@osm.de"
		)
			.run(context -> {
				assertThat(context).getBean(AnpassungswuenscheConfigurationProperties.class).isNotNull();
				Map<AnpassungswunschKategorie, String> emailProKategorie = context.getBean(
					AnpassungswuenscheConfigurationProperties.class).getEmailProKategorie();

				assertThat(emailProKategorie.get(AnpassungswunschKategorie.DLM)).isEqualTo("dlm@dlm.de");
				assertThat(emailProKategorie.get(AnpassungswunschKategorie.TOUBIZ)).isNull();
				assertThat(emailProKategorie.get(AnpassungswunschKategorie.RADVIS)).isEqualTo("radvis@wps.de");
				assertThat(emailProKategorie.get(AnpassungswunschKategorie.OSM)).isEqualTo("osm@osm.de");
				assertThat(emailProKategorie.get(AnpassungswunschKategorie.TT_SIB)).isNull();
				assertThat(emailProKategorie.get(AnpassungswunschKategorie.WEGWEISUNGSSYSTEM)).isNull();

				assertThat(logAppender.getLogs()).containsExactly(Pair.of("INFO",
					"Für folgende Anpassungswunschkategoerien ist keine Email hinterlegt: TOUBIZ, TT_SIB, WEGWEISUNGSSYSTEM, MOBIDATA"));

			});
	}

	@Test
	void ohneGesetzteEmailProperties_sollteFunktionieren() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(UserConfigurations.of(TestConfig.class));

		contextRunner.withPropertyValues(
			"radvis.anpassungswuensche.distanzZuFahrradrouteInMetern=20"
		)
			.run(context -> {
				assertThat(context).getBean(AnpassungswuenscheConfigurationProperties.class).isNotNull();
				Map<AnpassungswunschKategorie, String> emailProKategorie = context.getBean(
					AnpassungswuenscheConfigurationProperties.class).getEmailProKategorie();

				assertThat(emailProKategorie).isEmpty();

				assertThat(logAppender.getLogs()).containsExactly(Pair.of("INFO",
					"Für folgende Anpassungswunschkategoerien ist keine Email hinterlegt: DLM, TOUBIZ, RADVIS, OSM, TT_SIB, WEGWEISUNGSSYSTEM, MOBIDATA"));

			});
	}

	@Test
	void mitNichtExistierenderKategorie_sollteFehlerWerfen() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(UserConfigurations.of(TestConfig.class));

		contextRunner.withPropertyValues(
			"radvis.anpassungswuensche.distanzZuFahrradrouteInMetern=20",
			"radvis.anpassungswuensche.emailProKategorie.DML=dlm@dlm.de", // Oops!! Typo DLM != DML
			"radvis.anpassungswuensche.emailProKategorie.TOUBIZ=",
			"radvis.anpassungswuensche.emailProKategorie.RADVIS=radvis@wps.de",
			"radvis.anpassungswuensche.emailProKategorie.OSM=osm@osm.de")
			.run(context -> {
				assertThat(context).hasFailed();
				assertThat(context.getStartupFailure()).isInstanceOf(ConfigurationPropertiesBindException.class)
					.hasStackTraceContaining(
						"Failed to convert from type [java.lang.String] to type [de.wps.radvis.backend.netzfehler.domain.valueObject.AnpassungswunschKategorie] for value [DML]");
			});

	}

	@EnableConfigurationProperties(AnpassungswuenscheConfigurationProperties.class)
	static class TestConfig {
	}

	static class TestLogAppender extends AppenderBase<ILoggingEvent> {

		@Getter
		private final List<Pair<String, String>> logs = new ArrayList<>();

		@Override
		protected void append(ILoggingEvent eventObject) {
			logs.add(Pair.of(eventObject.getLevel().levelStr, eventObject.getFormattedMessage()));
		}
	}

}