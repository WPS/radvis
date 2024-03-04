package de.wps.radvis.backend.basicAuthentication.schnittstelle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import de.wps.radvis.backend.basicAuthentication.domain.BasicAuthBenutzerRepository;
import de.wps.radvis.backend.basicAuthentication.domain.BasicAuthPasswortService;
import de.wps.radvis.backend.basicAuthentication.domain.BasicAuthenticationConfigurationProperties;
import de.wps.radvis.backend.basicAuthentication.domain.entity.BenutzerBasicAuth;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;

public class BasicAuthenticationProviderTest {
	private AuthenticationProvider basicAuthenticationProvider;

	private final String testPasswort = "testPasswort1234!?öp";

	@Mock
	private BasicAuthBenutzerRepository basicAuthBenutzerRepository;

	@Mock
	private BasicAuthenticationConfigurationProperties basicAuthenticationConfigurationProperties;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		BasicAuthPasswortService basicAuthPasswortService = new BasicAuthPasswortService(new BCryptPasswordEncoder(),
			new SecureRandom(), basicAuthenticationConfigurationProperties);
		basicAuthenticationProvider = new BasicAuthenticationProvider(basicAuthBenutzerRepository,
			basicAuthPasswortService);
		when(basicAuthenticationConfigurationProperties.getPasswordLength()).thenReturn(20);
	}

	@Test
	void testAuthenticate_happy() {
		// arrange
		String testPasswortHash = "$2a$16$pt2rdLCUev3PaEw2D6s.teWT1UzZ8IyzAg3AEl2jqBIXphmdJ3lo2";

		String anmeldeName = "testMail@testRadvis.de";
		Authentication authentication = new UsernamePasswordAuthenticationToken(anmeldeName,
			testPasswort, new ArrayList<>());

		Benutzer benutzer = mock(Benutzer.class);
		when(benutzer.getBasicAuthAnmeldeName()).thenReturn(anmeldeName);
		BenutzerBasicAuth benutzerBasicAuth = new BenutzerBasicAuth(12L, testPasswortHash);
		ReflectionTestUtils.setField(benutzerBasicAuth, "benutzer", benutzer);

		when(basicAuthBenutzerRepository
			.findByBenutzerAnmeldenameAndStatusAktiv(any()))
			.thenReturn(Optional.of(benutzerBasicAuth));

		// act
		Authentication generatedAuthentication = basicAuthenticationProvider.authenticate(authentication);

		// assert
		assertThat(generatedAuthentication.getName()).isEqualTo(authentication.getName());
		assertThat(generatedAuthentication.getPrincipal()).isEqualTo(benutzer);
		assertThat(generatedAuthentication.getCredentials()).isEqualTo(authentication.getCredentials());
	}

	@Test
	void testAuthenticate_passwortFalsch() {
		// arrange
		String testPasswortHashFalsch = "dies_ist_nicht_der.passende_Hash_zum_Passwort";

		Authentication authentication = new UsernamePasswordAuthenticationToken("testMail@testRadvis.de",
			testPasswort, new ArrayList<>());

		Benutzer benutzer = mock(Benutzer.class);
		BenutzerBasicAuth benutzerBasicAuth = new BenutzerBasicAuth(12L, testPasswortHashFalsch);
		ReflectionTestUtils.setField(benutzerBasicAuth, "benutzer", benutzer);

		when(basicAuthBenutzerRepository
			.findByBenutzerAnmeldenameAndStatusAktiv(any()))
			.thenReturn(Optional.of(benutzerBasicAuth));

		// act
		assertThrows(BadCredentialsException.class,
			() -> basicAuthenticationProvider.authenticate(authentication));

	}

	@Test
	void testAuthenticate_emailNotFound() {
		// arrange
		Authentication authentication = new UsernamePasswordAuthenticationToken("testMail@testRadvis.de",
			testPasswort, new ArrayList<>());
		when(basicAuthBenutzerRepository
			.findByBenutzerAnmeldenameAndStatusAktiv(any()))
			.thenReturn(Optional.empty());

		// act + assert
		assertThrows(BadCredentialsException.class,
			() -> basicAuthenticationProvider.authenticate(authentication));
	}
}
