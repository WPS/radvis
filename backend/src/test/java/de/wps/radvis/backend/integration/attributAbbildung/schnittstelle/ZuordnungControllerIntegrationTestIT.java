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

package de.wps.radvis.backend.integration.attributAbbildung.schnittstelle;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.json.JsonMapper;

import de.wps.radvis.backend.application.JacksonConfiguration;
import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.benutzer.domain.TechnischerBenutzerConfigurationProperties;
import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.integration.attributAbbildung.IntegrationAttributAbbildungConfiguration;
import de.wps.radvis.backend.integration.attributAbbildung.domain.AttributProjektionsService;
import de.wps.radvis.backend.integration.attributAbbildung.domain.AttributeAnreicherungsService;
import de.wps.radvis.backend.integration.attributAbbildung.domain.AttributeProjektionsProtokollService;
import de.wps.radvis.backend.integration.attributAbbildung.domain.KantenAttributeMergeService;
import de.wps.radvis.backend.integration.attributAbbildung.domain.KantenMappingRepository;
import de.wps.radvis.backend.integration.attributAbbildung.domain.KantenMappingService;
import de.wps.radvis.backend.integration.attributAbbildung.domain.entity.KantenMapping;
import de.wps.radvis.backend.integration.attributAbbildung.schnittstelle.command.ChangeZuordnungCommand;
import de.wps.radvis.backend.integration.attributAbbildung.schnittstelle.command.LoescheZuordnungenCommand;
import de.wps.radvis.backend.integration.radnetz.IntegrationRadNetzConfiguration;
import de.wps.radvis.backend.integration.radnetz.domain.RadNetzNetzbildungProtokollService;
import de.wps.radvis.backend.integration.radwegedb.domain.RadwegeDBNetzbildungService;
import de.wps.radvis.backend.kommentar.KommentarConfiguration;
import de.wps.radvis.backend.konsistenz.pruefung.KonsistenzregelPruefungsConfiguration;
import de.wps.radvis.backend.konsistenz.regeln.KonsistenzregelnConfiguration;
import de.wps.radvis.backend.konsistenz.regeln.domain.KonsistenzregelnConfigurationProperties;
import de.wps.radvis.backend.netz.NetzConfiguration;
import de.wps.radvis.backend.netz.domain.entity.FahrtrichtungAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttribute;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.KantenAttribute;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.provider.GeschwindigkeitsAttributeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.ZustaendigkeitAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.valueObject.BelagArt;
import de.wps.radvis.backend.netz.domain.valueObject.Beleuchtung;
import de.wps.radvis.backend.netz.domain.valueObject.DlmId;
import de.wps.radvis.backend.netz.domain.valueObject.Hoechstgeschwindigkeit;
import de.wps.radvis.backend.netz.domain.valueObject.IstStandard;
import de.wps.radvis.backend.netz.domain.valueObject.KantenOrtslage;
import de.wps.radvis.backend.netz.domain.valueObject.Kommentar;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.Richtung;
import de.wps.radvis.backend.netz.domain.valueObject.Status;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenName;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenNummer;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenquerschnittRASt06;
import de.wps.radvis.backend.netz.domain.valueObject.Umfeld;
import de.wps.radvis.backend.netz.domain.valueObject.VerkehrStaerke;
import de.wps.radvis.backend.netz.domain.valueObject.WegeNiveau;
import de.wps.radvis.backend.netzfehler.NetzfehlerConfiguration;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.GebietskoerperschaftRepository;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import de.wps.radvis.backend.organisation.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.quellimport.common.ImportsCommonConfiguration;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Tag("group1")
@ContextConfiguration(classes = {
	NetzConfiguration.class,
	OrganisationConfiguration.class,
	BenutzerConfiguration.class,
	GeoConverterConfiguration.class,
	IntegrationAttributAbbildungConfiguration.class,
	ImportsCommonConfiguration.class,
	CommonConfiguration.class,
	IntegrationRadNetzConfiguration.class,
	ZuordnungControllerIntegrationTestIT.TestConfiguration.class,
	KommentarConfiguration.class,
	NetzfehlerConfiguration.class,
	KonsistenzregelPruefungsConfiguration.class,
	KonsistenzregelnConfiguration.class
})
@EnableConfigurationProperties(value = {
	CommonConfigurationProperties.class,
	FeatureToggleProperties.class,
	TechnischerBenutzerConfigurationProperties.class,
	PostgisConfigurationProperties.class,
	KonsistenzregelnConfigurationProperties.class,
	OrganisationConfigurationProperties.class
})
@ActiveProfiles(profiles = "test")
class ZuordnungControllerIntegrationTestIT extends DBIntegrationTestIT {

	@Configuration
	public static class TestConfiguration {
		@MockBean
		private AttributeProjektionsProtokollService attributeProjektionsProtokollService;
		@MockBean
		private RadNetzNetzbildungProtokollService radNetzNetzbildungProtokollService;
		@MockBean
		private RadwegeDBNetzbildungService radwegeDBNetzbildungService;
		@MockBean
		private AttributProjektionsService attributProjektionsService;
		@MockBean
		private AttributeAnreicherungsService attributeAnreicherungsService;

		@Autowired
		private NetzService netzService;
		@Autowired
		private KantenAttributeMergeService kantenAttributeMergeService;
		@Autowired
		private KantenMappingService kantenMappingService;

		@Bean
		public ZuordnungController zuordnungController() {
			return new ZuordnungController(netzService, kantenAttributeMergeService, kantenMappingService);
		}
	}

	@Autowired
	private ZuordnungController zuordnungController;

	@Autowired
	private KantenRepository kantenRepository;

	@Autowired
	private KantenMappingRepository kantenMappingRepository;

	@Autowired
	private GebietskoerperschaftRepository gebietskoerperschaftRepository;

	private final JsonMapper mapper = new JsonMapper();

	@PersistenceContext
	private EntityManager entityManager;

	@BeforeEach
	public void setUp() {
		mapper.registerModule(new JacksonConfiguration().customJacksonGeometryModule());
	}

	@Test
	void testChangeZuordnung_AttributeWerdenUebertragen()
		throws Exception {
		// Arrange

		Verwaltungseinheit eineOrganisation = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().name("dolle Organisation")
				.organisationsArt(OrganisationsArt.BUNDESLAND)
				.build());

		entityManager.flush();
		entityManager.clear();

		Knoten vonKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(1, 10), QuellSystem.RadNETZ)
			.build();
		Knoten nachKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(2, 20), QuellSystem.RadNETZ)
			.build();

		LineString lineString = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
			.createLineString(new Coordinate[] { vonKnoten.getKoordinate(), nachKnoten.getKoordinate() });

		KantenAttribute kantenAttribute = KantenAttribute.builder()
			.dtvFussverkehr(VerkehrStaerke.of(156))
			.dtvPkw(VerkehrStaerke.of(199))
			.dtvRadverkehr(VerkehrStaerke.of(299))
			.kommentar(Kommentar.of("Das ist ein Kommentar"))
			.laengeManuellErfasst(Laenge.of(89.12))
			.strassenName(StrassenName.of("HHWJ"))
			.strassenNummer(StrassenNummer.of("B126"))
			.sv(VerkehrStaerke.of(123))
			.wegeNiveau(WegeNiveau.FAHRBAHN)
			.beleuchtung(Beleuchtung.VORHANDEN)
			.umfeld(Umfeld.GEWERBEGEBIET)
			.strassenquerschnittRASt06(StrassenquerschnittRASt06.ANBAUFREIE_STRASSE)
			.status(Status.defaultWert())
			.build();
		Set<Netzklasse> netzklassen = Set.of(Netzklasse.RADNETZ_ALLTAG);
		Set<IstStandard> istStandards = Set.of(IstStandard.RADSCHNELLVERBINDUNG);

		final GeschwindigkeitAttribute geschwindigkeitsAttribute = GeschwindigkeitAttribute
			.builder()
			.ortslage(KantenOrtslage.INNERORTS)
			.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_30_KMH)
			.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung(Hoechstgeschwindigkeit.MAX_20_KMH).build();
		final FahrtrichtungAttributGruppe fahrtrichtungAttributGruppe = FahrtrichtungAttributGruppe.builder()
			.build();
		fahrtrichtungAttributGruppe.changeSeitenbezug(true);
		fahrtrichtungAttributGruppe.setRichtung(Richtung.BEIDE_RICHTUNGEN, Richtung.IN_RICHTUNG);
		List<ZustaendigkeitAttribute> zustaendigkeitsattribute = List.of(
			ZustaendigkeitAttribute.builder().unterhaltsZustaendiger(eineOrganisation)
				.build());
		List<FuehrungsformAttribute> fuehrunngsformAttribute = List.of(
			FuehrungsformAttribute.builder()
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 1))
				.belagArt(BelagArt.ASPHALT)
				.build());
		FuehrungsformAttributGruppe fuehrungsformAttributGruppe = FuehrungsformAttributGruppe
			.builder().isZweiseitig(true).build();
		fuehrungsformAttributGruppe.replaceFuehrungsformAttribute(fuehrunngsformAttribute);

		Kante radNetzKante = KanteTestDataProvider.withDefaultValuesAndQuelle(QuellSystem.RadNETZ)
			.ursprungsfeatureTechnischeID("123")
			.vonKnoten(vonKnoten)
			.nachKnoten(nachKnoten)
			.geometry(lineString)
			.isZweiseitig(true)
			.kantenAttributGruppe(new KantenAttributGruppe(kantenAttribute, netzklassen, istStandards))
			.geschwindigkeitAttributGruppe(
				GeschwindigkeitAttributGruppe.builder().geschwindigkeitAttribute(List.of(geschwindigkeitsAttribute))
					.build())
			.fahrtrichtungAttributGruppe(fahrtrichtungAttributGruppe)
			.zustaendigkeitAttributGruppe(ZustaendigkeitAttributGruppeTestDataProvider.withLeereGrundnetzAttribute()
				.zustaendigkeitAttribute(zustaendigkeitsattribute)
				.build())
			.fuehrungsformAttributGruppe(fuehrungsformAttributGruppe)
			.build();

		Knoten vonKnotenDlm = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(1, 10), QuellSystem.DLM)
			.build();
		Knoten nachKnotenDlm = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(2, 20), QuellSystem.DLM)
			.build();

		Kante kanteDlm = erstelleDlmKante(lineString, vonKnotenDlm, nachKnotenDlm);

		Long savedRadNETZKanteId = kantenRepository.save(radNetzKante).getId();
		Long savedKanteDlmId = kantenRepository.save(kanteDlm).getId();

		entityManager.flush();
		entityManager.clear();

		// act
		String json = "{"
			+ " \"radnetzKanteId\":" + savedRadNETZKanteId + ","
			+ " \"dlmnetzKanteIds\" : [ " + savedKanteDlmId + " ]"
			+ "}";

		ChangeZuordnungCommand changeZuordnungCommand = mapper.readValue(json,
			ChangeZuordnungCommand.class);
		zuordnungController.changeZuordnung(changeZuordnungCommand);

		entityManager.flush();
		entityManager.clear();

		// assert

		Kante kanteDlmGespeichert = kantenRepository.findById(savedKanteDlmId).get();

		assertThat(kanteDlmGespeichert.isZweiseitig()).isTrue();
		KantenAttribute gemappteKantenAttribute = kanteDlmGespeichert.getKantenAttributGruppe().getKantenAttribute();
		assertThat(gemappteKantenAttribute.getWegeNiveau())
			.isEqualTo(
				kantenAttribute.getWegeNiveau());
		assertThat(gemappteKantenAttribute).isEqualTo(kantenAttribute);
		assertThat(kanteDlmGespeichert.getKantenAttributGruppe().getNetzklassen()).containsExactlyInAnyOrderElementsOf(
			netzklassen);
		assertThat(kanteDlmGespeichert.getKantenAttributGruppe().getIstStandards()).containsExactlyInAnyOrderElementsOf(
			istStandards);

		GeschwindigkeitAttribute gemappteGeschwindigkeitAttribute = kanteDlmGespeichert
			.getGeschwindigkeitAttributGruppe().getImmutableGeschwindigkeitAttribute().get(0);
		assertThat(gemappteGeschwindigkeitAttribute)
			.extracting("ortslage", "hoechstgeschwindigkeit",
				"abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung")
			.containsExactly(gemappteGeschwindigkeitAttribute.getOrtslage(),
				geschwindigkeitsAttribute.getHoechstgeschwindigkeit(),
				geschwindigkeitsAttribute.getAbweichendeHoechstgeschwindigkeitGegenStationierungsrichtung());

		FahrtrichtungAttributGruppe gemappteFahrtrichtungAttributGruppe = kanteDlmGespeichert
			.getFahrtrichtungAttributGruppe();
		assertThat(gemappteFahrtrichtungAttributGruppe.isZweiseitig()).isTrue();
		assertThat(gemappteFahrtrichtungAttributGruppe.getFahrtrichtungLinks())
			.isEqualTo(fahrtrichtungAttributGruppe.getFahrtrichtungLinks());
		assertThat(gemappteFahrtrichtungAttributGruppe.getFahrtrichtungRechts())
			.isEqualTo(fahrtrichtungAttributGruppe.getFahrtrichtungRechts());

		List<ZustaendigkeitAttribute> gemappteZustaendigkeitAttribute = kanteDlmGespeichert
			.getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute();
		assertThat(gemappteZustaendigkeitAttribute).containsExactlyElementsOf(zustaendigkeitsattribute);

		FuehrungsformAttributGruppe gemappteFuehrungsformAttributGruppe = kanteDlmGespeichert
			.getFuehrungsformAttributGruppe();
		assertThat(gemappteFuehrungsformAttributGruppe.isZweiseitig()).isTrue();
		assertThat(gemappteFuehrungsformAttributGruppe.getImmutableFuehrungsformAttributeLinks())
			.containsExactlyElementsOf(fuehrunngsformAttribute);

		// Speicherung/Aktualisierung des Mappings prüfen
		Iterable<KantenMapping> kantenMappings = kantenMappingRepository.findAll();
		assertThat(kantenMappings).hasSize(1);
		KantenMapping kantenMapping = kantenMappings.iterator().next();
		assertThat(kantenMapping.getAbgebildeteKanten()).hasSize(1);
		assertThat(kantenMapping.getAbgebildeteKanten().get(0).getKanteId()).isEqualTo(savedRadNETZKanteId);
		assertThat(kantenMapping.getGrundnetzKantenId()).isEqualTo(savedKanteDlmId);
		assertThat(kantenMapping.getQuellsystem()).isEqualTo(radNetzKante.getQuelle());
	}

	@Test
	void getZuordnung() throws JsonMappingException, JsonProcessingException {
		// Arrange

		Knoten vonKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(1, 10), QuellSystem.RadNETZ)
			.build();
		Knoten nachKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(2, 20), QuellSystem.RadNETZ)
			.build();

		LineString lineString = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
			.createLineString(new Coordinate[] { vonKnoten.getKoordinate(), nachKnoten.getKoordinate() });

		Kante radNetzKante = KanteTestDataProvider.withDefaultValuesAndQuelle(QuellSystem.RadNETZ)
			.ursprungsfeatureTechnischeID("123")
			.vonKnoten(vonKnoten)
			.nachKnoten(nachKnoten)
			.geometry(lineString)
			.isZweiseitig(true)
			.fahrtrichtungAttributGruppe(FahrtrichtungAttributGruppe.builder().isZweiseitig(true).build())
			.fuehrungsformAttributGruppe(FuehrungsformAttributGruppe.builder().isZweiseitig(true).build())
			.kantenAttributGruppe(new KantenAttributGruppe(KantenAttribute.builder().build(),
				Set.of(), Set.of()))
			.geschwindigkeitAttributGruppe(
				GeschwindigkeitsAttributeTestDataProvider.gruppeWithGrundnetzDefaultwerte().build())
			.zustaendigkeitAttributGruppe(ZustaendigkeitAttributGruppeTestDataProvider.withLeereGrundnetzAttribute()
				.zustaendigkeitAttribute(
					Collections.singletonList(ZustaendigkeitAttribute.builder().build()))
				.build())
			.build();

		Knoten vonKnotenDlm = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(1, 10), QuellSystem.DLM)
			.build();
		Knoten nachKnotenDlm = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(2, 20), QuellSystem.DLM)
			.build();

		Kante kanteDlm = erstelleDlmKante(lineString, vonKnotenDlm, nachKnotenDlm);

		Kante kanteDlm2 = erstelleDlmKante(lineString, vonKnotenDlm, nachKnotenDlm);

		Long savedRadNETZKanteId = kantenRepository.save(radNetzKante).getId();
		Long savedKanteDlmId = kantenRepository.save(kanteDlm).getId();
		Long savedKanteDlmId2 = kantenRepository.save(kanteDlm2).getId();

		entityManager.flush();
		entityManager.clear();

		String json = "{"
			+ " \"radnetzKanteId\":" + savedRadNETZKanteId + ","
			+ " \"dlmnetzKanteIds\" : [ " + savedKanteDlmId + "," + savedKanteDlmId2 + " ]"
			+ "}";

		ChangeZuordnungCommand changeZuordnungCommand = mapper.readValue(json,
			ChangeZuordnungCommand.class);
		zuordnungController.changeZuordnung(changeZuordnungCommand);

		entityManager.flush();
		entityManager.clear();

		// act
		List<Long> dlmKanteIds = zuordnungController.getZuordnungRadNETZZuDLM(savedRadNETZKanteId);
		List<Long> radNETZKanteIds = zuordnungController.getZuordnungDLMZuRadNETZ(savedKanteDlmId);

		// assert
		assertThat(dlmKanteIds).containsExactlyInAnyOrder(savedKanteDlmId, savedKanteDlmId2);
		assertThat(radNETZKanteIds).containsExactlyInAnyOrder(savedRadNETZKanteId);
	}

	@Test
	void entferneZuordnung() throws JsonMappingException, JsonProcessingException {
		// Arrange

		Knoten vonKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(1, 10), QuellSystem.RadNETZ)
			.build();
		Knoten nachKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(2, 20), QuellSystem.RadNETZ)
			.build();

		LineString lineString = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
			.createLineString(new Coordinate[] { vonKnoten.getKoordinate(), nachKnoten.getKoordinate() });

		Kante radNetzKante = KanteTestDataProvider.withDefaultValuesAndQuelle(QuellSystem.RadNETZ)
			.ursprungsfeatureTechnischeID("123")
			.vonKnoten(vonKnoten)
			.nachKnoten(nachKnoten)
			.geometry(lineString)
			.isZweiseitig(true)
			.fahrtrichtungAttributGruppe(FahrtrichtungAttributGruppe.builder().isZweiseitig(true).build())
			.fuehrungsformAttributGruppe(FuehrungsformAttributGruppe
				.builder().isZweiseitig(true).build())
			.kantenAttributGruppe(new KantenAttributGruppe(KantenAttribute.builder().build(),
				Set.of(), Set.of()))
			.geschwindigkeitAttributGruppe(
				GeschwindigkeitsAttributeTestDataProvider.gruppeWithGrundnetzDefaultwerte().build())
			.zustaendigkeitAttributGruppe(ZustaendigkeitAttributGruppeTestDataProvider.withLeereGrundnetzAttribute()
				.zustaendigkeitAttribute(
					Collections.singletonList(ZustaendigkeitAttribute.builder().build()))
				.build())
			.build();

		Knoten vonKnotenDlm = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(1, 10), QuellSystem.DLM)
			.build();
		Knoten nachKnotenDlm = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(2, 20), QuellSystem.DLM)
			.build();

		Kante kanteDlm = erstelleDlmKante(lineString, vonKnotenDlm, nachKnotenDlm);
		Kante kanteDlm2 = erstelleDlmKante(lineString, vonKnotenDlm, nachKnotenDlm);
		Kante kanteDlm3 = erstelleDlmKante(lineString, vonKnotenDlm, nachKnotenDlm);

		Long savedRadNETZKanteId = kantenRepository.save(radNetzKante).getId();
		Long savedKanteDlmId = kantenRepository.save(kanteDlm).getId();
		Long savedKanteDlmId2 = kantenRepository.save(kanteDlm2).getId();
		Long savedKanteDlmId3 = kantenRepository.save(kanteDlm3).getId();

		entityManager.flush();
		entityManager.clear();

		String json = "{"
			+ " \"radnetzKanteId\":" + savedRadNETZKanteId + ","
			+ " \"dlmnetzKanteIds\" : [ " + savedKanteDlmId + "," + savedKanteDlmId2 + "," + savedKanteDlmId3 + " ]"
			+ "}";

		ChangeZuordnungCommand changeZuordnungCommand = mapper.readValue(json,
			ChangeZuordnungCommand.class);
		zuordnungController.changeZuordnung(changeZuordnungCommand);

		entityManager.flush();
		entityManager.clear();

		assertThat(zuordnungController.getZuordnungRadNETZZuDLM(savedRadNETZKanteId))
			.containsExactlyInAnyOrder(savedKanteDlmId, savedKanteDlmId2, savedKanteDlmId3);

		// act
		json = "{ \"dlmnetzKanteId\" :  " + savedKanteDlmId + " }";
		LoescheZuordnungenCommand loescheZuordnungCommand = mapper.readValue(json,
			LoescheZuordnungenCommand.class);
		zuordnungController.loescheZuordnung(loescheZuordnungCommand);

		json = "{ \"dlmnetzKanteId\" : " + savedKanteDlmId2 + " }";
		loescheZuordnungCommand = mapper.readValue(json,
			LoescheZuordnungenCommand.class);
		zuordnungController.loescheZuordnung(loescheZuordnungCommand);

		// assert
		List<Long> dlmKanteIds = zuordnungController.getZuordnungRadNETZZuDLM(savedRadNETZKanteId);
		assertThat(dlmKanteIds).containsExactly(savedKanteDlmId3);
	}

	private Kante erstelleDlmKante(LineString lineString, Knoten vonKnotenDlm, Knoten nachKnotenDlm) {
		return KanteTestDataProvider.withDefaultValues()
			.vonKnoten(vonKnotenDlm).nachKnoten(nachKnotenDlm)
			.dlmId(DlmId.of("dlmId"))
			.geometry(lineString).quelle(QuellSystem.DLM)
			.kantenAttributGruppe(new KantenAttributGruppe(
				KantenAttribute.builder().build(), new HashSet<>(), new HashSet<>()))
			.geschwindigkeitAttributGruppe(GeschwindigkeitAttributGruppe.builder().build())
			.fahrtrichtungAttributGruppe(FahrtrichtungAttributGruppe.builder().build())
			.zustaendigkeitAttributGruppe(ZustaendigkeitAttributGruppeTestDataProvider.withLeereGrundnetzAttribute()
				.build())
			.fuehrungsformAttributGruppe(FuehrungsformAttributGruppe.builder().build())
			.isZweiseitig(false)
			.build();
	}

	@Test
	void entferneZuordnung_vonDlmWerdenAufUrsprungZurueckgesetzt()
		throws JsonMappingException, JsonProcessingException {
		// Arrange

		Knoten vonKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(1, 10), QuellSystem.RadNETZ)
			.build();
		Knoten nachKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(2, 20), QuellSystem.RadNETZ)
			.build();

		LineString lineString = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
			.createLineString(new Coordinate[] { vonKnoten.getKoordinate(), nachKnoten.getKoordinate() });

		Set<Netzklasse> netzklassen = Set.of(Netzklasse.RADNETZ_ALLTAG);
		Set<IstStandard> istStandards = Set.of(IstStandard.RADSCHNELLVERBINDUNG);

		KantenAttribute kantenAttribute = KantenAttribute.builder()
			.dtvFussverkehr(VerkehrStaerke.of(156))
			.dtvPkw(VerkehrStaerke.of(199))
			.dtvRadverkehr(VerkehrStaerke.of(299))
			.kommentar(Kommentar.of("Das ist ein Kommentar"))
			.laengeManuellErfasst(Laenge.of(89.12))
			.strassenNummer(StrassenNummer.of("B126"))
			.sv(VerkehrStaerke.of(123))
			.wegeNiveau(WegeNiveau.FAHRBAHN)
			.beleuchtung(Beleuchtung.VORHANDEN)
			.umfeld(Umfeld.GEWERBEGEBIET)
			.strassenquerschnittRASt06(StrassenquerschnittRASt06.ANBAUFREIE_STRASSE)
			.status(Status.defaultWert())
			.build();

		GeschwindigkeitAttribute geschwindigkeitsAttribute = GeschwindigkeitAttribute
			.builder()
			.ortslage(KantenOrtslage.INNERORTS)
			.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_30_KMH)
			.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung(Hoechstgeschwindigkeit.MAX_20_KMH).build();

		Kante radNetzKante = KanteTestDataProvider.withDefaultValuesAndQuelle(QuellSystem.RadNETZ)
			.ursprungsfeatureTechnischeID("123")
			.vonKnoten(vonKnoten)
			.nachKnoten(nachKnoten)
			.geometry(lineString)
			.isZweiseitig(true)
			.fahrtrichtungAttributGruppe(FahrtrichtungAttributGruppe.builder().isZweiseitig(true).build())
			.fuehrungsformAttributGruppe(FuehrungsformAttributGruppe
				.builder().isZweiseitig(true).build())
			.kantenAttributGruppe(new KantenAttributGruppe(kantenAttribute,
				netzklassen, istStandards))
			.geschwindigkeitAttributGruppe(
				GeschwindigkeitAttributGruppe.builder().geschwindigkeitAttribute(List.of(geschwindigkeitsAttribute))
					.build())
			.zustaendigkeitAttributGruppe(ZustaendigkeitAttributGruppeTestDataProvider.withLeereGrundnetzAttribute()
				.zustaendigkeitAttribute(
					Collections.singletonList(ZustaendigkeitAttribute.builder().build()))
				.build())
			.build();

		Knoten vonKnotenDlm = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(1, 10), QuellSystem.DLM)
			.build();
		Knoten nachKnotenDlm = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(2, 20), QuellSystem.DLM)
			.build();

		Kante kanteDlm = erstelleDlmKante(lineString, vonKnotenDlm, nachKnotenDlm);
		kanteDlm.getKantenAttributGruppe().getKantenAttribute()
			.setStrassenName(StrassenName.of("irgendSoEinAlterName"));

		Long savedRadNETZKanteId = kantenRepository.save(radNetzKante).getId();
		Long savedKanteDlmId = kantenRepository.save(kanteDlm).getId();

		entityManager.flush();
		entityManager.clear();

		String json = "{"
			+ " \"radnetzKanteId\":" + savedRadNETZKanteId + ","
			+ " \"dlmnetzKanteIds\" : [ " + savedKanteDlmId + " ]"
			+ "}";

		ChangeZuordnungCommand changeZuordnungCommand = mapper.readValue(json,
			ChangeZuordnungCommand.class);
		zuordnungController.changeZuordnung(changeZuordnungCommand);

		entityManager.flush();
		entityManager.clear();

		Kante kanteDlmGespeichert = kantenRepository.findById(savedKanteDlmId).get();

		KantenAttribute gemappteKantenAttribute = kanteDlmGespeichert.getKantenAttributGruppe().getKantenAttribute();
		assertThat(gemappteKantenAttribute.getWegeNiveau()).isEqualTo(kantenAttribute.getWegeNiveau());
		assertThat(kanteDlmGespeichert.getKantenAttributGruppe().getNetzklassen()).containsExactlyInAnyOrderElementsOf(
			netzklassen);
		assertThat(kanteDlmGespeichert.getKantenAttributGruppe().getIstStandards()).containsExactlyInAnyOrderElementsOf(
			istStandards);

		GeschwindigkeitAttribute gemappteGeschwindigkeitAttribute = kanteDlmGespeichert
			.getGeschwindigkeitAttributGruppe().getImmutableGeschwindigkeitAttribute().get(0);
		assertThat(gemappteGeschwindigkeitAttribute)
			.extracting("ortslage", "hoechstgeschwindigkeit",
				"abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung")
			.containsExactly(geschwindigkeitsAttribute.getOrtslage(),
				geschwindigkeitsAttribute.getHoechstgeschwindigkeit(),
				geschwindigkeitsAttribute.getAbweichendeHoechstgeschwindigkeitGegenStationierungsrichtung());

		// act
		json = "{ \"dlmnetzKanteId\" : " + savedKanteDlmId + "}";

		LoescheZuordnungenCommand loescheZuordnungCommand = mapper.readValue(json,
			LoescheZuordnungenCommand.class);
		zuordnungController.loescheZuordnung(loescheZuordnungCommand);

		// assert
		Optional<Kante> savedDlmKante = kantenRepository.findById(savedKanteDlmId);
		assertThat(savedDlmKante.get().getKantenAttributGruppe().getNetzklassen()).isEmpty();
		assertThat(savedDlmKante.get().getKantenAttributGruppe().getIstStandards()).isEmpty();

		gemappteKantenAttribute = kanteDlmGespeichert.getKantenAttributGruppe().getKantenAttribute();
		assertThat(gemappteKantenAttribute.getUmfeld()).isEqualTo(Umfeld.UNBEKANNT);
		assertThat(gemappteKantenAttribute.getStrassenName()).contains(
			kanteDlm.getKantenAttributGruppe().getKantenAttribute().getStrassenName()
				.get());

		gemappteGeschwindigkeitAttribute = kanteDlmGespeichert
			.getGeschwindigkeitAttributGruppe().getImmutableGeschwindigkeitAttribute().get(0);
		assertThat(gemappteGeschwindigkeitAttribute)
			.extracting("ortslage", "hoechstgeschwindigkeit",
				"abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung")
			.containsExactly(
				Optional.empty(), Hoechstgeschwindigkeit.UNBEKANNT, Optional.empty());
	}
}
