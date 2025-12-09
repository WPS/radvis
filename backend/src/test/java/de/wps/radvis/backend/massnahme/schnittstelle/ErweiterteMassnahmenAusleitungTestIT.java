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

package de.wps.radvis.backend.massnahme.schnittstelle;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.util.Assert;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.envers.repository.config.EnableEnversRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.benutzer.domain.repository.BenutzerRepository;
import de.wps.radvis.backend.benutzer.domain.valueObject.Name;
import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.PostGisHelper;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.LineareReferenz;
import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.dokument.DokumentConfiguration;
import de.wps.radvis.backend.dokument.domain.entity.DokumentListe;
import de.wps.radvis.backend.dokument.domain.entity.provider.DokumentTestDataProvider;
import de.wps.radvis.backend.kommentar.KommentarConfiguration;
import de.wps.radvis.backend.kommentar.domain.entity.KommentarListe;
import de.wps.radvis.backend.kommentar.domain.entity.provider.KommentarTestDataProvider;
import de.wps.radvis.backend.massnahme.MassnahmeConfiguration;
import de.wps.radvis.backend.massnahme.domain.bezug.NetzBezugTestDataProvider;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.massnahme.domain.entity.MassnahmeNetzBezug;
import de.wps.radvis.backend.massnahme.domain.entity.MassnahmeTestDataProvider;
import de.wps.radvis.backend.massnahme.domain.entity.Umsetzungsstand;
import de.wps.radvis.backend.massnahme.domain.repository.MassnahmeRepository;
import de.wps.radvis.backend.massnahme.domain.repository.UmsetzungsstandRepository;
import de.wps.radvis.backend.massnahme.domain.valueObject.Bezeichnung;
import de.wps.radvis.backend.massnahme.domain.valueObject.Durchfuehrungszeitraum;
import de.wps.radvis.backend.massnahme.domain.valueObject.GrundFuerAbweichungZumMassnahmenblatt;
import de.wps.radvis.backend.massnahme.domain.valueObject.GrundFuerNichtUmsetzungDerMassnahme;
import de.wps.radvis.backend.massnahme.domain.valueObject.Handlungsverantwortlicher;
import de.wps.radvis.backend.massnahme.domain.valueObject.Konzeptionsquelle;
import de.wps.radvis.backend.massnahme.domain.valueObject.Kostenannahme;
import de.wps.radvis.backend.massnahme.domain.valueObject.LGVFGID;
import de.wps.radvis.backend.massnahme.domain.valueObject.MaViSID;
import de.wps.radvis.backend.massnahme.domain.valueObject.MassnahmeKonzeptID;
import de.wps.radvis.backend.massnahme.domain.valueObject.MassnahmenPaketId;
import de.wps.radvis.backend.massnahme.domain.valueObject.Massnahmenkategorie;
import de.wps.radvis.backend.massnahme.domain.valueObject.Prioritaet;
import de.wps.radvis.backend.massnahme.domain.valueObject.PruefungQualitaetsstandardsErfolgt;
import de.wps.radvis.backend.massnahme.domain.valueObject.Realisierungshilfe;
import de.wps.radvis.backend.massnahme.domain.valueObject.Umsetzungsstatus;
import de.wps.radvis.backend.massnahme.domain.valueObject.VerbaID;
import de.wps.radvis.backend.netz.NetzConfiguration;
import de.wps.radvis.backend.netz.domain.NetzConfigurationProperties;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.repository.KnotenRepository;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.SollStandard;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.GebietskoerperschaftRepository;
import de.wps.radvis.backend.organisation.domain.OrganisationRepository;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Tag("group5")
@EnableEnversRepositories(basePackageClasses = { MassnahmeConfiguration.class,
	NetzConfiguration.class,
	OrganisationConfiguration.class, DokumentConfiguration.class, KommentarConfiguration.class,
	BenutzerConfiguration.class })
@EntityScan(basePackageClasses = { MassnahmeConfiguration.class,
	NetzConfiguration.class, OrganisationConfiguration.class, DokumentConfiguration.class,
	KommentarConfiguration.class, BenutzerConfiguration.class })
@EnableConfigurationProperties(value = { CommonConfigurationProperties.class, FeatureToggleProperties.class,
	PostgisConfigurationProperties.class, NetzConfigurationProperties.class })
@ContextConfiguration(classes = { CommonConfiguration.class, GeoConverterConfiguration.class })
public class ErweiterteMassnahmenAusleitungTestIT extends DBIntegrationTestIT {

	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	KantenRepository kantenRepository;
	@Autowired
	KnotenRepository knotenRepository;
	@Autowired
	BenutzerRepository benutzerRepository;
	@Autowired
	GebietskoerperschaftRepository gebietskoerperschaftRepository;
	@Autowired
	OrganisationRepository organisationRepository;
	@Autowired
	MassnahmeRepository massnahmeRepository;
	@Autowired
	UmsetzungsstandRepository umsetzungsstandRepository;

	@PersistenceContext
	EntityManager entityManager;

	@Test
	public void MassnahmenAusleitung_geometryKorrekt() throws ParseException {
		Kante kante = kantenRepository.save(KanteTestDataProvider.withDefaultValues().build());
		Kante kante2 = kantenRepository.save(KanteTestDataProvider.withDefaultValues()
			.geometry(
				GeometryTestdataProvider.createLineString(
					new Coordinate(42, 1000),
					new Coordinate(420, 1000)))
			.build());
		Knoten knoten = knotenRepository.save(KnotenTestDataProvider.withDefaultValues().build());

		Verwaltungseinheit verwaltungseinheit = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build());
		Benutzer benutzer = benutzerRepository.save(
			BenutzerTestDataProvider.defaultBenutzer().organisation(verwaltungseinheit).build());

		// NetzBezüge erstellen
		MassnahmeNetzBezug netzBezugNurKanteAbschnitt = new MassnahmeNetzBezug(
			Set.of(
				new AbschnittsweiserKantenSeitenBezug(
					kante, LinearReferenzierterAbschnitt.of(0, 0.5),
					Seitenbezug.BEIDSEITIG
				)), new HashSet<>(), new HashSet<>()
		);
		MassnahmeNetzBezug netzBezugNurKantePunktuell = NetzBezugTestDataProvider.forKantePunktuell(kante);
		MassnahmeNetzBezug netzBezugNurKnoten = NetzBezugTestDataProvider.forKnoten(knoten);

		MassnahmeNetzBezug netzBezugZweiKanten = NetzBezugTestDataProvider.forKanteAbschnittsweise(kante, kante2);

		MassnahmeNetzBezug netzBezugKanteAbschnittUndPunktuell = MassnahmeNetzBezug.vereinige(
			Set.of(netzBezugNurKanteAbschnitt, netzBezugNurKantePunktuell));
		MassnahmeNetzBezug netzBezugKanteAbschnittUndKnoten = MassnahmeNetzBezug.vereinige(
			Set.of(netzBezugNurKnoten, netzBezugNurKanteAbschnitt));
		MassnahmeNetzBezug netzBezugKantePunktuellUndKnoten = MassnahmeNetzBezug.vereinige(
			Set.of(netzBezugNurKantePunktuell, netzBezugNurKnoten));

		MassnahmeNetzBezug netzBezugKomplett = MassnahmeNetzBezug.vereinige(
			Set.of(netzBezugNurKanteAbschnitt, netzBezugNurKantePunktuell, netzBezugNurKnoten));

		// Massnahmen erstellen
		Massnahme massnahmeNetzBezugNurKanteAbschnitt = massnahmeRepository.save(
			MassnahmeTestDataProvider.withDefaultValuesBenutzerAndOrganisation(benutzer, verwaltungseinheit)
				.netzbezug(netzBezugNurKanteAbschnitt).build());
		Massnahme massnahmeNetzBezugNurKantePunktuell = massnahmeRepository.save(
			MassnahmeTestDataProvider.withDefaultValuesBenutzerAndOrganisation(benutzer, verwaltungseinheit)
				.netzbezug(netzBezugNurKantePunktuell)
				.build());
		Massnahme massnahmeNetzBezugNurKnoten = massnahmeRepository.save(
			MassnahmeTestDataProvider.withDefaultValuesBenutzerAndOrganisation(benutzer, verwaltungseinheit)
				.netzbezug(netzBezugNurKnoten).build());

		Massnahme massnahmeNetzBezugZweiKanten = massnahmeRepository.save(
			MassnahmeTestDataProvider.withDefaultValuesBenutzerAndOrganisation(benutzer, verwaltungseinheit)
				.netzbezug(netzBezugZweiKanten).build());

		Massnahme massnahmeNetzBezugKanteAbschnittUndPunktuell = massnahmeRepository.save(
			MassnahmeTestDataProvider.withDefaultValuesBenutzerAndOrganisation(benutzer, verwaltungseinheit)
				.netzbezug(netzBezugKanteAbschnittUndPunktuell).build());
		Massnahme massnahmeNetzBezugKanteAbschnittUndKnoten = massnahmeRepository.save(
			MassnahmeTestDataProvider.withDefaultValuesBenutzerAndOrganisation(benutzer, verwaltungseinheit)
				.netzbezug(netzBezugKanteAbschnittUndKnoten)
				.build());
		Massnahme massnahmeNetzBezugKantePunktuellUndKnoten = massnahmeRepository.save(
			MassnahmeTestDataProvider.withDefaultValuesBenutzerAndOrganisation(benutzer, verwaltungseinheit)
				.netzbezug(netzBezugKantePunktuellUndKnoten)
				.build());
		Massnahme massnahmeNetzBezugKomplett = massnahmeRepository.save(
			MassnahmeTestDataProvider.withDefaultValuesBenutzerAndOrganisation(benutzer, verwaltungseinheit)
				.netzbezug(netzBezugKomplett).build());

		Massnahme massnahmeMitOriginalGeometrieUndNetzBezug = massnahmeRepository.save(
			MassnahmeTestDataProvider.withDefaultValuesBenutzerAndOrganisation(benutzer, verwaltungseinheit)
				.originalRadNETZGeometrie(
					GeometryTestdataProvider.createLineString()).netzbezug(NetzBezugTestDataProvider.forKnoten(knoten))
				.build());

		Massnahme massnahmeMitOriginalGeometrieOhneNetzBezug = massnahmeRepository.save(
			MassnahmeTestDataProvider.withDefaultValuesBenutzerAndOrganisation(benutzer, verwaltungseinheit)
				.originalRadNETZGeometrie(
					GeometryTestdataProvider.createLineString()).netzbezug(NetzBezugTestDataProvider.empty()).build());

		Massnahme massnahmeOhneOriginalGeometrieOhneNetzBezug = massnahmeRepository.save(
			MassnahmeTestDataProvider.withDefaultValuesBenutzerAndOrganisation(benutzer, verwaltungseinheit)
				.originalRadNETZGeometrie(null)
				.netzbezug(NetzBezugTestDataProvider.empty()).build());

		entityManager.flush();
		entityManager.clear();

		massnahmeRepository.refreshMassnahmeMaterializedViews();

		Map<Long, Map<String, Object>> allViewEntriesById = jdbcTemplate
			.queryForList("SELECT * FROM geoserver_massnahmen_erweitert_view").stream().collect(Collectors.toMap(
				properties -> (Long) properties.get("radvis_massnahme_id"),
				Function.identity()
			));

		assertThat(allViewEntriesById).hasSize(11);

		// Nur KantenAbschnitte
		Map<String, Object> propertiesNurKantenAbschnitt = allViewEntriesById.get(massnahmeNetzBezugNurKanteAbschnitt
			.getId());
		MultiLineString expectedGeomNurKantenAbschnitt = GeometryTestdataProvider.createMultiLineString(
			GeometryTestdataProvider.getAbschnitt(kante.getGeometry(), LinearReferenzierterAbschnitt.of(0, 0.5)));

		Geometry actualGeomNurKantenAbschnitt = PostGisHelper.getGeometryFromPGobject(
			(PGobject) propertiesNurKantenAbschnitt.get("geometry"));

		// AssertJ funktioniert aus irgendeinem Grund bekanntlich nicht mit locationtech-Geometrien
		// Locatintech bietet hier aber eine Utility für an!
		Assert.equals(expectedGeomNurKantenAbschnitt, actualGeomNurKantenAbschnitt);

		assertThat(propertiesNurKantenAbschnitt.get("ist_originalgeometrie")).isEqualTo("nein");

		// Nur Punkte auf Kanten
		Map<String, Object> propertiesNurKantenPunktuell = allViewEntriesById.get(massnahmeNetzBezugNurKantePunktuell
			.getId());
		MultiPoint expectedGeomNurKantenPunktuell = GeometryTestdataProvider.createMultiPoint(
			GeometryTestdataProvider.getPunktAufKante(kante.getGeometry(), LineareReferenz.of(0.5)));

		Geometry actualGeomNurKantenPunktuell = PostGisHelper.getGeometryFromPGobject(
			(PGobject) propertiesNurKantenPunktuell.get("geometry"));

		Assert.equals(expectedGeomNurKantenPunktuell, actualGeomNurKantenPunktuell);

		assertThat(propertiesNurKantenPunktuell.get("ist_originalgeometrie")).isEqualTo("nein");

		// Nur Knoten
		Map<String, Object> propertiesNurKnoten = allViewEntriesById.get(massnahmeNetzBezugNurKnoten.getId());
		MultiPoint expectedGeomNurKnoten = GeometryTestdataProvider.createMultiPoint(knoten.getPoint());

		Geometry actualGeomNurKnoten = PostGisHelper.getGeometryFromPGobject(
			(PGobject) propertiesNurKnoten
				.get("geometry"));
		Assert.equals(expectedGeomNurKnoten, actualGeomNurKnoten);

		assertThat(propertiesNurKnoten.get("ist_originalgeometrie")).isEqualTo("nein");

		// Zwei Kanten
		Map<String, Object> propertiesZweiKanten = allViewEntriesById.get(massnahmeNetzBezugZweiKanten.getId());
		MultiLineString expectedGeomZweiKanten = GeometryTestdataProvider.createMultiLineString(
			kante.getGeometry(), kante2.getGeometry());

		Geometry actualGeomZweiKanten = PostGisHelper.getGeometryFromPGobject(
			(PGobject) propertiesZweiKanten.get("geometry"));

		// Da hier die Reihenfolge der MultiLineString Komponenten eine Rolle spielt,
		// muss die Geometrie normalisiert verglichen werden
		assertThat(expectedGeomZweiKanten.equalsNorm(actualGeomZweiKanten)).isTrue();

		assertThat(propertiesZweiKanten.get("ist_originalgeometrie")).isEqualTo("nein");

		//  KantenAbschnitte und Punkte auf Kanten
		Map<String, Object> propertiesKanteAbschnittUndPunktuell = allViewEntriesById.get(
			massnahmeNetzBezugKanteAbschnittUndPunktuell.getId());
		GeometryCollection expectedGeomKanteAbschnittUndPunktuell = GeometryTestdataProvider.creatGeometryCollection(
			expectedGeomNurKantenPunktuell, expectedGeomNurKantenAbschnitt
		);

		Geometry actualGeomKanteAbschnittUndPunktuell = PostGisHelper.getGeometryFromPGobject(
			(PGobject) propertiesKanteAbschnittUndPunktuell
				.get("geometry"));
		Assert.equals(expectedGeomKanteAbschnittUndPunktuell, actualGeomKanteAbschnittUndPunktuell);
		assertThat(propertiesKanteAbschnittUndPunktuell.get("ist_originalgeometrie")).isEqualTo("nein");

		//  KantenAbschnitte und Knoten
		Map<String, Object> propertiesKanteAbschnittUndKnoten = allViewEntriesById.get(
			massnahmeNetzBezugKanteAbschnittUndKnoten.getId());
		GeometryCollection expectedGeomKanteAbschnittUndKnoten = GeometryTestdataProvider.creatGeometryCollection(
			expectedGeomNurKnoten, expectedGeomNurKantenAbschnitt
		);

		Geometry actualGeomKanteAbschnittUndKnoten = PostGisHelper.getGeometryFromPGobject(
			(PGobject) propertiesKanteAbschnittUndKnoten
				.get("geometry"));
		Assert.equals(expectedGeomKanteAbschnittUndKnoten, actualGeomKanteAbschnittUndKnoten);

		assertThat(propertiesKanteAbschnittUndKnoten.get("ist_originalgeometrie")).isEqualTo("nein");

		//  Knoten und Punkte auf Kanten
		Map<String, Object> propertiesKantenPunktuellUndKnoten = allViewEntriesById.get(
			massnahmeNetzBezugKantePunktuellUndKnoten.getId());

		MultiPoint expectedGeomKantePunktuellUndKnoten = GeometryTestdataProvider.mergeMultiPoint(
			expectedGeomNurKantenPunktuell, expectedGeomNurKnoten
		);

		Geometry actualGeomKantePunktuellUndKnoten = PostGisHelper.getGeometryFromPGobject(
			(PGobject) propertiesKantenPunktuellUndKnoten
				.get("geometry"));
		Assert.equals(expectedGeomKantePunktuellUndKnoten, actualGeomKantePunktuellUndKnoten);

		assertThat(propertiesKantenPunktuellUndKnoten.get("ist_originalgeometrie")).isEqualTo("nein");

		//  Kompletter Netzbezug - alle Arten
		Map<String, Object> propertiesGeomKomplett = allViewEntriesById.get(massnahmeNetzBezugKomplett.getId());

		GeometryCollection expectedGeomKomplett = GeometryTestdataProvider.creatGeometryCollection(
			GeometryTestdataProvider.mergeMultiPoint(expectedGeomNurKantenPunktuell, expectedGeomNurKnoten),
			expectedGeomNurKantenAbschnitt
		);

		Geometry actualGeomKomplett = PostGisHelper.getGeometryFromPGobject((PGobject) propertiesGeomKomplett
			.get("geometry"));
		Assert.equals(expectedGeomKomplett, actualGeomKomplett);

		assertThat(propertiesGeomKomplett.get("ist_originalgeometrie")).isEqualTo("nein");

		//  Original Geometrie ohne Netzbezug
		Map<String, Object> propertiesOriginalGeometrieOhneNetzbezug = allViewEntriesById.get(
			massnahmeMitOriginalGeometrieOhneNetzBezug.getId());
		Geometry actualGeomOriginalGeometrieOhneNetzBezug = PostGisHelper.getGeometryFromPGobject(
			(PGobject) propertiesOriginalGeometrieOhneNetzbezug
				.get("geometry"));
		Assert.equals(
			massnahmeMitOriginalGeometrieOhneNetzBezug.getOriginalRadNETZGeometrie(),
			actualGeomOriginalGeometrieOhneNetzBezug
		);

		assertThat(propertiesOriginalGeometrieOhneNetzbezug.get("ist_originalgeometrie")).isEqualTo("ja");

		//  Original Geometrie ohne Netzbezug
		Map<String, Object> propertiesOriginalGeometrieMitNetzBezug = allViewEntriesById.get(
			massnahmeMitOriginalGeometrieUndNetzBezug.getId());
		Geometry actualGeomOriginalGeometrieMitNetzBezug = PostGisHelper.getGeometryFromPGobject(
			(PGobject) propertiesOriginalGeometrieMitNetzBezug.get("geometry"));
		Assert.equals(
			massnahmeMitOriginalGeometrieOhneNetzBezug.getOriginalRadNETZGeometrie(),
			actualGeomOriginalGeometrieMitNetzBezug
		);
		assertThat(propertiesOriginalGeometrieMitNetzBezug.get("ist_originalgeometrie")).isEqualTo("ja");

		//  Ohne Original Geometrie und ohne Netzbezug
		Map<String, Object> propertiesOhneOriginalGeometrieOhneNetzbezug = allViewEntriesById.get(
			massnahmeOhneOriginalGeometrieOhneNetzBezug.getId());
		Object actualGeomOhneOriginalGeometrieOhneNetzBezug = propertiesOhneOriginalGeometrieOhneNetzbezug
			.get("geometry");

		assertThat(actualGeomOhneOriginalGeometrieOhneNetzBezug).isNull();
		assertThat(propertiesOhneOriginalGeometrieOhneNetzbezug.get("ist_originalgeometrie")).isEqualTo("nein");
	}

	@Test
	public void MassnahmenAusleitung_orgaKorrekt() throws SQLException, ParseException {
		Kante kante = kantenRepository.save(KanteTestDataProvider.withDefaultValues().build());

		Verwaltungseinheit baulasttraeger = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
				.name("Baulastträger")
				.organisationsArt(OrganisationsArt.KREIS)
				.build());

		Verwaltungseinheit zustaendiger = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
				.name("Zuständiger")
				.organisationsArt(OrganisationsArt.REGIERUNGSBEZIRK)
				.build());

		Verwaltungseinheit unterhaltsZustaendiger = organisationRepository.save(
			VerwaltungseinheitTestDataProvider.defaultOrganisation()
				.name("Unterhaltszuständiger")
				.organisationsArt(OrganisationsArt.TOURISMUSVERBAND)
				.build());

		Benutzer benutzer = benutzerRepository.save(
			BenutzerTestDataProvider.defaultBenutzer().organisation(zustaendiger).build());

		massnahmeRepository.save(
			MassnahmeTestDataProvider.withDefaultValuesBenutzerAndOrganisation(benutzer, zustaendiger)
				.zustaendiger(zustaendiger)
				.baulastZustaendiger(baulasttraeger)
				.unterhaltsZustaendiger(unterhaltsZustaendiger)
				.netzbezug(NetzBezugTestDataProvider.forKantePunktuell(kante))
				.build());

		entityManager.flush();
		entityManager.clear();

		massnahmeRepository.refreshMassnahmeMaterializedViews();

		List<Map<String, Object>> allViewEntries = jdbcTemplate
			.queryForList("SELECT * FROM geoserver_massnahmen_erweitert_view").stream().toList();

		assertThat(allViewEntries).hasSize(1);
		Map<String, Object> massnahmenRow = allViewEntries.get(0);
		assertThat(massnahmenRow.get("baulasttraeger_name")).isEqualTo(
			baulasttraeger.getName() + " (" + OrganisationsArt.KREIS.name() + ")");
		assertThat(massnahmenRow.get("zustaendiger_name")).isEqualTo(
			zustaendiger.getName() + " (" + OrganisationsArt.REGIERUNGSBEZIRK.name() + ")");
		assertThat(massnahmenRow.get("unterhaltszustaeniger_name")).isEqualTo(
			unterhaltsZustaendiger.getName() + " (" + OrganisationsArt.TOURISMUSVERBAND.name() + ")");
	}

	@Test
	public void MassnahmenAusleitung_dokumenteKorrekt() {
		Kante kante = kantenRepository.save(KanteTestDataProvider.withDefaultValues().build());

		Verwaltungseinheit verwaltungseinheit = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().organisationsArt(OrganisationsArt.KREIS)
				.name("Baulastträger").build());

		Benutzer benutzer = benutzerRepository.save(
			BenutzerTestDataProvider.defaultBenutzer().organisation(verwaltungseinheit).build());

		DokumentListe dokumentListe = new DokumentListe();

		dokumentListe.addDokument(DokumentTestDataProvider.withDefaultValues()
			.benutzer(benutzer)
			.dateiname("test.pdf")
			.build());

		dokumentListe.addDokument(DokumentTestDataProvider.withDefaultValues()
			.benutzer(benutzer)
			.dateiname("test2.pdf")
			.build());

		massnahmeRepository.save(
			MassnahmeTestDataProvider.withDefaultValuesBenutzerAndOrganisation(benutzer, verwaltungseinheit)
				.netzbezug(NetzBezugTestDataProvider.forKantePunktuell(kante))
				.dokumentListe(dokumentListe)
				.build());

		entityManager.flush();
		entityManager.clear();

		massnahmeRepository.refreshMassnahmeMaterializedViews();

		List<Map<String, Object>> allViewEntries = jdbcTemplate
			.queryForList("SELECT * FROM geoserver_massnahmen_erweitert_view").stream().toList();

		assertThat(allViewEntries).hasSize(1);
		Map<String, Object> massnahmenRow = allViewEntries.get(0);
		assertThat(massnahmenRow.get("dateinamen")).isEqualTo("test.pdf\ntest2.pdf");
	}

	@Test
	public void MassnahmenAusleitung_kommentareKorrekt() {
		Kante kante = kantenRepository.save(KanteTestDataProvider.withDefaultValues().build());

		Verwaltungseinheit verwaltungseinheit = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().organisationsArt(OrganisationsArt.KREIS)
				.name("Baulastträger").build());

		Benutzer hansenpeter = benutzerRepository.save(
			BenutzerTestDataProvider.kreiskoordinator(verwaltungseinheit)
				.organisation(verwaltungseinheit)
				.vorname(Name.of("Hans-Peter"))
				.nachname(Name.of("Hansenpeter"))
				.build());

		Benutzer saulgoodman = benutzerRepository.save(
			BenutzerTestDataProvider.defaultBenutzer()
				.organisation(verwaltungseinheit)
				.vorname(Name.of("Saul"))
				.nachname(Name.of("Goodman"))
				.build());

		KommentarListe kommentarListe = new KommentarListe();

		kommentarListe.addKommentar(KommentarTestDataProvider.withDefaultValues()
			.benutzer(hansenpeter)
			.kommentarText("So nicht, bitte!")
			.datum(LocalDateTime.of(2023, 1, 26, 14, 31))
			.build());

		kommentarListe.addKommentar(KommentarTestDataProvider.withDefaultValues()
			.benutzer(saulgoodman)
			.kommentarText("It's all good, man!")
			.datum(LocalDateTime.of(2023, 1, 26, 14, 32)) // ein min später
			.build());

		Massnahme massnahme = massnahmeRepository.save(
			MassnahmeTestDataProvider.withDefaultValuesBenutzerAndOrganisation(saulgoodman, verwaltungseinheit)
				.netzbezug(NetzBezugTestDataProvider.forKantePunktuell(kante))
				.kommentarListe(kommentarListe)
				.build());

		entityManager.flush();
		entityManager.clear();

		massnahmeRepository.refreshMassnahmeMaterializedViews();

		List<Map<String, Object>> allViewEntries = jdbcTemplate
			.queryForList("SELECT * FROM geoserver_massnahmen_erweitert_view").stream().toList();

		assertThat(allViewEntries).hasSize(1);
		Map<String, Object> massnahmenRow = allViewEntries.get(0);
		assertThat(massnahmenRow.get("kommentare")).isEqualTo(
			"Kommentar-ID: " + massnahme.getKommentarListe().getKommentare().get(0).getId() + "\n" +
				"Benutzer: Hans-Peter Hansenpeter\n" +
				"Datum: 2023-01-26 14:31:00\n" +
				"So nicht, bitte!\n" +
				"\n" +
				"Kommentar-ID: " + massnahme.getKommentarListe().getKommentare().get(1).getId() + "\n" +
				"Benutzer: Saul Goodman\n" +
				"Datum: 2023-01-26 14:32:00\n" +
				"It's all good, man!");
	}

	@Test
	public void MassnahmenAusleitung_umsetzungsstand() {
		Kante kante = kantenRepository.save(KanteTestDataProvider.withDefaultValues().build());

		Verwaltungseinheit verwaltungseinheit = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().organisationsArt(OrganisationsArt.KREIS)
				.name("Baulastträger").build());

		Benutzer benutzer = benutzerRepository.save(
			BenutzerTestDataProvider.defaultBenutzer().organisation(verwaltungseinheit).build());

		Massnahme massnahme = massnahmeRepository.save(
			MassnahmeTestDataProvider.withDefaultValuesBenutzerAndOrganisation(
				benutzer, verwaltungseinheit)
				.netzbezug(NetzBezugTestDataProvider.forKantePunktuell(kante))
				.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME_2024)
				.umsetzungsstatus(Umsetzungsstatus.PLANUNG)
				.build()
		);

		Umsetzungsstand umsetzungsstand = massnahme.getUmsetzungsstand().orElseThrow();
		umsetzungsstand.update(
			true,
			LocalDateTime.of(2023, 1, 26, 14, 31),
			benutzer,
			GrundFuerAbweichungZumMassnahmenblatt.RADNETZ_WURDE_VERLEGT,
			PruefungQualitaetsstandardsErfolgt.NEIN_ERFOLGT_NOCH,
			"beschreibungAbweichenderMassnahme",
			15000L,
			GrundFuerNichtUmsetzungDerMassnahme.NOCH_IN_PLANUNG_UMSETZUNG,
			"Anmerkung",
			Umsetzungsstatus.PLANUNG

		);

		umsetzungsstandRepository.save(umsetzungsstand);

		massnahmeRepository.save(massnahme);

		entityManager.flush();
		entityManager.clear();

		massnahmeRepository.refreshMassnahmeMaterializedViews();

		List<Map<String, Object>> allViewEntries = jdbcTemplate
			.queryForList("SELECT * FROM geoserver_massnahmen_erweitert_view").stream().toList();

		assertThat(allViewEntries).hasSize(1);

		Map<String, Object> properties = allViewEntries.get(0);

		assertThat(properties.get("radvis_umsetzungsstand_id")).isEqualTo(umsetzungsstand.getId());
		assertThat(properties.get("umsetzungsstand_status")).isEqualTo("AKTUALISIERT");
		assertThat(properties.get("umsetzung_gemaess_massnahmenblatt")).isEqualTo(true);
		assertThat(properties.get("grund_fuer_abweichung_zum_massnahmenblatt")).isEqualTo("RADNETZ_WURDE_VERLEGT");
		assertThat(properties.get("pruefung_qualitaetsstandards_erfolgt")).isEqualTo("NEIN_ERFOLGT_NOCH");
		assertThat(properties.get("beschreibung_abweichender_massnahme")).isEqualTo(
			"beschreibungAbweichenderMassnahme");
		assertThat(properties.get("kosten_der_massnahme")).isEqualTo(15000L);
		assertThat(properties.get("grund_fuer_nicht_umsetzung_der_massnahme")).isEqualTo("NOCH_IN_PLANUNG_UMSETZUNG");
		assertThat(properties.get("anmerkung")).isEqualTo("Anmerkung");
		assertThat(properties.get("umsetzungsstand_letzte_aenderung")).isEqualTo(Timestamp.valueOf(
			"2023-01-26 14:31:00"));
	}

	@Test
	public void MassnahmenAusleitung_basicAttributesKorrekt() {
		Kante kante = kantenRepository.save(KanteTestDataProvider.withDefaultValues().build());

		Verwaltungseinheit verwaltungseinheit = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().organisationsArt(OrganisationsArt.KREIS)
				.name("Baulastträger").build());

		Benutzer benutzer = benutzerRepository.save(
			BenutzerTestDataProvider.defaultBenutzer().organisation(verwaltungseinheit).build());

		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValuesBenutzerAndOrganisation(
			benutzer, verwaltungseinheit)
			.netzbezug(NetzBezugTestDataProvider.forKantePunktuell(kante))
			.massnahmeKonzeptId(MassnahmeKonzeptID.of("massnahmeKonzeptId"))
			.massnahmenkategorien(Set.of(
				Massnahmenkategorie.MARKIERUNGSTECHNISCHE_MASSNAHME,
				Massnahmenkategorie.EINRICHTUNG_FAHRRADSTRASSE))
			.netzklassen(Set.of(
				Netzklasse.RADNETZ_ALLTAG,
				Netzklasse.RADNETZ_FREIZEIT,
				Netzklasse.KREISNETZ_FREIZEIT
			))
			.umsetzungsstatus(Umsetzungsstatus.PLANUNG)
			.bezeichnung(Bezeichnung.of("Bezeichnung"))
			.durchfuehrungszeitraum(Durchfuehrungszeitraum.of(2025))
			.konzeptionsquelle(Konzeptionsquelle.SONSTIGE)
			.sonstigeKonzeptionsquelle("Sonstige Konzeptionsquelle")
			.veroeffentlicht(true)
			.sollStandard(SollStandard.ZIELSTANDARD_RADNETZ)
			.lgvfgid(LGVFGID.of("lgvfgid"))
			.maViSID(MaViSID.of("maViSID"))
			.verbaID(VerbaID.of("verbaID"))
			.prioritaet(Prioritaet.of(8))
			.letzteAenderung(LocalDateTime.of(2023, 1, 26, 14, 31))
			.handlungsverantwortlicher(Handlungsverantwortlicher.BAULASTTRAEGER)
			.massnahmenPaketId(MassnahmenPaketId.of("massnahmenPaketId"))
			.planungErforderlich(false)
			.realisierungshilfe(Realisierungshilfe.NR_2_2_1)
			.kostenannahme(Kostenannahme.of(10000L))
			.build();

		massnahmeRepository.save(massnahme);

		entityManager.flush();
		entityManager.clear();

		massnahmeRepository.refreshMassnahmeMaterializedViews();

		List<Map<String, Object>> allViewEntries = jdbcTemplate
			.queryForList("SELECT * FROM geoserver_massnahmen_erweitert_view").stream().toList();

		assertThat(allViewEntries).hasSize(1);

		Map<String, Object> properties = allViewEntries.get(0);

		assertThat(properties.get("massnahme_konzept_id")).isEqualTo("massnahmeKonzeptId");
		assertThat(properties.get("radvis_massnahme_id")).isEqualTo(massnahme.getId());
		assertThat(properties.get("massnahmenkategorien")).isEqualTo(
			"EINRICHTUNG_FAHRRADSTRASSE;MARKIERUNGSTECHNISCHE_MASSNAHME");
		assertThat(properties.get("massnahmennetzklassen")).isEqualTo(
			"KREISNETZ_FREIZEIT;RADNETZ_ALLTAG;RADNETZ_FREIZEIT");
		assertThat(properties.get("umsetzungsstatus")).isEqualTo("PLANUNG");
		assertThat(properties.get("bezeichnung")).isEqualTo("Bezeichnung");
		assertThat(properties.get("durchfuehrungszeitraum")).isEqualTo(2025);
		assertThat(properties.get("konzeptionsquelle")).isEqualTo("SONSTIGE");
		assertThat(properties.get("sonstige_konzeptionsquelle")).isEqualTo("Sonstige Konzeptionsquelle");
		assertThat(properties.get("veroeffentlicht")).isEqualTo(true);
		assertThat(properties.get("soll_standard")).isEqualTo("ZIELSTANDARD_RADNETZ");
		assertThat(properties.get("lgvfgid")).isEqualTo("lgvfgid");
		assertThat(properties.get("ma_visid")).isEqualTo("maViSID");
		assertThat(properties.get("verbaid")).isEqualTo("verbaID");
		assertThat(properties.get("prioritaet")).isEqualTo(8);
		assertThat(properties.get("massnahmen_letzte_aenderung")).isEqualTo(Timestamp.valueOf("2023-01-26 14:31:00"));
		assertThat(properties.get("von_zeitpunkt")).isEqualTo(Timestamp.valueOf("2025-01-01 00:00:00"));
		assertThat(properties.get("bis_zeitpunkt")).isEqualTo(Timestamp.valueOf("2025-12-31 23:59:00"));
		assertThat(properties.get("handlungsverantwortlicher")).isEqualTo("BAULASTTRAEGER");
		assertThat(properties.get("massnahmen_umsetzungsstand_id")).isNull();
		assertThat(properties.get("massnahmen_paket_id")).isEqualTo("massnahmenPaketId");
		assertThat(properties.get("planung_erforderlich")).isEqualTo(false);
		assertThat(properties.get("realisierungshilfe")).isEqualTo("NR_2_2_1");
		assertThat(properties.get("kostenannahme")).isEqualTo(10000L);
	}
}
