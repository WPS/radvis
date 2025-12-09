package de.wps.radvis.backend.netzfehler.schnittstelle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.netzfehler.domain.AnpassungswunschRepository;
import de.wps.radvis.backend.netzfehler.domain.AnpassungswunschService;
import de.wps.radvis.backend.netzfehler.domain.AnpassungswunschTestDataProvider;
import de.wps.radvis.backend.netzfehler.domain.entity.Anpassungswunsch;
import de.wps.radvis.backend.netzfehler.domain.valueObject.AnpassungswunschKategorie;
import de.wps.radvis.backend.netzfehler.domain.valueObject.AnpassungswunschStatus;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitResolver;
import de.wps.radvis.backend.organisation.domain.entity.Organisation;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;

@ExtendWith(MockitoExtension.class)
class AnpassungswunschControllerTest {

	@Mock
	private AnpassungswunschService anpassungswunschService;
	@SuppressWarnings("unused")
	@Mock
	private AnpassungswunschGuard anpassungswunschGuard;
	@Mock
	private BenutzerResolver benutzerResolver;
	@Mock
	private VerwaltungseinheitResolver verwaltungseinheitResolver;
	@Mock
	private AnpassungswunschRepository anpassungswunschRepository;
	@SuppressWarnings("unused")
	@Mock
	private SaveAnpassungswunschCommandConverter saveAnpassungswunschCommandConverter;

	@InjectMocks
	private AnpassungswunschController anpassungswunschController;

	@Captor
	private ArgumentCaptor<Anpassungswunsch> captor;

	public static Stream<Arguments> kategorieAenderung() {
		return Arrays.stream(AnpassungswunschKategorie.values())
			.flatMap(alteKategorie -> Arrays.stream(AnpassungswunschKategorie.values())
				.filter(neueKategorie -> !neueKategorie.equals(alteKategorie))
				.map(neueKategorie -> Arguments.of(alteKategorie, neueKategorie)));
	}

	@ParameterizedTest
	@EnumSource(AnpassungswunschKategorie.class)
	void createAnpassungswunsch_versendetMail(AnpassungswunschKategorie kategorie) {
		// arrange
		Point point = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
			.createPoint(new Coordinate(2, 3));
		String beschreibung = "Neue Beschreibung";
		AnpassungswunschStatus status = AnpassungswunschStatus.KLAERUNGSBEDARF;
		Organisation organisation = VerwaltungseinheitTestDataProvider.defaultOrganisation().id(2L).build();

		Authentication authentication = mock(Authentication.class);
		Benutzer benutzer = BenutzerTestDataProvider.defaultBenutzer().id(2L).build();
		when(benutzerResolver.fromAuthentication(authentication)).thenReturn(benutzer);
		when(verwaltungseinheitResolver.resolve(organisation.getId())).thenReturn(organisation);
		Anpassungswunsch expectedAnpassungswunsch = new Anpassungswunsch(
			point,
			beschreibung,
			status,
			kategorie,
			benutzer,
			Optional.of(organisation),
			Optional.empty()
		);
		when(anpassungswunschService.create(
			point,
			beschreibung,
			status,
			kategorie,
			benutzer,
			Optional.of(organisation),
			Optional.empty())
		).thenReturn(
			expectedAnpassungswunsch
		);

		// act
		SaveAnpassungswunschCommand command = new SaveAnpassungswunschCommand(
			point,
			beschreibung,
			status,
			organisation.getId(),
			kategorie,
			null);

		anpassungswunschController.createAnpassungswunsch(authentication, command);
		verify(anpassungswunschService).versendeInfoMailZuNeuemAnpassungswunsch(captor.capture());
		assertThat(captor.getValue()).usingRecursiveComparison().isEqualTo(expectedAnpassungswunsch);
	}

	@MethodSource("kategorieAenderung")
	@ParameterizedTest
	void updateAnpassungswunsch_kategorieVeraendert_versendetMail(
		AnpassungswunschKategorie kategorieAlt,
		AnpassungswunschKategorie kategorieNeu) {
		// arrange
		Point point = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
			.createPoint(new Coordinate(2, 3));
		String beschreibung = "Neue Beschreibung";
		AnpassungswunschStatus status = AnpassungswunschStatus.KLAERUNGSBEDARF;
		Organisation organisation = VerwaltungseinheitTestDataProvider.defaultOrganisation().id(2L).build();

		Authentication authentication = mock(Authentication.class);
		Benutzer benutzer = BenutzerTestDataProvider.defaultBenutzer().id(2L).build();
		when(benutzerResolver.fromAuthentication(authentication)).thenReturn(benutzer);

		Anpassungswunsch.AnpassungswunschBuilder anpassungswunschBuilder = AnpassungswunschTestDataProvider
			.defaultValue()
			.erstellung(null)
			.aenderung(null)
			.id(42L)
			.geometrie(point)
			.beschreibung(beschreibung)
			.status(status)
			.kategorie(kategorieAlt)
			.benutzerLetzteAenderung(benutzer)
			.verantwortlicheOrganisation(organisation)
			.konsistenzregelVerletzungReferenz(null);
		Anpassungswunsch anpassungswunsch = anpassungswunschBuilder.build();

		when(anpassungswunschService.getAnpassungswunsch(42L)).thenReturn(
			anpassungswunsch
		);
		when(anpassungswunschRepository.save(any())).thenReturn(
			anpassungswunschBuilder.kategorie(kategorieNeu).build()
		);

		// act
		SaveAnpassungswunschCommand command = new SaveAnpassungswunschCommand(
			point,
			beschreibung,
			status,
			organisation.getId(),
			kategorieNeu,
			null);

		anpassungswunschController.updateAnpassungswunsch(authentication, 42L, command);

		verify(anpassungswunschService).versendeInfoMailZuNeuemAnpassungswunsch(captor.capture());
		assertThat(captor.getValue()).usingRecursiveComparison().isEqualTo(
			anpassungswunschBuilder.kategorie(kategorieNeu).build()
		);
	}

	@EnumSource(AnpassungswunschKategorie.class)
	@ParameterizedTest
	void updateAnpassungswunsch_kategorieBleibtGleich_versendetKEINEMail(
		AnpassungswunschKategorie kategorie) {
		// arrange
		Point point = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
			.createPoint(new Coordinate(2, 3));
		String beschreibung = "Neue Beschreibung";
		AnpassungswunschStatus status = AnpassungswunschStatus.KLAERUNGSBEDARF;
		Organisation organisation = VerwaltungseinheitTestDataProvider.defaultOrganisation().id(2L).build();

		Authentication authentication = mock(Authentication.class);
		Benutzer benutzer = BenutzerTestDataProvider.defaultBenutzer().id(2L).build();
		when(benutzerResolver.fromAuthentication(authentication)).thenReturn(benutzer);

		Anpassungswunsch anpassungswunsch = AnpassungswunschTestDataProvider.defaultValue()
			.id(42L)
			.geometrie(point)
			.beschreibung(beschreibung)
			.status(status)
			.kategorie(kategorie)
			.benutzerLetzteAenderung(benutzer)
			.verantwortlicheOrganisation(organisation)
			.konsistenzregelVerletzungReferenz(null)
			.build();

		when(anpassungswunschService.getAnpassungswunsch(42L)).thenReturn(
			anpassungswunsch
		);

		when(anpassungswunschRepository.save(anpassungswunsch)).thenReturn(anpassungswunsch);

		// act
		SaveAnpassungswunschCommand command = new SaveAnpassungswunschCommand(
			point,
			"neue Beschreibung",
			AnpassungswunschStatus.OFFEN,
			organisation.getId(),
			kategorie,
			null);

		anpassungswunschController.updateAnpassungswunsch(authentication, 42L, command);
		verify(anpassungswunschService, never()).versendeInfoMailZuNeuemAnpassungswunsch(any());
	}
}