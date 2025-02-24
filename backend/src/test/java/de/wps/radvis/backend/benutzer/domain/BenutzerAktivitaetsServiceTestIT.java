package de.wps.radvis.backend.benutzer.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import de.wps.radvis.backend.authentication.domain.RadVisAuthentication;
import de.wps.radvis.backend.authentication.domain.entity.RadVisUserDetails;
import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.benutzer.domain.repository.BenutzerRepository;
import de.wps.radvis.backend.benutzer.domain.valueObject.ServiceBwId;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.GebietskoerperschaftRepository;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitImportRepository;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitRepository;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Tag("group4")
@ContextConfiguration(classes = {
	BenutzerConfiguration.class,
	OrganisationConfiguration.class,
	GeoConverterConfiguration.class,
})
@EnableConfigurationProperties(value = {
	TechnischerBenutzerConfigurationProperties.class,
	OrganisationConfigurationProperties.class,
	CommonConfigurationProperties.class,
})
class BenutzerAktivitaetsServiceTestIT extends DBIntegrationTestIT {

	@Autowired
	BenutzerAktivitaetsService benutzerAktivitaetsService;

	@Autowired
	BenutzerRepository benutzerRepository;

	@Mock
	BenutzerRepository benutzerRepositoryMock;

	@Autowired
	GebietskoerperschaftRepository gebietskoerperschaftRepository;

	@Autowired
	VerwaltungseinheitRepository verwaltungseinheitRepository;

	@Autowired
	ApplicationEventPublisher publisher;

	@Autowired
	EntityManager entityManager;

	@Autowired
	PlatformTransactionManager platformTransactionManager;

	@MockitoBean
	VerwaltungseinheitImportRepository verwaltungseinheitImportRepository;

	@Test
	@Transactional(value = Transactional.TxType.NEVER)
	void testOnAuthenticationSuccess_handleOptimisticLockingException()
		throws InterruptedException, ExecutionException {
		// arrange
		ServiceBwId serviceBwId = ServiceBwId.of("testId");
		new TransactionTemplate(platformTransactionManager).executeWithoutResult((status) -> {
			Verwaltungseinheit verwaltungseinheit = gebietskoerperschaftRepository.save(
				VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build());

			benutzerRepository.save(BenutzerTestDataProvider
				.defaultBenutzer()
				.organisation(verwaltungseinheit)
				.serviceBwId(serviceBwId)
				.letzteAktivitaet(LocalDate.now().minusDays(2))
				.build());

			entityManager.flush();
			entityManager.clear();
		});

		Benutzer benutzer1 = benutzerRepository.findByServiceBwId(serviceBwId).get();
		entityManager.clear();
		Benutzer benutzer2 = benutzerRepository.findByServiceBwId(serviceBwId).get();
		entityManager.clear();

		Authentication authentication = new RadVisAuthentication(
			new RadVisUserDetails(benutzer1, new ArrayList<>()));
		publisher.publishEvent(new AuthenticationSuccessEvent(authentication));

		// act + assert

		Authentication authentication2 = new RadVisAuthentication(
			new RadVisUserDetails(benutzer2, new ArrayList<>()));
		assertThatNoException()
			.isThrownBy(() -> publisher.publishEvent(new AuthenticationSuccessEvent(authentication2)));

		entityManager.clear();

		new TransactionTemplate(platformTransactionManager).executeWithoutResult((status) -> {
			benutzerRepository.deleteAll();
			entityManager.createNativeQuery("DELETE FROM organisation").executeUpdate();
		});
	}

	@Test
	void testOnAuthenticationSuccess_gleichzeitigeAufrufe()
		throws InterruptedException, ExecutionException {
		// arrange
		openMocks(this);

		benutzerAktivitaetsService = new BenutzerAktivitaetsService(benutzerRepositoryMock);

		Verwaltungseinheit verwaltungseinheit = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build());

		Benutzer benutzer = benutzerRepository.save(BenutzerTestDataProvider
			.defaultBenutzer()
			.organisation(verwaltungseinheit)
			.serviceBwId(ServiceBwId.of("testId"))
			.letzteAktivitaet(LocalDate.now().minusDays(2))
			.build());

		TestTransaction.flagForCommit();
		TestTransaction.end();
		TestTransaction.start();

		Authentication authentication = new RadVisAuthentication(
			new RadVisUserDetails(benutzer, new ArrayList<>()));

		int numberOfThreads = 2;
		ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
		CountDownLatch latch = new CountDownLatch(numberOfThreads);

		// Künstlich alle bis auf den ersten Aufruf verlangsamen. Damit konnte eine wegen Nebenläufigkeit auftretende
		// Optimistic-Locking-Exception provoziert werden.
		AtomicInteger atomInt = new AtomicInteger(0);
		when(benutzerRepositoryMock.findById(any())).thenAnswer(invocation -> {
			Optional<Benutzer> result = benutzerRepository.findById(invocation.getArgument(0));
			if (atomInt.getAndIncrement() > 0) {
				Thread.sleep(1000);
			}
			return result;
		});
		when(benutzerRepositoryMock.save(any())).thenAnswer(invocation -> {
			return benutzerRepository.save(invocation.getArgument(0));
		});

		ArrayList<Exception> thrownExceptions = new ArrayList<>();

		// act
		for (int i = 0; i < numberOfThreads; i++) {
			executorService.submit(() -> {
				try {
					benutzerAktivitaetsService.onAuthenticationSuccess(new AuthenticationSuccessEvent(authentication));
				} catch (Exception t) {
					thrownExceptions.add(t);
				} finally {
					latch.countDown();
				}
			});
		}
		latch.await();

		// assert
		assertThat(thrownExceptions).isEmpty();
		assertThat(benutzerRepository.findByServiceBwId(benutzer.getServiceBwId()).get().getLetzteAktivitaet())
			.isEqualTo(LocalDate.now());

		entityManager.clear();

		// cleanup
		benutzerRepository.deleteAll();
		entityManager.createNativeQuery("DELETE FROM organisation").executeUpdate();

		TestTransaction.flagForCommit();
		TestTransaction.end();
	}
}