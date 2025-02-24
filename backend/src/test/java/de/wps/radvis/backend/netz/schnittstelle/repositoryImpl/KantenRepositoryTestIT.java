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

package de.wps.radvis.backend.netz.schnittstelle.repositoryImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

import de.wps.radvis.backend.application.JacksonConfiguration;
import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.benutzer.domain.TechnischerBenutzerConfigurationProperties;
import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.PostGisHelper;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.fahrradroute.FahrradrouteConfiguration;
import de.wps.radvis.backend.fahrradroute.domain.entity.Fahrradroute;
import de.wps.radvis.backend.fahrradroute.domain.entity.provider.FahrradrouteTestDataProvider;
import de.wps.radvis.backend.fahrradroute.domain.repository.FahrradrouteRepository;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.DrouteId;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradrouteName;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.Kategorie;
import de.wps.radvis.backend.netz.NetzConfiguration;
import de.wps.radvis.backend.netz.domain.NetzConfigurationProperties;
import de.wps.radvis.backend.netz.domain.dbView.KanteOsmMatchWithAttribute;
import de.wps.radvis.backend.netz.domain.entity.FahrtrichtungAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttribute;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KanteElevationView;
import de.wps.radvis.backend.netz.domain.entity.KanteGeometryView;
import de.wps.radvis.backend.netz.domain.entity.KanteOsmWayIdsInsert;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.KantenAttribute;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.NahegelegeneneKantenDbView;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.provider.FahrtrichtungAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.FuehrungsformAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.FuehrungsformAttributeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KantenAttributeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.valueObject.Absenkung;
import de.wps.radvis.backend.netz.domain.valueObject.BelagArt;
import de.wps.radvis.backend.netz.domain.valueObject.Beleuchtung;
import de.wps.radvis.backend.netz.domain.valueObject.Benutzungspflicht;
import de.wps.radvis.backend.netz.domain.valueObject.Beschilderung;
import de.wps.radvis.backend.netz.domain.valueObject.Bordstein;
import de.wps.radvis.backend.netz.domain.valueObject.DlmId;
import de.wps.radvis.backend.netz.domain.valueObject.Hoechstgeschwindigkeit;
import de.wps.radvis.backend.netz.domain.valueObject.IstStandard;
import de.wps.radvis.backend.netz.domain.valueObject.KanteElevationUpdate;
import de.wps.radvis.backend.netz.domain.valueObject.KantenOrtslage;
import de.wps.radvis.backend.netz.domain.valueObject.KfzParkenForm;
import de.wps.radvis.backend.netz.domain.valueObject.KfzParkenTyp;
import de.wps.radvis.backend.netz.domain.valueObject.Kommentar;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.netz.domain.valueObject.LinearReferenzierteOsmWayId;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.NetzklasseFilter;
import de.wps.radvis.backend.netz.domain.valueObject.Oberflaechenbeschaffenheit;
import de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung;
import de.wps.radvis.backend.netz.domain.valueObject.Richtung;
import de.wps.radvis.backend.netz.domain.valueObject.Schadenart;
import de.wps.radvis.backend.netz.domain.valueObject.Status;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenName;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenNummer;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenkategorieRIN;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenquerschnittRASt06;
import de.wps.radvis.backend.netz.domain.valueObject.TrennstreifenForm;
import de.wps.radvis.backend.netz.domain.valueObject.TrennungZu;
import de.wps.radvis.backend.netz.domain.valueObject.Umfeld;
import de.wps.radvis.backend.netz.domain.valueObject.VereinbarungsKennung;
import de.wps.radvis.backend.netz.domain.valueObject.VerkehrStaerke;
import de.wps.radvis.backend.netz.domain.valueObject.WegeNiveau;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.GebietskoerperschaftRepository;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;

@Tag("group3")
@Slf4j
@EnableJpaRepositories(basePackageClasses = { FahrradrouteConfiguration.class })
@EntityScan(basePackageClasses = { FahrradrouteConfiguration.class })
@ContextConfiguration(classes = {
	NetzConfiguration.class,
	CommonConfiguration.class,
	OrganisationConfiguration.class,
	BenutzerConfiguration.class,
	GeoConverterConfiguration.class,
	JacksonConfiguration.class
})
@EnableConfigurationProperties(value = {
	CommonConfigurationProperties.class,
	FeatureToggleProperties.class,
	TechnischerBenutzerConfigurationProperties.class,
	PostgisConfigurationProperties.class,
	OrganisationConfigurationProperties.class,
	NetzConfigurationProperties.class
})
class KantenRepositoryTestIT extends DBIntegrationTestIT {

	@Autowired
	FahrradrouteRepository fahrradrouteRepository;

	private static final GeometryFactory GEO_FACTORY = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory();

	@Autowired
	private KantenRepository kantenRepository;

	@Autowired
	private GebietskoerperschaftRepository gebietskoerperschaftRepository;

	@PersistenceContext
	EntityManager entityManager;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@SuppressWarnings("unchecked")
	@Test
	void saveAndGet() {
		// Arrange
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
			.strassenkategorieRIN(StrassenkategorieRIN.NAHRAEUMIG)
			.strassenquerschnittRASt06(StrassenquerschnittRASt06.ANBAUFREIE_STRASSE)
			.status(Status.defaultWert())
			.build();

		Verwaltungseinheit gebietskoerperschaft = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().name("tolle Organisation")
				.organisationsArt(
					OrganisationsArt.KREIS)
				.build());

		entityManager.flush();
		entityManager.clear();

		Kante kante = KanteTestDataProvider.fromKnotenUndQuelle(vonKnoten, nachKnoten, QuellSystem.DLM)
			.dlmId(DlmId.of("123"))
			.kantenAttributGruppe(new KantenAttributGruppe(kantenAttribute, new HashSet<>(), new HashSet<>()))
			.fuehrungsformAttributGruppe(new FuehrungsformAttributGruppe(List
				.of(new FuehrungsformAttribute(LinearReferenzierterAbschnitt.of(0, 1),
					BelagArt.ASPHALT,
					Oberflaechenbeschaffenheit.ANLASS_ZUR_INTENSIVEN_BEOBACHTUNG_UND_ANALYSE,
					Bordstein.KEINE_ABSENKUNG,
					Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND,
					KfzParkenTyp.LAENGS_PARKEN,
					KfzParkenForm.HALBES_GEHWEGPARKEN_UNMARKIERT,
					Laenge.of(1),
					Benutzungspflicht.VORHANDEN,
					Beschilderung.UNBEKANNT,
					Collections.emptySet(),
					Absenkung.UNBEKANNT,
					null,
					null,
					null,
					null,
					TrennstreifenForm.UNBEKANNT,
					TrennstreifenForm.UNBEKANNT)),
				true))
			.isZweiseitig(true)
			.geschwindigkeitAttributGruppe(GeschwindigkeitAttributGruppe.builder()
				.geschwindigkeitAttribute(
					List.of(GeschwindigkeitAttribute.builder()
						.ortslage(KantenOrtslage.INNERORTS)
						.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_100_KMH)
						.build()))
				.build())
			.zustaendigkeitAttributGruppe(new ZustaendigkeitAttributGruppe(List.of(
				new ZustaendigkeitAttribute(LinearReferenzierterAbschnitt.of(0, 1), gebietskoerperschaft,
					gebietskoerperschaft,
					gebietskoerperschaft,
					VereinbarungsKennung.of("123")))))
			.fahrtrichtungAttributGruppe(new FahrtrichtungAttributGruppe(Richtung.BEIDE_RICHTUNGEN, true)).build();

		// Act
		Long savedKanteId = kantenRepository.save(kante).getId();

		entityManager.flush();
		entityManager.clear();

		Optional<Kante> result = kantenRepository.findById(savedKanteId);

		// Assert
		assertThat(result).isPresent();
		Kante resultKante = result.get();
		assertThat(resultKante.getVonKnoten().getKoordinate()).isEqualTo(vonKnoten.getKoordinate());
		assertThat(resultKante.getNachKnoten().getKoordinate()).isEqualTo(nachKnoten.getKoordinate());
		assertThat(resultKante.getGeometry()).isEqualTo(lineString);
		assertThat(resultKante.getKantenLaengeInCm()).isEqualTo(Math.round(lineString.getLength() * 100));
		assertThat(resultKante.getQuelle()).isEqualTo(QuellSystem.DLM);

		assertThat(resultKante.getKantenAttributGruppe().getKantenAttribute()).isEqualTo(kantenAttribute);

		assertThat(
			resultKante.getGeschwindigkeitAttributGruppe().getImmutableGeschwindigkeitAttribute().get(0)
				.getHoechstgeschwindigkeit())
					.isEqualTo(
						kante.getGeschwindigkeitAttributGruppe().getImmutableGeschwindigkeitAttribute().get(0)
							.getHoechstgeschwindigkeit());
		assertThat(
			resultKante.getGeschwindigkeitAttributGruppe().getImmutableGeschwindigkeitAttribute().get(0)
				.getAbweichendeHoechstgeschwindigkeitGegenStationierungsrichtung())
					.isEqualTo(kante.getGeschwindigkeitAttributGruppe().getImmutableGeschwindigkeitAttribute().get(0)
						.getAbweichendeHoechstgeschwindigkeitGegenStationierungsrichtung());

		assertThat(resultKante.getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute()).hasSize(1);
		final ZustaendigkeitAttribute zustaendigkeitAttribute = resultKante.getZustaendigkeitAttributGruppe()
			.getImmutableZustaendigkeitAttribute().get(0);
		assertThat(zustaendigkeitAttribute.getBaulastTraeger()).contains(gebietskoerperschaft);
		assertThat(zustaendigkeitAttribute.getUnterhaltsZustaendiger()).contains(gebietskoerperschaft);
		assertThat(zustaendigkeitAttribute.getErhaltsZustaendiger()).contains(gebietskoerperschaft);
		assertThat(zustaendigkeitAttribute.getVereinbarungsKennung()).contains(VereinbarungsKennung.of("123"));

		assertThat(resultKante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks()).hasSize(1);
		final FuehrungsformAttribute resultFuehrungsformAttribute = resultKante.getFuehrungsformAttributGruppe()
			.getImmutableFuehrungsformAttributeLinks().get(0);
		final FuehrungsformAttribute sollFuehrungsformAttribute = kante.getFuehrungsformAttributGruppe()
			.getImmutableFuehrungsformAttributeLinks().get(0);
		assertThat(resultFuehrungsformAttribute.getLinearReferenzierterAbschnitt())
			.isEqualTo(sollFuehrungsformAttribute.getLinearReferenzierterAbschnitt());
		assertThat(resultFuehrungsformAttribute.getRadverkehrsfuehrung())
			.isEqualTo(sollFuehrungsformAttribute.getRadverkehrsfuehrung());
		assertThat(resultFuehrungsformAttribute.getParkenTyp())
			.isEqualTo(sollFuehrungsformAttribute.getParkenTyp());
		assertThat(resultFuehrungsformAttribute.getParkenForm())
			.isEqualTo(sollFuehrungsformAttribute.getParkenForm());
		assertThat(resultFuehrungsformAttribute.getBreite())
			.isEqualTo(sollFuehrungsformAttribute.getBreite());
		assertThat(resultFuehrungsformAttribute.getBelagArt())
			.isEqualTo(sollFuehrungsformAttribute.getBelagArt());
		assertThat(resultFuehrungsformAttribute.getOberflaechenbeschaffenheit())
			.isEqualTo(sollFuehrungsformAttribute.getOberflaechenbeschaffenheit());
		assertThat(resultFuehrungsformAttribute.getBordstein())
			.isEqualTo(sollFuehrungsformAttribute.getBordstein());
		assertThat(resultFuehrungsformAttribute.getBenutzungspflicht())
			.isEqualTo(sollFuehrungsformAttribute.getBenutzungspflicht());

		assertThat(kante.isZweiseitig()).isTrue();
	}

	@Test
	void testGetKantenInBereichNachQuelleNachOsmGematcht() {
		// Arrange
		LineString osmLineString = GEO_FACTORY
			.createLineString(new Coordinate[] { new Coordinate(2, 2), new Coordinate(3, 3) });

		Kante radNETZInnerhalbMitOSM = createKante(new Coordinate(1, 10), new Coordinate(2, 20), QuellSystem.RadNETZ);
		radNETZInnerhalbMitOSM.setAufDlmAbgebildeteGeometry(osmLineString);
		Kante radNETZInnerhalbOhneOSM = createKante(new Coordinate(1, 10), new Coordinate(2, 20), QuellSystem.RadNETZ);

		Kante DLMTeilweiseAusserhalbMitOSM = createKante(new Coordinate(1, 10), new Coordinate(2, 40));
		DLMTeilweiseAusserhalbMitOSM.setAufDlmAbgebildeteGeometry(osmLineString);
		Kante DLMTeilweiseAusserhalbOhneOSM = createKante(new Coordinate(1, 10), new Coordinate(2, 40));

		Kante kanteKomplettAusserhalb = createKante(new Coordinate(31, 31), new Coordinate(40, 40));
		kanteKomplettAusserhalb.setAufDlmAbgebildeteGeometry(osmLineString);

		radNETZInnerhalbMitOSM = kantenRepository.save(radNETZInnerhalbMitOSM);
		radNETZInnerhalbOhneOSM = kantenRepository.save(radNETZInnerhalbOhneOSM);
		DLMTeilweiseAusserhalbMitOSM = kantenRepository.save(DLMTeilweiseAusserhalbMitOSM);
		DLMTeilweiseAusserhalbOhneOSM = kantenRepository.save(DLMTeilweiseAusserhalbOhneOSM);
		kanteKomplettAusserhalb = kantenRepository.save(kanteKomplettAusserhalb);

		entityManager.flush();
		entityManager.clear();

		Envelope bereich = new Envelope(0, 30, 0, 30);
		// Act
		Set<Kante> resultDLM = kantenRepository.getKantenInBereichNachQuelleUndIsAbgebildet(bereich, QuellSystem.DLM);
		Set<Kante> resultRadNETZ = kantenRepository.getKantenInBereichNachQuelleUndIsAbgebildet(bereich,
			QuellSystem.RadNETZ);

		// Assert
		assertThat(resultDLM).hasSize(1);
		assertThat(resultDLM).containsExactlyInAnyOrder(DLMTeilweiseAusserhalbMitOSM);
		assertThat(resultRadNETZ).size().isEqualTo(1);
		assertThat(resultRadNETZ).containsExactlyInAnyOrder(radNETZInnerhalbMitOSM);
	}

	@Test
	void testGetKantenInBereichNachQuelle() {
		// Arrange
		LineString osmLineString = GEO_FACTORY
			.createLineString(new Coordinate[] { new Coordinate(2, 2), new Coordinate(3, 3) });

		Kante radNETZInnerhalbMitOSM = createKante(new Coordinate(1, 10), new Coordinate(2, 20), QuellSystem.RadNETZ);
		radNETZInnerhalbMitOSM.setAufDlmAbgebildeteGeometry(osmLineString);
		Kante radNETZInnerhalbOhneOSM = createKante(new Coordinate(1, 10), new Coordinate(2, 20), QuellSystem.RadNETZ);

		Kante DLMTeilweiseAusserhalbMitOSM = createKante(new Coordinate(1, 10), new Coordinate(2, 40));
		DLMTeilweiseAusserhalbMitOSM.setAufDlmAbgebildeteGeometry(osmLineString);
		Kante DLMTeilweiseAusserhalbOhneOSM = createKante(new Coordinate(1, 10), new Coordinate(2, 40));

		Kante kanteKomplettAusserhalb = createKante(new Coordinate(31, 31), new Coordinate(40, 40));
		kanteKomplettAusserhalb.setAufDlmAbgebildeteGeometry(osmLineString);

		radNETZInnerhalbMitOSM = kantenRepository.save(radNETZInnerhalbMitOSM);
		radNETZInnerhalbOhneOSM = kantenRepository.save(radNETZInnerhalbOhneOSM);
		DLMTeilweiseAusserhalbMitOSM = kantenRepository.save(DLMTeilweiseAusserhalbMitOSM);
		DLMTeilweiseAusserhalbOhneOSM = kantenRepository.save(DLMTeilweiseAusserhalbOhneOSM);
		kanteKomplettAusserhalb = kantenRepository.save(kanteKomplettAusserhalb);

		entityManager.flush();
		entityManager.clear();

		Envelope bereich = new Envelope(0, 30, 0, 30);
		// Act
		Set<Kante> resultDLM = kantenRepository.getKantenInBereichNachQuellen(bereich, Set.of(QuellSystem.DLM)).collect(
			Collectors.toSet());
		Set<Kante> resultRadNETZ = kantenRepository.getKantenInBereichNachQuellen(bereich,
			Set.of(QuellSystem.RadNETZ)).collect(Collectors.toSet());

		// Assert
		assertThat(resultDLM).hasSize(2);
		assertThat(resultDLM).containsExactlyInAnyOrder(DLMTeilweiseAusserhalbMitOSM, DLMTeilweiseAusserhalbOhneOSM);
		assertThat(resultRadNETZ).hasSize(2);
		assertThat(resultRadNETZ).containsExactlyInAnyOrder(radNETZInnerhalbMitOSM, radNETZInnerhalbOhneOSM);
	}

	@Test
	void testGetKantenInBereichNachQuellenEagerFetchFahrtrichtungEagerFetchFuehrungsformAttributeLinks() {
		// Arrange

		Kante kante = KanteTestDataProvider.withDefaultValuesAndZweiseitig()
			.vonKnoten(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(1, 10), QuellSystem.DLM).build())
			.nachKnoten(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(2, 20), QuellSystem.DLM).build())
			.build();

		kante = kantenRepository.save(kante);

		entityManager.flush();
		entityManager.clear();

		Envelope bereich = new Envelope(0, 30, 0, 30);
		// Act
		Set<Kante> resultDLM = kantenRepository
			.getKantenInBereichNachQuellenEagerFetchFahrtrichtungEagerFetchFuehrungsformAttributeLinks(
				bereich,
				Set.of(QuellSystem.DLM))
			.collect(
				Collectors.toSet());

		entityManager.flush();
		entityManager.clear();

		// Assert
		assertThat(resultDLM).hasSize(1);
		assertThat(resultDLM).containsExactlyInAnyOrder(kante);
		Kante resultKante = resultDLM.stream().findFirst().get();
		assertThat(resultKante.getFahrtrichtungAttributGruppe().isZweiseitig()).isTrue();
		assertThat(resultKante.getFuehrungsformAttributGruppe()
			.getImmutableFuehrungsformAttributeLinks()).hasSize(1);
		assertThat(resultKante.getFuehrungsformAttributGruppe()
			.getImmutableFuehrungsformAttributeRechts()).hasSize(1);
		assertThatThrownBy(
			() -> resultKante.getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute().isEmpty());
	}

	@Test
	@SuppressWarnings("unchecked")
	void testeGetKantenInBereichNachNetzklasse() {
		assertThat(kantenRepository.findAll()).isEmpty();
		assertThat(entityManager.createNativeQuery("SELECT netzklasse FROM kanten_attribut_gruppe_netzklassen")
			.getResultList()).isEmpty();

		Kante radNetz = kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(2, 2, 2, 20, QuellSystem.RadNETZ)
				.kantenAttributGruppe(new KantenAttributGruppe(KantenAttribute.builder()
					.build(),
					Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_ZIELNETZ, Netzklasse.RADNETZ_FREIZEIT),
					new HashSet<>()))
				.isGrundnetz(true)
				.build());

		Kante kommunal = kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(3, 20, 20, 3, QuellSystem.DLM)
				.kantenAttributGruppe(new KantenAttributGruppe(KantenAttribute.builder()
					.build(), Set.of(Netzklasse.KOMMUNALNETZ_ALLTAG), new HashSet<>()))
				.isGrundnetz(true)
				.build());

		Kante radNetzUndKommunal = kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(300, 300, 350, 350, QuellSystem.RadNETZ)
				.kantenAttributGruppe(new KantenAttributGruppe(KantenAttribute.builder()
					.build(),
					Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_ZIELNETZ, Netzklasse.KOMMUNALNETZ_ALLTAG),
					new HashSet<>()))
				.isGrundnetz(true)
				.build());

		Kante keineNetzklasse = kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(300, 400, 400, 400, QuellSystem.DLM)
				.kantenAttributGruppe(new KantenAttributGruppe(KantenAttribute.builder()
					.build(), new HashSet<>(), new HashSet<>()))
				.isGrundnetz(true)
				.build());

		Kante imNiemandslandAusRadNetz = kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(1231209, 102838978,
				12312309, 10283897, QuellSystem.RadNETZ)
				.kantenAttributGruppe(new KantenAttributGruppe(KantenAttribute.builder()
					.build(), Set.of(Netzklasse.RADNETZ_ALLTAG), new HashSet<>()))
				.isGrundnetz(true)
				.build());

		Kante imNiemandslandOhneNetzklassen = kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(3231209, 2838978,
				33312309, 10283897, QuellSystem.DLM)
				.kantenAttributGruppe(new KantenAttributGruppe(KantenAttribute.builder()
					.build(), new HashSet<>(), new HashSet<>()))
				.isGrundnetz(true)
				.build());
		Kante radwegeDB = kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(50, 50,
				150, 150, QuellSystem.RadwegeDB).build());

		entityManager.flush();
		entityManager.clear();

		Envelope bereich = new Envelope(0, 1000, 0, 1000);

		Set<Kante> kantenInBereichNachRadNetz = kantenRepository
			.getKantenInBereichNachNetzklasse(bereich,
				Set.of(NetzklasseFilter.RADNETZ), false);
		Set<Kante> kantenInBereichNachOhneNetzklasse = kantenRepository
			.getKantenInBereichNachNetzklasse(bereich,
				Set.of(NetzklasseFilter.NICHT_KLASSIFIZIERT), false);
		Set<Kante> kantenInBereichNachAllenNetzklassen = kantenRepository
			.getKantenInBereichNachNetzklasse(bereich,
				Set.of(NetzklasseFilter.NICHT_KLASSIFIZIERT, NetzklasseFilter.RADNETZ, NetzklasseFilter.KOMMUNALNETZ,
					NetzklasseFilter.KREISNETZ),
				false);
		Set<Kante> alleKanten = kantenRepository
			.getKantenInBereichNachNetzklasse(new Envelope(0, 1000000000, 0, 100000000),
				Set.of(NetzklasseFilter.NICHT_KLASSIFIZIERT, NetzklasseFilter.RADNETZ, NetzklasseFilter.KOMMUNALNETZ,
					NetzklasseFilter.KREISNETZ),
				false);

		assertThat(kantenRepository.findAll()).containsExactlyInAnyOrder(radNetz, radNetzUndKommunal,
			imNiemandslandOhneNetzklassen, imNiemandslandAusRadNetz, keineNetzklasse, kommunal, radwegeDB);
		assertThat(kantenInBereichNachRadNetz).size().isEqualTo(2);
		assertThat(kantenInBereichNachRadNetz).containsExactlyInAnyOrder(radNetz, radNetzUndKommunal);
		assertThat(
			kantenRepository.findById(radNetz.getId()).get().getKantenAttributGruppe().getNetzklassen()).contains(
				Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_ZIELNETZ, Netzklasse.RADNETZ_FREIZEIT);

		assertThat(
			kantenRepository.findById(radNetzUndKommunal.getId()).get().getKantenAttributGruppe()
				.getNetzklassen()).contains(
					Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_ZIELNETZ, Netzklasse.KOMMUNALNETZ_ALLTAG);

		assertThat(kantenInBereichNachRadNetz
			.stream().map(Kante::getKantenAttributGruppe)
			.map(KantenAttributGruppe::getNetzklassen)
			.collect(Collectors.toList()))
				.containsExactlyInAnyOrder(
					Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_ZIELNETZ, Netzklasse.RADNETZ_FREIZEIT),
					Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_ZIELNETZ, Netzklasse.KOMMUNALNETZ_ALLTAG));

		assertThat(kantenInBereichNachOhneNetzklasse).size().isEqualTo(1);
		assertThat(kantenInBereichNachOhneNetzklasse).containsExactlyInAnyOrder(keineNetzklasse);

		assertThat(kantenInBereichNachAllenNetzklassen)
			.containsExactlyInAnyOrder(keineNetzklasse, radNetz, kommunal, radNetzUndKommunal);
		assertThat(kantenInBereichNachAllenNetzklassen).size().isEqualTo(4);

		assertThat(alleKanten).size().isEqualTo(6);
		assertThat(alleKanten)
			.containsExactlyInAnyOrder(keineNetzklasse, radNetz, kommunal, radNetzUndKommunal, imNiemandslandAusRadNetz,
				imNiemandslandOhneNetzklassen);
	}

	@Test
	@SuppressWarnings("unchecked")
	void testeGetKantenInBereichNachNetzklasse_netzklassenVollstaendig() {
		assertThat(kantenRepository.findAll()).isEmpty();
		assertThat(entityManager.createNativeQuery("SELECT netzklasse FROM kanten_attribut_gruppe_netzklassen")
			.getResultList()).isEmpty();

		Kante radNetz = kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(2, 2, 2, 20, QuellSystem.RadNETZ)
				.kantenAttributGruppe(new KantenAttributGruppe(KantenAttribute.builder()
					.build(),
					Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_ZIELNETZ, Netzklasse.RADNETZ_FREIZEIT),
					new HashSet<>()))
				.isGrundnetz(true)
				.build());

		Kante radNetzUndKommunal = kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(300, 300, 350, 350, QuellSystem.RadNETZ)
				.kantenAttributGruppe(new KantenAttributGruppe(KantenAttribute.builder()
					.build(),
					Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_ZIELNETZ, Netzklasse.KOMMUNALNETZ_ALLTAG),
					new HashSet<>()))
				.isGrundnetz(true)
				.build());

		entityManager.flush();
		entityManager.clear();

		entityManager.getEntityManagerFactory().getCache().evictAll();

		Envelope bereich = new Envelope(0, 1000, 0, 1000);

		Set<Kante> kantenInBereichNachRadNetz = kantenRepository
			.getKantenInBereichNachNetzklasse(bereich,
				Set.of(NetzklasseFilter.RADNETZ), false);

		assertThat(kantenRepository.findAll()).containsExactlyInAnyOrder(radNetz, radNetzUndKommunal);
		assertThat(kantenInBereichNachRadNetz).size().isEqualTo(2);
		assertThat(kantenInBereichNachRadNetz).containsExactlyInAnyOrder(radNetz, radNetzUndKommunal);
		assertThat(
			kantenRepository.findById(radNetz.getId()).get().getKantenAttributGruppe().getNetzklassen()).contains(
				Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_ZIELNETZ, Netzklasse.RADNETZ_FREIZEIT);

		assertThat(
			kantenRepository.findById(radNetzUndKommunal.getId()).get().getKantenAttributGruppe()
				.getNetzklassen()).contains(
					Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_ZIELNETZ, Netzklasse.KOMMUNALNETZ_ALLTAG);
		assertThat(kantenInBereichNachRadNetz
			.stream().map(Kante::getKantenAttributGruppe)
			.map(KantenAttributGruppe::getNetzklassen))
				.containsExactlyInAnyOrder(
					Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_ZIELNETZ, Netzklasse.RADNETZ_FREIZEIT),
					Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_ZIELNETZ, Netzklasse.KOMMUNALNETZ_ALLTAG));
		assertThat(kantenInBereichNachRadNetz).containsExactlyInAnyOrder(radNetz, radNetzUndKommunal);

	}

	@Test
	void testGetNahegelegeneKanten() {
		// Arrange
		Kante baseKante = kantenRepository.save(KanteTestDataProvider.withCoordinatesAndQuelle(100, 0, 100, 100,
			QuellSystem.DLM).build());

		// Kanten links von der Basiskante
		Kante kanteLinks = kantenRepository.save(
			KanteTestDataProvider.withCoordinatesAndQuelle(90, 0, 90, 100, QuellSystem.DLM).build()
		);

		// Kanten rechts von der Basiskante
		Kante kanteRechts1 = kantenRepository.save(
			KanteTestDataProvider.withCoordinatesAndQuelle(105, 0, 105, 100, QuellSystem.DLM).build()
		);
		Kante kanteRechts2 = kantenRepository.save(
			KanteTestDataProvider.withCoordinatesAndQuelle(110, 100, 110, 0, QuellSystem.DLM).build()
		);

		// Act & Assert
		List<NahegelegeneneKantenDbView> result = kantenRepository.getNahegelegeneKantenAufSeite(baseKante,
			LinearReferenzierterAbschnitt.of(0, 1), Seitenbezug.RECHTS, Laenge.of(15));

		assertThat(result).hasSize(2);
		assertThat(result.stream().map(k -> k.getNahegelegeneKante())).containsExactlyInAnyOrder(kanteRechts1,
			kanteRechts2);
		assertThat(result.stream().map(k -> k.getNahegelegeneKanteSegment())).containsExactlyInAnyOrder(kanteRechts1
			.getGeometry(), kanteRechts2.getGeometry());
		assertThat(result.stream().map(k -> k.getBasisKanteSegment())).containsExactlyInAnyOrder(baseKante
			.getGeometry(), baseKante.getGeometry());

		// Act & Assert
		result = kantenRepository.getNahegelegeneKantenAufSeite(baseKante, LinearReferenzierterAbschnitt.of(0, 1),
			Seitenbezug.LINKS, Laenge.of(15));

		assertThat(result).hasSize(1);
		assertThat(result.stream().map(k -> k.getNahegelegeneKante())).containsExactlyInAnyOrder(kanteLinks);
		assertThat(result.stream().map(k -> k.getNahegelegeneKanteSegment())).containsExactlyInAnyOrder(kanteLinks
			.getGeometry());
		assertThat(result.stream().map(k -> k.getBasisKanteSegment())).containsExactlyInAnyOrder(baseKante
			.getGeometry());

		// Act & Assert
		result = kantenRepository.getNahegelegeneKantenAufSeite(baseKante, LinearReferenzierterAbschnitt.of(0, 1),
			Seitenbezug.LINKS, Laenge.of(1));
		assertThat(result).isEmpty();

		result = kantenRepository.getNahegelegeneKantenAufSeite(baseKante, LinearReferenzierterAbschnitt.of(0, 1),
			Seitenbezug.RECHTS, Laenge.of(1));
		assertThat(result).isEmpty();
	}

	@Test
	@SuppressWarnings("unchecked")
	void testGetNahegelegeneKanten_ergebnisMitNichtLineStringGeometrien() {
		// Arrange
		Kante baseKante = kantenRepository.save(KanteTestDataProvider.withCoordinatesAndQuelle(100, 0, 100, 100,
			QuellSystem.DLM).build());

		// Berührt die Basiskante, macht einen Bogen und führt dann parallel entlang der Basiskante. Die Geometrie ist
		// also quasi ein "J". Daher liefert die Query einen Punkt (an dem gemeinsamen Knoten) und einen LineString für
		// das tatsächlich parallel verlaufende Segment zurück.
		Kante nahegelegeneKante = kantenRepository.save(
			KanteTestDataProvider
				.withCoordinates(new Coordinate[] {
					new Coordinate(100, 0),
					new Coordinate(100, -30),
					new Coordinate(110, -30),
					new Coordinate(110, 100),
				})
				.build()
		);

		// Act & Assert
		List<NahegelegeneneKantenDbView> result = kantenRepository.getNahegelegeneKantenAufSeite(baseKante,
			LinearReferenzierterAbschnitt.of(0, 1), Seitenbezug.RECHTS, Laenge.of(15)).stream().toList();

		LineString expectedNahegelegeneKanteLineString = GeometryTestdataProvider.createLineString(new Coordinate(110,
			0), new Coordinate(110, 100));

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getNahegelegeneKante()).isEqualTo(nahegelegeneKante);
		assertThat(result.get(0).getNahegelegeneKanteSegment()).isEqualTo(expectedNahegelegeneKanteLineString);

		// Act & Assert
		result = kantenRepository.getNahegelegeneKantenAufSeite(baseKante, LinearReferenzierterAbschnitt.of(0, 1),
			Seitenbezug.LINKS, Laenge.of(15))
			.stream().toList();
		assertThat(result).isEmpty();
	}

	@Test
	@SuppressWarnings("unchecked")
	void testGetNahegelegeneKanten_leereGeometrieBeiBufferBerechnungen() {
		// Arrange
		Kante baseKante = kantenRepository.save(KanteTestDataProvider.withCoordinatesAndQuelle(100, 0, 100, 100,
			QuellSystem.DLM).build());

		// Liegt technisch gesehen innerhalb des Buffers. Allerdings besteht der Buffer am Ende von LineStrings aus
		// Rundungen, die durch mehrere Segmente abstrahiert werden um ein normales Polygon zu bilden. In diesem Fall
		// liegt dadurch die baseKante nicht mehr im Buffer-Polygon, sondern leicht außerhalb.
		Kante nahegelegeneKante = kantenRepository.save(KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 90.001,
			100, QuellSystem.DLM).build());

		// Act & Assert
		List<NahegelegeneneKantenDbView> result = kantenRepository.getNahegelegeneKantenAufSeite(baseKante,
			LinearReferenzierterAbschnitt.of(0, 1),
			Seitenbezug.LINKS, Laenge.of(10))
			.stream().toList();
		assertThat(result).isEmpty();
	}

	@Test
	@SuppressWarnings("unchecked")
	void testGetNahegelegeneKanten_ergebnisIstMultiLineString() {
		// Arrange
		Kante baseKante = kantenRepository.save(KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 100, 0,
			QuellSystem.DLM).build());

		// Ist an zwei Stellen (ca. 20-30 und 70-80) nahe genug an der Basiskante dran, wodruch sich ein MultiLineString ergibt.
		Kante nahegelegeneKante = kantenRepository.save(
			KanteTestDataProvider
				.withCoordinates(new Coordinate[] {
					new Coordinate(0, 16),
					new Coordinate(20, 14),
					new Coordinate(30, 14),
					new Coordinate(50, 16),
					new Coordinate(70, 14),
					new Coordinate(80, 14),
					new Coordinate(100, 16),
				})
				.build()
		);

		// Act & Assert
		List<NahegelegeneneKantenDbView> result = kantenRepository.getNahegelegeneKantenAufSeite(baseKante,
			LinearReferenzierterAbschnitt.of(0, 1), Seitenbezug.LINKS, Laenge.of(15)).stream().toList();

		assertThat(result).hasSize(2);
		assertThat(result.stream().map(k -> k.getNahegelegeneKante())).containsExactlyInAnyOrder(nahegelegeneKante,
			nahegelegeneKante);
		assertThat(result.stream().map(k -> k.getNahegelegeneKanteSegment())).containsExactlyInAnyOrder(
			GeometryTestdataProvider.createLineString(new Coordinate(10, 15), new Coordinate(20, 14), new Coordinate(30,
				14), new Coordinate(40, 15)),
			GeometryTestdataProvider.createLineString(new Coordinate(60, 15), new Coordinate(70, 14), new Coordinate(80,
				14), new Coordinate(90, 15))
		);

		// Act & Assert
		result = kantenRepository.getNahegelegeneKantenAufSeite(baseKante, LinearReferenzierterAbschnitt.of(0, 1),
			Seitenbezug.RECHTS, Laenge.of(15)).stream()
			.toList();
		assertThat(result).isEmpty();
	}

	@SuppressWarnings("unchecked")
	@Test
	void testGetNahegelegeneKanten_linearReferenzierterAbschnitt() {
		// Arrange
		Kante baseKante = kantenRepository.save(KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 0, 100,
			QuellSystem.DLM).build());

		// Kanten links von der Basiskante
		Kante kanteLinks1 = kantenRepository.save(
			KanteTestDataProvider.withCoordinatesAndQuelle(-10, 0, -10, 25, QuellSystem.DLM).build()
		);

		// Kanten rechts von der Basiskante
		Kante kanteRechts1 = kantenRepository.save(
			KanteTestDataProvider.withCoordinatesAndQuelle(10, 25, 10, 50, QuellSystem.DLM).build()
		);

		// Kanten auf beiden Seiten von der Basiskante
		Kante kanteLinks2 = kantenRepository.save(
			KanteTestDataProvider.withCoordinatesAndQuelle(-10, 50, -10, 75, QuellSystem.DLM).build()
		);
		Kante kanteRechts2 = kantenRepository.save(
			KanteTestDataProvider.withCoordinatesAndQuelle(10, 50, 10, 75, QuellSystem.DLM).build()
		);

		// Act & Assert (0 - 0,25)
		LinearReferenzierterAbschnitt abschnitt = LinearReferenzierterAbschnitt.of(0, 0.25);

		List<NahegelegeneneKantenDbView> resultRechts = kantenRepository.getNahegelegeneKantenAufSeite(baseKante,
			abschnitt, Seitenbezug.RECHTS, Laenge.of(15));
		assertThat(resultRechts).isEmpty();

		List<NahegelegeneneKantenDbView> resultLinks = kantenRepository.getNahegelegeneKantenAufSeite(baseKante,
			abschnitt, Seitenbezug.LINKS, Laenge.of(15));
		assertThat(resultLinks).hasSize(1);
		assertThat(resultLinks.get(0).getNahegelegeneKante()).isEqualTo(kanteLinks1);
		assertThat(resultLinks.get(0).getNahegelegeneKanteSegment()).isEqualTo(kanteLinks1.getGeometry());
		assertThat(resultLinks.get(0).getBasisKante()).isEqualTo(baseKante);
		assertThat(resultLinks.get(0).getBasisKanteSegment()).isEqualTo(abschnitt.toSegment(baseKante.getGeometry()));

		// Act & Assert (0,25 - 0,5)
		abschnitt = LinearReferenzierterAbschnitt.of(0.25, 0.5);

		resultRechts = kantenRepository.getNahegelegeneKantenAufSeite(baseKante, abschnitt, Seitenbezug.RECHTS, Laenge
			.of(15));
		assertThat(resultRechts).hasSize(1);
		assertThat(resultRechts.get(0).getNahegelegeneKante()).isEqualTo(kanteRechts1);
		assertThat(resultRechts.get(0).getNahegelegeneKanteSegment()).isEqualTo(kanteRechts1.getGeometry());
		assertThat(resultRechts.get(0).getBasisKante()).isEqualTo(baseKante);
		assertThat(resultRechts.get(0).getBasisKanteSegment()).isEqualTo(abschnitt.toSegment(baseKante.getGeometry()));

		resultLinks = kantenRepository.getNahegelegeneKantenAufSeite(baseKante, abschnitt, Seitenbezug.LINKS, Laenge.of(
			15));
		assertThat(resultLinks).isEmpty();

		// Act & Assert (0,5 - 0,75)
		abschnitt = LinearReferenzierterAbschnitt.of(0.5, 0.75);

		resultRechts = kantenRepository.getNahegelegeneKantenAufSeite(baseKante, abschnitt, Seitenbezug.RECHTS, Laenge
			.of(15));
		assertThat(resultRechts).hasSize(1);
		assertThat(resultRechts.get(0).getNahegelegeneKante()).isEqualTo(kanteRechts2);
		assertThat(resultRechts.get(0).getNahegelegeneKanteSegment()).isEqualTo(kanteRechts2.getGeometry());
		assertThat(resultRechts.get(0).getBasisKante()).isEqualTo(baseKante);
		assertThat(resultRechts.get(0).getBasisKanteSegment()).isEqualTo(abschnitt.toSegment(baseKante.getGeometry()));

		resultLinks = kantenRepository.getNahegelegeneKantenAufSeite(baseKante, abschnitt, Seitenbezug.LINKS, Laenge.of(
			15));
		assertThat(resultLinks).hasSize(1);
		assertThat(resultLinks.get(0).getNahegelegeneKante()).isEqualTo(kanteLinks2);
		assertThat(resultLinks.get(0).getNahegelegeneKanteSegment()).isEqualTo(kanteLinks2.getGeometry());
		assertThat(resultLinks.get(0).getBasisKante()).isEqualTo(baseKante);
		assertThat(resultLinks.get(0).getBasisKanteSegment()).isEqualTo(abschnitt.toSegment(baseKante.getGeometry()));

		// Act & Assert (0,75 - 1)
		abschnitt = LinearReferenzierterAbschnitt.of(0.75, 1);

		resultRechts = kantenRepository.getNahegelegeneKantenAufSeite(baseKante, abschnitt, Seitenbezug.RECHTS, Laenge
			.of(15));
		assertThat(resultRechts).isEmpty();

		resultLinks = kantenRepository.getNahegelegeneKantenAufSeite(baseKante, abschnitt, Seitenbezug.LINKS, Laenge.of(
			15));
		assertThat(resultLinks).isEmpty();
	}

	@Test
	void testFindKanteByQuelle() {
		// Arrange
		Kante kanteRN1 = createKante(new Coordinate(1, 10), new Coordinate(2, 20), QuellSystem.RadNETZ);
		Kante kanteRN2 = createKante(new Coordinate(2, 20), new Coordinate(3, 30), QuellSystem.RadNETZ);

		Kante kanteTF1 = createKante(new Coordinate(1, 10), new Coordinate(2, 20), QuellSystem.DLM);
		Kante kanteTF2 = createKante(new Coordinate(2, 20), new Coordinate(3, 30), QuellSystem.DLM);
		Kante kanteTF3 = createKante(new Coordinate(3, 30), new Coordinate(4, 40), QuellSystem.DLM);
		Kante kanteTF4 = createKante(new Coordinate(4, 40), new Coordinate(5, 50), QuellSystem.DLM);

		kantenRepository.save(kanteRN1);
		kantenRepository.save(kanteRN2);
		kantenRepository.save(kanteTF1);
		kantenRepository.save(kanteTF2);
		kantenRepository.save(kanteTF3);
		kantenRepository.save(kanteTF4);

		kantenRepository.delete(kanteTF3);
		kantenRepository.delete(kanteTF4);

		entityManager.flush();
		entityManager.clear();

		// Act
		List<Kante> resultDLM = kantenRepository.findKanteByQuelle(QuellSystem.DLM)
			.collect(Collectors.toList());
		List<Kante> resultRadNETZ = kantenRepository.findKanteByQuelle(QuellSystem.RadNETZ)
			.collect(Collectors.toList());

		// Assert
		assertThat(resultDLM)
			.isNotEmpty()
			.containsExactlyInAnyOrder(kanteTF1, kanteTF2);
		assertThat(resultRadNETZ)
			.isNotEmpty()
			.containsExactlyInAnyOrder(kanteRN1, kanteRN2);
	}

	@Test
	void testGetRadnetzKantenEagerFetchKnoten() {
		// Arrange
		Kante nichtRadvisNETZ = createKante(new Coordinate(1, 1), new Coordinate(2, 20), QuellSystem.RadNETZ);
		Kante alltag = createKante(new Coordinate(1, 10000), new Coordinate(20, 200), QuellSystem.DLM);
		Kante freizeit = createKante(new Coordinate(2000000, 20), new Coordinate(300, 30), QuellSystem.DLM);
		Kante zielNetz = createKante(new Coordinate(300, 30), new Coordinate(40, 40), QuellSystem.RadVis);
		Kante alle = createKante(new Coordinate(3, 30000), new Coordinate(4, 4000000), QuellSystem.RadVis);
		Kante notRadnetz = createKante(new Coordinate(400, 40), new Coordinate(50, 5000), QuellSystem.DLM);

		kantenRepository.save(nichtRadvisNETZ);
		kantenRepository.save(alltag);
		kantenRepository.save(freizeit);
		kantenRepository.save(zielNetz);
		kantenRepository.save(alle);
		kantenRepository.save(notRadnetz);

		nichtRadvisNETZ.ueberschreibeNetzklassen(Set.of(Netzklasse.RADNETZ_ZIELNETZ));
		alltag.ueberschreibeNetzklassen(Set.of(Netzklasse.RADNETZ_ALLTAG));
		freizeit.ueberschreibeNetzklassen(Set.of(Netzklasse.RADNETZ_FREIZEIT));
		zielNetz.ueberschreibeNetzklassen(Set.of(Netzklasse.RADNETZ_ZIELNETZ));
		alle.ueberschreibeNetzklassen(Netzklasse.RADNETZ_NETZKLASSEN);

		entityManager.flush();
		entityManager.clear();

		// Act
		List<Kante> resultDLM = kantenRepository.getKantenForNetzklassenEagerFetchKnoten(
			Netzklasse.RADNETZ_NETZKLASSEN);
		// Assert
		List<LineString> expected = Stream.of(alltag, freizeit, zielNetz, alle).map(Kante::getGeometry).collect(
			Collectors.toList());
		entityManager.flush();
		entityManager.clear();

		assertThat(resultDLM).hasSize(4);
		assertThat(resultDLM).extracting(Kante::getVonKnoten).extracting(Knoten::getPoint).hasSize(4);
		assertThat(resultDLM).extracting(Kante::getNachKnoten).extracting(Knoten::getPoint).hasSize(4);
		assertThat(resultDLM)
			.extracting(Kante::getGeometry)
			.containsExactlyInAnyOrderElementsOf(expected);
	}

	@Test
	void testKanteByLineString_keineGeloeschtenKanten() {
		// Arrange
		Kante kante1 = createKante(new Coordinate(1, 10), new Coordinate(2, 20));
		Kante kante2 = createKante(new Coordinate(1, 10), new Coordinate(2, 20));
		Kante kante3 = createKante(new Coordinate(3, 30), new Coordinate(4, 40));

		kantenRepository.save(kante1);
		kantenRepository.save(kante2);
		kantenRepository.save(kante3);

		kantenRepository.delete(kante2);

		LineString lineStringForRequest = GEO_FACTORY
			.createLineString(new Coordinate[] { new Coordinate(1, 10), new Coordinate(2, 20) });

		// Act
		List<Kante> kantenByLineString = kantenRepository.getKantenByLineString(lineStringForRequest);

		// Assert
		assertThat(kantenByLineString.size()).isEqualTo(1);
		assertThat(kantenByLineString.get(0)).isEqualTo(kante1);
	}

	@Test
	void testFindKanteByStatusNotAndQuelleIn() {
		// Arrange
		Kante kanteDlm = createKante(new Coordinate(1, 10), new Coordinate(10, 10), QuellSystem.DLM);
		kanteDlm.getKantenAttributGruppe().getKantenAttribute().setStatus(Status.UNTER_VERKEHR);
		Kante kanteRadVis = createKante(new Coordinate(2, 20), new Coordinate(20, 20), QuellSystem.RadVis);
		kanteRadVis.getKantenAttributGruppe().getKantenAttribute().setStatus(Status.UNTER_VERKEHR);

		Kante kanteDlmInBau = createKante(new Coordinate(10, 10), new Coordinate(100, 10), QuellSystem.DLM);
		kanteDlmInBau.getKantenAttributGruppe().getKantenAttribute().setStatus(Status.IN_BAU);
		Kante kanteRadVisInBau = createKante(new Coordinate(20, 20), new Coordinate(200, 20), QuellSystem.RadVis);
		kanteRadVisInBau.getKantenAttributGruppe().getKantenAttribute().setStatus(Status.IN_BAU);

		kantenRepository.save(kanteDlm);
		kantenRepository.save(kanteRadVis);
		kantenRepository.save(kanteDlmInBau);
		kantenRepository.save(kanteRadVisInBau);

		// Act & Assert
		List<Kante> kanten = kantenRepository.findKanteByStatusNotAndQuelleIn(Status.IN_BAU, List.of(QuellSystem.DLM))
			.toList();
		assertThat(kanten).containsExactly(kanteDlm);

		kanten = kantenRepository.findKanteByStatusNotAndQuelleIn(Status.IN_BAU, List.of(QuellSystem.DLM,
			QuellSystem.RadVis)).toList();
		assertThat(kanten).containsExactly(kanteDlm, kanteRadVis);

		kanten = kantenRepository.findKanteByStatusNotAndQuelleIn(Status.UNTER_VERKEHR, List.of(QuellSystem.DLM))
			.toList();
		assertThat(kanten).containsExactly(kanteDlmInBau);

		kanten = kantenRepository.findKanteByStatusNotAndQuelleIn(Status.UNTER_VERKEHR, List.of(QuellSystem.DLM,
			QuellSystem.RadVis)).toList();
		assertThat(kanten).containsExactly(kanteDlmInBau, kanteRadVisInBau);

		kanten = kantenRepository.findKanteByStatusNotAndQuelleIn(Status.KONZEPTION, List.of(QuellSystem.DLM,
			QuellSystem.RadVis)).toList();
		assertThat(kanten).containsExactly(kanteDlm, kanteRadVis, kanteDlmInBau, kanteRadVisInBau);

		kanten = kantenRepository.findKanteByStatusNotAndQuelleIn(Status.UNTER_VERKEHR, List.of(QuellSystem.RadNETZ))
			.toList();
		assertThat(kanten).isEmpty();

		kanten = kantenRepository.findKanteByStatusNotAndQuelleIn(Status.UNTER_VERKEHR, List.of()).toList();
		assertThat(kanten).isEmpty();
	}

	@Test
	void testBuildIndex_wirftKeineFehler() {
		// act & assert
		assertThatNoException().isThrownBy(() -> kantenRepository.buildIndex());
	}

	@SuppressWarnings("unused")
	@Test
	void testGetKantenInBereich() {
		// Arrange
		Kante radNETZInnerhalb = kantenRepository.save(
			createKante(new Coordinate(1, 10), new Coordinate(2, 20), QuellSystem.RadNETZ));
		Kante DLMTeilweiseAusserhalb = kantenRepository.save(createKante(new Coordinate(1, 10), new Coordinate(2, 40)));
		Kante kanteKomplettAusserhalb = kantenRepository.save(
			createKante(new Coordinate(31, 31), new Coordinate(40, 40)));
		Kante radvisInnerhalb = kantenRepository.save(
			createKante(new Coordinate(10, 10), new Coordinate(20, 20), QuellSystem.RadVis));
		Kante radvisAusserhalb = kantenRepository.save(
			createKante(new Coordinate(40, 40), new Coordinate(20, 31), QuellSystem.RadVis));

		entityManager.flush();
		entityManager.clear();

		Envelope bereich = new Envelope(0, 30, 0, 30);
		// Act
		Set<Kante> result = kantenRepository.getKantenInBereich(bereich);

		// Assert
		assertThat(result).hasSize(2);
		assertThat(result).containsExactlyInAnyOrder(DLMTeilweiseAusserhalb, radvisInnerhalb);
	}

	@Test
	void testGetKantenBereich() {
		Kante kanteInBereich = kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(0, 10, 1, 20, QuellSystem.DLM)
				.build());
		// ausserhalb
		kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(101, 101, 120, 120, QuellSystem.DLM)
				.build());

		// act
		Set<Kante> kanten = kantenRepository
			.getKantenInBereich(GeometryTestdataProvider.createQuadratischerBereich(0, 0, 100, 100));

		// assert
		assertThat(kanten).containsExactlyInAnyOrder(kanteInBereich);
	}

	@Test
	void testGetKantenBereich_BereichEmpty_keineKanten() {
		kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(0, 10, 1, 20, QuellSystem.DLM)
				.build());
		kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(101, 101, 120, 120, QuellSystem.DLM)
				.build());

		// act
		Set<Kante> kanten = kantenRepository.getKantenInBereich(GEO_FACTORY.createMultiPolygon());

		// assert
		assertThat(kanten).isEmpty();
	}

	@Test
	void testInsertOsmWayIds() {
		Kante kante1 = kantenRepository
			.save(KanteTestDataProvider.withDefaultValuesAndQuelle(QuellSystem.DLM).build());
		Kante kante2 = kantenRepository
			.save(KanteTestDataProvider.withDefaultValuesAndQuelle(QuellSystem.DLM).build());

		List<LinearReferenzierteOsmWayId> wayIdWithLRSKante1 = List.of(
			LinearReferenzierteOsmWayId.of(1L, LinearReferenzierterAbschnitt.of(0, 1)),
			LinearReferenzierteOsmWayId.of(2L, LinearReferenzierterAbschnitt.of(0, 1)),
			LinearReferenzierteOsmWayId.of(3L, LinearReferenzierterAbschnitt.of(0, 1)));
		List<LinearReferenzierteOsmWayId> wayIdWithLRSKante2 = List.of(
			LinearReferenzierteOsmWayId.of(4L, LinearReferenzierterAbschnitt.of(0, 1)),
			LinearReferenzierteOsmWayId.of(5L, LinearReferenzierterAbschnitt.of(0, 1)),
			LinearReferenzierteOsmWayId.of(6L, LinearReferenzierterAbschnitt.of(0, 1)));
		List<KanteOsmWayIdsInsert> inserts = List.of(
			new KanteOsmWayIdsInsert(kante1.getId(), wayIdWithLRSKante1),
			new KanteOsmWayIdsInsert(kante2.getId(), wayIdWithLRSKante2));
		// act
		kantenRepository.insertOsmWayIds(inserts);

		entityManager.flush();
		entityManager.clear();

		// assert
		assertThat(kantenRepository.findKanteByQuelle(QuellSystem.DLM).map(Kante::getOsmWayIds))
			.containsExactlyInAnyOrder(Set.copyOf(wayIdWithLRSKante1),
				Set.copyOf(wayIdWithLRSKante2));
	}

	@Test
	void testTruncateOsmWayIds() {
		Kante kante1 = kantenRepository
			.save(KanteTestDataProvider.withDefaultValuesAndQuelle(QuellSystem.DLM).build());
		Kante kante2 = kantenRepository
			.save(KanteTestDataProvider.withDefaultValuesAndQuelle(QuellSystem.DLM).build());

		kante1.setOsmWayIds(Set.of(
			LinearReferenzierteOsmWayId.of(1L, LinearReferenzierterAbschnitt.of(0, 1)),
			LinearReferenzierteOsmWayId.of(2L, LinearReferenzierterAbschnitt.of(0, 1)),
			LinearReferenzierteOsmWayId.of(3L, LinearReferenzierterAbschnitt.of(0, 1))));
		kante2.setOsmWayIds(Set.of(
			LinearReferenzierteOsmWayId.of(4L, LinearReferenzierterAbschnitt.of(0, 1)),
			LinearReferenzierteOsmWayId.of(5L, LinearReferenzierterAbschnitt.of(0, 1)),
			LinearReferenzierteOsmWayId.of(6L, LinearReferenzierterAbschnitt.of(0, 1))));

		// act
		kantenRepository.truncateOsmWayIds();

		entityManager.flush();
		entityManager.clear();

		// assert
		assertThat(kantenRepository.findKanteByQuelle(QuellSystem.DLM).map(Kante::getOsmWayIds))
			.containsExactlyInAnyOrder(Collections.emptySet(), Collections.emptySet());
	}

	@Test
	void testeGetKantenInOrganisationsbereichEagerFetchNetzklassen() {
		Kante kante1 = kantenRepository
			.save(KanteTestDataProvider.withDefaultValuesAndQuelle(QuellSystem.DLM)
				.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue().netzklassen(
					Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_FREIZEIT, Netzklasse.RADNETZ_ZIELNETZ,
						Netzklasse.KOMMUNALNETZ_ALLTAG))
					.build())
				.geometry(GeometryTestdataProvider.createLineString(new Coordinate(10, 10), new Coordinate(20, 20)))
				.build());
		Kante kante2 = kantenRepository
			.save(KanteTestDataProvider.withDefaultValuesAndQuelle(QuellSystem.DLM)
				.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue().netzklassen(
					Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_ZIELNETZ,
						Netzklasse.KOMMUNALNETZ_ALLTAG))
					.build())
				.geometry(GeometryTestdataProvider.createLineString(new Coordinate(10, 20), new Coordinate(20, 20)))
				.build());
		entityManager.flush();
		entityManager.clear();

		// act
		Set<Kante> result = kantenRepository.getKantenInOrganisationsbereichEagerFetchNetzklassen(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
				.bereich(GeometryTestdataProvider.createQuadratischerBereich(0, 0, 100, 100))
				.build());

		// assert
		assertThat(result).hasSize(2);
		assertThat(result).containsExactlyInAnyOrder(kante1, kante2);
		assertThat(result.stream().map(Kante::getKantenAttributGruppe).map(KantenAttributGruppe::getNetzklassen))
			.containsExactlyInAnyOrder(
				Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_FREIZEIT, Netzklasse.RADNETZ_ZIELNETZ,
					Netzklasse.KOMMUNALNETZ_ALLTAG),
				Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_ZIELNETZ,
					Netzklasse.KOMMUNALNETZ_ALLTAG));
	}

	@Test
	public void testCreateOrRefreshRadVisNetzMaterializedView() throws SQLException {
		// Arrange
		Knoten vonKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(400000, 5000010),
			QuellSystem.DLM)
			.build();
		Knoten nachKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(400000, 5000020),
			QuellSystem.DLM)
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
			.strassenkategorieRIN(StrassenkategorieRIN.NAHRAEUMIG)
			.strassenquerschnittRASt06(StrassenquerschnittRASt06.ANBAUFREIE_STRASSE)
			.status(Status.defaultWert())
			.build();

		Verwaltungseinheit gebietskoerperschaft = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().name("tolle Organisation")
				.organisationsArt(
					OrganisationsArt.KREIS)
				.build());

		entityManager.flush();
		entityManager.clear();

		Kante kante = Kante.builder()
			.ursprungsfeatureTechnischeID("123")
			.vonKnoten(vonKnoten)
			.nachKnoten(nachKnoten)
			.geometry(lineString)
			.isZweiseitig(true)
			.quelle(QuellSystem.DLM)
			.dlmId(DlmId.of("dlmId"))
			.kantenAttributGruppe(
				new KantenAttributGruppe(kantenAttribute, new HashSet<>(), new HashSet<>()))
			.fahrtrichtungAttributGruppe(new FahrtrichtungAttributGruppe(Richtung.BEIDE_RICHTUNGEN, true))
			.zustaendigkeitAttributGruppe(new ZustaendigkeitAttributGruppe(List.of(
				new ZustaendigkeitAttribute(LinearReferenzierterAbschnitt.of(0, 1), gebietskoerperschaft,
					gebietskoerperschaft,
					gebietskoerperschaft,
					VereinbarungsKennung.of("123")))))
			.geschwindigkeitAttributGruppe(GeschwindigkeitAttributGruppe.builder()
				.geschwindigkeitAttribute(
					List.of(GeschwindigkeitAttribute.builder()
						.ortslage(KantenOrtslage.INNERORTS)
						.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_100_KMH)
						.build()))
				.build())
			.fuehrungsformAttributGruppe(
				new FuehrungsformAttributGruppe(List
					.of(new FuehrungsformAttribute(LinearReferenzierterAbschnitt.of(0, 1),
						BelagArt.ASPHALT,
						Oberflaechenbeschaffenheit.ANLASS_ZUR_INTENSIVEN_BEOBACHTUNG_UND_ANALYSE,
						Bordstein.KEINE_ABSENKUNG,
						Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND,
						KfzParkenTyp.LAENGS_PARKEN,
						KfzParkenForm.HALBES_GEHWEGPARKEN_UNMARKIERT,
						Laenge.of(1),
						Benutzungspflicht.VORHANDEN,
						Beschilderung.UNBEKANNT,
						Set.of(Schadenart.ABPLATZUNGEN_SCHLAGLOECHER),
						Absenkung.UNBEKANNT,
						null,
						null,
						null,
						null,
						TrennstreifenForm.UNBEKANNT,
						TrennstreifenForm.UNBEKANNT)),
					true))
			.build();

		kantenRepository.save(kante).getId();

		entityManager.flush();
		entityManager.clear();

		// Act
		kantenRepository.refreshNetzMaterializedViews();

		List<Map<String, Object>> resultList = jdbcTemplate.queryForList(
			"SELECT * FROM " + KantenRepository.GEOSERVER_RADVISNETZ_MAT_VIEW_NAME);

		// Assert

		PGobject geometry = PostGisHelper.getPGobject(kante.getGeometry());

		Map<String, Object> expected = new HashMap<>() {
			{
				put("id", kante.getId());
				put("geometry", geometry);
				// KantenAttributegruppe
				put("dtv_fussverkehr", kantenAttribute.getDtvFussverkehr().map(VerkehrStaerke::getValue).orElse(null));
				put("dtv_pkw", kantenAttribute.getDtvPkw().map(VerkehrStaerke::getValue).orElse(null));
				put("dtv_radverkehr", kantenAttribute.getDtvRadverkehr().map(VerkehrStaerke::getValue).orElse(null));
				put("kommentar", kantenAttribute.getKommentar().map(Kommentar::getValue).orElse(null));
				put("laenge_manuell_erfasst",
					kantenAttribute.getLaengeManuellErfasst().map(Laenge::getValue).map(BigDecimal::valueOf)
						.orElse(null));
				put("strassen_name", kantenAttribute.getStrassenName().map(StrassenName::getValue).orElse(null));
				put("strassen_nummer", kantenAttribute.getStrassenNummer().map(StrassenNummer::getValue).orElse(null));
				put("sv", kantenAttribute.getSv().map(VerkehrStaerke::getValue).orElse(null));
				put("wege_niveau", kantenAttribute.getWegeNiveau().map(Enum::name).orElse(""));
				put("gemeinde_name", null);
				put("landkreis_name", null);
				put("beleuchtung", kantenAttribute.getBeleuchtung().name());
				put("umfeld", kantenAttribute.getUmfeld().name());
				put("strassenkategorierin", kantenAttribute.getStrassenkategorieRIN().map(Enum::name).orElse(""));
				put("strassenquerschnittrast06", kantenAttribute.getStrassenquerschnittRASt06().name());
				put("status", kantenAttribute.getStatus().name());

				Set<Netzklasse> netzklassen = kante.getKantenAttributGruppe().getNetzklassen();
				put("netzklassen", netzklassen.isEmpty() ? null
					: netzklassen.stream().map(Netzklasse::name)
						.collect(Collectors.joining(";")));
				Set<IstStandard> istStandards = kante.getKantenAttributGruppe().getIstStandards();
				put("standards", istStandards.isEmpty() ? null
					: istStandards.stream().map(IstStandard::name)
						.collect(Collectors.joining(";")));
				// fuehrungsform attribute

				put("radverkehrsfuehrung", Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND.name());
				put("breite", BigDecimal.valueOf(Laenge.of(1).getValue()));
				put("parken_typ", KfzParkenTyp.LAENGS_PARKEN.name());
				put("parken_form", KfzParkenForm.HALBES_GEHWEGPARKEN_UNMARKIERT.name());
				put("bordstein", Bordstein.KEINE_ABSENKUNG.name());
				put("belag_art", BelagArt.ASPHALT.name());
				put("beschilderung", Beschilderung.UNBEKANNT.name());
				put("absenkung", Absenkung.UNBEKANNT.name());
				put("schaeden", Schadenart.ABPLATZUNGEN_SCHLAGLOECHER.name());
				put("oberflaechenbeschaffenheit",
					Oberflaechenbeschaffenheit.ANLASS_ZUR_INTENSIVEN_BEOBACHTUNG_UND_ANALYSE.name());
				put("benutzungspflicht", Benutzungspflicht.VORHANDEN.name());

				// -- geschwindigkeitsattribute

				put("ortslage", KantenOrtslage.INNERORTS.name());
				put("hoechstgeschwindigkeit", Hoechstgeschwindigkeit.MAX_100_KMH.name());
				put("abweichende_hoechstgeschwindigkeit_gegen_stationierungsrichtung", null);

				// -- zustaendigkeitattribute

				put("baulast_traeger", gebietskoerperschaft.getName());
				put("baulast_traeger_orga_typ", gebietskoerperschaft.getOrganisationsArt().name());
				put("unterhalts_zustaendiger", gebietskoerperschaft.getName());
				put("unterhalts_zustaendiger_orga_typ", gebietskoerperschaft.getOrganisationsArt().name());
				put("erhalts_zustaendiger", gebietskoerperschaft.getName());
				put("erhalts_zustaendiger_orga_typ", gebietskoerperschaft.getOrganisationsArt().name());
				put("vereinbarungs_kennung", VereinbarungsKennung.of("123").getValue());

				// -- fahrtrichtungattribute
				put("fahrtrichtung_links", Richtung.BEIDE_RICHTUNGEN.name());
				put("fahrtrichtung_rechts", Richtung.BEIDE_RICHTUNGEN.name());
				put("is_zweiseitig", Boolean.TRUE);
				put("hoechstenk", null);
			}
		};

		assertThat(resultList).hasSize(1);
		assertThat(resultList).contains(expected);
	}

	@Test
	public void testCreateOrRefreshRadVisNetzMaterializedView_hoechsteNK_AlltagFreizeitRadNETZ() {
		// Arrange
		Set<Netzklasse> netzklassen = Set.of(
			Netzklasse.RADNETZ_ALLTAG,
			Netzklasse.RADNETZ_FREIZEIT,
			Netzklasse.KOMMUNALNETZ_ALLTAG,
			Netzklasse.KREISNETZ_FREIZEIT,
			Netzklasse.RADSCHNELLVERBINDUNG);

		Kante kante = KanteTestDataProvider.withCoordinates(
			new Coordinate[] { new Coordinate(400000, 5000010), new Coordinate(400000, 5000020) })
			.quelle(QuellSystem.DLM)
			.dlmId(DlmId.of("dlmId"))
			.kantenAttributGruppe(
				new KantenAttributGruppe(
					KantenAttributeTestDataProvider.withLeereGrundnetzAttribute().build(),
					netzklassen,
					new HashSet<>()))
			.build();

		kantenRepository.save(kante).getId();

		entityManager.flush();
		entityManager.clear();

		// Act
		kantenRepository.refreshNetzMaterializedViews();

		List<Map<String, Object>> resultList = jdbcTemplate.queryForList(
			"SELECT * FROM " + KantenRepository.GEOSERVER_RADVISNETZ_MAT_VIEW_NAME);

		// Assert
		Map<String, Object> expected = new HashMap<>() {
			{
				put("id", kante.getId());
				put("hoechstenk", "Alltag und Freizeit (RadNETZ)");
			}
		};

		assertThat(resultList).hasSize(1);
		assertThat(resultList.get(0)).containsAllEntriesOf(expected);
	}

	@Test
	public void testCreateOrRefreshRadVisNetzMaterializedView_hoechsteNK_KreisNetz() {
		// Arrange
		Set<Netzklasse> netzklassen = Set.of(
			Netzklasse.KREISNETZ_FREIZEIT,
			Netzklasse.KOMMUNALNETZ_ALLTAG);

		Kante kante = KanteTestDataProvider.withCoordinates(
			new Coordinate[] { new Coordinate(400000, 5000010), new Coordinate(400000, 5000020) })
			.quelle(QuellSystem.DLM)
			.dlmId(DlmId.of("dlmId"))
			.kantenAttributGruppe(
				new KantenAttributGruppe(
					KantenAttributeTestDataProvider.withLeereGrundnetzAttribute().build(),
					netzklassen,
					new HashSet<>()))
			.build();

		kantenRepository.save(kante).getId();

		entityManager.flush();
		entityManager.clear();

		// Act
		kantenRepository.refreshNetzMaterializedViews();

		List<Map<String, Object>> resultList = jdbcTemplate.queryForList(
			"SELECT * FROM " + KantenRepository.GEOSERVER_RADVISNETZ_MAT_VIEW_NAME);

		// Assert
		Map<String, Object> expected = new HashMap<>() {
			{
				put("id", kante.getId());
				put("hoechstenk", "Freizeit (Kreisnetz)");
			}
		};

		assertThat(resultList).hasSize(1);
		assertThat(resultList.get(0)).containsAllEntriesOf(expected);
	}

	@Test
	public void testCreateOrRefreshRadVisNetzAbschnitteMaterializedView_kanteZweiseitig() throws SQLException {
		// Arrange
		Knoten vonKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(400000, 5000010),
			QuellSystem.DLM)
			.build();
		Knoten nachKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(400000, 5000020),
			QuellSystem.DLM)
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
			.strassenkategorieRIN(StrassenkategorieRIN.NAHRAEUMIG)
			.strassenquerschnittRASt06(StrassenquerschnittRASt06.ANBAUFREIE_STRASSE)
			.status(Status.defaultWert())
			.build();

		Verwaltungseinheit gebietskoerperschaft = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().name("tolle Organisation")
				.organisationsArt(
					OrganisationsArt.KREIS)
				.build());

		entityManager.flush();
		entityManager.clear();

		Kante kante = Kante.builder()
			.ursprungsfeatureTechnischeID("123")
			.vonKnoten(vonKnoten)
			.nachKnoten(nachKnoten)
			.geometry(lineString)
			.isZweiseitig(true)
			.quelle(QuellSystem.DLM)
			.dlmId(DlmId.of("dlmId"))
			.kantenAttributGruppe(
				new KantenAttributGruppe(kantenAttribute, new HashSet<>(), new HashSet<>()))
			.fahrtrichtungAttributGruppe(
				new FahrtrichtungAttributGruppe(Richtung.BEIDE_RICHTUNGEN, Richtung.IN_RICHTUNG, true))
			.zustaendigkeitAttributGruppe(new ZustaendigkeitAttributGruppe(List.of(
				new ZustaendigkeitAttribute(LinearReferenzierterAbschnitt.of(0, 0.3), gebietskoerperschaft,
					gebietskoerperschaft,
					gebietskoerperschaft,
					VereinbarungsKennung.of("123")),
				new ZustaendigkeitAttribute(LinearReferenzierterAbschnitt.of(0.3, 1), gebietskoerperschaft,
					gebietskoerperschaft,
					gebietskoerperschaft,
					VereinbarungsKennung.of("456")))))
			.geschwindigkeitAttributGruppe(GeschwindigkeitAttributGruppe.builder()
				.geschwindigkeitAttribute(
					List.of(
						GeschwindigkeitAttribute.builder()
							.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.5))
							.ortslage(KantenOrtslage.INNERORTS)
							.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_100_KMH)
							.build(),
						GeschwindigkeitAttribute.builder()
							.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.5, 1))
							.ortslage(KantenOrtslage.AUSSERORTS)
							.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.UEBER_100_KMH)
							.build()))
				.build())
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppe.builder()
					.fuehrungsformAttributeLinks(
						List.of(
							new FuehrungsformAttribute(
								LinearReferenzierterAbschnitt.of(0, 0.7),
								BelagArt.ASPHALT,
								Oberflaechenbeschaffenheit.ANLASS_ZUR_INTENSIVEN_BEOBACHTUNG_UND_ANALYSE,
								Bordstein.KEINE_ABSENKUNG,
								Radverkehrsfuehrung.SONDERWEG_RADWEG_STRASSENBEGLEITEND,
								KfzParkenTyp.LAENGS_PARKEN,
								KfzParkenForm.HALBES_GEHWEGPARKEN_UNMARKIERT,
								Laenge.of(1),
								Benutzungspflicht.VORHANDEN,
								Beschilderung.UNBEKANNT,
								Collections.emptySet(),
								Absenkung.UNBEKANNT,
								Laenge.of(3.5),
								Laenge.of(2.5),
								TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN,
								TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR,
								TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN,
								TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART

							),
							new FuehrungsformAttribute(
								LinearReferenzierterAbschnitt.of(0.7, 1),
								BelagArt.BETON,
								Oberflaechenbeschaffenheit.ANLASS_ZUR_INTENSIVEN_BEOBACHTUNG_UND_ANALYSE,
								Bordstein.KEINE_ABSENKUNG,
								Radverkehrsfuehrung.SONDERWEG_RADWEG_STRASSENBEGLEITEND,
								KfzParkenTyp.LAENGS_PARKEN,
								KfzParkenForm.HALBES_GEHWEGPARKEN_UNMARKIERT,
								Laenge.of(1),
								Benutzungspflicht.VORHANDEN,
								Beschilderung.UNBEKANNT,
								Collections.emptySet(),
								Absenkung.UNBEKANNT,
								Laenge.of(3.5),
								Laenge.of(2.5),
								TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN,
								TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR,
								TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN,
								TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART

							)))
					.fuehrungsformAttributeRechts(
						List.of(
							new FuehrungsformAttribute(
								LinearReferenzierterAbschnitt.of(0, 0.8),
								BelagArt.NATURSTEINPFLASTER,
								Oberflaechenbeschaffenheit.ANLASS_ZUR_INTENSIVEN_BEOBACHTUNG_UND_ANALYSE,
								Bordstein.KEINE_ABSENKUNG,
								Radverkehrsfuehrung.SONDERWEG_RADWEG_STRASSENBEGLEITEND,
								KfzParkenTyp.LAENGS_PARKEN,
								KfzParkenForm.HALBES_GEHWEGPARKEN_UNMARKIERT,
								Laenge.of(1),
								Benutzungspflicht.VORHANDEN,
								Beschilderung.UNBEKANNT,
								Collections.emptySet(),
								Absenkung.UNBEKANNT,
								Laenge.of(3.5),
								Laenge.of(2.5),
								TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN,
								TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR,
								TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN,
								TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART

							),
							new FuehrungsformAttribute(
								LinearReferenzierterAbschnitt.of(0.8, 1),
								BelagArt.WASSERGEBUNDENE_DECKE,
								Oberflaechenbeschaffenheit.ANLASS_ZUR_INTENSIVEN_BEOBACHTUNG_UND_ANALYSE,
								Bordstein.KEINE_ABSENKUNG,
								Radverkehrsfuehrung.SONDERWEG_RADWEG_STRASSENBEGLEITEND,
								KfzParkenTyp.LAENGS_PARKEN,
								KfzParkenForm.HALBES_GEHWEGPARKEN_UNMARKIERT,
								Laenge.of(1),
								Benutzungspflicht.VORHANDEN,
								Beschilderung.UNBEKANNT,
								Collections.emptySet(),
								Absenkung.UNBEKANNT,
								Laenge.of(3.5),
								Laenge.of(2.5),
								TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN,
								TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR,
								TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN,
								TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART

							)))
					.isZweiseitig(true)
					.build())
			.build();

		kantenRepository.save(kante).getId();

		entityManager.flush();
		entityManager.clear();

		// Act
		kantenRepository.refreshNetzMaterializedViews();

		List<Map<String, Object>> resultList = jdbcTemplate.queryForList(
			"SELECT * FROM " + KantenRepository.GEOSERVER_RADVISNETZ_ABSCHNITTE_MAT_VIEW_NAME);

		// Assert

		Map<String, Object> expectedCommon = new HashMap<>() {
			{
				put("kante_id", kante.getId());
				// KantenAttributegruppe
				put("dtv_fussverkehr", kantenAttribute.getDtvFussverkehr().map(VerkehrStaerke::getValue).orElse(null));
				put("dtv_pkw", kantenAttribute.getDtvPkw().map(VerkehrStaerke::getValue).orElse(null));
				put("dtv_radverkehr", kantenAttribute.getDtvRadverkehr().map(VerkehrStaerke::getValue).orElse(null));
				put("kommentar", kantenAttribute.getKommentar().map(Kommentar::getValue).orElse(null));
				put("laenge_manuell_erfasst",
					kantenAttribute.getLaengeManuellErfasst().map(Laenge::getValue).map(BigDecimal::valueOf)
						.orElse(null));
				put("strassen_name", kantenAttribute.getStrassenName().map(StrassenName::getValue).orElse(null));
				put("strassen_nummer", kantenAttribute.getStrassenNummer().map(StrassenNummer::getValue).orElse(null));
				put("sv", kantenAttribute.getSv().map(VerkehrStaerke::getValue).orElse(null));
				put("wege_niveau", kantenAttribute.getWegeNiveau().map(Enum::name).orElse(""));
				put("gemeinde_name", null);
				put("landkreis_name", null);
				put("beleuchtung", kantenAttribute.getBeleuchtung().name());
				put("umfeld", kantenAttribute.getUmfeld().name());
				put("strassenkategorierin", kantenAttribute.getStrassenkategorieRIN().map(Enum::name).orElse(""));
				put("strassenquerschnittrast06", kantenAttribute.getStrassenquerschnittRASt06().name());
				put("status", kantenAttribute.getStatus().name());

				Set<Netzklasse> netzklassen = kante.getKantenAttributGruppe().getNetzklassen();
				put("netzklassen", netzklassen.isEmpty() ? null
					: netzklassen.stream().map(Netzklasse::name)
						.collect(Collectors.joining(";")));
				Set<IstStandard> istStandards = kante.getKantenAttributGruppe().getIstStandards();
				put("standards", istStandards.isEmpty() ? null
					: istStandards.stream().map(IstStandard::name)
						.collect(Collectors.joining(";")));

				// fuehrungsform attribute
				put("radverkehrsfuehrung", Radverkehrsfuehrung.SONDERWEG_RADWEG_STRASSENBEGLEITEND.name());
				put("absenkung", "UNBEKANNT");
				put("beschilderung", "UNBEKANNT");
				put("schaeden", "");
				put("breite", BigDecimal.valueOf(Laenge.of(1).getValue()));
				put("parken_typ", KfzParkenTyp.LAENGS_PARKEN.name());
				put("parken_form", KfzParkenForm.HALBES_GEHWEGPARKEN_UNMARKIERT.name());
				put("bordstein", Bordstein.KEINE_ABSENKUNG.name());
				put("oberflaechenbeschaffenheit",
					Oberflaechenbeschaffenheit.ANLASS_ZUR_INTENSIVEN_BEOBACHTUNG_UND_ANALYSE.name());
				put("benutzungspflicht", Benutzungspflicht.VORHANDEN.name());
				// belag_art variiert:
				// Links 0-0.7: ASPHALT,
				// Links 0.7-1 BETON,
				// Rechts 0-0.8: NATURSTEINPFLASTER,
				// Rechts 0.8-1 WASSERGEBUNDENE_DECKE

				// -- geschwindigkeitsattribute
				put("abweichende_hoechstgeschwindigkeit_gegen_stationierungsrichtung", null);
				// ortslage & hoechstgeschwindigkeit variieren (0-0.5: INNERORTS/MAX_100, 0.5-1: AUSSERORTS/UERBER_100)

				// -- zustaendigkeitattribute
				put("baulast_traeger",
					gebietskoerperschaft.getName() + " (" + gebietskoerperschaft.getOrganisationsArt().name() + ")");
				put("baulast_traeger_art", gebietskoerperschaft.getOrganisationsArt().name());
				put("unterhalts_zustaendiger",
					gebietskoerperschaft.getName() + " (" + gebietskoerperschaft.getOrganisationsArt().name() + ")");
				put("erhalts_zustaendiger",
					gebietskoerperschaft.getName() + " (" + gebietskoerperschaft.getOrganisationsArt().name() + ")");
				// vereinbarungs_kennung variiert (0-0.3: "123", 0.3-1:"456")

				// -- fahrtrichtungattribute
				// fahrtrichtung varriert: (Links: Richtung.BEIDE_RICHTUNGEN, Rechts: Richtung.IN_RICHTUNG)

				// keine Auditing Daten vorhanden, da Kante im selben DB-Commit gespeichert wurde
				// (Auditing-Daten werden erst nach Commit gespeichert)
				put("letzte_aenderung", null);
			}
		};

		// LRs LINKS: 0, 0.3, 0.5, 0.7, 1
		// LRs RECHTS: 0, 0.3, 0.5, 0.8, 1

		PGobject geometry_links_0__0_3 = PostGisHelper.getPGobject(
			GeometryTestdataProvider.getAbschnitt(kante.getGeometry(), LinearReferenzierterAbschnitt.of(0, 0.3)));

		Map<String, Object> expected_links_0__0_3 = new HashMap<>(expectedCommon) {
			{
				put("geometry", geometry_links_0__0_3);
				put("balm_id", "dlmId_0-0.3");
				put("seite", "LINKS");
				put("id", kante.getId() + "_0-0.3_L");

				put("belag_art", BelagArt.ASPHALT.name());

				put("ortslage", KantenOrtslage.INNERORTS.name());
				put("hoechstgeschwindigkeit", Hoechstgeschwindigkeit.MAX_100_KMH.name());

				put("vereinbarungs_kennung", VereinbarungsKennung.of("123").getValue());

				put("fahrtrichtung", Richtung.BEIDE_RICHTUNGEN.name());

				put("sts_b_l", BigDecimal.valueOf(Laenge.of(2.5).getValue()));
				put("sts_b_r", BigDecimal.valueOf(Laenge.of(3.5).getValue()));
				put("sts_f_l", TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART.name());
				put("sts_f_r", TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN.name());
				put("sts_t_l", TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR.name());
				put("sts_t_r", TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN.name());
			}
		};

		PGobject geometry_links_0_3__0_5 = PostGisHelper.getPGobject(
			GeometryTestdataProvider.getAbschnitt(kante.getGeometry(), LinearReferenzierterAbschnitt.of(0.3, 0.5)));

		Map<String, Object> expected_links_0_3__0_5 = new HashMap<>(expectedCommon) {
			{
				put("geometry", geometry_links_0_3__0_5);
				put("balm_id", "dlmId_0.3-0.5");
				put("seite", "LINKS");
				put("id", kante.getId() + "_0.3-0.5_L");

				put("belag_art", BelagArt.ASPHALT.name());

				put("ortslage", KantenOrtslage.INNERORTS.name());
				put("hoechstgeschwindigkeit", Hoechstgeschwindigkeit.MAX_100_KMH.name());

				put("vereinbarungs_kennung", VereinbarungsKennung.of("456").getValue());

				put("fahrtrichtung", Richtung.BEIDE_RICHTUNGEN.name());

				put("sts_b_l", BigDecimal.valueOf(Laenge.of(2.5).getValue()));
				put("sts_b_r", BigDecimal.valueOf(Laenge.of(3.5).getValue()));
				put("sts_f_l", TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART.name());
				put("sts_f_r", TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN.name());
				put("sts_t_l", TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR.name());
				put("sts_t_r", TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN.name());
			}
		};

		PGobject geometry_links_0_5__0_7 = PostGisHelper.getPGobject(
			GeometryTestdataProvider.getAbschnitt(kante.getGeometry(), LinearReferenzierterAbschnitt.of(0.5, 0.7)));

		Map<String, Object> expected_links_0_5__0_7 = new HashMap<>(expectedCommon) {
			{
				put("geometry", geometry_links_0_5__0_7);
				put("balm_id", "dlmId_0.5-0.7");
				put("seite", "LINKS");
				put("id", kante.getId() + "_0.5-0.7_L");

				put("belag_art", BelagArt.ASPHALT.name());

				put("ortslage", KantenOrtslage.AUSSERORTS.name());
				put("hoechstgeschwindigkeit", Hoechstgeschwindigkeit.UEBER_100_KMH.name());

				put("vereinbarungs_kennung", VereinbarungsKennung.of("456").getValue());

				put("fahrtrichtung", Richtung.BEIDE_RICHTUNGEN.name());

				put("sts_b_l", BigDecimal.valueOf(Laenge.of(2.5).getValue()));
				put("sts_b_r", BigDecimal.valueOf(Laenge.of(3.5).getValue()));
				put("sts_f_l", TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART.name());
				put("sts_f_r", TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN.name());
				put("sts_t_l", TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR.name());
				put("sts_t_r", TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN.name());
			}
		};

		PGobject geometry_links_0_7__1 = PostGisHelper.getPGobject(
			GeometryTestdataProvider.getAbschnitt(kante.getGeometry(), LinearReferenzierterAbschnitt.of(0.7, 1)));

		Map<String, Object> expected_links_0_7__1 = new HashMap<>(expectedCommon) {
			{
				put("geometry", geometry_links_0_7__1);
				put("balm_id", "dlmId_0.7-1");
				put("seite", "LINKS");
				put("id", kante.getId() + "_0.7-1_L");

				put("belag_art", BelagArt.BETON.name());

				put("ortslage", KantenOrtslage.AUSSERORTS.name());
				put("hoechstgeschwindigkeit", Hoechstgeschwindigkeit.UEBER_100_KMH.name());

				put("vereinbarungs_kennung", VereinbarungsKennung.of("456").getValue());

				put("fahrtrichtung", Richtung.BEIDE_RICHTUNGEN.name());

				put("sts_b_l", BigDecimal.valueOf(Laenge.of(2.5).getValue()));
				put("sts_b_r", BigDecimal.valueOf(Laenge.of(3.5).getValue()));
				put("sts_f_l", TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART.name());
				put("sts_f_r", TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN.name());
				put("sts_t_l", TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR.name());
				put("sts_t_r", TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN.name());
			}
		};

		PGobject geometry_rechts_0__0_3 = PostGisHelper.getPGobject(
			GeometryTestdataProvider.getAbschnitt(kante.getGeometry(), LinearReferenzierterAbschnitt.of(0, 0.3)));

		Map<String, Object> expected_rechts_0__0_3 = new HashMap<>(expectedCommon) {
			{
				put("geometry", geometry_rechts_0__0_3);
				put("balm_id", "dlmId_0-0.3");
				put("seite", "RECHTS");
				put("id", kante.getId() + "_0-0.3_R");

				put("belag_art", BelagArt.NATURSTEINPFLASTER.name());

				put("ortslage", KantenOrtslage.INNERORTS.name());
				put("hoechstgeschwindigkeit", Hoechstgeschwindigkeit.MAX_100_KMH.name());

				put("vereinbarungs_kennung", VereinbarungsKennung.of("123").getValue());

				put("fahrtrichtung", Richtung.IN_RICHTUNG.name());

				put("sts_b_l", BigDecimal.valueOf(Laenge.of(2.5).getValue()));
				put("sts_b_r", BigDecimal.valueOf(Laenge.of(3.5).getValue()));
				put("sts_f_l", TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART.name());
				put("sts_f_r", TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN.name());
				put("sts_t_l", TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR.name());
				put("sts_t_r", TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN.name());
			}
		};

		PGobject geometry_rechts_0_3__0_5 = PostGisHelper.getPGobject(
			GeometryTestdataProvider.getAbschnitt(kante.getGeometry(), LinearReferenzierterAbschnitt.of(0.3, 0.5)));

		Map<String, Object> expected_rechts_0_3__0_5 = new HashMap<>(expectedCommon) {
			{
				put("geometry", geometry_rechts_0_3__0_5);
				put("balm_id", "dlmId_0.3-0.5");
				put("seite", "RECHTS");
				put("id", kante.getId() + "_0.3-0.5_R");

				put("belag_art", BelagArt.NATURSTEINPFLASTER.name());

				put("ortslage", KantenOrtslage.INNERORTS.name());
				put("hoechstgeschwindigkeit", Hoechstgeschwindigkeit.MAX_100_KMH.name());

				put("vereinbarungs_kennung", VereinbarungsKennung.of("456").getValue());

				put("fahrtrichtung", Richtung.IN_RICHTUNG.name());

				put("sts_b_l", BigDecimal.valueOf(Laenge.of(2.5).getValue()));
				put("sts_b_r", BigDecimal.valueOf(Laenge.of(3.5).getValue()));
				put("sts_f_l", TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART.name());
				put("sts_f_r", TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN.name());
				put("sts_t_l", TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR.name());
				put("sts_t_r", TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN.name());
			}
		};

		PGobject geometry_rechts_0_5__0_8 = PostGisHelper.getPGobject(
			GeometryTestdataProvider.getAbschnitt(kante.getGeometry(), LinearReferenzierterAbschnitt.of(0.5, 0.8)));

		Map<String, Object> expected_rechts_0_5__0_8 = new HashMap<>(expectedCommon) {
			{
				put("geometry", geometry_rechts_0_5__0_8);
				put("balm_id", "dlmId_0.5-0.8");
				put("seite", "RECHTS");
				put("id", kante.getId() + "_0.5-0.8_R");

				put("belag_art", BelagArt.NATURSTEINPFLASTER.name());

				put("ortslage", KantenOrtslage.AUSSERORTS.name());
				put("hoechstgeschwindigkeit", Hoechstgeschwindigkeit.UEBER_100_KMH.name());

				put("vereinbarungs_kennung", VereinbarungsKennung.of("456").getValue());

				put("fahrtrichtung", Richtung.IN_RICHTUNG.name());

				put("sts_b_l", BigDecimal.valueOf(Laenge.of(2.5).getValue()));
				put("sts_b_r", BigDecimal.valueOf(Laenge.of(3.5).getValue()));
				put("sts_f_l", TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART.name());
				put("sts_f_r", TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN.name());
				put("sts_t_l", TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR.name());
				put("sts_t_r", TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN.name());
			}
		};

		PGobject geometry_rechts_0_8__1 = PostGisHelper.getPGobject(
			GeometryTestdataProvider.getAbschnitt(kante.getGeometry(), LinearReferenzierterAbschnitt.of(0.8, 1)));

		Map<String, Object> expected_rechts_0_8__1 = new HashMap<>(expectedCommon) {
			{
				put("geometry", geometry_rechts_0_8__1);
				put("balm_id", "dlmId_0.8-1");
				put("seite", "RECHTS");
				put("id", kante.getId() + "_0.8-1_R");

				put("belag_art", BelagArt.WASSERGEBUNDENE_DECKE.name());

				put("ortslage", KantenOrtslage.AUSSERORTS.name());
				put("hoechstgeschwindigkeit", Hoechstgeschwindigkeit.UEBER_100_KMH.name());

				put("vereinbarungs_kennung", VereinbarungsKennung.of("456").getValue());

				put("fahrtrichtung", Richtung.IN_RICHTUNG.name());

				put("sts_b_l", BigDecimal.valueOf(Laenge.of(2.5).getValue()));
				put("sts_b_r", BigDecimal.valueOf(Laenge.of(3.5).getValue()));
				put("sts_f_l", TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART.name());
				put("sts_f_r", TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN.name());
				put("sts_t_l", TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR.name());
				put("sts_t_r", TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN.name());
			}
		};

		assertThat(resultList).hasSize(8);
		assertThat(resultList).contains(expected_links_0__0_3);
		assertThat(resultList).contains(expected_links_0_3__0_5);
		assertThat(resultList).contains(expected_links_0_5__0_7);
		assertThat(resultList).contains(expected_links_0_7__1);
		assertThat(resultList).contains(expected_rechts_0__0_3);
		assertThat(resultList).contains(expected_rechts_0_3__0_5);
		assertThat(resultList).contains(expected_rechts_0_5__0_8);
		assertThat(resultList).contains(expected_rechts_0_8__1);
	}

	@Test
	public void testCreateOrRefreshRadVisNetzAbschnitteMaterializedView_kanteEinseitig() throws SQLException {
		// Arrange
		Knoten vonKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(400000, 5000010),
			QuellSystem.DLM)
			.build();
		Knoten nachKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(400000, 5000020),
			QuellSystem.DLM)
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
			.strassenkategorieRIN(StrassenkategorieRIN.NAHRAEUMIG)
			.strassenquerschnittRASt06(StrassenquerschnittRASt06.ANBAUFREIE_STRASSE)
			.status(Status.defaultWert())
			.build();

		Verwaltungseinheit gebietskoerperschaft = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().name("tolle Organisation")
				.organisationsArt(
					OrganisationsArt.KREIS)
				.build());

		entityManager.flush();
		entityManager.clear();

		Kante kante = Kante.builder()
			.ursprungsfeatureTechnischeID("123")
			.vonKnoten(vonKnoten)
			.nachKnoten(nachKnoten)
			.geometry(lineString)
			.isZweiseitig(false)
			.quelle(QuellSystem.DLM)
			.dlmId(DlmId.of("dlmId"))
			.kantenAttributGruppe(
				new KantenAttributGruppe(kantenAttribute, new HashSet<>(), new HashSet<>()))
			.fahrtrichtungAttributGruppe(
				new FahrtrichtungAttributGruppe(Richtung.BEIDE_RICHTUNGEN, false))
			.zustaendigkeitAttributGruppe(new ZustaendigkeitAttributGruppe(List.of(
				new ZustaendigkeitAttribute(LinearReferenzierterAbschnitt.of(0, 0.3), gebietskoerperschaft,
					gebietskoerperschaft,
					gebietskoerperschaft,
					VereinbarungsKennung.of("123")),
				new ZustaendigkeitAttribute(LinearReferenzierterAbschnitt.of(0.3, 1), gebietskoerperschaft,
					gebietskoerperschaft,
					gebietskoerperschaft,
					VereinbarungsKennung.of("456")))))
			.geschwindigkeitAttributGruppe(GeschwindigkeitAttributGruppe.builder()
				.geschwindigkeitAttribute(
					List.of(
						GeschwindigkeitAttribute.builder()
							.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.5))
							.ortslage(KantenOrtslage.INNERORTS)
							.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_100_KMH)
							.build(),
						GeschwindigkeitAttribute.builder()
							.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.5, 1))
							.ortslage(KantenOrtslage.AUSSERORTS)
							.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.UEBER_100_KMH)
							.build()))
				.build())
			.fuehrungsformAttributGruppe(
				new FuehrungsformAttributGruppe(
					List.of(
						new FuehrungsformAttribute(
							LinearReferenzierterAbschnitt.of(0, 0.7),
							BelagArt.ASPHALT,
							Oberflaechenbeschaffenheit.ANLASS_ZUR_INTENSIVEN_BEOBACHTUNG_UND_ANALYSE,
							Bordstein.KEINE_ABSENKUNG,
							Radverkehrsfuehrung.SONDERWEG_RADWEG_STRASSENBEGLEITEND,
							KfzParkenTyp.LAENGS_PARKEN,
							KfzParkenForm.HALBES_GEHWEGPARKEN_UNMARKIERT,
							Laenge.of(1),
							Benutzungspflicht.VORHANDEN,
							Beschilderung.UNBEKANNT,
							Collections.emptySet(),
							Absenkung.UNBEKANNT,
							Laenge.of(3.5),
							Laenge.of(2.5),
							TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN,
							TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR,
							TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN,
							TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART

						),
						new FuehrungsformAttribute(
							LinearReferenzierterAbschnitt.of(0.7, 1),
							BelagArt.BETON,
							Oberflaechenbeschaffenheit.ANLASS_ZUR_INTENSIVEN_BEOBACHTUNG_UND_ANALYSE,
							Bordstein.KEINE_ABSENKUNG,
							Radverkehrsfuehrung.SONDERWEG_RADWEG_STRASSENBEGLEITEND,
							KfzParkenTyp.LAENGS_PARKEN,
							KfzParkenForm.HALBES_GEHWEGPARKEN_UNMARKIERT,
							Laenge.of(1),
							Benutzungspflicht.VORHANDEN,
							Beschilderung.UNBEKANNT,
							Collections.emptySet(),
							Absenkung.UNBEKANNT,
							Laenge.of(3.5),
							Laenge.of(2.5),
							TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN,
							TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR,
							TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN,
							TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART

						)), false))
			.build();

		kantenRepository.save(kante).getId();

		entityManager.flush();
		entityManager.clear();

		// Act
		kantenRepository.refreshNetzMaterializedViews();

		List<Map<String, Object>> resultList = jdbcTemplate.queryForList(
			"SELECT * FROM " + KantenRepository.GEOSERVER_RADVISNETZ_ABSCHNITTE_MAT_VIEW_NAME);

		// Assert

		Map<String, Object> expectedCommon = new HashMap<>() {
			{
				put("kante_id", kante.getId());
				// KantenAttributegruppe
				put("dtv_fussverkehr", kantenAttribute.getDtvFussverkehr().map(VerkehrStaerke::getValue).orElse(null));
				put("dtv_pkw", kantenAttribute.getDtvPkw().map(VerkehrStaerke::getValue).orElse(null));
				put("dtv_radverkehr", kantenAttribute.getDtvRadverkehr().map(VerkehrStaerke::getValue).orElse(null));
				put("kommentar", kantenAttribute.getKommentar().map(Kommentar::getValue).orElse(null));
				put("laenge_manuell_erfasst",
					kantenAttribute.getLaengeManuellErfasst().map(Laenge::getValue).map(BigDecimal::valueOf)
						.orElse(null));
				put("strassen_name", kantenAttribute.getStrassenName().map(StrassenName::getValue).orElse(null));
				put("strassen_nummer", kantenAttribute.getStrassenNummer().map(StrassenNummer::getValue).orElse(null));
				put("sv", kantenAttribute.getSv().map(VerkehrStaerke::getValue).orElse(null));
				put("wege_niveau", kantenAttribute.getWegeNiveau().map(Enum::name).orElse(""));
				put("gemeinde_name", null);
				put("landkreis_name", null);
				put("beleuchtung", kantenAttribute.getBeleuchtung().name());
				put("umfeld", kantenAttribute.getUmfeld().name());
				put("strassenkategorierin", kantenAttribute.getStrassenkategorieRIN().map(Enum::name).orElse(""));
				put("strassenquerschnittrast06", kantenAttribute.getStrassenquerschnittRASt06().name());
				put("status", kantenAttribute.getStatus().name());

				Set<Netzklasse> netzklassen = kante.getKantenAttributGruppe().getNetzklassen();
				put("netzklassen", netzklassen.isEmpty() ? null
					: netzklassen.stream().map(Netzklasse::name)
						.collect(Collectors.joining(";")));
				Set<IstStandard> istStandards = kante.getKantenAttributGruppe().getIstStandards();
				put("standards", istStandards.isEmpty() ? null
					: istStandards.stream().map(IstStandard::name)
						.collect(Collectors.joining(";")));

				// fuehrungsform attribute
				put("radverkehrsfuehrung", Radverkehrsfuehrung.SONDERWEG_RADWEG_STRASSENBEGLEITEND.name());
				put("absenkung", "UNBEKANNT");
				put("beschilderung", "UNBEKANNT");
				put("schaeden", "");
				put("breite", BigDecimal.valueOf(Laenge.of(1).getValue()));
				put("parken_typ", KfzParkenTyp.LAENGS_PARKEN.name());
				put("parken_form", KfzParkenForm.HALBES_GEHWEGPARKEN_UNMARKIERT.name());
				put("bordstein", Bordstein.KEINE_ABSENKUNG.name());
				put("oberflaechenbeschaffenheit",
					Oberflaechenbeschaffenheit.ANLASS_ZUR_INTENSIVEN_BEOBACHTUNG_UND_ANALYSE.name());
				put("benutzungspflicht", Benutzungspflicht.VORHANDEN.name());
				// belag_art variiert:
				// 0-0.7: ASPHALT,
				// 0.7-1 BETON,

				// -- geschwindigkeitsattribute
				put("abweichende_hoechstgeschwindigkeit_gegen_stationierungsrichtung", null);
				// ortslage & hoechstgeschwindigkeit variieren (0-0.5: INNERORTS/MAX_100, 0.5-1: AUSSERORTS/UERBER_100)

				// -- zustaendigkeitattribute
				put("baulast_traeger",
					gebietskoerperschaft.getName() + " (" + gebietskoerperschaft.getOrganisationsArt().name() + ")");
				put("baulast_traeger_art", gebietskoerperschaft.getOrganisationsArt().name());
				put("unterhalts_zustaendiger",
					gebietskoerperschaft.getName() + " (" + gebietskoerperschaft.getOrganisationsArt().name() + ")");
				put("erhalts_zustaendiger",
					gebietskoerperschaft.getName() + " (" + gebietskoerperschaft.getOrganisationsArt().name() + ")");
				// vereinbarungs_kennung variiert (0-0.3: "123", 0.3-1:"456")

				// -- fahrtrichtungattribute
				put("fahrtrichtung", Richtung.BEIDE_RICHTUNGEN.name());

				// keine Auditing Daten vorhanden, da Kante im selben DB-Commit gespeichert wurde
				// (Auditing-Daten werden erst nach Commit gespeichert)
				put("letzte_aenderung", null);
			}
		};

		// LRs LINKS: 0, 0.3, 0.5, 0.7, 1
		// LRs RECHTS: 0, 0.3, 0.5, 0.8, 1

		PGobject geometry_0__0_3 = PostGisHelper.getPGobject(
			GeometryTestdataProvider.getAbschnitt(kante.getGeometry(), LinearReferenzierterAbschnitt.of(0, 0.3)));

		Map<String, Object> expected_0__0_3 = new HashMap<>(expectedCommon) {
			{
				put("geometry", geometry_0__0_3);
				put("balm_id", "dlmId_0-0.3");
				put("seite", "BEIDSEITIG");
				put("id", kante.getId() + "_0-0.3");

				put("belag_art", BelagArt.ASPHALT.name());

				put("ortslage", KantenOrtslage.INNERORTS.name());
				put("hoechstgeschwindigkeit", Hoechstgeschwindigkeit.MAX_100_KMH.name());

				put("vereinbarungs_kennung", VereinbarungsKennung.of("123").getValue());

				put("sts_b_l", BigDecimal.valueOf(Laenge.of(2.5).getValue()));
				put("sts_b_r", BigDecimal.valueOf(Laenge.of(3.5).getValue()));
				put("sts_f_l", TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART.name());
				put("sts_f_r", TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN.name());
				put("sts_t_l", TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR.name());
				put("sts_t_r", TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN.name());
			}
		};

		PGobject geometry_0_3__0_5 = PostGisHelper.getPGobject(
			GeometryTestdataProvider.getAbschnitt(kante.getGeometry(), LinearReferenzierterAbschnitt.of(0.3, 0.5)));

		Map<String, Object> expected_0_3__0_5 = new HashMap<>(expectedCommon) {
			{
				put("geometry", geometry_0_3__0_5);
				put("balm_id", "dlmId_0.3-0.5");
				put("seite", "BEIDSEITIG");
				put("id", kante.getId() + "_0.3-0.5");

				put("belag_art", BelagArt.ASPHALT.name());

				put("ortslage", KantenOrtslage.INNERORTS.name());
				put("hoechstgeschwindigkeit", Hoechstgeschwindigkeit.MAX_100_KMH.name());

				put("vereinbarungs_kennung", VereinbarungsKennung.of("456").getValue());

				put("sts_b_l", BigDecimal.valueOf(Laenge.of(2.5).getValue()));
				put("sts_b_r", BigDecimal.valueOf(Laenge.of(3.5).getValue()));
				put("sts_f_l", TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART.name());
				put("sts_f_r", TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN.name());
				put("sts_t_l", TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR.name());
				put("sts_t_r", TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN.name());
			}
		};

		PGobject geometry_0_5__0_7 = PostGisHelper.getPGobject(
			GeometryTestdataProvider.getAbschnitt(kante.getGeometry(), LinearReferenzierterAbschnitt.of(0.5, 0.7)));

		Map<String, Object> expected_0_5__0_7 = new HashMap<>(expectedCommon) {
			{
				put("geometry", geometry_0_5__0_7);
				put("balm_id", "dlmId_0.5-0.7");
				put("seite", "BEIDSEITIG");
				put("id", kante.getId() + "_0.5-0.7");

				put("belag_art", BelagArt.ASPHALT.name());

				put("ortslage", KantenOrtslage.AUSSERORTS.name());
				put("hoechstgeschwindigkeit", Hoechstgeschwindigkeit.UEBER_100_KMH.name());

				put("vereinbarungs_kennung", VereinbarungsKennung.of("456").getValue());

				put("sts_b_l", BigDecimal.valueOf(Laenge.of(2.5).getValue()));
				put("sts_b_r", BigDecimal.valueOf(Laenge.of(3.5).getValue()));
				put("sts_f_l", TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART.name());
				put("sts_f_r", TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN.name());
				put("sts_t_l", TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR.name());
				put("sts_t_r", TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN.name());
			}
		};

		PGobject geometry_0_7__1 = PostGisHelper.getPGobject(
			GeometryTestdataProvider.getAbschnitt(kante.getGeometry(), LinearReferenzierterAbschnitt.of(0.7, 1)));

		Map<String, Object> expected_0_7__1 = new HashMap<>(expectedCommon) {
			{
				put("geometry", geometry_0_7__1);
				put("balm_id", "dlmId_0.7-1");
				put("seite", "BEIDSEITIG");
				put("id", kante.getId() + "_0.7-1");

				put("belag_art", BelagArt.BETON.name());

				put("ortslage", KantenOrtslage.AUSSERORTS.name());
				put("hoechstgeschwindigkeit", Hoechstgeschwindigkeit.UEBER_100_KMH.name());

				put("vereinbarungs_kennung", VereinbarungsKennung.of("456").getValue());

				put("sts_b_l", BigDecimal.valueOf(Laenge.of(2.5).getValue()));
				put("sts_b_r", BigDecimal.valueOf(Laenge.of(3.5).getValue()));
				put("sts_f_l", TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART.name());
				put("sts_f_r", TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN.name());
				put("sts_t_l", TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR.name());
				put("sts_t_r", TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN.name());

			}
		};

		assertThat(resultList).hasSize(4);
		assertThat(resultList).contains(expected_0__0_3);
		assertThat(resultList).contains(expected_0_3__0_5);
		assertThat(resultList).contains(expected_0_5__0_7);
		assertThat(resultList).contains(expected_0_7__1);
	}

	@Test
	public void testGeoserverBalmView_KanteEinseitig() throws SQLException {
		// Arrange
		Knoten vonKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(400000, 5000010),
			QuellSystem.DLM)
			.build();
		Knoten nachKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(400000, 5000020),
			QuellSystem.DLM)
			.build();

		LineString lineString = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
			.createLineString(new Coordinate[] { vonKnoten.getKoordinate(), nachKnoten.getKoordinate() });
		LineString lineString_3d = GeometryTestdataProvider.createLineString(
			new Coordinate(lineString.getStartPoint().getX(), lineString.getStartPoint().getY(), 666.),
			new Coordinate(lineString.getEndPoint().getX(), lineString.getEndPoint().getY(), 777.));

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
			.strassenkategorieRIN(StrassenkategorieRIN.NAHRAEUMIG)
			.strassenquerschnittRASt06(StrassenquerschnittRASt06.ANBAUFREIE_STRASSE)
			.status(Status.defaultWert())
			.build();

		Verwaltungseinheit gebietskoerperschaft = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().name("tolle Organisation")
				.organisationsArt(
					OrganisationsArt.KREIS)
				.build());

		entityManager.flush();
		entityManager.clear();

		Kante kante = Kante.builder()
			.ursprungsfeatureTechnischeID("123")
			.vonKnoten(vonKnoten)
			.nachKnoten(nachKnoten)
			.geometry(lineString)
			.isZweiseitig(false)
			.quelle(QuellSystem.DLM)
			.dlmId(DlmId.of("dlmId"))
			.kantenAttributGruppe(
				new KantenAttributGruppe(kantenAttribute, new HashSet<>(Set.of(Netzklasse.RADNETZ_ALLTAG)),
					new HashSet<>()))
			.fahrtrichtungAttributGruppe(
				new FahrtrichtungAttributGruppe(Richtung.BEIDE_RICHTUNGEN, false))
			.zustaendigkeitAttributGruppe(new ZustaendigkeitAttributGruppe(List.of(
				new ZustaendigkeitAttribute(LinearReferenzierterAbschnitt.of(0, 0.3), gebietskoerperschaft,
					gebietskoerperschaft,
					gebietskoerperschaft,
					VereinbarungsKennung.of("123")),
				new ZustaendigkeitAttribute(LinearReferenzierterAbschnitt.of(0.3, 1), gebietskoerperschaft,
					gebietskoerperschaft,
					gebietskoerperschaft,
					VereinbarungsKennung.of("456")))))
			.geschwindigkeitAttributGruppe(GeschwindigkeitAttributGruppe.builder()
				.geschwindigkeitAttribute(
					List.of(
						GeschwindigkeitAttribute.builder()
							.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.5))
							.ortslage(KantenOrtslage.INNERORTS)
							.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_100_KMH)
							.build(),
						GeschwindigkeitAttribute.builder()
							.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.5, 1))
							.ortslage(KantenOrtslage.AUSSERORTS)
							.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.UEBER_100_KMH)
							.build()))
				.build())
			.fuehrungsformAttributGruppe(
				new FuehrungsformAttributGruppe(
					List.of(
						new FuehrungsformAttribute(
							LinearReferenzierterAbschnitt.of(0, 0.7),
							BelagArt.ASPHALT,
							Oberflaechenbeschaffenheit.ANLASS_ZUR_INTENSIVEN_BEOBACHTUNG_UND_ANALYSE,
							Bordstein.KEINE_ABSENKUNG,
							Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND,
							KfzParkenTyp.LAENGS_PARKEN,
							KfzParkenForm.HALBES_GEHWEGPARKEN_UNMARKIERT,
							Laenge.of(1),
							Benutzungspflicht.VORHANDEN,
							Beschilderung.UNBEKANNT,
							Collections.emptySet(),
							Absenkung.UNBEKANNT,
							null,
							null,
							null,
							null,
							TrennstreifenForm.UNBEKANNT,
							TrennstreifenForm.UNBEKANNT

						),
						new FuehrungsformAttribute(
							LinearReferenzierterAbschnitt.of(0.7, 1),
							BelagArt.BETON,
							Oberflaechenbeschaffenheit.ANLASS_ZUR_INTENSIVEN_BEOBACHTUNG_UND_ANALYSE,
							Bordstein.KEINE_ABSENKUNG,
							Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND,
							KfzParkenTyp.LAENGS_PARKEN,
							KfzParkenForm.HALBES_GEHWEGPARKEN_UNMARKIERT,
							Laenge.of(1),
							Benutzungspflicht.VORHANDEN,
							Beschilderung.UNBEKANNT,
							Collections.emptySet(),
							Absenkung.UNBEKANNT,
							null,
							null,
							null,
							null,
							TrennstreifenForm.UNBEKANNT,
							TrennstreifenForm.UNBEKANNT

						)), false))
			.build();

		kantenRepository.save(kante);

		kantenRepository.save(KanteTestDataProvider.withDefaultValues()
			.build()); // sollte nicht in der view auftauchen, da keine Netzklasse gesetzt ist

		kantenRepository.save(KanteTestDataProvider.withDefaultValues().kantenAttributGruppe(
			new KantenAttributGruppe(kantenAttribute,
				new HashSet<>(Set.of(Netzklasse.RADNETZ_ZIELNETZ)), new HashSet<>()))
			.build()); // sollte nicht in der View auftauchen, da nur RADNETZ_ZIELNETZ

		Kante kanteMitRoutenzugehoerigkeit = kantenRepository.save(KanteTestDataProvider.withDefaultValues().geometry(
			GeometryTestdataProvider.createLineStringWithCoordinatesMovedToValidBounds(new Coordinate(0, 0),
				new Coordinate(30, 30)))
			.build()); // Sollte mit drin sein, da Routenzugehoerigkeit gegeben
		LineString kanteMitRoutenzugehoerigkeit_3dLineString = GeometryTestdataProvider.createLineString(
			new Coordinate(kanteMitRoutenzugehoerigkeit.getGeometry().getStartPoint().getX(),
				kanteMitRoutenzugehoerigkeit.getGeometry().getStartPoint().getY(), 888.),
			new Coordinate(kanteMitRoutenzugehoerigkeit.getGeometry().getEndPoint().getX(),
				kanteMitRoutenzugehoerigkeit.getGeometry().getEndPoint().getY(), 999.));

		Kante kanteMitFalscherRoutenzugehoerigkeit = kantenRepository.save(
			KanteTestDataProvider.withDefaultValues().geometry(
				GeometryTestdataProvider.createLineStringWithCoordinatesMovedToValidBounds(new Coordinate(99, 99),
					new Coordinate(66, 66)))
				.build()); // Sollte nicht mit drin sein, da kein LRFW oder DRoute

		entityManager.flush();
		entityManager.clear();

		// Setzte Geometry3D von Kanten, die mit in dem BALM View drin sein sollen.
		kantenRepository.updateKanteElevation(
			new SliceImpl<>(List.of(
				new KanteElevationUpdate(kante.getId(), lineString_3d))));
		kantenRepository.updateKanteElevation(
			new SliceImpl<>(List.of(
				new KanteElevationUpdate(kanteMitRoutenzugehoerigkeit.getId(),
					kanteMitRoutenzugehoerigkeit_3dLineString))));

		Fahrradroute landesradfernweg = FahrradrouteTestDataProvider.onKante(kanteMitRoutenzugehoerigkeit)
			.kategorie(Kategorie.LANDESRADFERNWEG)
			.verantwortlich(gebietskoerperschaft)
			.name(FahrradrouteName.of("LRFW"))
			.build();
		fahrradrouteRepository.save(landesradfernweg);

		Fahrradroute gemeinerRadfernweg = FahrradrouteTestDataProvider.onKante(kanteMitFalscherRoutenzugehoerigkeit)
			.kategorie(Kategorie.RADFERNWEG)
			.verantwortlich(gebietskoerperschaft)
			.name(FahrradrouteName.of("Fiese Route"))
			.build();
		fahrradrouteRepository.save(gemeinerRadfernweg);

		entityManager.flush();
		entityManager.clear();

		// Act
		kantenRepository.refreshNetzMaterializedViews();

		List<Map<String, Object>> resultList = jdbcTemplate.queryForList(
			"SELECT * FROM " + KantenRepository.GEOSERVER_BALM_KANTEN_VIEW_NAME);

		// Assert

		// LRs LINKS: 0, 0.3, 0.5, 0.7, 1
		// LRs RECHTS: 0, 0.3, 0.5, 0.8, 1

		PGobject geometry_0__0_3 = PostGisHelper.getPGobject3D(
			GeometryTestdataProvider.getAbschnitt(lineString_3d, LinearReferenzierterAbschnitt.of(0, 0.3)));

		Map<String, Object> expected_0__0_3 = new HashMap<>() {
			{
				put("StreckenID", "084000005000010084000005000013");
				put("Quell-ID", "dlmId_0-0.3");
				put("GeometrieAbschnitt", geometry_0__0_3);
				put("Fuehrung", "401");
				put("Richtung", "1");
				put("Belag", "110");
				put("Laenge", 3);
				put("Licht", "1");
				put("Breite", 100);
				put("Lage", "1");
				put("B-Pflicht", 1);
				put("Bewertung", "4");
				put("Bewert_1", null);
				put("Baulast", "3");
				put("Baulast_3", null);
				put("Status", "10");
				put("Status_1", null);
				put("Status_2", null);
				put("Tempo", 100);
				put("Str_name", "HHWJ");
				put("Datum", null);
				put("lebenszeitIntervallAnfang", null);
				put("lebenszeitIntervallEnde", null);
				put("Routen_ID", null);
				put("Routenname", null);
				put("ext_bw_EuroVelo", null);
				put("ext_bw_netzklassen", Netzklasse.RADNETZ_ALLTAG.name());
				put("ext_bw_RadNETZ_D", 0);
				put("ext_bw_Landesnetz", 1);
				put("ext_bw_Kreisnetz", 0);
				put("ext_bw_KommuNetz", 0);
				put("ext_bw_hat_gegenseite", 0);
			}
		};

		PGobject geometry_0_3__0_5 = PostGisHelper.getPGobject3D(
			GeometryTestdataProvider.getAbschnitt(lineString_3d, LinearReferenzierterAbschnitt.of(0.3, 0.5)));

		Map<String, Object> expected_0_3__0_5 = new HashMap<>() {
			{
				put("StreckenID", "084000005000013084000005000015");
				put("Quell-ID", "dlmId_0.3-0.5");
				put("GeometrieAbschnitt", geometry_0_3__0_5);
				put("Fuehrung", "401");
				put("Richtung", "1");
				put("Belag", "110");
				put("Laenge", 2);
				put("Licht", "1");
				put("Breite", 100);
				put("Lage", "1");
				put("B-Pflicht", 1);
				put("Bewertung", "4");
				put("Bewert_1", null);
				put("Baulast", "3");
				put("Baulast_3", null);
				put("Status", "10");
				put("Status_1", null);
				put("Status_2", null);
				put("Tempo", 100);
				put("Str_name", "HHWJ");
				put("Datum", null);
				put("lebenszeitIntervallAnfang", null);
				put("lebenszeitIntervallEnde", null);
				put("Routen_ID", null);
				put("Routenname", null);
				put("ext_bw_EuroVelo", null);
				put("ext_bw_netzklassen", Netzklasse.RADNETZ_ALLTAG.name());
				put("ext_bw_RadNETZ_D", 0);
				put("ext_bw_Landesnetz", 1);
				put("ext_bw_Kreisnetz", 0);
				put("ext_bw_KommuNetz", 0);
				put("ext_bw_hat_gegenseite", 0);
			}
		};

		PGobject geometry_0_5__0_7 = PostGisHelper.getPGobject3D(
			GeometryTestdataProvider.getAbschnitt(lineString_3d, LinearReferenzierterAbschnitt.of(0.5, 0.7)));

		Map<String, Object> expected_0_5__0_7 = new HashMap<>() {
			{
				put("StreckenID", "084000005000015084000005000017");
				put("Quell-ID", "dlmId_0.5-0.7");
				put("GeometrieAbschnitt", geometry_0_5__0_7);
				put("Fuehrung", "401");
				put("Richtung", "1");
				put("Belag", "110");
				put("Laenge", 2);
				put("Licht", "1");
				put("Breite", 100);
				put("Lage", "2");
				put("B-Pflicht", 1);
				put("Bewertung", "4");
				put("Bewert_1", null);
				put("Baulast", "3");
				put("Baulast_3", null);
				put("Status", "10");
				put("Status_1", null);
				put("Status_2", null);
				put("Tempo", null);
				put("Str_name", "HHWJ");
				put("Datum", null);
				put("lebenszeitIntervallAnfang", null);
				put("lebenszeitIntervallEnde", null);
				put("Routen_ID", null);
				put("Routenname", null);
				put("ext_bw_EuroVelo", null);
				put("ext_bw_netzklassen", Netzklasse.RADNETZ_ALLTAG.name());
				put("ext_bw_RadNETZ_D", 0);
				put("ext_bw_Landesnetz", 1);
				put("ext_bw_Kreisnetz", 0);
				put("ext_bw_KommuNetz", 0);
				put("ext_bw_hat_gegenseite", 0);
			}
		};

		PGobject geometry_0_7__1 = PostGisHelper.getPGobject3D(
			GeometryTestdataProvider.getAbschnitt(lineString_3d, LinearReferenzierterAbschnitt.of(0.7, 1)));

		Map<String, Object> expected_0_7__1 = new HashMap<>() {
			{
				put("StreckenID", "084000005000017084000005000020");
				put("Quell-ID", "dlmId_0.7-1");
				put("GeometrieAbschnitt", geometry_0_7__1);
				put("Fuehrung", "401");
				put("Richtung", "1");
				put("Belag", "120");
				put("Laenge", 3);
				put("Licht", "1");
				put("Breite", 100);
				put("Lage", "2");
				put("B-Pflicht", 1);
				put("Bewertung", "4");
				put("Bewert_1", null);
				put("Baulast", "3");
				put("Baulast_3", null);
				put("Status", "10");
				put("Status_1", null);
				put("Status_2", null);
				put("Tempo", null);
				put("Str_name", "HHWJ");
				put("Datum", null);
				put("lebenszeitIntervallAnfang", null);
				put("lebenszeitIntervallEnde", null);
				put("Routen_ID", null);
				put("Routenname", null);
				put("ext_bw_EuroVelo", null);
				put("ext_bw_netzklassen", Netzklasse.RADNETZ_ALLTAG.name());
				put("ext_bw_RadNETZ_D", 0);
				put("ext_bw_Landesnetz", 1);
				put("ext_bw_Kreisnetz", 0);
				put("ext_bw_KommuNetz", 0);
				put("ext_bw_hat_gegenseite", 0);
			}
		};

		PGobject geometry_mitRoutenzugehoerigkeit = PostGisHelper.getPGobject3D(
			kanteMitRoutenzugehoerigkeit_3dLineString);

		Map<String, Object> expected_mitRoutenzugehoerigkeit = new HashMap<>() {
			{
				put("StreckenID", "084500005400000084500305400030");
				put("Quell-ID", "dlm-id");
				put("GeometrieAbschnitt", geometry_mitRoutenzugehoerigkeit);
				put("Fuehrung", "900");
				put("Richtung", "9");
				put("Belag", "900");
				put("Laenge", 42);
				put("Licht", null);
				put("Breite", null);
				put("Lage", null);
				put("B-Pflicht", null);
				put("Bewertung", "9");
				put("Bewert_1", null);
				put("Baulast", "9");
				put("Baulast_3", null);
				put("Status", "10");
				put("Status_1", null);
				put("Status_2", null);
				put("Tempo", null);
				put("Str_name", null);
				put("Datum", null);
				put("lebenszeitIntervallAnfang", null);
				put("lebenszeitIntervallEnde", null);
				put("Routen_ID", "08-" + landesradfernweg.getId());
				put("Routenname", landesradfernweg.getName().getName());
				put("ext_bw_EuroVelo", null);
				put("ext_bw_netzklassen", null);
				put("ext_bw_RadNETZ_D", 0);
				put("ext_bw_Landesnetz", 0);
				put("ext_bw_Kreisnetz", 0);
				put("ext_bw_KommuNetz", 0);
				put("ext_bw_hat_gegenseite", 0);
			}
		};

		assertThat(resultList).hasSize(5);
		assertThat(resultList).contains(expected_0__0_3);
		assertThat(resultList).contains(expected_0_3__0_5);
		assertThat(resultList).contains(expected_0_5__0_7);
		assertThat(resultList).contains(expected_0_7__1);
		assertThat(resultList).contains(expected_mitRoutenzugehoerigkeit);
	}

	@Test
	public void testGeoserverBalmView_kanteZweiseitig() throws SQLException {
		// Arrange
		Knoten vonKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(400000, 5000010),
			QuellSystem.DLM)
			.build();
		Knoten nachKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(400000, 5000020),
			QuellSystem.DLM)
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
			.strassenkategorieRIN(StrassenkategorieRIN.NAHRAEUMIG)
			.strassenquerschnittRASt06(StrassenquerschnittRASt06.ANBAUFREIE_STRASSE)
			.status(Status.defaultWert())
			.build();

		Verwaltungseinheit gebietskoerperschaft = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().name("tolle Organisation")
				.organisationsArt(
					OrganisationsArt.KREIS)
				.build());

		entityManager.flush();
		entityManager.clear();

		Kante kante = Kante.builder()
			.ursprungsfeatureTechnischeID("123")
			.vonKnoten(vonKnoten)
			.nachKnoten(nachKnoten)
			.geometry(lineString)
			.isZweiseitig(true)
			.quelle(QuellSystem.DLM)
			.dlmId(DlmId.of("dlmId"))
			.kantenAttributGruppe(
				new KantenAttributGruppe(kantenAttribute,
					new HashSet<>(Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.KOMMUNALNETZ_FREIZEIT)),
					new HashSet<>()))
			.fahrtrichtungAttributGruppe(
				new FahrtrichtungAttributGruppe(Richtung.BEIDE_RICHTUNGEN, Richtung.IN_RICHTUNG, true))
			.zustaendigkeitAttributGruppe(new ZustaendigkeitAttributGruppe(List.of(
				new ZustaendigkeitAttribute(LinearReferenzierterAbschnitt.of(0, 0.3), gebietskoerperschaft,
					gebietskoerperschaft,
					gebietskoerperschaft,
					VereinbarungsKennung.of("123")),
				new ZustaendigkeitAttribute(LinearReferenzierterAbschnitt.of(0.3, 1), gebietskoerperschaft,
					gebietskoerperschaft,
					gebietskoerperschaft,
					VereinbarungsKennung.of("456")))))
			.geschwindigkeitAttributGruppe(GeschwindigkeitAttributGruppe.builder()
				.geschwindigkeitAttribute(
					List.of(
						GeschwindigkeitAttribute.builder()
							.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.5))
							.ortslage(KantenOrtslage.INNERORTS)
							.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_100_KMH)
							.build(),
						GeschwindigkeitAttribute.builder()
							.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.5, 1))
							.ortslage(KantenOrtslage.AUSSERORTS)
							.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.UEBER_100_KMH)
							.build()))
				.build())
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppe.builder()
					.fuehrungsformAttributeLinks(
						List.of(
							new FuehrungsformAttribute(
								LinearReferenzierterAbschnitt.of(0, 0.7),
								BelagArt.ASPHALT,
								Oberflaechenbeschaffenheit.ANLASS_ZUR_INTENSIVEN_BEOBACHTUNG_UND_ANALYSE,
								Bordstein.KEINE_ABSENKUNG,
								Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND,
								KfzParkenTyp.LAENGS_PARKEN,
								KfzParkenForm.HALBES_GEHWEGPARKEN_UNMARKIERT,
								Laenge.of(1),
								Benutzungspflicht.VORHANDEN,
								Beschilderung.UNBEKANNT,
								Collections.emptySet(),
								Absenkung.UNBEKANNT,
								null,
								null,
								null,
								null,
								TrennstreifenForm.UNBEKANNT,
								TrennstreifenForm.UNBEKANNT

							),
							new FuehrungsformAttribute(
								LinearReferenzierterAbschnitt.of(0.7, 1),
								BelagArt.BETON,
								Oberflaechenbeschaffenheit.ANLASS_ZUR_INTENSIVEN_BEOBACHTUNG_UND_ANALYSE,
								Bordstein.KEINE_ABSENKUNG,
								Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND,
								KfzParkenTyp.LAENGS_PARKEN,
								KfzParkenForm.HALBES_GEHWEGPARKEN_UNMARKIERT,
								Laenge.of(1),
								Benutzungspflicht.VORHANDEN,
								Beschilderung.UNBEKANNT,
								Collections.emptySet(),
								Absenkung.UNBEKANNT,
								null,
								null,
								null,
								null,
								TrennstreifenForm.UNBEKANNT,
								TrennstreifenForm.UNBEKANNT

							)))
					.fuehrungsformAttributeRechts(
						List.of(
							new FuehrungsformAttribute(
								LinearReferenzierterAbschnitt.of(0, 0.8),
								BelagArt.NATURSTEINPFLASTER,
								Oberflaechenbeschaffenheit.ANLASS_ZUR_INTENSIVEN_BEOBACHTUNG_UND_ANALYSE,
								Bordstein.KEINE_ABSENKUNG,
								Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND,
								KfzParkenTyp.LAENGS_PARKEN,
								KfzParkenForm.HALBES_GEHWEGPARKEN_UNMARKIERT,
								Laenge.of(1),
								Benutzungspflicht.VORHANDEN,
								Beschilderung.UNBEKANNT,
								Collections.emptySet(),
								Absenkung.UNBEKANNT,
								null,
								null,
								null,
								null,
								TrennstreifenForm.UNBEKANNT,
								TrennstreifenForm.UNBEKANNT

							),
							new FuehrungsformAttribute(
								LinearReferenzierterAbschnitt.of(0.8, 1),
								BelagArt.WASSERGEBUNDENE_DECKE,
								Oberflaechenbeschaffenheit.ANLASS_ZUR_INTENSIVEN_BEOBACHTUNG_UND_ANALYSE,
								Bordstein.KEINE_ABSENKUNG,
								Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND,
								KfzParkenTyp.LAENGS_PARKEN,
								KfzParkenForm.HALBES_GEHWEGPARKEN_UNMARKIERT,
								Laenge.of(1),
								Benutzungspflicht.VORHANDEN,
								Beschilderung.UNBEKANNT,
								Collections.emptySet(),
								Absenkung.UNBEKANNT,
								null,
								null,
								null,
								null,
								TrennstreifenForm.UNBEKANNT,
								TrennstreifenForm.UNBEKANNT

							)))
					.isZweiseitig(true)
					.build())
			.build();

		kantenRepository.save(kante).getId();

		Fahrradroute routeDRoute = FahrradrouteTestDataProvider.onKante(kante)
			.verantwortlich(null)
			.kategorie(Kategorie.D_ROUTE)
			.name(FahrradrouteName.of("De;Ruhte"))
			.build();
		Fahrradroute routeLrfw = FahrradrouteTestDataProvider.onKante(kante)
			.verantwortlich(null)
			.kategorie(Kategorie.LANDESRADFERNWEG)
			.name(FahrradrouteName.of("LRFW"))
			.build();
		Fahrradroute routeTouri = FahrradrouteTestDataProvider.onKante(kante)
			.verantwortlich(null)
			.kategorie(Kategorie.TOURISTISCHE_ROUTE)
			.name(FahrradrouteName.of("Touri-Route"))
			.build();
		fahrradrouteRepository.save(routeDRoute);
		fahrradrouteRepository.save(routeLrfw);
		fahrradrouteRepository.save(routeTouri);

		entityManager.createNativeQuery("UPDATE kante SET geometry3d = geometry").executeUpdate();
		entityManager.flush();
		entityManager.clear();

		// Act

		kantenRepository.refreshNetzMaterializedViews();

		List<Map<String, Object>> resultList = jdbcTemplate.queryForList(
			"SELECT * FROM " + KantenRepository.GEOSERVER_BALM_KANTEN_VIEW_NAME);

		// Assert

		// LRs LINKS: 0, 0.3, 0.5, 0.7, 1
		// LRs RECHTS: 0, 0.3, 0.5, 0.8, 1

		PGobject geometry_links_0__0_3 = PostGisHelper.getPGobject(
			GeometryTestdataProvider.getAbschnitt(kante.getGeometry(), LinearReferenzierterAbschnitt.of(0, 0.3))
				.reverse());

		Map<String, Object> expected_links_0__0_3 = new HashMap<>() {
			{
				put("StreckenID", "084000005000013084000005000010");
				put("Quell-ID", "-dlmId_0-0.3");
				put("GeometrieAbschnitt", geometry_links_0__0_3);
				put("Fuehrung", "401");
				put("Richtung", "1");
				put("Belag", "110");
				put("Laenge", 3);
				put("Licht", "1");
				put("Breite", 100);
				put("Lage", "1");
				put("B-Pflicht", 1);
				put("Bewertung", "4");
				put("Bewert_1", null);
				put("Baulast", "3");
				put("Baulast_3", null);
				put("Status", "10");
				put("Status_1", null);
				put("Status_2", null);
				put("Tempo", 100);
				put("Str_name", "HHWJ");
				put("Datum", null);
				put("lebenszeitIntervallAnfang", null);
				put("lebenszeitIntervallEnde", null);
				put("Routen_ID", "08-" + routeDRoute.getId() + ";08-" + routeLrfw.getId());
				put("Routenname", "De,Ruhte;LRFW");
				put("ext_bw_EuroVelo", null);
				put("ext_bw_netzklassen", Netzklasse.KOMMUNALNETZ_FREIZEIT.name() + ";" + Netzklasse.RADNETZ_ALLTAG
					.name());
				put("ext_bw_RadNETZ_D", 1);
				put("ext_bw_Landesnetz", 1);
				put("ext_bw_Kreisnetz", 0);
				put("ext_bw_KommuNetz", 1);
				put("ext_bw_hat_gegenseite", 1);
			}
		};

		PGobject geometry_links_0_3__0_5 = PostGisHelper.getPGobject(
			GeometryTestdataProvider.getAbschnitt(kante.getGeometry(), LinearReferenzierterAbschnitt.of(0.3, 0.5))
				.reverse());

		Map<String, Object> expected_links_0_3__0_5 = new HashMap<>() {
			{
				put("StreckenID", "084000005000015084000005000013");
				put("Quell-ID", "-dlmId_0.3-0.5");
				put("GeometrieAbschnitt", geometry_links_0_3__0_5);
				put("Fuehrung", "401");
				put("Richtung", "1");
				put("Belag", "110");
				put("Laenge", 2);
				put("Licht", "1");
				put("Breite", 100);
				put("Lage", "1");
				put("B-Pflicht", 1);
				put("Bewertung", "4");
				put("Bewert_1", null);
				put("Baulast", "3");
				put("Baulast_3", null);
				put("Status", "10");
				put("Status_1", null);
				put("Status_2", null);
				put("Tempo", 100);
				put("Str_name", "HHWJ");
				put("Datum", null);
				put("lebenszeitIntervallAnfang", null);
				put("lebenszeitIntervallEnde", null);
				put("Routen_ID", "08-" + routeDRoute.getId() + ";08-" + routeLrfw.getId());
				put("Routenname", "De,Ruhte;LRFW");
				put("ext_bw_EuroVelo", null);
				put("ext_bw_netzklassen", Netzklasse.KOMMUNALNETZ_FREIZEIT.name() + ";" + Netzklasse.RADNETZ_ALLTAG
					.name());
				put("ext_bw_RadNETZ_D", 1);
				put("ext_bw_Landesnetz", 1);
				put("ext_bw_Kreisnetz", 0);
				put("ext_bw_KommuNetz", 1);
				put("ext_bw_hat_gegenseite", 1);
			}
		};

		PGobject geometry_links_0_5__0_7 = PostGisHelper.getPGobject(
			GeometryTestdataProvider.getAbschnitt(kante.getGeometry(), LinearReferenzierterAbschnitt.of(0.5, 0.7))
				.reverse());

		Map<String, Object> expected_links_0_5__0_7 = new HashMap<>() {
			{
				put("StreckenID", "084000005000017084000005000015");
				put("Quell-ID", "-dlmId_0.5-0.7");
				put("GeometrieAbschnitt", geometry_links_0_5__0_7);
				put("Fuehrung", "401");
				put("Richtung", "1");
				put("Belag", "110");
				put("Laenge", 2);
				put("Licht", "1");
				put("Breite", 100);
				put("Lage", "2");
				put("B-Pflicht", 1);
				put("Bewertung", "4");
				put("Bewert_1", null);
				put("Baulast", "3");
				put("Baulast_3", null);
				put("Status", "10");
				put("Status_1", null);
				put("Status_2", null);
				put("Tempo", null);
				put("Str_name", "HHWJ");
				put("Datum", null);
				put("lebenszeitIntervallAnfang", null);
				put("lebenszeitIntervallEnde", null);
				put("Routen_ID", "08-" + routeDRoute.getId() + ";08-" + routeLrfw.getId());
				put("Routenname", "De,Ruhte;LRFW");
				put("ext_bw_EuroVelo", null);
				put("ext_bw_netzklassen", Netzklasse.KOMMUNALNETZ_FREIZEIT.name() + ";" + Netzklasse.RADNETZ_ALLTAG
					.name());
				put("ext_bw_RadNETZ_D", 1);
				put("ext_bw_Landesnetz", 1);
				put("ext_bw_Kreisnetz", 0);
				put("ext_bw_KommuNetz", 1);
				put("ext_bw_hat_gegenseite", 1);
			}
		};

		PGobject geometry_links_0_7__1 = PostGisHelper.getPGobject(
			GeometryTestdataProvider.getAbschnitt(kante.getGeometry(), LinearReferenzierterAbschnitt.of(0.7, 1))
				.reverse());

		Map<String, Object> expected_links_0_7__1 = new HashMap<>() {
			{
				put("StreckenID", "084000005000020084000005000017");
				put("Quell-ID", "-dlmId_0.7-1");
				put("GeometrieAbschnitt", geometry_links_0_7__1);
				put("Fuehrung", "401");
				put("Richtung", "1");
				put("Belag", "120");
				put("Laenge", 3);
				put("Licht", "1");
				put("Breite", 100);
				put("Lage", "2");
				put("B-Pflicht", 1);
				put("Bewertung", "4");
				put("Bewert_1", null);
				put("Baulast", "3");
				put("Baulast_3", null);
				put("Status", "10");
				put("Status_1", null);
				put("Status_2", null);
				put("Tempo", null);
				put("Str_name", "HHWJ");
				put("Datum", null);
				put("lebenszeitIntervallAnfang", null);
				put("lebenszeitIntervallEnde", null);
				put("Routen_ID", "08-" + routeDRoute.getId() + ";08-" + routeLrfw.getId());
				put("Routenname", "De,Ruhte;LRFW");
				put("ext_bw_EuroVelo", null);
				put("ext_bw_netzklassen", Netzklasse.KOMMUNALNETZ_FREIZEIT.name() + ";" + Netzklasse.RADNETZ_ALLTAG
					.name());
				put("ext_bw_RadNETZ_D", 1);
				put("ext_bw_Landesnetz", 1);
				put("ext_bw_Kreisnetz", 0);
				put("ext_bw_KommuNetz", 1);
				put("ext_bw_hat_gegenseite", 1);
			}
		};

		PGobject geometry_rechts_0__0_3 = PostGisHelper.getPGobject(
			GeometryTestdataProvider.getAbschnitt(kante.getGeometry(), LinearReferenzierterAbschnitt.of(0, 0.3)));

		Map<String, Object> expected_rechts_0__0_3 = new HashMap<>() {
			{
				put("StreckenID", "084000005000010084000005000013");
				put("Quell-ID", "dlmId_0-0.3");
				put("GeometrieAbschnitt", geometry_rechts_0__0_3);
				put("Fuehrung", "401");
				put("Richtung", "2");
				put("Belag", "132");
				put("Laenge", 3);
				put("Licht", "1");
				put("Breite", 100);
				put("Lage", "1");
				put("B-Pflicht", 1);
				put("Bewertung", "4");
				put("Bewert_1", null);
				put("Baulast", "3");
				put("Baulast_3", null);
				put("Status", "10");
				put("Status_1", null);
				put("Status_2", null);
				put("Tempo", 100);
				put("Str_name", "HHWJ");
				put("Datum", null);
				put("lebenszeitIntervallAnfang", null);
				put("lebenszeitIntervallEnde", null);
				put("Routen_ID", "08-" + routeDRoute.getId() + ";08-" + routeLrfw.getId());
				put("Routenname", "De,Ruhte;LRFW");
				put("ext_bw_EuroVelo", null);
				put("ext_bw_netzklassen", Netzklasse.KOMMUNALNETZ_FREIZEIT.name() + ";" + Netzklasse.RADNETZ_ALLTAG
					.name());
				put("ext_bw_RadNETZ_D", 1);
				put("ext_bw_Landesnetz", 1);
				put("ext_bw_Kreisnetz", 0);
				put("ext_bw_KommuNetz", 1);
				put("ext_bw_hat_gegenseite", 1);
			}
		};

		PGobject geometry_rechts_0_3__0_5 = PostGisHelper.getPGobject(
			GeometryTestdataProvider.getAbschnitt(kante.getGeometry(), LinearReferenzierterAbschnitt.of(0.3, 0.5)));

		Map<String, Object> expected_rechts_0_3__0_5 = new HashMap<>() {
			{
				put("StreckenID", "084000005000013084000005000015");
				put("Quell-ID", "dlmId_0.3-0.5");
				put("GeometrieAbschnitt", geometry_rechts_0_3__0_5);
				put("Fuehrung", "401");
				put("Richtung", "2");
				put("Belag", "132");
				put("Laenge", 2);
				put("Licht", "1");
				put("Breite", 100);
				put("Lage", "1");
				put("B-Pflicht", 1);
				put("Bewertung", "4");
				put("Bewert_1", null);
				put("Baulast", "3");
				put("Baulast_3", null);
				put("Status", "10");
				put("Status_1", null);
				put("Status_2", null);
				put("Tempo", 100);
				put("Str_name", "HHWJ");
				put("Datum", null);
				put("lebenszeitIntervallAnfang", null);
				put("lebenszeitIntervallEnde", null);
				put("Routen_ID", "08-" + routeDRoute.getId() + ";08-" + routeLrfw.getId());
				put("Routenname", "De,Ruhte;LRFW");
				put("ext_bw_EuroVelo", null);
				put("ext_bw_netzklassen", Netzklasse.KOMMUNALNETZ_FREIZEIT.name() + ";" + Netzklasse.RADNETZ_ALLTAG
					.name());
				put("ext_bw_RadNETZ_D", 1);
				put("ext_bw_Landesnetz", 1);
				put("ext_bw_Kreisnetz", 0);
				put("ext_bw_KommuNetz", 1);
				put("ext_bw_hat_gegenseite", 1);
			}
		};

		PGobject geometry_rechts_0_5__0_8 = PostGisHelper.getPGobject(
			GeometryTestdataProvider.getAbschnitt(kante.getGeometry(), LinearReferenzierterAbschnitt.of(0.5, 0.8)));

		Map<String, Object> expected_rechts_0_5__0_8 = new HashMap<>() {
			{
				put("StreckenID", "084000005000015084000005000018");
				put("Quell-ID", "dlmId_0.5-0.8");
				put("GeometrieAbschnitt", geometry_rechts_0_5__0_8);
				put("Fuehrung", "401");
				put("Richtung", "2");
				put("Belag", "132");
				put("Laenge", 3);
				put("Licht", "1");
				put("Breite", 100);
				put("Lage", "2");
				put("B-Pflicht", 1);
				put("Bewertung", "4");
				put("Bewert_1", null);
				put("Baulast", "3");
				put("Baulast_3", null);
				put("Status", "10");
				put("Status_1", null);
				put("Status_2", null);
				put("Tempo", null);
				put("Str_name", "HHWJ");
				put("Datum", null);
				put("lebenszeitIntervallAnfang", null);
				put("lebenszeitIntervallEnde", null);
				put("Routen_ID", "08-" + routeDRoute.getId() + ";08-" + routeLrfw.getId());
				put("Routenname", "De,Ruhte;LRFW");
				put("ext_bw_EuroVelo", null);
				put("ext_bw_netzklassen", Netzklasse.KOMMUNALNETZ_FREIZEIT.name() + ";" + Netzklasse.RADNETZ_ALLTAG
					.name());
				put("ext_bw_RadNETZ_D", 1);
				put("ext_bw_Landesnetz", 1);
				put("ext_bw_Kreisnetz", 0);
				put("ext_bw_KommuNetz", 1);
				put("ext_bw_hat_gegenseite", 1);
			}
		};

		PGobject geometry_rechts_0_8__1 = PostGisHelper.getPGobject(
			GeometryTestdataProvider.getAbschnitt(kante.getGeometry(), LinearReferenzierterAbschnitt.of(0.8, 1)));

		Map<String, Object> expected_rechts_0_8__1 = new HashMap<>() {
			{
				put("StreckenID", "084000005000018084000005000020");
				put("Quell-ID", "dlmId_0.8-1");
				put("GeometrieAbschnitt", geometry_rechts_0_8__1);
				put("Fuehrung", "401");
				put("Richtung", "2");
				put("Belag", "200");
				put("Laenge", 2);
				put("Licht", "1");
				put("Breite", 100);
				put("Lage", "2");
				put("B-Pflicht", 1);
				put("Bewertung", "4");
				put("Bewert_1", null);
				put("Baulast", "3");
				put("Baulast_3", null);
				put("Status", "10");
				put("Status_1", null);
				put("Status_2", null);
				put("Tempo", null);
				put("Str_name", "HHWJ");
				put("Datum", null);
				put("lebenszeitIntervallAnfang", null);
				put("lebenszeitIntervallEnde", null);
				put("Routen_ID", "08-" + routeDRoute.getId() + ";08-" + routeLrfw.getId());
				put("Routenname", "De,Ruhte;LRFW");
				put("ext_bw_EuroVelo", null);
				put("ext_bw_netzklassen", Netzklasse.KOMMUNALNETZ_FREIZEIT.name() + ";" + Netzklasse.RADNETZ_ALLTAG
					.name());
				put("ext_bw_RadNETZ_D", 1);
				put("ext_bw_Landesnetz", 1);
				put("ext_bw_Kreisnetz", 0);
				put("ext_bw_KommuNetz", 1);
				put("ext_bw_hat_gegenseite", 1);
			}
		};

		assertThat(resultList).hasSize(8);
		assertThat(resultList).contains(expected_links_0__0_3);
		assertThat(resultList).contains(expected_links_0_3__0_5);
		assertThat(resultList).contains(expected_links_0_5__0_7);
		assertThat(resultList).contains(expected_links_0_7__1);
		assertThat(resultList).contains(expected_rechts_0__0_3);
		assertThat(resultList).contains(expected_rechts_0_3__0_5);
		assertThat(resultList).contains(expected_rechts_0_5__0_8);
		assertThat(resultList).contains(expected_rechts_0_8__1);
	}

	@Test
	public void testGeoserverBalmView_kleineAbschnitte() throws SQLException {
		// Arrange
		Knoten vonKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(400000, 5000010),
			QuellSystem.DLM)
			.build();
		Knoten nachKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(400000, 5000110),
			QuellSystem.DLM)
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
			.strassenkategorieRIN(StrassenkategorieRIN.NAHRAEUMIG)
			.strassenquerschnittRASt06(StrassenquerschnittRASt06.ANBAUFREIE_STRASSE)
			.status(Status.defaultWert())
			.build();

		Verwaltungseinheit gebietskoerperschaft = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().name("tolle Organisation")
				.organisationsArt(
					OrganisationsArt.KREIS)
				.build());

		entityManager.flush();
		entityManager.clear();

		Kante kante = Kante.builder()
			.ursprungsfeatureTechnischeID("123")
			.vonKnoten(vonKnoten)
			.nachKnoten(nachKnoten)
			.geometry(lineString)
			.isZweiseitig(true)
			.quelle(QuellSystem.DLM)
			.dlmId(DlmId.of("dlmId"))
			.kantenAttributGruppe(
				new KantenAttributGruppe(kantenAttribute,
					new HashSet<>(Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.KOMMUNALNETZ_FREIZEIT)),
					new HashSet<>()))
			.fahrtrichtungAttributGruppe(
				new FahrtrichtungAttributGruppe(Richtung.BEIDE_RICHTUNGEN, Richtung.IN_RICHTUNG, true))
			.zustaendigkeitAttributGruppe(new ZustaendigkeitAttributGruppe(List.of(
				new ZustaendigkeitAttribute(LinearReferenzierterAbschnitt.of(0, 0.3), gebietskoerperschaft,
					gebietskoerperschaft,
					gebietskoerperschaft,
					VereinbarungsKennung.of("123")),
				new ZustaendigkeitAttribute(LinearReferenzierterAbschnitt.of(0.3, 0.99), gebietskoerperschaft,
					gebietskoerperschaft,
					gebietskoerperschaft,
					VereinbarungsKennung.of("456")),
				new ZustaendigkeitAttribute(LinearReferenzierterAbschnitt.of(0.99, 1), gebietskoerperschaft,
					gebietskoerperschaft,
					gebietskoerperschaft,
					VereinbarungsKennung.of("789")))))
			.geschwindigkeitAttributGruppe(GeschwindigkeitAttributGruppe.builder()
				.geschwindigkeitAttribute(
					List.of(
						GeschwindigkeitAttribute.builder()
							.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.01))
							.ortslage(KantenOrtslage.AUSSERORTS)
							.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_30_KMH)
							.build(),
						GeschwindigkeitAttribute.builder()
							.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.01, 0.5))
							.ortslage(KantenOrtslage.INNERORTS)
							.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_100_KMH)
							.build(),
						GeschwindigkeitAttribute.builder()
							.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.5, 1))
							.ortslage(KantenOrtslage.AUSSERORTS)
							.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.UEBER_100_KMH)
							.build()))
				.build())
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppe.builder()
					.fuehrungsformAttributeLinks(
						List.of(
							new FuehrungsformAttribute(
								LinearReferenzierterAbschnitt.of(0, 0.7),
								BelagArt.ASPHALT,
								Oberflaechenbeschaffenheit.ANLASS_ZUR_INTENSIVEN_BEOBACHTUNG_UND_ANALYSE,
								Bordstein.KEINE_ABSENKUNG,
								Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND,
								KfzParkenTyp.LAENGS_PARKEN,
								KfzParkenForm.HALBES_GEHWEGPARKEN_UNMARKIERT,
								Laenge.of(1),
								Benutzungspflicht.VORHANDEN,
								Beschilderung.UNBEKANNT,
								Collections.emptySet(),
								Absenkung.UNBEKANNT,
								null,
								null,
								null,
								null,
								TrennstreifenForm.UNBEKANNT,
								TrennstreifenForm.UNBEKANNT

							),
							new FuehrungsformAttribute(
								LinearReferenzierterAbschnitt.of(0.7, 0.71),
								BelagArt.BETONSTEINPFLASTER_PLATTENBELAG,
								Oberflaechenbeschaffenheit.SEHR_GUTER_BIS_GUTER_ZUSTAND,
								Bordstein.KEINE_ABSENKUNG,
								Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND,
								KfzParkenTyp.LAENGS_PARKEN,
								KfzParkenForm.HALBES_GEHWEGPARKEN_UNMARKIERT,
								Laenge.of(1),
								Benutzungspflicht.VORHANDEN,
								Beschilderung.UNBEKANNT,
								Collections.emptySet(),
								Absenkung.UNBEKANNT,
								null,
								null,
								null,
								null,
								TrennstreifenForm.UNBEKANNT,
								TrennstreifenForm.UNBEKANNT

							),
							new FuehrungsformAttribute(
								LinearReferenzierterAbschnitt.of(0.71, 1),
								BelagArt.BETON,
								Oberflaechenbeschaffenheit.ANLASS_ZUR_INTENSIVEN_BEOBACHTUNG_UND_ANALYSE,
								Bordstein.KEINE_ABSENKUNG,
								Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND,
								KfzParkenTyp.LAENGS_PARKEN,
								KfzParkenForm.HALBES_GEHWEGPARKEN_UNMARKIERT,
								Laenge.of(1),
								Benutzungspflicht.VORHANDEN,
								Beschilderung.UNBEKANNT,
								Collections.emptySet(),
								Absenkung.UNBEKANNT,
								null,
								null,
								null,
								null,
								TrennstreifenForm.UNBEKANNT,
								TrennstreifenForm.UNBEKANNT

							)))
					.fuehrungsformAttributeRechts(
						List.of(
							new FuehrungsformAttribute(
								LinearReferenzierterAbschnitt.of(0, 0.8),
								BelagArt.NATURSTEINPFLASTER,
								Oberflaechenbeschaffenheit.ANLASS_ZUR_INTENSIVEN_BEOBACHTUNG_UND_ANALYSE,
								Bordstein.KEINE_ABSENKUNG,
								Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND,
								KfzParkenTyp.LAENGS_PARKEN,
								KfzParkenForm.HALBES_GEHWEGPARKEN_UNMARKIERT,
								Laenge.of(1),
								Benutzungspflicht.VORHANDEN,
								Beschilderung.UNBEKANNT,
								Collections.emptySet(),
								Absenkung.UNBEKANNT,
								null,
								null,
								null,
								null,
								TrennstreifenForm.UNBEKANNT,
								TrennstreifenForm.UNBEKANNT

							),
							new FuehrungsformAttribute(
								LinearReferenzierterAbschnitt.of(0.8, 1),
								BelagArt.WASSERGEBUNDENE_DECKE,
								Oberflaechenbeschaffenheit.ANLASS_ZUR_INTENSIVEN_BEOBACHTUNG_UND_ANALYSE,
								Bordstein.KEINE_ABSENKUNG,
								Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND,
								KfzParkenTyp.LAENGS_PARKEN,
								KfzParkenForm.HALBES_GEHWEGPARKEN_UNMARKIERT,
								Laenge.of(1),
								Benutzungspflicht.VORHANDEN,
								Beschilderung.UNBEKANNT,
								Collections.emptySet(),
								Absenkung.UNBEKANNT,
								null,
								null,
								null,
								null,
								TrennstreifenForm.UNBEKANNT,
								TrennstreifenForm.UNBEKANNT

							)))
					.isZweiseitig(true)
					.build())
			.build();

		kantenRepository.save(kante).getId();

		entityManager.createNativeQuery("UPDATE kante SET geometry3d = geometry").executeUpdate();
		entityManager.flush();
		entityManager.clear();

		// Act

		kantenRepository.refreshNetzMaterializedViews();

		List<Map<String, Object>> resultList = jdbcTemplate.queryForList(
			"SELECT * FROM " + KantenRepository.GEOSERVER_BALM_KANTEN_VIEW_NAME);

		// Assert

		// LRs LINKS: 0, 0.3, 0.5, 0.7, 1
		// LRs RECHTS: 0, 0.3, 0.5, 0.8, 1

		PGobject geometry_links_0__0_3 = PostGisHelper.getPGobject(
			GeometryTestdataProvider.getAbschnitt(kante.getGeometry(), LinearReferenzierterAbschnitt.of(0, 0.3))
				.reverse());

		Map<String, Object> expected_links_0__0_3 = new HashMap<>() {
			{
				put("StreckenID", "084000005000040084000005000010");
				put("Quell-ID", "-dlmId_0-0.3");
				put("GeometrieAbschnitt", geometry_links_0__0_3);
				put("Fuehrung", "401");
				put("Richtung", "1");
				put("Belag", "110");
				put("Laenge", 30);
				put("Licht", "1");
				put("Breite", 100);
				put("Lage", "1");
				put("B-Pflicht", 1);
				put("Bewertung", "4");
				put("Bewert_1", null);
				put("Baulast", "3");
				put("Baulast_3", null);
				put("Status", "10");
				put("Status_1", null);
				put("Status_2", null);
				put("Tempo", 100);
				put("Str_name", "HHWJ");
				put("Datum", null);
				put("lebenszeitIntervallAnfang", null);
				put("lebenszeitIntervallEnde", null);
				put("Routen_ID", null);
				put("Routenname", null);
				put("ext_bw_EuroVelo", null);
				put("ext_bw_netzklassen", Netzklasse.KOMMUNALNETZ_FREIZEIT.name() + ";" + Netzklasse.RADNETZ_ALLTAG
					.name());
				put("ext_bw_RadNETZ_D", 0);
				put("ext_bw_Landesnetz", 1);
				put("ext_bw_Kreisnetz", 0);
				put("ext_bw_KommuNetz", 1);
				put("ext_bw_hat_gegenseite", 1);
			}
		};

		PGobject geometry_links_0_3__0_5 = PostGisHelper.getPGobject(
			GeometryTestdataProvider.getAbschnitt(kante.getGeometry(), LinearReferenzierterAbschnitt.of(0.3, 0.5))
				.reverse());

		Map<String, Object> expected_links_0_3__0_5 = new HashMap<>() {
			{
				put("StreckenID", "084000005000060084000005000040");
				put("Quell-ID", "-dlmId_0.3-0.5");
				put("GeometrieAbschnitt", geometry_links_0_3__0_5);
				put("Fuehrung", "401");
				put("Richtung", "1");
				put("Belag", "110");
				put("Laenge", 20);
				put("Licht", "1");
				put("Breite", 100);
				put("Lage", "1");
				put("B-Pflicht", 1);
				put("Bewertung", "4");
				put("Bewert_1", null);
				put("Baulast", "3");
				put("Baulast_3", null);
				put("Status", "10");
				put("Status_1", null);
				put("Status_2", null);
				put("Tempo", 100);
				put("Str_name", "HHWJ");
				put("Datum", null);
				put("lebenszeitIntervallAnfang", null);
				put("lebenszeitIntervallEnde", null);
				put("Routen_ID", null);
				put("Routenname", null);
				put("ext_bw_EuroVelo", null);
				put("ext_bw_netzklassen", Netzklasse.KOMMUNALNETZ_FREIZEIT.name() + ";" + Netzklasse.RADNETZ_ALLTAG
					.name());
				put("ext_bw_RadNETZ_D", 0);
				put("ext_bw_Landesnetz", 1);
				put("ext_bw_Kreisnetz", 0);
				put("ext_bw_KommuNetz", 1);
				put("ext_bw_hat_gegenseite", 1);
			}
		};

		PGobject geometry_links_0_5__0_7 = PostGisHelper.getPGobject(
			GeometryTestdataProvider.getAbschnitt(kante.getGeometry(), LinearReferenzierterAbschnitt.of(0.5, 0.7))
				.reverse());

		Map<String, Object> expected_links_0_5__0_7 = new HashMap<>() {
			{
				put("StreckenID", "084000005000080084000005000060");
				put("Quell-ID", "-dlmId_0.5-0.7");
				put("GeometrieAbschnitt", geometry_links_0_5__0_7);
				put("Fuehrung", "401");
				put("Richtung", "1");
				put("Belag", "110");
				put("Laenge", 20);
				put("Licht", "1");
				put("Breite", 100);
				put("Lage", "2");
				put("B-Pflicht", 1);
				put("Bewertung", "4");
				put("Bewert_1", null);
				put("Baulast", "3");
				put("Baulast_3", null);
				put("Status", "10");
				put("Status_1", null);
				put("Status_2", null);
				put("Tempo", null);
				put("Str_name", "HHWJ");
				put("Datum", null);
				put("lebenszeitIntervallAnfang", null);
				put("lebenszeitIntervallEnde", null);
				put("Routen_ID", null);
				put("Routenname", null);
				put("ext_bw_EuroVelo", null);
				put("ext_bw_netzklassen", Netzklasse.KOMMUNALNETZ_FREIZEIT.name() + ";" + Netzklasse.RADNETZ_ALLTAG
					.name());
				put("ext_bw_RadNETZ_D", 0);
				put("ext_bw_Landesnetz", 1);
				put("ext_bw_Kreisnetz", 0);
				put("ext_bw_KommuNetz", 1);
				put("ext_bw_hat_gegenseite", 1);
			}
		};

		PGobject geometry_links_0_7__1 = PostGisHelper.getPGobject(
			GeometryTestdataProvider.getAbschnitt(kante.getGeometry(), LinearReferenzierterAbschnitt.of(0.7, 1))
				.reverse());

		Map<String, Object> expected_links_0_7__1 = new HashMap<>() {
			{
				put("StreckenID", "084000005000110084000005000080");
				put("Quell-ID", "-dlmId_0.7-1");
				put("GeometrieAbschnitt", geometry_links_0_7__1);
				put("Fuehrung", "401");
				put("Richtung", "1");
				put("Belag", "120");
				put("Laenge", 30);
				put("Licht", "1");
				put("Breite", 100);
				put("Lage", "2");
				put("B-Pflicht", 1);
				put("Bewertung", "4");
				put("Bewert_1", null);
				put("Baulast", "3");
				put("Baulast_3", null);
				put("Status", "10");
				put("Status_1", null);
				put("Status_2", null);
				put("Tempo", null);
				put("Str_name", "HHWJ");
				put("Datum", null);
				put("lebenszeitIntervallAnfang", null);
				put("lebenszeitIntervallEnde", null);
				put("Routen_ID", null);
				put("Routenname", null);
				put("ext_bw_EuroVelo", null);
				put("ext_bw_netzklassen", Netzklasse.KOMMUNALNETZ_FREIZEIT.name() + ";" + Netzklasse.RADNETZ_ALLTAG
					.name());
				put("ext_bw_RadNETZ_D", 0);
				put("ext_bw_Landesnetz", 1);
				put("ext_bw_Kreisnetz", 0);
				put("ext_bw_KommuNetz", 1);
				put("ext_bw_hat_gegenseite", 1);
			}
		};

		PGobject geometry_rechts_0__0_3 = PostGisHelper.getPGobject(
			GeometryTestdataProvider.getAbschnitt(kante.getGeometry(), LinearReferenzierterAbschnitt.of(0, 0.3)));

		Map<String, Object> expected_rechts_0__0_3 = new HashMap<>() {
			{
				put("StreckenID", "084000005000010084000005000040");
				put("Quell-ID", "dlmId_0-0.3");
				put("GeometrieAbschnitt", geometry_rechts_0__0_3);
				put("Fuehrung", "401");
				put("Richtung", "2");
				put("Belag", "132");
				put("Laenge", 30);
				put("Licht", "1");
				put("Breite", 100);
				put("Lage", "1");
				put("B-Pflicht", 1);
				put("Bewertung", "4");
				put("Bewert_1", null);
				put("Baulast", "3");
				put("Baulast_3", null);
				put("Status", "10");
				put("Status_1", null);
				put("Status_2", null);
				put("Tempo", 100);
				put("Str_name", "HHWJ");
				put("Datum", null);
				put("lebenszeitIntervallAnfang", null);
				put("lebenszeitIntervallEnde", null);
				put("Routen_ID", null);
				put("Routenname", null);
				put("ext_bw_EuroVelo", null);
				put("ext_bw_netzklassen", Netzklasse.KOMMUNALNETZ_FREIZEIT.name() + ";" + Netzklasse.RADNETZ_ALLTAG
					.name());
				put("ext_bw_RadNETZ_D", 0);
				put("ext_bw_Landesnetz", 1);
				put("ext_bw_Kreisnetz", 0);
				put("ext_bw_KommuNetz", 1);
				put("ext_bw_hat_gegenseite", 1);
			}
		};

		PGobject geometry_rechts_0_3__0_5 = PostGisHelper.getPGobject(
			GeometryTestdataProvider.getAbschnitt(kante.getGeometry(), LinearReferenzierterAbschnitt.of(0.3, 0.5)));

		Map<String, Object> expected_rechts_0_3__0_5 = new HashMap<>() {
			{
				put("StreckenID", "084000005000040084000005000060");
				put("Quell-ID", "dlmId_0.3-0.5");
				put("GeometrieAbschnitt", geometry_rechts_0_3__0_5);
				put("Fuehrung", "401");
				put("Richtung", "2");
				put("Belag", "132");
				put("Laenge", 20);
				put("Licht", "1");
				put("Breite", 100);
				put("Lage", "1");
				put("B-Pflicht", 1);
				put("Bewertung", "4");
				put("Bewert_1", null);
				put("Baulast", "3");
				put("Baulast_3", null);
				put("Status", "10");
				put("Status_1", null);
				put("Status_2", null);
				put("Tempo", 100);
				put("Str_name", "HHWJ");
				put("Datum", null);
				put("lebenszeitIntervallAnfang", null);
				put("lebenszeitIntervallEnde", null);
				put("Routen_ID", null);
				put("Routenname", null);
				put("ext_bw_EuroVelo", null);
				put("ext_bw_netzklassen", Netzklasse.KOMMUNALNETZ_FREIZEIT.name() + ";" + Netzklasse.RADNETZ_ALLTAG
					.name());
				put("ext_bw_RadNETZ_D", 0);
				put("ext_bw_Landesnetz", 1);
				put("ext_bw_Kreisnetz", 0);
				put("ext_bw_KommuNetz", 1);
				put("ext_bw_hat_gegenseite", 1);
			}
		};

		PGobject geometry_rechts_0_5__0_8 = PostGisHelper.getPGobject(
			GeometryTestdataProvider.getAbschnitt(kante.getGeometry(), LinearReferenzierterAbschnitt.of(0.5, 0.8)));

		Map<String, Object> expected_rechts_0_5__0_8 = new HashMap<>() {
			{
				put("StreckenID", "084000005000060084000005000090");
				put("Quell-ID", "dlmId_0.5-0.8");
				put("GeometrieAbschnitt", geometry_rechts_0_5__0_8);
				put("Fuehrung", "401");
				put("Richtung", "2");
				put("Belag", "132");
				put("Laenge", 30);
				put("Licht", "1");
				put("Breite", 100);
				put("Lage", "2");
				put("B-Pflicht", 1);
				put("Bewertung", "4");
				put("Bewert_1", null);
				put("Baulast", "3");
				put("Baulast_3", null);
				put("Status", "10");
				put("Status_1", null);
				put("Status_2", null);
				put("Tempo", null);
				put("Str_name", "HHWJ");
				put("Datum", null);
				put("lebenszeitIntervallAnfang", null);
				put("lebenszeitIntervallEnde", null);
				put("Routen_ID", null);
				put("Routenname", null);
				put("ext_bw_EuroVelo", null);
				put("ext_bw_netzklassen", Netzklasse.KOMMUNALNETZ_FREIZEIT.name() + ";" + Netzklasse.RADNETZ_ALLTAG
					.name());
				put("ext_bw_RadNETZ_D", 0);
				put("ext_bw_Landesnetz", 1);
				put("ext_bw_Kreisnetz", 0);
				put("ext_bw_KommuNetz", 1);
				put("ext_bw_hat_gegenseite", 1);
			}
		};

		PGobject geometry_rechts_0_8__1 = PostGisHelper.getPGobject(
			GeometryTestdataProvider.getAbschnitt(kante.getGeometry(), LinearReferenzierterAbschnitt.of(0.8, 1)));

		Map<String, Object> expected_rechts_0_8__1 = new HashMap<>() {
			{
				put("StreckenID", "084000005000090084000005000110");
				put("Quell-ID", "dlmId_0.8-1");
				put("GeometrieAbschnitt", geometry_rechts_0_8__1);
				put("Fuehrung", "401");
				put("Richtung", "2");
				put("Belag", "200");
				put("Laenge", 20);
				put("Licht", "1");
				put("Breite", 100);
				put("Lage", "2");
				put("B-Pflicht", 1);
				put("Bewertung", "4");
				put("Bewert_1", null);
				put("Baulast", "3");
				put("Baulast_3", null);
				put("Status", "10");
				put("Status_1", null);
				put("Status_2", null);
				put("Tempo", null);
				put("Str_name", "HHWJ");
				put("Datum", null);
				put("lebenszeitIntervallAnfang", null);
				put("lebenszeitIntervallEnde", null);
				put("Routen_ID", null);
				put("Routenname", null);
				put("ext_bw_EuroVelo", null);
				put("ext_bw_netzklassen", Netzklasse.KOMMUNALNETZ_FREIZEIT.name() + ";" + Netzklasse.RADNETZ_ALLTAG
					.name());
				put("ext_bw_RadNETZ_D", 0);
				put("ext_bw_Landesnetz", 1);
				put("ext_bw_Kreisnetz", 0);
				put("ext_bw_KommuNetz", 1);
				put("ext_bw_hat_gegenseite", 1);
			}
		};

		assertThat(resultList).hasSize(8);
		assertThat(resultList).contains(expected_links_0__0_3);
		assertThat(resultList).contains(expected_links_0_3__0_5);
		assertThat(resultList).contains(expected_links_0_5__0_7);
		assertThat(resultList).contains(expected_links_0_7__1);
		assertThat(resultList).contains(expected_rechts_0__0_3);
		assertThat(resultList).contains(expected_rechts_0_3__0_5);
		assertThat(resultList).contains(expected_rechts_0_5__0_8);
		assertThat(resultList).contains(expected_rechts_0_8__1);
	}

	@Test
	void testeGetEinseitigBefahrbareKanten() {
		Kante richtungsBasiert1 = KanteTestDataProvider.withFahrtrichtung(10, 10, 20, 20, Richtung.IN_RICHTUNG).build();
		Kante richtungsBasiert2 = KanteTestDataProvider.withFahrtrichtung(20, 20, 30, 30, Richtung.GEGEN_RICHTUNG)
			.build();
		Kante richtungsBasiert3 = KanteTestDataProvider.withFahrtrichtung(20, 20, 30, 30, Richtung.GEGEN_RICHTUNG)
			.dlmId(null)
			.quelle(QuellSystem.RadVis)
			.build();
		Kante richtungsBasiertAberNichtGrundnetz = KanteTestDataProvider.withFahrtrichtung(100, 100, 200, 200,
			Richtung.IN_RICHTUNG)
			.dlmId(null)
			.quelle(QuellSystem.RadNETZ).build();
		Kante beidseitig = KanteTestDataProvider.withFahrtrichtung(40, 40, 50, 50, Richtung.BEIDE_RICHTUNGEN)
			.build();
		Kante unbekannt = KanteTestDataProvider.withFahrtrichtung(30, 30, 40, 40, Richtung.UNBEKANNT)
			.build();

		kantenRepository.saveAll(
			List.of(richtungsBasiert1, richtungsBasiert2, richtungsBasiert3, richtungsBasiertAberNichtGrundnetz,
				beidseitig, unbekannt));

		entityManager.flush();
		entityManager.clear();

		List<Kante> result = kantenRepository.getEinseitigBefahrbareKanten().collect(Collectors.toList());

		assertThat(result.stream().map(Kante::getGeometry))
			.containsExactlyInAnyOrderElementsOf(
				Stream.of(richtungsBasiert1, richtungsBasiert2, richtungsBasiert3).map(Kante::getGeometry)
					.collect(Collectors.toList()));
	}

	@Test
	void getAnzahlKantenWhereLinestringEndsAreNotOnKnoten() {
		// arrange
		Knoten knoten1 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 0), QuellSystem.DLM).build();
		Knoten knoten2 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(100, 100), QuellSystem.DLM)
			.build();
		Knoten knoten3 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 100), QuellSystem.DLM)
			.build();

		// vernetzt
		kantenRepository.save(KanteTestDataProvider.fromKnoten(knoten1, knoten2)
			.geometry(GeometryTestdataProvider.createLineString(new Coordinate(0, 0.9), new Coordinate(100, 100)))
			.quelle(QuellSystem.DLM).build());

		Kante kanteNichtVernetztVon = kantenRepository.save(KanteTestDataProvider.fromKnoten(knoten1, knoten2)
			.geometry(GeometryTestdataProvider.createLineString(new Coordinate(0, 2), new Coordinate(100, 100)))
			.quelle(QuellSystem.DLM).build());
		Kante kanteNichtVernetztNach = kantenRepository.save(KanteTestDataProvider.fromKnoten(knoten2, knoten3)
			.geometry(GeometryTestdataProvider.createLineString(new Coordinate(100, 100), new Coordinate(2, 100)))
			.quelle(QuellSystem.DLM).build());

		// nicht vernetzt, aber falsches quellsystem
		kantenRepository.save(KanteTestDataProvider.fromKnoten(knoten1, knoten2)
			.geometry(GeometryTestdataProvider.createLineString(new Coordinate(0, 2), new Coordinate(100, 100)))
			.quelle(QuellSystem.RadNETZ).dlmId(null).build());

		entityManager.flush();
		entityManager.clear();

		// act
		List<Kante> kantenWhereLinestringEndsAreNotOnKnoten = kantenRepository
			.findKantenWhereLinestringEndsAreNotOnKnoten(1.0);

		// assert
		assertThat(kantenWhereLinestringEndsAreNotOnKnoten).containsExactlyInAnyOrder(kanteNichtVernetztVon,
			kanteNichtVernetztNach);

	}

	@Test
	void getFuerOsmAbbildungRelevanteKanten_inBereich_StatusNotDefault() {
		// arrange
		Kante innerhalb = kantenRepository.save(KanteTestDataProvider.withDefaultValues()
			.geometry(GeometryTestdataProvider.createLineString(new Coordinate(1, 1), new Coordinate(5, 5)))
			.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue()
				// Status weicht vom default ab
				.kantenAttribute(KantenAttribute.builder().status(Status.KONZEPTION).build())
				.build())
			.build());

		Kante ausserhalb = kantenRepository.save(KanteTestDataProvider.withDefaultValues()
			.geometry(GeometryTestdataProvider.createLineString(new Coordinate(100, 100), new Coordinate(105, 105)))
			.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue()
				// Status weicht vom default ab
				.kantenAttribute(KantenAttribute.builder().status(Status.IN_BAU).build())
				.build())
			.build());

		Envelope bereich = new Envelope(0, 30, 0, 30);

		entityManager.flush();
		entityManager.clear();
		kantenRepository.refreshNetzMaterializedViews();

		// act
		List<KanteGeometryView> kantenInBereich = kantenRepository.getFuerOsmAbbildungRelevanteKanten(bereich);
		// assert
		assertThat(kantenInBereich).extracting(KanteGeometryView::getId).doesNotContain(ausserhalb.getId());
		assertThat(kantenInBereich).extracting(KanteGeometryView::getId).containsExactlyInAnyOrder(innerhalb.getId());
		assertThat(kantenInBereich).extracting(KanteGeometryView::getGeometry).extracting(LineString::getCoordinates)
			.containsExactly(innerhalb.getGeometry().getCoordinates());
	}

	@Test
	void getFuerOsmAbbildungRelevanteKanten_noKantenWithDefault() {
		// arrange
		Kante defaultKante = kantenRepository.save(KanteTestDataProvider.withDefaultValues()
			.geometry(GeometryTestdataProvider.createLineString(new Coordinate(1, 1), new Coordinate(5, 5)))
			.build());

		Envelope bereich = new Envelope(0, 30, 0, 30);

		entityManager.flush();
		entityManager.clear();
		kantenRepository.refreshNetzMaterializedViews();

		// act
		List<KanteGeometryView> kantenInBereich = kantenRepository.getFuerOsmAbbildungRelevanteKanten(bereich);

		// assert
		assertThat(kantenInBereich).isEmpty();
	}

	@Test
	void getFuerOsmAbbildungRelevanteKanten_BelagNotDefault_statusFiktiv() {
		// arrange
		FuehrungsformAttribute fuehrungsformAttributeLinksRechts = FuehrungsformAttributeTestDataProvider
			.withGrundnetzDefaultwerte()
			.belagArt(BelagArt.BETON)
			.build();

		Kante notDefaultKante_statusFiktiv = kantenRepository.save(KanteTestDataProvider.withDefaultValues()
			.geometry(GeometryTestdataProvider.createLineString(new Coordinate(1, 1), new Coordinate(5, 5)))
			.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue()
				.kantenAttribute(KantenAttribute.builder().status(Status.FIKTIV).build()).build())
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
					.fuehrungsformAttributeLinks(List.of(fuehrungsformAttributeLinksRechts))
					.fuehrungsformAttributeRechts(List.of(fuehrungsformAttributeLinksRechts))
					.isZweiseitig(false)
					.build())
			.build());

		Kante notDefaultKante = createAndSaveDefaultKanteWithCustomFuehrungsform(fuehrungsformAttributeLinksRechts);

		Envelope bereich = new Envelope(0, 30, 0, 30);

		entityManager.flush();
		entityManager.clear();
		kantenRepository.refreshNetzMaterializedViews();

		// act
		List<KanteGeometryView> kantenInBereich = kantenRepository.getFuerOsmAbbildungRelevanteKanten(bereich);

		// assert
		assertThat(kantenInBereich).extracting(KanteGeometryView::getId)
			.doesNotContain(notDefaultKante_statusFiktiv.getId());
		assertThat(kantenInBereich).extracting(KanteGeometryView::getId)
			.containsExactlyInAnyOrder(notDefaultKante.getId());
	}

	@Test
	void getFuerOsmAbbildungRelevanteKanten_netzklassen() {
		// arrange
		Kante notDefaultKante = kantenRepository.save(KanteTestDataProvider.withDefaultValues()
			.geometry(GeometryTestdataProvider.createLineString(new Coordinate(1, 1), new Coordinate(5, 5)))
			.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue()
				.kantenAttribute(KantenAttribute.builder().build())
				.netzklassen(Set.of(Netzklasse.RADNETZ_ALLTAG))
				.build())
			.build());

		Envelope bereich = new Envelope(0, 30, 0, 30);

		entityManager.flush();
		entityManager.clear();
		kantenRepository.refreshNetzMaterializedViews();

		// act
		List<KanteGeometryView> kantenInBereich = kantenRepository.getFuerOsmAbbildungRelevanteKanten(bereich);

		// assert
		assertThat(kantenInBereich).extracting(KanteGeometryView::getId)
			.containsExactlyInAnyOrder(notDefaultKante.getId());
	}

	@Test
	void getFuerOsmAbbildungRelevanteKanten_defaultKante_droute() {
		// arrange
		Kante defaultKante = kantenRepository.save(KanteTestDataProvider.withDefaultValues()
			.geometry(GeometryTestdataProvider.createLineString(new Coordinate(1, 1), new Coordinate(5, 5)))
			.build());
		fahrradrouteRepository.save(
			FahrradrouteTestDataProvider.onKante(defaultKante)
				.drouteId(DrouteId.of("8"))
				.verantwortlich(null)
				.build());

		Envelope bereich = new Envelope(0, 30, 0, 30);

		entityManager.flush();
		entityManager.clear();
		kantenRepository.refreshNetzMaterializedViews();

		// act
		List<KanteGeometryView> kantenInBereich = kantenRepository.getFuerOsmAbbildungRelevanteKanten(bereich);

		// assert
		assertThat(kantenInBereich).extracting(KanteGeometryView::getId)
			.containsExactlyInAnyOrder(defaultKante.getId());
	}

	// Dieser Test dauert etwas zu lange, um in der Pipeline ausgeführt zu werden.
	// Hintergrund ist ein Bug bei dem die max. Anzahl gebundener Parametervalues
	// überschritten wurde (Postgres limit = 2 Bytes ~ 32000).
	@Test
	@Disabled
	void getFuerOsmAbbildungRelevanteKanten_largeResult() {
		// arrange
		List<Kante> kanten = new ArrayList<>();
		for (int i = 0; i < 40000; i++) {
			kanten.add(KanteTestDataProvider.withDefaultValues()
				.geometry(GeometryTestdataProvider.createLineString(new Coordinate(1, 1), new Coordinate(5, 5)))
				.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue()
					.kantenAttribute(KantenAttribute.builder().build())
					.netzklassen(Set.of(Netzklasse.RADNETZ_ALLTAG))
					.build())
				.build());
		}

		log.debug("Saving ...");
		kantenRepository.saveAll(kanten);
		log.debug("...done");

		Envelope bereich = new Envelope(0, 30, 0, 30);

		log.debug("Flush/Clear...");
		entityManager.flush();
		entityManager.clear();
		log.debug("...done");

		log.debug("Refreshing views...");
		kantenRepository.refreshNetzMaterializedViews();
		log.debug("... done");

		// act
		log.debug("Querying...");
		List<KanteGeometryView> kantenInBereich = kantenRepository.getFuerOsmAbbildungRelevanteKanten(bereich);
		log.debug("... done");

		// assert
		assertThat(kantenInBereich).hasSize(40000);
	}

	@Test
	void getFuerOsmAbbildungRelevanteKanten_FuehrungsformNotDefault() {
		// arrange
		Kante notDefaultKante_radverkehrsfuehrung = createAndSaveDefaultKanteWithCustomFuehrungsform(
			FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
				.radverkehrsfuehrung(Radverkehrsfuehrung.BETRIEBSWEG_FORST)
				.build());

		Kante notDefaultKante_breite = createAndSaveDefaultKanteWithCustomFuehrungsform(
			FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
				.breite(Laenge.of(2.1))
				.build());

		Kante notDefaultKante_oberfl = createAndSaveDefaultKanteWithCustomFuehrungsform(
			FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
				.oberflaechenbeschaffenheit(Oberflaechenbeschaffenheit.NEUWERTIG)
				.build());

		Envelope bereich = new Envelope(0, 30, 0, 30);

		entityManager.flush();
		entityManager.clear();
		kantenRepository.refreshNetzMaterializedViews();

		// act
		List<KanteGeometryView> kantenInBereich = kantenRepository.getFuerOsmAbbildungRelevanteKanten(bereich);

		// assert
		assertThat(kantenInBereich).hasSize(3);
		assertThat(kantenInBereich).extracting(KanteGeometryView::getId).containsExactlyInAnyOrder(
			notDefaultKante_breite.getId(), notDefaultKante_oberfl.getId(),
			notDefaultKante_radverkehrsfuehrung.getId());
	}

	@Test
	void getFuerOsmAbbildungRelevanteKanten_FuehrungsformZweiseitig() {
		// arrange
		Kante notDefaultKante_zweiseitig = kantenRepository.save(KanteTestDataProvider.withDefaultValues()
			.geometry(GeometryTestdataProvider.createLineString(new Coordinate(1, 1), new Coordinate(5, 5)))
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
					.isZweiseitig(true)
					// Links default
					.fuehrungsformAttributeLinks(
						List.of(FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte().build()))
					// Rechts nicht default
					.fuehrungsformAttributeRechts(
						List.of(FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
							.breite(Laenge.of(2.1))
							.build()))
					.build())
			.fahrtrichtungAttributGruppe(
				FahrtrichtungAttributGruppeTestDataProvider.withGrundnetzDefaultwerte().isZweiseitig(true).build())
			// Muss einmal oben an der FuehrungsformAG, einmal an der FahrtrichtungAG und hier an der Kante gesetzt
			// werden
			.isZweiseitig(true)
			.build());

		Envelope bereich = new Envelope(0, 30, 0, 30);

		entityManager.flush();
		entityManager.clear();
		kantenRepository.refreshNetzMaterializedViews();

		// act
		List<KanteGeometryView> kantenInBereich = kantenRepository.getFuerOsmAbbildungRelevanteKanten(bereich);

		// assert
		assertThat(kantenInBereich).extracting(KanteGeometryView::getId)
			.containsExactlyInAnyOrder(notDefaultKante_zweiseitig.getId());
	}

	@Test
	void getFuerOsmAbbildungRelevanteKanten_FuehrungsformLR() {
		// arrange
		// Erster Abschnitt default
		FuehrungsformAttribute fuehrungsformAttributeLinksRechts1 = FuehrungsformAttributeTestDataProvider
			.withGrundnetzDefaultwerte()
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.5))
			.build();
		// Zweiter Abschnitt nicht default
		FuehrungsformAttribute fuehrungsformAttributeLinksRechts2 = FuehrungsformAttributeTestDataProvider
			.withGrundnetzDefaultwerte()
			.belagArt(BelagArt.NATURSTEINPFLASTER)
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.5, 1.0))
			.build();
		Kante notDefaultKante_LR = kantenRepository.save(KanteTestDataProvider.withDefaultValues()
			.geometry(GeometryTestdataProvider.createLineString(new Coordinate(1, 1), new Coordinate(5, 5)))
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
					.fuehrungsformAttributeLinks(
						List.of(fuehrungsformAttributeLinksRechts1, fuehrungsformAttributeLinksRechts2))
					.fuehrungsformAttributeRechts(
						List.of(fuehrungsformAttributeLinksRechts1, fuehrungsformAttributeLinksRechts2))
					.isZweiseitig(false)
					.build())
			.build());

		Envelope bereich = new Envelope(0, 30, 0, 30);

		entityManager.flush();
		entityManager.clear();
		kantenRepository.refreshNetzMaterializedViews();

		// act
		List<KanteGeometryView> kantenInBereich = kantenRepository.getFuerOsmAbbildungRelevanteKanten(bereich);

		// assert
		assertThat(kantenInBereich).extracting(KanteGeometryView::getId)
			.containsExactlyInAnyOrder(notDefaultKante_LR.getId());
	}

	@Test
	void getKanteOsmAnreicherung_eineKompletteKante_alleAttribute() {
		// arrange
		Oberflaechenbeschaffenheit oberflaeche = Oberflaechenbeschaffenheit.GUTER_BIS_MITTLERER_ZUSTAND;
		Laenge breite = Laenge.of(0.25);
		Radverkehrsfuehrung radverkehrsfuehrung = Radverkehrsfuehrung.BETRIEBSWEG_FORST;
		BelagArt belagArt = BelagArt.NATURSTEINPFLASTER;
		Status status = Status.KONZEPTION;
		Set<Netzklasse> netzklassen = Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_FREIZEIT);

		long osmWayId1 = 123L;

		FuehrungsformAttribute fuehrungsformLinksRechts = FuehrungsformAttributeTestDataProvider
			.withGrundnetzDefaultwerte()
			.oberflaechenbeschaffenheit(oberflaeche)
			.breite(breite)
			.radverkehrsfuehrung(radverkehrsfuehrung)
			.belagArt(belagArt)
			.build();
		Kante attributierteKante = kantenRepository.save(KanteTestDataProvider.withDefaultValues()
			.kantenAttributGruppe(KantenAttributGruppe.builder()
				.kantenAttribute(KantenAttribute.builder().status(status).build())
				.netzklassen(netzklassen)
				.build())
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
					.fuehrungsformAttributeLinks(List.of(fuehrungsformLinksRechts))
					.fuehrungsformAttributeRechts(List.of(fuehrungsformLinksRechts))
					.isZweiseitig(false)
					.build())
			.build());

		long osmWayId2 = 456;
		Kante defaultKante = kantenRepository.save(KanteTestDataProvider.withDefaultValues().build());

		kantenRepository.insertOsmWayIds(List.of(
			new KanteOsmWayIdsInsert(
				attributierteKante.getId(),
				List.of(LinearReferenzierteOsmWayId.of(osmWayId1, LinearReferenzierterAbschnitt.of(0, 1)))),
			new KanteOsmWayIdsInsert(
				defaultKante.getId(),
				List.of(LinearReferenzierteOsmWayId.of(osmWayId2, LinearReferenzierterAbschnitt.of(0, 1))))));

		entityManager.flush();
		entityManager.clear();
		kantenRepository.refreshNetzMaterializedViews();

		// act
		List<KanteOsmMatchWithAttribute> flacheKanten = kantenRepository.getKanteOsmMatchesWithOsmAttributes(0.8)
			.collect(Collectors.toList());

		// assert
		assertThat(flacheKanten).hasSize(2);

		Optional<KanteOsmMatchWithAttribute> attrFlacheKante = flacheKanten.stream()
			.filter(k -> k.getKanteId().equals(attributierteKante.getId())).findFirst();
		assertThat(attrFlacheKante).isPresent();
		assertThat(attrFlacheKante.get()).usingRecursiveComparison().usingOverriddenEquals()
			.ignoringFields("netzklassen")
			.isEqualTo(new KanteOsmMatchWithAttribute(attributierteKante.getId(), osmWayId1, status.name(),
				null, // Netzklassen werden extra getestet
				radverkehrsfuehrung.name(), breite.getValue(), belagArt.name(),
				oberflaeche.name(), false));

		// Netzklassen werden als Set an der Kante gespeichert und sind hier als String zusammen gejoint.
		// Muessen also in allen permutationen getestet werden (hier einfach (a,b oder b,a).
		List<String> netzklassenAsStrings = netzklassen.stream().map(Enum::name).collect(Collectors.toList());
		List<String> netzklassenAsStringsReversedOrder = new ArrayList<>(netzklassenAsStrings);
		Collections.reverse(netzklassenAsStrings);

		assertThat(attrFlacheKante.get().getNetzklassen().get())
			.isIn(netzklassenAsStrings.stream().collect(Collectors.joining(";")),
				netzklassenAsStringsReversedOrder.stream().collect(Collectors.joining(";")));

		Optional<KanteOsmMatchWithAttribute> attrDefaultKante = flacheKanten.stream()
			.filter(k -> k.getKanteId().equals(defaultKante.getId())).findFirst();
		assertThat(attrDefaultKante).isPresent();
		assertThat(attrDefaultKante.get()).usingRecursiveComparison().usingOverriddenEquals()
			.isEqualTo(new KanteOsmMatchWithAttribute(defaultKante.getId(), osmWayId2, Status.UNTER_VERKEHR.name(),
				null, Radverkehrsfuehrung.UNBEKANNT.name(), null, BelagArt.UNBEKANNT.name(),
				Oberflaechenbeschaffenheit.UNBEKANNT.name(), false));
	}

	@Test
	void getKanteOsmAnreicherung_eineKanteUnter80Proz_nichtDrin() {
		// arrange
		long osmWayId = 123L;
		Kante kante = kantenRepository.save(KanteTestDataProvider.withDefaultValues().build());

		kantenRepository.insertOsmWayIds(List.of(new KanteOsmWayIdsInsert(
			kante.getId(),
			List.of(LinearReferenzierteOsmWayId.of(osmWayId, LinearReferenzierterAbschnitt.of(0, 0.5))))));

		entityManager.flush();
		entityManager.clear();
		kantenRepository.refreshNetzMaterializedViews();

		// act
		List<KanteOsmMatchWithAttribute> flacheKanten = kantenRepository.getKanteOsmMatchesWithOsmAttributes(0.8)
			.collect(Collectors.toList());

		// assert
		assertThat(flacheKanten).isEmpty();
	}

	@Test
	void getKanteOsmAnreicherung_zweiKanten_eineOsmWay() {
		// arrange
		long osmWayId = 123L;

		Oberflaechenbeschaffenheit oberflaeche = Oberflaechenbeschaffenheit.GUTER_BIS_MITTLERER_ZUSTAND;
		Laenge breite = Laenge.of(0.25);
		Radverkehrsfuehrung radverkehrsfuehrung = Radverkehrsfuehrung.BETRIEBSWEG_FORST;
		BelagArt belagArt = BelagArt.NATURSTEINPFLASTER;
		Status status = Status.KONZEPTION;
		Set<Netzklasse> netzklassen = Set.of(Netzklasse.RADNETZ_ALLTAG);

		FuehrungsformAttribute fuehrungsformLinksRechts = FuehrungsformAttributeTestDataProvider
			.withGrundnetzDefaultwerte()
			.oberflaechenbeschaffenheit(oberflaeche)
			.breite(breite)
			.radverkehrsfuehrung(radverkehrsfuehrung)
			.belagArt(belagArt)
			.build();
		Kante attributierteKante = kantenRepository.save(KanteTestDataProvider.withDefaultValues()
			.kantenAttributGruppe(KantenAttributGruppe.builder()
				.kantenAttribute(KantenAttribute.builder().status(status).build())
				.netzklassen(netzklassen)
				.build())
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
					.fuehrungsformAttributeLinks(List.of(fuehrungsformLinksRechts))
					.fuehrungsformAttributeRechts(List.of(fuehrungsformLinksRechts))
					.isZweiseitig(false)
					.build())
			.build());

		Kante defaultKante = kantenRepository.save(KanteTestDataProvider.withDefaultValues().build());

		kantenRepository.insertOsmWayIds(List.of(
			new KanteOsmWayIdsInsert(
				attributierteKante.getId(),
				List.of(LinearReferenzierteOsmWayId.of(osmWayId, LinearReferenzierterAbschnitt.of(0, 0.5)))),
			new KanteOsmWayIdsInsert(
				defaultKante.getId(),
				List.of(LinearReferenzierteOsmWayId.of(osmWayId, LinearReferenzierterAbschnitt.of(0.5, 0.9))))));

		entityManager.flush();
		entityManager.clear();
		kantenRepository.refreshNetzMaterializedViews();

		// act
		List<KanteOsmMatchWithAttribute> flacheKanten = kantenRepository.getKanteOsmMatchesWithOsmAttributes(0.8)
			.collect(Collectors.toList());

		// assert
		assertThat(flacheKanten).hasSize(1);

		Optional<KanteOsmMatchWithAttribute> attrFlacheKante = flacheKanten.stream()
			.filter(k -> k.getKanteId().equals(attributierteKante.getId())).findFirst();
		assertThat(attrFlacheKante).isPresent();
		assertThat(attrFlacheKante.get()).usingRecursiveComparison().usingOverriddenEquals()
			.isEqualTo(new KanteOsmMatchWithAttribute(attributierteKante.getId(), osmWayId, status.name(),
				netzklassen.stream().map(Enum::name).collect(Collectors.joining(";")),
				radverkehrsfuehrung.name(), breite.getValue(), belagArt.name(),
				oberflaeche.name(), false));

		Optional<KanteOsmMatchWithAttribute> defaultFlacheKante = flacheKanten.stream()
			.filter(k -> k.getKanteId().equals(defaultKante.getId())).findFirst();
		assertThat(defaultFlacheKante).isEmpty();
	}

	@Test
	void getKanteOsmAnreicherung_zweiKanten_beide0bis1_eineOsmWay() {
		// arrange
		long osmWayId = 123L;

		Kante defaultKante = kantenRepository.save(KanteTestDataProvider.withDefaultValues().build());
		Kante defaultKante2 = kantenRepository.save(KanteTestDataProvider.withDefaultValues().build());

		kantenRepository.insertOsmWayIds(List.of(
			new KanteOsmWayIdsInsert(
				defaultKante.getId(),
				List.of(LinearReferenzierteOsmWayId.of(osmWayId, LinearReferenzierterAbschnitt.of(0, 1)))),
			new KanteOsmWayIdsInsert(
				defaultKante2.getId(),
				List.of(LinearReferenzierteOsmWayId.of(osmWayId, LinearReferenzierterAbschnitt.of(0, 1))))));

		entityManager.flush();
		entityManager.clear();
		kantenRepository.refreshNetzMaterializedViews();

		// act
		List<KanteOsmMatchWithAttribute> flacheKanten = kantenRepository.getKanteOsmMatchesWithOsmAttributes(0.8)
			.collect(Collectors.toList());

		// assert
		// Es sollten beide Kanten gefunden werden, da sie beide von 0 bis 1 gehen und somit im Java Code entschieden
		// wird
		// welche verwendet wird
		assertThat(flacheKanten).hasSize(2);
	}

	@Test
	void teste_geoserver_radvisnetz_kante_materialized_view_getNetzklassenVonKante_AlleNetzklassen() {
		Kante kante_mitAllenNetzklassen = kantenRepository
			.save(KanteTestDataProvider.withDefaultValuesAndQuelle(QuellSystem.DLM)
				.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue().netzklassen(
					Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_FREIZEIT, Netzklasse.RADNETZ_ZIELNETZ,
						Netzklasse.KOMMUNALNETZ_ALLTAG, Netzklasse.KOMMUNALNETZ_FREIZEIT, Netzklasse.KREISNETZ_ALLTAG,
						Netzklasse.KREISNETZ_FREIZEIT, Netzklasse.RADSCHNELLVERBINDUNG, Netzklasse.RADVORRANGROUTEN))
					.build())
				.geometry(GeometryTestdataProvider.createLineString(new Coordinate(10, 10), new Coordinate(20, 20)))
				.build());

		entityManager.flush();
		entityManager.clear();
		kantenRepository.refreshNetzMaterializedViews();

		// act + assert
		Optional<String> netzklassenVonKante = kantenRepository.getNetzklassenVonKante(
			kante_mitAllenNetzklassen.getId());
		assertThat(netzklassenVonKante).isPresent();
		assertThat(netzklassenVonKante.get()).contains("RADNETZ_ALLTAG");
		assertThat(netzklassenVonKante.get()).contains("RADNETZ_FREIZEIT");
		assertThat(netzklassenVonKante.get()).contains("RADNETZ_ZIELNETZ");
		assertThat(netzklassenVonKante.get()).contains("RADSCHNELLVERBINDUNG");
		assertThat(netzklassenVonKante.get()).contains("RADVORRANGROUTEN");
		assertThat(netzklassenVonKante.get()).contains("KREISNETZ_FREIZEIT");
		assertThat(netzklassenVonKante.get()).contains("KREISNETZ_ALLTAG");
		assertThat(netzklassenVonKante.get()).contains("KOMMUNALNETZ_FREIZEIT");
		assertThat(netzklassenVonKante.get()).contains("KOMMUNALNETZ_ALLTAG");
	}

	@Test
	void teste_geoserver_radvisnetz_kante_materialized_view_getNetzklassenVonKante() {
		Kante kante_ohneNetzklasse = kantenRepository
			.save(KanteTestDataProvider.withDefaultValuesAndQuelle(QuellSystem.DLM)
				.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue()
					.build())
				.geometry(GeometryTestdataProvider.createLineString(new Coordinate(10, 20), new Coordinate(20, 20)))
				.build());

		entityManager.flush();
		entityManager.clear();
		kantenRepository.refreshNetzMaterializedViews();

		// act + assert
		Optional<String> netzklassenVonKante = kantenRepository.getNetzklassenVonKante(kante_ohneNetzklasse.getId());
		assertThat(netzklassenVonKante).isEmpty();
	}

	@Test
	void find_count_AllByQuelleDLMOrQuelleRadVisAndOutdated3dGeometry_3dgometry_null() {
		// arrange
		LineString lineString = GeometryTestdataProvider.createLineString(new Coordinate(10, 10),
			new Coordinate(20, 20));
		Kante kante = kantenRepository
			.save(KanteTestDataProvider.withDefaultValuesAndQuelle(QuellSystem.DLM)
				.geometry(lineString)
				.build());

		entityManager.flush();
		entityManager.clear();

		// act
		List<KanteElevationView> kanteElevationViews = kantenRepository
			.findFirst10ThousandByQuelleDLMOrQuelleRadVisAndOutdated3dGeometry()
			.stream().toList();

		// assert
		assertThat(kanteElevationViews).hasSize(1);
		assertThat(kanteElevationViews.get(0).getId()).isEqualTo(kante.getId());
		// Start und End, X und Y einzeln asserten, weil das eine ein LineString<C2D> ist und das andere ein "normaler"
		// LineString
		assertThat(kanteElevationViews.get(0).getGeometry().getStartPosition().getX())
			.isEqualTo(lineString.getStartPoint().getX());
		assertThat(kanteElevationViews.get(0).getGeometry().getStartPosition().getY())
			.isEqualTo(lineString.getStartPoint().getY());
		assertThat(kanteElevationViews.get(0).getGeometry().getEndPosition().getX())
			.isEqualTo(lineString.getEndPoint().getX());
		assertThat(kanteElevationViews.get(0).getGeometry().getEndPosition().getY())
			.isEqualTo(lineString.getEndPoint().getY());
	}

	@Test
	void find_count_AllByQuelleDLMOrQuelleRadVisAndOutdated3dGeometry_3dgometry_different() {
		// arrange
		LineString lineString = GeometryTestdataProvider.createLineString(new Coordinate(10, 10),
			new Coordinate(20, 20));
		LineString lineString_3d = GeometryTestdataProvider.createLineString(new Coordinate(12, 10, 666),
			new Coordinate(20, 20, 777));

		Kante kante = kantenRepository
			.save(KanteTestDataProvider.withDefaultValuesAndQuelle(QuellSystem.DLM)
				.geometry(lineString)
				.build());

		entityManager.flush();
		entityManager.clear();

		kantenRepository.updateKanteElevation(
			new SliceImpl<>(List.of(
				new KanteElevationUpdate(kante.getId(), lineString_3d))));

		// act
		List<KanteElevationView> kanteElevationViews = kantenRepository
			.findFirst10ThousandByQuelleDLMOrQuelleRadVisAndOutdated3dGeometry()
			.stream().toList();

		// assert
		assertThat(kanteElevationViews).hasSize(1);
		assertThat(kanteElevationViews.get(0).getId()).isEqualTo(kante.getId());
		// Start und End, X und Y einzeln asserten, weil das eine ein LineString<C2D> ist und das andere ein "normaler"
		// LineString
		assertThat(kanteElevationViews.get(0).getGeometry().getStartPosition().getX())
			.isEqualTo(lineString.getStartPoint().getX());
		assertThat(kanteElevationViews.get(0).getGeometry().getStartPosition().getY())
			.isEqualTo(lineString.getStartPoint().getY());
		assertThat(kanteElevationViews.get(0).getGeometry().getEndPosition().getX())
			.isEqualTo(lineString.getEndPoint().getX());
		assertThat(kanteElevationViews.get(0).getGeometry().getEndPosition().getY())
			.isEqualTo(lineString.getEndPoint().getY());
	}

	@Test
	void find_count_AllByQuelleDLMOrQuelleRadVisAndOutdated3dGeometry_3dgometry_same() {
		// arrange
		LineString lineString = GeometryTestdataProvider.createLineString(new Coordinate(10, 10),
			new Coordinate(20, 20));
		LineString lineString_3d = GeometryTestdataProvider.createLineString(new Coordinate(10, 10, 666),
			new Coordinate(20, 20, 777));

		Kante kante = kantenRepository
			.save(KanteTestDataProvider.withDefaultValuesAndQuelle(QuellSystem.DLM)
				.geometry(lineString)
				.build());

		entityManager.flush();
		entityManager.clear();

		kantenRepository.updateKanteElevation(
			new SliceImpl<>(List.of(
				new KanteElevationUpdate(kante.getId(), lineString_3d))));

		// act
		List<KanteElevationView> kanteElevationViews = kantenRepository
			.findFirst10ThousandByQuelleDLMOrQuelleRadVisAndOutdated3dGeometry()
			.stream().toList();

		// assert
		assertThat(kanteElevationViews).isEmpty();
	}

	@Test
	void findAllByZustaendigkeitAttributGruppeIn() {
		// arrange
		Kante kante1 = kantenRepository.save(KanteTestDataProvider.withDefaultValues().build());
		Kante kante2 = kantenRepository.save(KanteTestDataProvider.withDefaultValues().build());
		Kante kante3 = kantenRepository.save(KanteTestDataProvider.withDefaultValues().build());

		entityManager.flush();
		entityManager.clear();

		// act
		Set<Kante> result = kantenRepository.findAllByZustaendigkeitAttributGruppeIn(
			List.of(kante1.getZustaendigkeitAttributGruppe(), kante2.getZustaendigkeitAttributGruppe()));

		// arrange
		assertThat(result).hasSize(2);
		assertThat(result).contains(kante1, kante2);
		assertThat(result).doesNotContain(kante3);
	}

	@Test
	void findAllByGeschwindigkeitAttributeGruppeIn() {
		// arrange
		Kante kante1 = kantenRepository.save(KanteTestDataProvider.withDefaultValues().build());
		Kante kante2 = kantenRepository.save(KanteTestDataProvider.withDefaultValues().build());
		Kante kante3 = kantenRepository.save(KanteTestDataProvider.withDefaultValues().build());

		entityManager.flush();
		entityManager.clear();

		// act
		Set<Kante> result = kantenRepository.findAllByGeschwindigkeitAttributeGruppeIn(
			List.of(kante1.getGeschwindigkeitAttributGruppe(), kante2.getGeschwindigkeitAttributGruppe()));

		// arrange
		assertThat(result).hasSize(2);
		assertThat(result).contains(kante1, kante2);
		assertThat(result).doesNotContain(kante3);
	}

	@Test
	void findAllByFuehrungsformAttributGruppeIn() {
		// arrange
		Kante kante1 = kantenRepository.save(KanteTestDataProvider.withDefaultValues().build());
		Kante kante2 = kantenRepository.save(KanteTestDataProvider.withDefaultValues().build());
		Kante kante3 = kantenRepository.save(KanteTestDataProvider.withDefaultValues().build());

		entityManager.flush();
		entityManager.clear();

		// act
		Set<Kante> result = kantenRepository.findAllByFuehrungsformAttributGruppeIn(
			List.of(kante1.getFuehrungsformAttributGruppe(), kante2.getFuehrungsformAttributGruppe()));

		// arrange
		assertThat(result).hasSize(2);
		assertThat(result).contains(kante1, kante2);
		assertThat(result).doesNotContain(kante3);
	}

	private Kante createAndSaveDefaultKanteWithCustomFuehrungsform(FuehrungsformAttribute fuehrungsformLinksRechts) {
		return kantenRepository.save(KanteTestDataProvider.withDefaultValues()
			.geometry(GeometryTestdataProvider.createLineString(new Coordinate(1, 1), new Coordinate(5, 5)))
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
					.fuehrungsformAttributeLinks(List.of(fuehrungsformLinksRechts))
					.fuehrungsformAttributeRechts(List.of(fuehrungsformLinksRechts))
					.isZweiseitig(false)
					.build())
			.build());
	}

	private Kante createKante(Coordinate vonKoordinate, Coordinate nachKoordinate) {
		return createKante(vonKoordinate, nachKoordinate, QuellSystem.DLM);
	}

	private Kante createKante(Coordinate vonKoordinate, Coordinate nachKoordinate, QuellSystem quellSystem) {
		Knoten vonKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(vonKoordinate, quellSystem).build();
		Knoten nachKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(nachKoordinate, quellSystem).build();

		LineString lineStringInnerhalb = GEO_FACTORY
			.createLineString(new Coordinate[] { vonKnoten.getKoordinate(), nachKnoten.getKoordinate() });

		return KanteTestDataProvider.withDefaultValuesAndQuelle(quellSystem).vonKnoten(vonKnoten)
			.nachKnoten(nachKnoten)
			.geometry(lineStringInnerhalb).aufDlmAbgebildeteGeometry(null).kantenAttributGruppe(
				KantenAttributGruppeTestDataProvider.defaultValue().build())
			.build();
	}

	private static void assertThatLineStringsAreSimilar(LineString lineStringA, LineString lineStringB,
		double similarityThreshold) {
		assertThat(lineStringA.getCoordinates().length).isEqualTo(lineStringB.getCoordinates().length);

		double cummulatedError = 0d;

		for (int i = 0; i < lineStringA.getCoordinates().length; i++) {
			Coordinate coordA = lineStringA.getCoordinates()[i];
			Coordinate coordB = lineStringB.getCoordinates()[i];
			cummulatedError += coordA.distance(coordB);
		}

		double similarity = cummulatedError / lineStringA.getCoordinates().length;
		if (similarity < similarityThreshold) {
			log.info("Similarity: {}", similarity);
		}
		assertThat(similarity).isLessThan(similarityThreshold);
	}
}
