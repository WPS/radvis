package de.wps.radvis.backend.benutzer.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.openMocks;

import java.time.LocalDate;
import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import de.wps.radvis.backend.authentication.domain.RadVisAuthentication;
import de.wps.radvis.backend.authentication.domain.entity.RadVisUserDetails;
import de.wps.radvis.backend.basicAuthentication.domain.BenutzerBasicAuthenticationToken;
import de.wps.radvis.backend.basicAuthentication.domain.entity.BenutzerBasicAuth;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.benutzer.domain.repository.BenutzerRepository;

class BenutzerAktivitaetsServiceTest {

	BenutzerAktivitaetsService benutzerAktivitaetsService;

	@Mock
	BenutzerRepository benutzerRepository;

	@BeforeEach
	void setup() {
		openMocks(this);
		benutzerAktivitaetsService = new BenutzerAktivitaetsService(benutzerRepository);
	}

	@Test
	void testOnAuthenticationSuccess_basicAuth_aktualisiertLetzteAktivitaet() {
		// arrange
		Benutzer benutzer = BenutzerTestDataProvider
			.defaultBenutzer()
			.id(1L)
			.letzteAktivitaet(LocalDate.now().minusDays(30))
			.build();

		Authentication authentication = new BenutzerBasicAuthenticationToken(benutzer, "testPasswort");

		BenutzerBasicAuth benutzerBasicAuth = new BenutzerBasicAuth(12L, "testPasswortHash");
		ReflectionTestUtils.setField(benutzerBasicAuth, "benutzer", benutzer);

		// act
		benutzerAktivitaetsService.onAuthenticationSuccess(new AuthenticationSuccessEvent(authentication));

		// assert
		assertThat(benutzer.getLetzteAktivitaet()).isEqualTo(LocalDate.now());
		verify(benutzerRepository, times(1)).save(any());
	}

	@Test
	void testOnAuthenticationSuccess_basicAuthBereitsAktualisiert_aktualisiertLetzteAktivitaetNicht() {
		// arrange
		Benutzer benutzer = BenutzerTestDataProvider
			.defaultBenutzer()
			.id(1L)
			.letzteAktivitaet(LocalDate.now())
			.build();

		Authentication authentication = new BenutzerBasicAuthenticationToken(benutzer, "testPasswort");

		BenutzerBasicAuth benutzerBasicAuth = new BenutzerBasicAuth(12L, "testPasswortHash");
		ReflectionTestUtils.setField(benutzerBasicAuth, "benutzer", benutzer);

		// act
		benutzerAktivitaetsService.onAuthenticationSuccess(new AuthenticationSuccessEvent(authentication));

		// assert
		assertThat(benutzer.getLetzteAktivitaet()).isEqualTo(LocalDate.now());
		verify(benutzerRepository, never()).save(any());
	}

	@Test
	void testOnAuthenticationSuccess_RadVisAuth_aktualisiertLetzteAktivitaet() {
		// arrange
		Benutzer benutzer = BenutzerTestDataProvider
			.defaultBenutzer()
			.id(1L)
			.letzteAktivitaet(LocalDate.now().minusDays(30))
			.build();

		Authentication authentication = new RadVisAuthentication(new RadVisUserDetails(benutzer, new ArrayList<>()));

		// act
		benutzerAktivitaetsService.onAuthenticationSuccess(new AuthenticationSuccessEvent(authentication));

		// assert
		assertThat(benutzer.getLetzteAktivitaet()).isEqualTo(LocalDate.now());
		verify(benutzerRepository, times(1)).save(any());
	}

	@Test
	void testOnAuthenticationSuccess_RadVisAuthBereitsAktualisiert_aktualisiertLetzteAktivitaetNicht() {
		// arrange
		Benutzer benutzer = BenutzerTestDataProvider
			.defaultBenutzer()
			.id(1L)
			.letzteAktivitaet(LocalDate.now())
			.build();

		Authentication authentication = new RadVisAuthentication(new RadVisUserDetails(benutzer, new ArrayList<>()));

		// act
		benutzerAktivitaetsService.onAuthenticationSuccess(new AuthenticationSuccessEvent(authentication));

		// assert
		assertThat(benutzer.getLetzteAktivitaet()).isEqualTo(LocalDate.now());
		verify(benutzerRepository, never()).save(any());
	}

	@Test
	void testOnAuthenticationSuccess_RadVisAuthBenutzerIsNull_keineException() {
		// arrange
		// Bei Benutzer-Registrierung der Fall
		Benutzer benutzer = null;

		Authentication authentication = new BenutzerBasicAuthenticationToken(benutzer, "testPasswort");

		BenutzerBasicAuth benutzerBasicAuth = new BenutzerBasicAuth(12L, "testPasswortHash");
		ReflectionTestUtils.setField(benutzerBasicAuth, "benutzer", benutzer);

		// act & assert
		assertThatNoException().isThrownBy(
			() -> benutzerAktivitaetsService.onAuthenticationSuccess(new AuthenticationSuccessEvent(authentication))
		);

		verify(benutzerRepository, never()).save(any());
	}

	@Test
	void testOnAuthenticationSuccess_RadVisAuthBenutzerIdIsNull_aktualisiertLetzteAktivitaetNicht() {
		// arrange
		Benutzer benutzer = BenutzerTestDataProvider
			.defaultBenutzer()
			.id(null)
			.letzteAktivitaet(LocalDate.now().minusDays(2))
			.build();

		Authentication authentication = new RadVisAuthentication(new RadVisUserDetails(benutzer, new ArrayList<>()));

		// act
		benutzerAktivitaetsService.onAuthenticationSuccess(new AuthenticationSuccessEvent(authentication));

		// assert
		assertThat(benutzer.getLetzteAktivitaet()).isEqualTo(LocalDate.now().minusDays(2));
		verify(benutzerRepository, never()).save(any());
	}
}