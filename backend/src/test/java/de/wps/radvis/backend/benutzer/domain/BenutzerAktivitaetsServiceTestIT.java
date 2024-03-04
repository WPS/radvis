package de.wps.radvis.backend.benutzer.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.MockitoAnnotations.openMocks;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.transaction.BeforeTransaction;

import de.wps.radvis.backend.authentication.domain.RadVisAuthentication;
import de.wps.radvis.backend.authentication.domain.entity.RadVisUserDetails;
import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.benutzer.domain.repository.BenutzerRepository;
import de.wps.radvis.backend.benutzer.domain.valueObject.ServiceBwId;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.MailService;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.GebietskoerperschaftRepository;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitImportRepository;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitRepository;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import jakarta.persistence.EntityManager;

@Tag("group4")
@ContextConfiguration(classes = {
	BenutzerConfiguration.class,
	OrganisationConfiguration.class,
})
@MockBeans({
	@MockBean(GeoConverterConfiguration.class),
	@MockBean(VerwaltungseinheitImportRepository.class),
	@MockBean(MailService.class),
	@MockBean(CommonConfigurationProperties.class)
})
@EnableConfigurationProperties(value = {
	TechnischerBenutzerConfigurationProperties.class
})
class BenutzerAktivitaetsServiceTestIT extends DBIntegrationTestIT {

	@Autowired
	BenutzerAktivitaetsService benutzerAktivitaetsService;

	@Autowired
	BenutzerRepository benutzerRepository;

	@Autowired
	GebietskoerperschaftRepository gebietskoerperschaftRepository;

	@Autowired
	VerwaltungseinheitRepository verwaltungseinheitRepository;

	@Autowired
	ApplicationEventPublisher publisher;

	Benutzer benutzer;

	@BeforeEach
	void setup() {
		openMocks(this);
	}

	@BeforeTransaction
	void fillDatabase() {
		Verwaltungseinheit verwaltungseinheit = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build());

		benutzer = benutzerRepository.save(BenutzerTestDataProvider
			.defaultBenutzer()
			.organisation(verwaltungseinheit)
			.serviceBwId(ServiceBwId.of("testId"))
			.letzteAktivitaet(LocalDate.now().minusDays(2))
			.build());
	}

	@AfterEach
	void cleanupDataBase(@Autowired EntityManager entityManager) {
		benutzerRepository.deleteAll();
		entityManager.createNativeQuery(
			"DELETE FROM organisation"
		).executeUpdate();
	}

	@Test
	void testOnAuthenticationSuccess_RadVisAuthConcurrentEvents_aktualisiertLetzteAktivitaetNurEinmal()
		throws InterruptedException, ExecutionException {
		// arrange
		Authentication authentication = new RadVisAuthentication(new RadVisUserDetails(benutzer, new ArrayList<>()));

		// act
		Future<?> future1;
		Future<?> future2;
		ExecutorService es = null;
		try {
			es = Executors.newFixedThreadPool(2);
			future1 = es.submit(() -> {
				assertThatNoException().isThrownBy(
					() -> publisher.publishEvent(new AuthenticationSuccessEvent(authentication)));
				return null;
			});

			future2 = es.submit(() -> {
				assertThatNoException().isThrownBy(
					() -> publisher.publishEvent(new AuthenticationSuccessEvent(authentication)));
				return null;
			});
		} finally {
			if (es != null)
				es.shutdown();
		}

		future1.get();
		future2.get();

		// assert
		assertThat(benutzer.getLetzteAktivitaet()).isEqualTo(LocalDate.now());
	}
}