/*
 * Copyright (c) 2023 WPS - Workplace Solutions GmbH
 *
 * Licensed under the EUPL, Version 1.2 or as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */

package de.wps.radvis.backend.netz.schnittstelle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.assertj.core.groups.Tuple;
import org.jaitools.jts.CoordinateSequence2D;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.togglz.junit5.AllDisabled;
import org.togglz.testing.TestFeatureManager;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.benutzer.domain.valueObject.Rolle;
import de.wps.radvis.backend.common.domain.FeatureTogglz;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.netz.domain.entity.FahrtrichtungAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttribute;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.KantenAttribute;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.KnotenAttribute;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.provider.FuehrungsformAttributeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.GeschwindigkeitsAttributeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.service.ZustaendigkeitsService;
import de.wps.radvis.backend.netz.domain.valueObject.Beleuchtung;
import de.wps.radvis.backend.netz.domain.valueObject.Hoechstgeschwindigkeit;
import de.wps.radvis.backend.netz.domain.valueObject.IstStandard;
import de.wps.radvis.backend.netz.domain.valueObject.KantenOrtslage;
import de.wps.radvis.backend.netz.domain.valueObject.KantenSeite;
import de.wps.radvis.backend.netz.domain.valueObject.KnotenOrtslage;
import de.wps.radvis.backend.netz.domain.valueObject.Kommentar;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.Richtung;
import de.wps.radvis.backend.netz.domain.valueObject.Status;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenquerschnittRASt06;
import de.wps.radvis.backend.netz.domain.valueObject.Umfeld;
import de.wps.radvis.backend.netz.schnittstelle.command.ChangeSeitenbezugCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.CreateKanteCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.SaveFuehrungsformAttributGruppeCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.SaveFuehrungsformAttributeCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.SaveGeschwindigkeitAttributGruppeCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.SaveGeschwindigkeitAttributeCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.SaveKanteAttributeCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.SaveKanteFahrtrichtungCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.SaveKanteVerlaufCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.SaveKnotenCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.SaveZustaendigkeitAttributGruppeCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.SaveZustaendigkeitAttributeCommand;
import de.wps.radvis.backend.netz.schnittstelle.view.GeschwindigkeitAttributeEditView;
import de.wps.radvis.backend.netz.schnittstelle.view.KanteEditView;
import de.wps.radvis.backend.netz.schnittstelle.view.KnotenEditView;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;

class NetzControllerTest {

	@Mock
	private NetzService netzService;
	@Mock
	private BenutzerResolver benutzerResolver;
	@Mock
	private Authentication authentication;
	@Mock
	private SaveKanteCommandConverter saveKanteCommandConverter;
	@Mock
	private NetzToFeatureDetailsConverter netzToFeatureDetailsConverter;
	@Mock
	private OrganisationConfigurationProperties organisationConfigurationProperties;

	@Captor
	private ArgumentCaptor<List<KantenAttributGruppe>> kanteAttributGruppeCaptor;
	@Captor
	private ArgumentCaptor<List<FuehrungsformAttributGruppe>> fuehrungsformAttributGruppeCaptor;
	@Captor
	private ArgumentCaptor<Kante> kanteCaptor;
	@Captor
	private ArgumentCaptor<Benutzer> benutzerCaptor;

	private NetzController netzController;

	GeometryFactory geometryFactory = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory();
	private Verwaltungseinheit organisation;
	private Benutzer benutzer;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);

		when(organisationConfigurationProperties.getZustaendigkeitBufferInMeter()).thenReturn(500);

		ZustaendigkeitsService zustaendigkeitsService = new ZustaendigkeitsService(organisationConfigurationProperties);

		netzController = new NetzController(netzService,
			new NetzGuard(netzService, zustaendigkeitsService, benutzerResolver),
			benutzerResolver, zustaendigkeitsService, saveKanteCommandConverter, netzToFeatureDetailsConverter);

		organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(101L).name("Bundesland")
			.organisationsArt(
				OrganisationsArt.BUNDESLAND)
			.bereich(geometryFactory
				.createMultiPolygon(new Polygon[] { geometryFactory.createPolygon(
					geometryFactory.createLinearRing(new CoordinateSequence2D(0, 0, 0, 10, 10, 10, 10, 0, 0, 0))) }))
			.build();

		// Standartfall, falls abweichendes benoetigt wird -> ueberschreiben
		benutzer = buildBenutzerMitRolle(Rolle.RADWEGE_ERFASSERIN);
		when(benutzerResolver.fromAuthentication(authentication)).thenReturn(benutzer);
	}

	@Test
	void testeSaveKanteAllgemein_BenutzerAutorisiert() {
		// arrange
		Long kanteId = 1L;
		Long gruppeId = 10L;

		SaveKanteAttributeCommand command = SaveKanteAttributeCommand.builder()
			.kanteId(kanteId)
			.gruppenId(gruppeId)
			.gruppenVersion(1L)
			.netzklassen(Set.of(Netzklasse.KOMMUNALNETZ_ALLTAG))
			.beleuchtung(Beleuchtung.VORHANDEN)
			.umfeld(Umfeld.UNBEKANNT)
			.strassenquerschnittRASt06(StrassenquerschnittRASt06.UNBEKANNT)
			.status(Status.FIKTIV)
			.istStandards(Set.of(IstStandard.BASISSTANDARD))
			.build();

		Benutzer benutzer = buildBenutzerMitRolle(Rolle.RADVERKEHRSBEAUFTRAGTER);
		when(benutzerResolver.fromAuthentication(authentication)).thenReturn(benutzer);

		KantenAttributGruppe kantenAttributGruppe = KantenAttributGruppeTestDataProvider.defaultValue()
			.id(2L).version(1L).build();

		Kante kante = KanteTestDataProvider.withDefaultValues().id(kanteId).version(1L).quelle(QuellSystem.DLM)
			.kantenAttributGruppe(kantenAttributGruppe)
			.geometry(geometryFactory.createLineString(new CoordinateSequence2D(1, 1, 5, 5))).build();
		when(netzService.getKanten(any())).thenReturn(List.of(kante)).thenReturn(List.of(kante))
			.thenReturn(List.of(kante));

		KantenAttribute expectedKantenAttribute = KantenAttribute.builder().build();
		KantenAttributGruppe expectedKantenAttributGruppe = KantenAttributGruppeTestDataProvider.defaultValue()
			.id(command.getGruppenId())
			.version(command.getGruppenVersion())
			.netzklassen(command.getNetzklassen())
			.istStandards(command.getIstStandards())
			.kantenAttribute(expectedKantenAttribute)
			.build();
		when(saveKanteCommandConverter.convertKantenAttributeCommand(command)).thenReturn(expectedKantenAttribute);

		// act
		assertDoesNotThrow(
			() -> netzController.saveKanteAllgemein(authentication, List.of(command)));

		// assert
		verify(netzService, times(1)).aktualisiereKantenAttribute(kanteAttributGruppeCaptor.capture());
		List<List<KantenAttributGruppe>> allValues = kanteAttributGruppeCaptor.getAllValues();
		assertThat(allValues).hasSize(1);
		assertThat(allValues.get(0)).hasSize(1);
		final var actualKanteAttributGruppe = allValues.get(0).get(0);
		assertThat(actualKanteAttributGruppe.getId()).isEqualTo(expectedKantenAttributGruppe.getId());
		assertThat(actualKanteAttributGruppe.getVersion()).isEqualTo(expectedKantenAttributGruppe.getVersion());
		assertThat(
			actualKanteAttributGruppe.getNetzklassen().equals(expectedKantenAttributGruppe.getNetzklassen())).isTrue();
		assertThat(actualKanteAttributGruppe.getIstStandards()
			.equals(expectedKantenAttributGruppe.getIstStandards())).isTrue();
		assertThat(actualKanteAttributGruppe.getKantenAttribute()
			.equals(expectedKantenAttributGruppe.getKantenAttribute())).isTrue();
	}

	@Test
	void testeSaveKanteAllgemein_deny_keinRecht() {
		// arrange
		Long kanteId = 1L;
		Long gruppeId = 10L;

		SaveKanteAttributeCommand saveKantenAttributeCommand = SaveKanteAttributeCommand.builder()
			.kanteId(kanteId)
			.gruppenId(gruppeId)
			.gruppenVersion(1L)
			.netzklassen(Set.of(Netzklasse.KOMMUNALNETZ_ALLTAG))
			.beleuchtung(Beleuchtung.VORHANDEN)
			.umfeld(Umfeld.UNBEKANNT)
			.strassenquerschnittRASt06(StrassenquerschnittRASt06.UNBEKANNT)
			.status(Status.FIKTIV)
			.istStandards(Set.of(IstStandard.BASISSTANDARD))
			.build();

		Benutzer benutzer = buildBenutzerMitRolle(Rolle.RADVIS_BETRACHTER);
		when(benutzerResolver.fromAuthentication(authentication)).thenReturn(benutzer);

		// act + assert
		assertThatThrownBy(() -> netzController.saveKanteAllgemein(authentication, List.of(saveKantenAttributeCommand)))
			.isInstanceOf(AccessDeniedException.class)
			.hasMessage(
				"Sie haben nicht die Berechtigung Kanten zu bearbeiten");
	}

	@Test
	void testeSaveKanteAllgemein_deny_kanteNichtInZustaendigkeitBereich() {
		// arrange
		Long kanteId = 1L;
		Long gruppeId = 10L;

		SaveKanteAttributeCommand saveKantenAttributeCommand = SaveKanteAttributeCommand.builder()
			.kanteId(kanteId)
			.gruppenId(gruppeId)
			.gruppenVersion(1L)
			.netzklassen(Set.of(Netzklasse.KOMMUNALNETZ_ALLTAG))
			.beleuchtung(Beleuchtung.VORHANDEN)
			.umfeld(Umfeld.UNBEKANNT)
			.strassenquerschnittRASt06(StrassenquerschnittRASt06.UNBEKANNT)
			.status(Status.FIKTIV)
			.istStandards(Set.of(IstStandard.BASISSTANDARD))
			.build();

		Benutzer benutzer = buildBenutzerMitRolle(Rolle.RADVERKEHRSBEAUFTRAGTER);
		when(benutzerResolver.fromAuthentication(authentication)).thenReturn(benutzer);

		KantenAttributGruppe kantenAttributGruppe = KantenAttributGruppeTestDataProvider.defaultValue()
			.id(2L).version(1L).build();
		Kante kante = KanteTestDataProvider.withDefaultValues().id(kanteId).version(1L).quelle(QuellSystem.DLM)
			.kantenAttributGruppe(kantenAttributGruppe)
			.geometry(geometryFactory.createLineString(new CoordinateSequence2D(600, 600, 550, 550))).build();
		when(netzService.getKanten(any())).thenReturn(List.of(kante)).thenReturn(List.of(kante))
			.thenReturn(List.of(kante));

		// act + assert
		assertThatThrownBy(() -> netzController.saveKanteAllgemein(authentication, List.of(saveKantenAttributeCommand)))
			.isInstanceOf(AccessDeniedException.class)
			.hasMessage(
				"Die Kante liegt nicht in Ihrem Zuständigkeitsbereich");
	}

	@Test
	void testeSaveKanteAllgemein_deny_RadnetzVerlegung() {
		// arrange
		SaveKanteAttributeCommand saveKantenAttributeCommand = SaveKanteAttributeCommand.builder().kanteId(2L)
			.gruppenId(2L).gruppenId(1L).netzklassen(Set.of(Netzklasse.RADNETZ_ALLTAG)).build();

		KantenAttributGruppe kantenAttributGruppe = KantenAttributGruppeTestDataProvider.defaultValue()
			.id(2L).version(1L).build();

		Kante kante = KanteTestDataProvider.withDefaultValues().id(2L).version(1L).quelle(QuellSystem.DLM)
			.kantenAttributGruppe(kantenAttributGruppe).build();
		when(netzService.getKante(saveKantenAttributeCommand.getKanteId()))
			.thenReturn(kante);
		when(netzService.getKanten(any())).thenReturn(List.of(kante)).thenReturn(List.of(kante))
			.thenReturn(List.of(kante));

		// act + assert
		assertThrows(AccessDeniedException.class,
			() -> netzController.saveKanteAllgemein(authentication, List.of(saveKantenAttributeCommand)));
	}

	@Test
	void testeSaveKanteAllgemein_keine_RadnetzVerlegung() {
		// arrange
		Long kanteId = 1L;
		Long gruppeId = 10L;

		SaveKanteAttributeCommand saveKantenAttributeCommand = SaveKanteAttributeCommand.builder()
			.kanteId(kanteId)
			.gruppenId(gruppeId)
			.gruppenVersion(1L)
			.netzklassen(Set.of(Netzklasse.KOMMUNALNETZ_ALLTAG))
			.beleuchtung(Beleuchtung.VORHANDEN)
			.umfeld(Umfeld.UNBEKANNT)
			.strassenquerschnittRASt06(StrassenquerschnittRASt06.UNBEKANNT)
			.status(Status.FIKTIV)
			.istStandards(Set.of(IstStandard.BASISSTANDARD))
			.build();

		KantenAttributGruppe kantenAttributGruppe = KantenAttributGruppeTestDataProvider.defaultValue()
			.id(gruppeId)
			.version(1L)
			.istStandards(Set.of(IstStandard.BASISSTANDARD))
			.build();

		Kante kante = KanteTestDataProvider.withDefaultValues().id(kanteId).version(1L).quelle(QuellSystem.DLM)
			.kantenAttributGruppe(kantenAttributGruppe).build();
		when(netzService.getKante(saveKantenAttributeCommand.getKanteId()))
			.thenReturn(kante);
		when(netzService.getKanten(any())).thenReturn(List.of(kante)).thenReturn(List.of(kante))
			.thenReturn(List.of(kante));

		// act + assert
		assertDoesNotThrow(
			() -> netzController.saveKanteAllgemein(authentication, List.of(saveKantenAttributeCommand)));
	}

	@Test
	void testeGetKanteEditView_kanteInAllowedArea_flagAufTrue() {
		// arrange
		long kanteId = 2L;
		LineString lineString = geometryFactory.createLineString(new CoordinateSequence2D(1, 1, 5, 5));
		Kante kante = KanteTestDataProvider.withDefaultValues().geometry(lineString).id(kanteId).version(1L)
			.quelle(QuellSystem.DLM).build();
		when(netzService.getKante(kanteId)).thenReturn(kante);

		// act
		KanteEditView kanteEditView = netzController.getKanteEditView(kanteId, authentication);

		// assert
		assertThat(kanteEditView.isLiegtInZustaendigkeitsbereich()).isTrue();
	}

	@Test
	void testeGetKanteEditView_kanteNotInAllowedArea_flagAufFalse() {
		// arrange
		long kanteId = 2L;
		LineString lineString = geometryFactory.createLineString(new CoordinateSequence2D(601, 601, 605, 605));
		Kante kante = KanteTestDataProvider.withDefaultValues().geometry(lineString).id(kanteId).version(1L)
			.quelle(QuellSystem.DLM).build();
		when(netzService.getKante(kanteId)).thenReturn(kante);

		// act
		KanteEditView kanteEditView = netzController.getKanteEditView(kanteId, authentication);

		// assert
		assertThat(kanteEditView.isLiegtInZustaendigkeitsbereich()).isFalse();
	}

	@Test
	void testeGetKnotenEditView_knotenInAllowedArea_flagAufTrue() {
		// arrange
		long knotenId = 2L;
		Point point = geometryFactory.createPoint(new CoordinateSequence2D(1, 1));
		Knoten knoten = KnotenTestDataProvider.withDefaultValues().point(point).knotenAttribute(
			KnotenAttribute.builder().build()).build();
		when(netzService.getKnoten(knotenId)).thenReturn(knoten);

		// act
		KnotenEditView knotenEditView = netzController.getKnotenEditView(knotenId, authentication);

		// assert
		assertThat(knotenEditView.isLiegtInZustaendigkeitsbereich()).isTrue();
	}

	@Test
	void testeGetKnotenEditView_knotenNotInAllowedArea_flagAufFalse() {
		// arrange
		long knotenId = 2L;
		Point point = geometryFactory.createPoint(new CoordinateSequence2D(601, 101));
		Knoten knoten = KnotenTestDataProvider.withDefaultValues().point(point).knotenAttribute(
			KnotenAttribute.builder().build()).build();
		when(netzService.getKnoten(knotenId)).thenReturn(knoten);

		// act
		KnotenEditView knotenEditView = netzController.getKnotenEditView(knotenId, authentication);

		// assert
		assertThat(knotenEditView.isLiegtInZustaendigkeitsbereich()).isFalse();
	}

	@Test
	void testeCreateKante_allow_knotenNichtInZustaendigkeitBereichAdmin() {
		// arrange
		CreateKanteCommand command = CreateKanteCommand.builder().vonKnotenId(1L).bisKnotenId(2L).build();

		Benutzer benutzer = buildBenutzerMitRolle(Rolle.RADVIS_ADMINISTRATOR);
		when(benutzerResolver.fromAuthentication(authentication)).thenReturn(benutzer);

		when(netzService.getKnoten(1L)).thenReturn(
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(10, 10), QuellSystem.DLM).build());
		when(netzService.getKnoten(2L)).thenReturn(
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(20, 20), QuellSystem.DLM).build());

		when(netzService.createGrundnetzKante(any(), any(), any())).thenReturn(
			KanteTestDataProvider.withDefaultValues().build());

		// act + assert
		assertDoesNotThrow(() -> netzController.createKante(command, authentication));
		verify(netzService, times(1)).createGrundnetzKante(1L, 2L, Status.FIKTIV);
	}

	@Test
	void testeCreateKante_deny_knotenNichtDLM() {
		// arrange
		CreateKanteCommand command = CreateKanteCommand.builder().vonKnotenId(1L).bisKnotenId(2L).build();

		Benutzer benutzer = buildBenutzerMitRolle(Rolle.RADVIS_ADMINISTRATOR);
		when(benutzerResolver.fromAuthentication(authentication)).thenReturn(benutzer);

		when(netzService.getKnoten(1L)).thenReturn(
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(10, 10), QuellSystem.LGL).build());
		when(netzService.getKnoten(2L)).thenReturn(
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(20, 20), QuellSystem.DLM).build());

		when(netzService.createGrundnetzKante(any(), any(), any())).thenReturn(
			KanteTestDataProvider.withDefaultValues().build());

		// act + assert
		assertThatThrownBy(() -> netzController.createKante(command, authentication))
			.isInstanceOf(AccessDeniedException.class)
			.hasMessage("Es können nur Kanten zwischen DLM- oder RadVis-Knoten erstellt werden");
		verify(netzService, never()).createGrundnetzKante(any(), any(), any());
	}

	@Test
	void testeCreateKante_deny_knotenNichtInZustaendigkeitsbereich() {
		// arrange
		CreateKanteCommand command = CreateKanteCommand.builder().vonKnotenId(1L).bisKnotenId(2L).build();

		Benutzer benutzer = buildBenutzerMitRolle(Rolle.RADWEGE_ERFASSERIN);
		when(benutzerResolver.fromAuthentication(authentication)).thenReturn(benutzer);

		when(netzService.getKnoten(1L)).thenReturn(
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(510, 10), QuellSystem.DLM).build());
		when(netzService.getKnoten(2L)).thenReturn(
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(520, 20), QuellSystem.DLM).build());

		when(netzService.createGrundnetzKante(any(), any(), any())).thenReturn(
			KanteTestDataProvider.withDefaultValues().build());

		// act + assert
		assertThatThrownBy(() -> netzController.createKante(command, authentication))
			.isInstanceOf(AccessDeniedException.class)
			.hasMessage("Die erstellte Kante liegt nicht in Ihrem Zuständigkeitsbereich");
		verify(netzService, never()).createGrundnetzKante(any(), any(), any());
	}

	@Test
	void testeCreateKante_deny_keineBerechtigung() {
		// arrange
		CreateKanteCommand command = CreateKanteCommand.builder().vonKnotenId(1L).bisKnotenId(2L).build();

		Benutzer benutzer = buildBenutzerMitRolle(Rolle.LGL_MITARBEITERIN);
		when(benutzerResolver.fromAuthentication(authentication)).thenReturn(benutzer);

		when(netzService.getKnoten(1L)).thenReturn(
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(10, 10), QuellSystem.DLM).build());
		when(netzService.getKnoten(2L)).thenReturn(
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(20, 20), QuellSystem.DLM).build());

		when(netzService.createGrundnetzKante(any(), any(), any())).thenReturn(
			KanteTestDataProvider.withDefaultValues().build());

		// act + assert
		assertThatThrownBy(() -> netzController.createKante(command, authentication))
			.isInstanceOf(AccessDeniedException.class)
			.hasMessage("Sie haben nicht die Berechtigung Kanten zu erstellen");
		verify(netzService, never()).createGrundnetzKante(any(), any(), any());
	}

	@Test
	void testeSaveKnoten() {
		// arrange
		Long knotenVersion = 1L;
		Long knotenID = knotenVersion;
		Kommentar kommentar = Kommentar.of("Foo geht in die Bar.");

		SaveKnotenCommand saveKnotenCommand = SaveKnotenCommand.builder()
			.id(knotenID)
			.knotenVersion(knotenVersion)
			.gemeinde(123L)
			.kommentar(kommentar)
			.build();

		Knoten knoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(10, 10), QuellSystem.DLM)
			.id(knotenID)
			.version(knotenVersion)
			.knotenAttribute(mock(KnotenAttribute.class))
			.build();
		when(netzService.getKnoten(knotenID)).thenReturn(knoten);

		when(netzService.getKnoten(saveKnotenCommand.getId())).thenReturn(knoten);
		when(netzService.berechneOrtslage(knoten)).thenReturn(KnotenOrtslage.AUSSERORTS);

		// act
		netzController.saveKnoten(authentication, saveKnotenCommand);

		// assert
		verify(netzService).aktualisiereKnoten(knotenID, knotenVersion, 123L, kommentar, null, null);
	}

	@Test
	void testeSaveKnoten_deny_keinRecht() {
		// arrange
		Long knotenID = 1L;

		SaveKnotenCommand saveKnotenCommand = SaveKnotenCommand.builder()
			.id(knotenID)
			.knotenVersion(1L)
			.build();

		// Überschreibe Test-Benutzer mit einem Ohne veränderungsrechte
		Benutzer benutzer = buildBenutzerMitRolle(Rolle.RADVIS_BETRACHTER);
		when(benutzerResolver.fromAuthentication(authentication)).thenReturn(benutzer);

		Knoten knoten = KnotenTestDataProvider.withDefaultValues().id(knotenID).version(1L).quelle(QuellSystem.DLM)
			.point(geometryFactory.createPoint(new CoordinateSequence2D(100, 100))).build();
		when(netzService.getKnoten(knotenID)).thenReturn(knoten);

		// act + assert
		assertThatThrownBy(() -> netzController.saveKnoten(authentication, saveKnotenCommand))
			.isInstanceOf(AccessDeniedException.class)
			.hasMessage(
				"Sie haben nicht die Berechtigung Kanten zu bearbeiten");
		Mockito.verify(netzService, never()).saveKnoten(any());
	}

	@Test
	void testeSaveKnoten_deny_knotenNichtInZustaendigkeitBereich() {
		// arrange
		Long knotenID = 1L;

		SaveKnotenCommand saveKnotenCommand = SaveKnotenCommand.builder()
			.id(knotenID)
			.knotenVersion(1L)
			.build();

		Knoten knoten = KnotenTestDataProvider.withDefaultValues().id(knotenID).version(1L).quelle(QuellSystem.DLM)
			.point(geometryFactory.createPoint(new CoordinateSequence2D(600, 100))).build();
		when(netzService.getKnoten(knotenID)).thenReturn(knoten);

		// act + assert
		assertThatThrownBy(() -> netzController.saveKnoten(authentication, saveKnotenCommand))
			.isInstanceOf(AccessDeniedException.class)
			.hasMessage(
				"Der Knoten liegt nicht in Ihrem Zuständigkeitsbereich");
		Mockito.verify(netzService, never()).saveKnoten(any());
	}

	@Test
	void testeGetKanteVerlauf() {
		// arrange
		Kante kante = mock(Kante.class);
		LineString kantenverlauf = mock(LineString.class);
		when(kante.getGeometry()).thenReturn(kantenverlauf);
		when(netzService.getKante(1L)).thenReturn(kante);

		Geometry nebenkantenVerlauf = geometryFactory.createLineString(new CoordinateSequence2D(101, 101, 105, 105));
		when(netzService.berechneNebenkante(kantenverlauf, KantenSeite.LINKS)).thenReturn(nebenkantenVerlauf);

		// act
		org.geojson.Feature feature = netzController.getKanteVerlauf(1L, KantenSeite.LINKS);

		// assert
		assertThat(feature.getGeometry()).isNotNull();
	}

	@Test
	void testeSaveFuehrungsformAttributGruppen() {
		// arrange
		Long kanteId = 10L;
		Kante kante = KanteTestDataProvider.withDefaultValues().id(kanteId)
			.isZweiseitig(true)
			.fahrtrichtungAttributGruppe(FahrtrichtungAttributGruppe.builder()
				.isZweiseitig(true)
				.build())
			.fuehrungsformAttributGruppe(FuehrungsformAttributGruppe.builder()
				.id(11L)
				.version(123L)
				.isZweiseitig(true)
				.fuehrungsformAttributeRechts(
					List.of(FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
						.breite(Laenge.of(42))
						.build()))
				.fuehrungsformAttributeLinks(
					List.of(FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
						.breite(Laenge.of(42))
						.build()))
				.build())
			.build();

		when(netzService.getKante(kanteId)).thenReturn(kante);
		when(netzService.getKanten(Set.of(kanteId))).thenReturn(List.of(kante));

		List<SaveFuehrungsformAttributeCommand> fuehrungsformenLinks = List.of(
			SaveFuehrungsformAttributeCommand.builder().breite(Laenge.of(123)).build());
		List<SaveFuehrungsformAttributeCommand> fuehrungsformenRechts = List.of(
			SaveFuehrungsformAttributeCommand.builder().breite(Laenge.of(234)).build());

		when(saveKanteCommandConverter.convertFuehrungsformAtttributeCommands(eq(fuehrungsformenLinks))).thenReturn(
			List.of(
				FuehrungsformAttribute.builder().breite(Laenge.of(123)).build()));
		when(saveKanteCommandConverter.convertFuehrungsformAtttributeCommands(eq(fuehrungsformenRechts))).thenReturn(
			List.of(
				FuehrungsformAttribute.builder().breite(Laenge.of(234)).build()));

		SaveFuehrungsformAttributGruppeCommand command = SaveFuehrungsformAttributGruppeCommand.builder()
			.fuehrungsformAttributeRechts(fuehrungsformenRechts)
			.fuehrungsformAttributeLinks(fuehrungsformenLinks)
			.kanteId(kanteId)
			.build();

		// act
		List<KanteEditView> result = netzController.saveFuehrungsformAttributGruppen(authentication, List.of(command));

		// assert
		verify(netzService, times(1)).aktualisiereFuehrungsformen(fuehrungsformAttributGruppeCaptor.capture());

		List<FuehrungsformAttributGruppe> arguments = fuehrungsformAttributGruppeCaptor.getValue();
		assertThat(arguments).hasSize(1);
		FuehrungsformAttributGruppe argument = arguments.get(0);
		assertThat(argument.getImmutableFuehrungsformAttributeLinks()).hasSize(1);
		assertThat(argument.getImmutableFuehrungsformAttributeLinks().get(0).getBreite()).contains(Laenge.of(123));
		assertThat(argument.getImmutableFuehrungsformAttributeRechts()).hasSize(1);
		assertThat(argument.getImmutableFuehrungsformAttributeRechts().get(0).getBreite()).contains(Laenge.of(234));

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getFuehrungsformAttributGruppe().getFuehrungsformAttributeRechts().get(0)
			.getBreite().getValue()).isEqualTo(42);
	}

	@Test
	void testeSaveZustaendigkeitAttributGruppen() {
		// arrange
		Long kanteId = 10L;
		final var organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().name("Bundesländ")
			.organisationsArt(OrganisationsArt.BUNDESLAND)
			.build();
		Kante kante = KanteTestDataProvider.withDefaultValues().id(kanteId)
			.zustaendigkeitAttributGruppe(ZustaendigkeitAttributGruppe.builder()
				.zustaendigkeitAttribute(List.of(
					ZustaendigkeitAttribute.builder().erhaltsZustaendiger(organisation).build()))
				.build())
			.build();

		when(netzService.getKante(kanteId)).thenReturn(kante);
		when(netzService.getKanten(Set.of(kanteId))).thenReturn(List.of(kante));

		SaveZustaendigkeitAttributeCommand saveAttributCommand = SaveZustaendigkeitAttributeCommand.builder()
			.build();
		SaveZustaendigkeitAttributGruppeCommand command = SaveZustaendigkeitAttributGruppeCommand.builder()
			.kanteId(kanteId)
			.zustaendigkeitAttribute(List.of(saveAttributCommand))
			.build();

		ZustaendigkeitAttribute zustaendigkeitAttribute = ZustaendigkeitAttribute.builder()
			.erhaltsZustaendiger(organisation).build();
		when(saveKanteCommandConverter.convertZustaendigkeitsAttributeCommand(saveAttributCommand)).thenReturn(
			zustaendigkeitAttribute);

		// act
		final var result = netzController.saveZustaendigkeitAttributGruppen(authentication, List.of(command));

		// assert
		verify(netzService, times(1)).aktualisiereZustaendigkeitsAttribute(anyMap());
		assertThat(result).hasSize(1);
		assertThat(
			result.get(0).getZustaendigkeitAttributGruppe().getZustaendigkeitAttribute().get(0).getErhaltsZustaendiger()
				.getName()).isEqualTo(organisation.getName());
		assertThat(
			result.get(0).getZustaendigkeitAttributGruppe().getZustaendigkeitAttribute().get(0).getErhaltsZustaendiger()
				.getOrganisationsArt()).isEqualTo(organisation.getOrganisationsArt());
	}

	@Test
	void testeSaveFahrtrichtungAttributGruppe() {
		// arrange
		Richtung fahrtrichtungLinks = Richtung.BEIDE_RICHTUNGEN;
		Richtung fahrtrichtungRechts = Richtung.UNBEKANNT;

		Long kanteId = 10L;
		Kante kante = KanteTestDataProvider.withDefaultValuesAndZweiseitig().id(kanteId)
			.fahrtrichtungAttributGruppe(FahrtrichtungAttributGruppe.builder()
				.isZweiseitig(true)
				.fahrtrichtungLinks(fahrtrichtungLinks)
				.fahrtrichtungRechts(fahrtrichtungRechts)
				.build())
			.build();

		when(netzService.getKante(kanteId)).thenReturn(kante);
		when(netzService.getKanten(Set.of(kanteId))).thenReturn(List.of(kante));

		final var command = SaveKanteFahrtrichtungCommand.builder()
			.kanteId(kanteId)
			.fahrtrichtungLinks(fahrtrichtungLinks)
			.fahrtrichtungRechts(fahrtrichtungRechts)
			.build();

		// act
		final var result = netzController.saveFahrtrichtungAttributGruppe(authentication, List.of(command));

		// assert
		assertThat(result).hasSize(1);
		assertThat(
			result.get(0).getFahrtrichtungAttributGruppe().getFahrtrichtungLinks()).isEqualTo(
				fahrtrichtungLinks);
		assertThat(
			result.get(0).getFahrtrichtungAttributGruppe().getFahrtrichtungRechts()).isEqualTo(
				fahrtrichtungRechts);
	}

	@Test
	void testeChangeSeitenbezug() {
		// arrange
		Long kanteId = 10L;
		Kante kante = KanteTestDataProvider.withDefaultValues().id(10L).isZweiseitig(false).build();
		when(netzService.loadKanteForModification(kanteId, 1L)).thenReturn(kante);
		when(netzService.saveKanten(any())).thenReturn(List.of(kante));

		Kante aktualisierteKante = KanteTestDataProvider.withDefaultValuesAndZweiseitig()
			.id(10L)
			.build();
		when(netzService.getKanten(Set.of(kanteId))).thenReturn(List.of(aktualisierteKante));

		// act
		List<ChangeSeitenbezugCommand> commands = new ArrayList<>();
		commands.add(ChangeSeitenbezugCommand.builder().id(kanteId).version(1L).zweiseitig(true).build());
		List<KanteEditView> result = netzController.changeSeitenbezug(authentication, commands);

		// assert
		verify(netzService, times(1)).aktualisiereKantenZweiseitig(any());
		assertThat(result).hasSize(1);
		assertThat(result.get(0).isZweiseitig()).isTrue();
	}

	@Test
	void testeChangeSeitenbezug_deny_nichtImZustaendigkeitsbereich() {
		// arrange
		Long kanteId = 10L;
		Kante kante = KanteTestDataProvider.withDefaultValues().id(10L)
			.isZweiseitig(false)
			.geometry(geometryFactory.createLineString(new CoordinateSequence2D(601, 601, 605, 605)))
			.build();
		when(netzService.loadKanteForModification(kanteId, 1L)).thenReturn(kante);
		when(netzService.saveKanten(any())).thenReturn(List.of(kante));
		when(netzService.getKanten(Set.of(kanteId))).thenReturn(List.of(kante));

		// act + assert
		List<ChangeSeitenbezugCommand> commands = new ArrayList<>();
		commands.add(ChangeSeitenbezugCommand.builder().id(kanteId).version(1L).zweiseitig(true).build());
		assertThatThrownBy(() -> netzController.changeSeitenbezug(authentication, commands))
			.isInstanceOf(AccessDeniedException.class)
			.hasMessageContaining("Die Kante liegt nicht in Ihrem Zuständigkeitsbereich");
	}

	@Test
	void testeChangeSeitenbezug_allow_nichtImZustaendigkeitsbereichAdmin() {
		// arrange
		Long kanteId = 10L;
		Kante kante = KanteTestDataProvider.withDefaultValues().id(10L)
			.isZweiseitig(false)
			.geometry(geometryFactory.createLineString(new CoordinateSequence2D(101, 101, 105, 105)))
			.build();
		when(netzService.loadKanteForModification(kanteId, 1L)).thenReturn(kante);
		when(netzService.saveKanten(any())).thenReturn(List.of(kante));
		when(netzService.getKanten(Set.of(kanteId))).thenReturn(List.of(kante));
		Benutzer benutzer = buildBenutzerMitRolle(Rolle.RADVIS_ADMINISTRATOR);
		when(benutzerResolver.fromAuthentication(authentication)).thenReturn(benutzer);

		List<ChangeSeitenbezugCommand> commands = new ArrayList<>();
		commands.add(ChangeSeitenbezugCommand.builder().id(kanteId).version(1L).zweiseitig(true).build());

		// act + assert
		assertDoesNotThrow(() -> netzController.changeSeitenbezug(authentication, commands));
	}

	private Benutzer buildBenutzerMitRolle(Rolle radnetzErfasserinRegierungsbezirk) {
		return BenutzerTestDataProvider.admin(organisation)
			.id(1L)
			.version(2L)
			.rollen(Set.of(radnetzErfasserinRegierungsbezirk))
			.build();
	}

	@Test
	void testeSaveGeschwindigkeit() {
		// arrange
		Long kanteId = 10L;

		final var geschwindigkeitAttribute = List.of(GeschwindigkeitAttribute.builder()
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.0, 0.65))
			.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.UEBER_100_KMH)
			.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung(
				Hoechstgeschwindigkeit.MAX_90_KMH)
			.ortslage(KantenOrtslage.INNERORTS)
			.build(),
			GeschwindigkeitAttribute.builder()
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.65, 1.0))
				.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_50_KMH)
				.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung(Hoechstgeschwindigkeit.MAX_40_KMH)
				.ortslage(KantenOrtslage.AUSSERORTS)
				.build());
		GeschwindigkeitAttributGruppe aktualisiertGeschwindigkeitAttributGruppe = GeschwindigkeitAttributGruppe
			.builder()
			.id(111L)
			.version(1L)
			.geschwindigkeitAttribute(geschwindigkeitAttribute)
			.build();
		Kante aktualisierteKante = KanteTestDataProvider.withDefaultValues().id(kanteId)
			.geschwindigkeitAttributGruppe(aktualisiertGeschwindigkeitAttributGruppe)
			.build();

		when(netzService.getKante(kanteId)).thenReturn(aktualisierteKante);
		when(netzService.getKanten(Set.of(kanteId))).thenReturn(List.of(aktualisierteKante));

		final var saveGeschwindigkeitAttributeCommands = geschwindigkeitAttribute.stream()
			.map(attribut -> SaveGeschwindigkeitAttributeCommand.builder()
				.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung(
					attribut.getAbweichendeHoechstgeschwindigkeitGegenStationierungsrichtung().get())
				.hoechstgeschwindigkeit(attribut.getHoechstgeschwindigkeit())
				.linearReferenzierterAbschnitt(attribut.getLinearReferenzierterAbschnitt())
				.ortslage(attribut.getOrtslage().get())
				.build())
			.collect(Collectors.toList());
		SaveGeschwindigkeitAttributGruppeCommand command = SaveGeschwindigkeitAttributGruppeCommand.builder()
			.geschwindigkeitAttribute(saveGeschwindigkeitAttributeCommands)
			.kanteId(kanteId)
			.build();
		List<SaveGeschwindigkeitAttributGruppeCommand> commands = List.of(command);

		// act
		List<KanteEditView> result = netzController.saveGeschwindigkeitAttributGruppen(authentication, commands);

		// assert
		List<GeschwindigkeitAttributeEditView> geschwindigkeitAttributeResult = result.get(0)
			.getGeschwindigkeitAttributGruppe().getGeschwindigkeitAttribute();

		assertThat(geschwindigkeitAttributeResult)
			.extracting(
				"ortslage",
				"hoechstgeschwindigkeit",
				"abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung",
				"linearReferenzierterAbschnitt")
			.containsExactlyInAnyOrder(
				new Tuple(KantenOrtslage.INNERORTS, Hoechstgeschwindigkeit.UEBER_100_KMH,
					Hoechstgeschwindigkeit.MAX_90_KMH, LinearReferenzierterAbschnitt.of(0.0, 0.65)),
				new Tuple(KantenOrtslage.AUSSERORTS, Hoechstgeschwindigkeit.MAX_50_KMH,
					Hoechstgeschwindigkeit.MAX_40_KMH, LinearReferenzierterAbschnitt.of(0.65, 1.0)));
	}

	@Test
	void testeSaveGeschwindigkeit_deny_nichtImZustaendigkeitsbereich() {
		// arrange
		Long kanteId = 10L;
		GeschwindigkeitAttributGruppe geschwindigkeitAttributGruppe = GeschwindigkeitAttributGruppe.builder()
			.id(111L)
			.version(1L)
			.geschwindigkeitAttribute(
				List.of(GeschwindigkeitsAttributeTestDataProvider.withGrundnetzDefaultwerte().build()))
			.build();
		Kante kante = KanteTestDataProvider.withDefaultValues().id(kanteId)
			.geschwindigkeitAttributGruppe(geschwindigkeitAttributGruppe)
			.geometry(geometryFactory.createLineString(new CoordinateSequence2D(601, 601, 605, 605)))
			.build();

		when(netzService.loadGeschwindigkeitAttributGruppeForModification(
			kante.getGeschwindigkeitAttributGruppe().getId(), kante.getGeschwindigkeitAttributGruppe().getVersion()))
				.thenReturn(geschwindigkeitAttributGruppe);
		when(netzService.saveKanten(any())).thenReturn(List.of(kante));
		when(netzService.getKanten(Set.of(kanteId))).thenReturn(List.of(kante));

		List<SaveGeschwindigkeitAttributGruppeCommand> commands = List.of(
			SaveGeschwindigkeitAttributGruppeCommand.builder()
				.gruppenID(kante.getGeschwindigkeitAttributGruppe().getId())
				.gruppenVersion(kante.getGeschwindigkeitAttributGruppe().getVersion())
				.kanteId(kante.getId())
				.geschwindigkeitAttribute(List.of(SaveGeschwindigkeitAttributeCommand.builder()
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.0, 1.0))
					.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.UEBER_100_KMH)
					.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung(Hoechstgeschwindigkeit.UEBER_100_KMH)
					.ortslage(KantenOrtslage.INNERORTS)
					.build()))
				.build());

		// act + assert
		assertThatThrownBy(() -> netzController.saveGeschwindigkeitAttributGruppen(authentication, commands))
			.isInstanceOf(AccessDeniedException.class)
			.hasMessageContaining("Die Kante liegt nicht in Ihrem Zuständigkeitsbereich");
	}

	@Test
	void testeSaveGeschwindigkeit_deny_KeinRecht() {
		// arrange
		Long kanteId = 10L;
		GeschwindigkeitAttributGruppe geschwindigkeitAttributGruppe = GeschwindigkeitAttributGruppe.builder()
			.id(111L)
			.version(1L)
			.geschwindigkeitAttribute(
				List.of(GeschwindigkeitsAttributeTestDataProvider.withGrundnetzDefaultwerte().build()))
			.build();
		Kante kante = KanteTestDataProvider.withDefaultValues().id(kanteId)
			.geschwindigkeitAttributGruppe(geschwindigkeitAttributGruppe)
			.build();

		when(netzService.loadGeschwindigkeitAttributGruppeForModification(
			kante.getGeschwindigkeitAttributGruppe().getId(), kante.getGeschwindigkeitAttributGruppe().getVersion()))
				.thenReturn(geschwindigkeitAttributGruppe);
		when(netzService.saveKanten(any())).thenReturn(List.of(kante));
		when(netzService.getKanten(Set.of(kanteId))).thenReturn(List.of(kante));

		List<SaveGeschwindigkeitAttributGruppeCommand> commands = List.of(
			SaveGeschwindigkeitAttributGruppeCommand.builder()
				.gruppenID(kante.getGeschwindigkeitAttributGruppe().getId())
				.gruppenVersion(kante.getGeschwindigkeitAttributGruppe().getVersion())
				.kanteId(kante.getId())
				.geschwindigkeitAttribute(List.of(SaveGeschwindigkeitAttributeCommand.builder()
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.0, 1.0))
					.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.UEBER_100_KMH)
					.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung(Hoechstgeschwindigkeit.UEBER_100_KMH)
					.ortslage(KantenOrtslage.INNERORTS)
					.build()))
				.build());

		Benutzer benutzer = buildBenutzerMitRolle(Rolle.RADVIS_BETRACHTER);
		when(benutzerResolver.fromAuthentication(authentication)).thenReturn(benutzer);

		// act + assert
		assertThatThrownBy(() -> netzController.saveGeschwindigkeitAttributGruppen(authentication, commands))
			.isInstanceOf(AccessDeniedException.class)
			.hasMessageContaining("Sie haben nicht die Berechtigung Kanten zu bearbeiten");
	}

	@Test
	void testeSaveKanteVerlauf_deny_keinRecht() {
		// arrange
		Kante kante = KanteTestDataProvider.withDefaultValuesAndQuelle(QuellSystem.RadVis).id(10L).version(1L).build();

		when(netzService.loadKanteForModification(kante.getId(), kante.getVersion())).thenReturn(kante);
		when(netzService.getKanten(Set.of(kante.getId()))).thenReturn(List.of(kante));

		LineString newGeometry = geometryFactory.createLineString(new CoordinateSequence2D(1, 1, 5, 5));

		SaveKanteVerlaufCommand command = SaveKanteVerlaufCommand.builder()
			.id(kante.getId())
			.kantenVersion(kante.getVersion())
			.geometry(newGeometry)
			.build();

		Benutzer benutzer = buildBenutzerMitRolle(Rolle.RADVIS_BETRACHTER);
		when(benutzerResolver.fromAuthentication(authentication)).thenReturn(benutzer);

		// act + assert
		assertThatThrownBy(() -> netzController.saveKanteVerlauf(authentication, List.of(command)))
			.isInstanceOf(AccessDeniedException.class)
			.hasMessageContaining("Sie haben nicht die Berechtigung Kanten zu bearbeiten");
	}

	@Test
	void testeSaveKanteVerlauf_deny_nichtImZustaendigkeitsbereich() {
		// arrange
		Kante kante = KanteTestDataProvider.withDefaultValuesAndQuelle(QuellSystem.RadVis).id(10L).version(1L)
			.isZweiseitig(false)
			.geometry(geometryFactory.createLineString(new CoordinateSequence2D(601, 601, 605, 605)))
			.build();

		when(netzService.loadKanteForModification(kante.getId(), kante.getVersion())).thenReturn(kante);
		when(netzService.getKanten(Set.of(kante.getId()))).thenReturn(List.of(kante));

		LineString newGeometry = geometryFactory.createLineString(new CoordinateSequence2D(1, 1, 5, 5));

		SaveKanteVerlaufCommand command = SaveKanteVerlaufCommand.builder()
			.id(kante.getId())
			.kantenVersion(kante.getVersion())
			.geometry(newGeometry)
			.build();

		// act + assert
		assertThatThrownBy(() -> netzController.saveKanteVerlauf(authentication, List.of(command)))
			.isInstanceOf(AccessDeniedException.class)
			.hasMessageContaining("Die Kante liegt nicht in Ihrem Zuständigkeitsbereich");
	}

	@Test
	void testeSaveKanteVerlauf_allow_nichtImZustaendigkeitsbereichAdmin() {
		// arrange
		Kante kante = KanteTestDataProvider.withDefaultValuesAndQuelle(QuellSystem.RadVis).id(10L).version(1L)
			.isZweiseitig(false)
			.geometry(geometryFactory.createLineString(new CoordinateSequence2D(101, 101, 105, 105)))
			.build();

		when(netzService.loadKanteForModification(kante.getId(), kante.getVersion())).thenReturn(kante);
		when(netzService.getKanten(Set.of(kante.getId()))).thenReturn(List.of(kante));

		LineString newGeometry = geometryFactory.createLineString(new CoordinateSequence2D(101, 101, 105, 105));

		SaveKanteVerlaufCommand command = SaveKanteVerlaufCommand.builder()
			.id(kante.getId())
			.kantenVersion(kante.getVersion())
			.geometry(newGeometry)
			.build();

		Benutzer benutzer = buildBenutzerMitRolle(Rolle.RADVIS_ADMINISTRATOR);
		when(benutzerResolver.fromAuthentication(authentication)).thenReturn(benutzer);

		// act + assert
		assertDoesNotThrow(() -> netzController.saveKanteVerlauf(authentication, List.of(command)));
	}

	@Nested
	@AllDisabled(FeatureTogglz.class)
	class WithKanteLoeschenToggleEnabled {
		@BeforeEach
		void setup(TestFeatureManager featureManager) {
			featureManager.enable(FeatureTogglz.KANTE_LOESCHEN_ENDPUNKT);
		}

		@Test
		void testeDeleteEigeneRadvisKante() {
			// arrange
			long kanteId = 10L;
			Kante kante = KanteTestDataProvider.withDefaultValuesAndQuelle(QuellSystem.RadVis).id(kanteId).version(1L)
				.build();
			when(netzService.getKante(kanteId)).thenReturn(kante);
			when(netzService.wurdeAngelegtVon(kante, benutzer)).thenReturn(true);

			// act
			netzController.deleteRadVISKanteById(kanteId, authentication);

			// assert
			verify(netzService, times(2)).getKante(kanteId); // Guard & Controller holen die Entity unabhängig
			verify(netzService, times(1)).deleteKante(kanteCaptor.capture());
			verify(netzService, times(1)).wurdeAngelegtVon(kanteCaptor.capture(), benutzerCaptor.capture());
			verifyNoMoreInteractions(netzService);
			assertThat(kanteCaptor.getAllValues()).contains(kante, kante);
			assertThat(benutzerCaptor.getValue()).isEqualTo(benutzer);
		}

		@Test
		void testeDeleteFremdeRadvisKante() {
			// arrange
			long kanteId = 10L;
			Kante kante = KanteTestDataProvider.withDefaultValuesAndQuelle(QuellSystem.RadVis).id(kanteId).version(1L)
				.build();
			when(netzService.getKante(kanteId)).thenReturn(kante);
			when(netzService.wurdeAngelegtVon(kante, benutzer)).thenReturn(false);

			// act
			assertThatThrownBy(() -> netzController.deleteRadVISKanteById(kanteId, authentication)).isInstanceOf(
				AccessDeniedException.class);
			// assert
			verify(netzService, times(1)).getKante(kanteId);
			verify(netzService, times(1)).wurdeAngelegtVon(kanteCaptor.capture(), benutzerCaptor.capture());

			verifyNoMoreInteractions(netzService);
			assertThat(benutzerCaptor.getValue()).isEqualTo(benutzer);
		}

		@Test
		void testeDeleteDlmKante() {
			// arrange
			long kanteId = 10L;
			Kante kante = KanteTestDataProvider.withDefaultValuesAndQuelle(QuellSystem.DLM).id(kanteId).version(1L)
				.build();
			when(netzService.getKante(kanteId)).thenReturn(kante);

			// act
			assertThatThrownBy(() -> netzController.deleteRadVISKanteById(kanteId, authentication)).isInstanceOf(
				AccessDeniedException.class);

			// assert
			verify(netzService, times(1)).getKante(kanteId);
			verifyNoMoreInteractions(netzService);
		}
	}

	@Nested
	@AllDisabled(FeatureTogglz.class)
	class WithAllFeatureTogglesDisabled {
		@Test
		void testeDeleteKante() {
			assertThrows(AccessDeniedException.class, () -> netzController.deleteRadVISKanteById(10L, authentication));
		}
	}
}
