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

package de.wps.radvis.backend.manuellerimport.attributeimport.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.mockito.Mock;
import org.mockito.Mockito;

import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.entity.AttributeImportKonfliktProtokoll;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.entity.AttributeImportSession;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.entity.FeatureMapping;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.valueObject.KantenKonfliktProtokoll;
import de.wps.radvis.backend.manuellerimport.common.domain.repository.InMemoryKantenRepository;
import de.wps.radvis.backend.manuellerimport.common.domain.repository.InMemoryKantenRepositoryFactory;
import de.wps.radvis.backend.manuellerimport.common.domain.repository.InMemoryKantenRepositoryTestProvider;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.AttributeImportFormat;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.Konflikt;
import de.wps.radvis.backend.matching.domain.entity.MappedGrundnetzkante;
import de.wps.radvis.backend.netz.domain.entity.FahrtrichtungAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttribute;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.KantenAttribute;
import de.wps.radvis.backend.netz.domain.entity.LinearReferenzierteAttribute;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.provider.FuehrungsformAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.FuehrungsformAttributeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.valueObject.BelagArt;
import de.wps.radvis.backend.netz.domain.valueObject.Beleuchtung;
import de.wps.radvis.backend.netz.domain.valueObject.Benutzungspflicht;
import de.wps.radvis.backend.netz.domain.valueObject.Bordstein;
import de.wps.radvis.backend.netz.domain.valueObject.Hoechstgeschwindigkeit;
import de.wps.radvis.backend.netz.domain.valueObject.IstStandard;
import de.wps.radvis.backend.netz.domain.valueObject.KantenOrtslage;
import de.wps.radvis.backend.netz.domain.valueObject.KfzParkenForm;
import de.wps.radvis.backend.netz.domain.valueObject.KfzParkenTyp;
import de.wps.radvis.backend.netz.domain.valueObject.Kommentar;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.Oberflaechenbeschaffenheit;
import de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung;
import de.wps.radvis.backend.netz.domain.valueObject.Richtung;
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
import de.wps.radvis.backend.netz.domain.valueObject.provider.LineareReferenzTestProvider;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import de.wps.radvis.backend.organisation.domain.entity.Gebietskoerperschaft;
import de.wps.radvis.backend.organisation.domain.entity.Organisation;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import de.wps.radvis.backend.organisation.domain.valueObject.OrganisationsArt;
import jakarta.persistence.EntityManager;
import jakarta.validation.constraints.NotNull;

public class ManuellerAttributeImportUebernahmeServiceIntegrationTest {

	@Mock
	private InMemoryKantenRepositoryFactory inMemoryKantenRepositoryFactory;

	@Mock
	private EntityManager entityManager;

	private ManuellerAttributeImportUebernahmeService manuellerAttributeImportUebernahmeService;

	private MappingService mappingService;

	@Mock
	private VerwaltungseinheitService verwaltungseinheitService;

	@BeforeEach
	void setup() {
		openMocks(this);
		this.mappingService = new MappingService();
		this.manuellerAttributeImportUebernahmeService = new ManuellerAttributeImportUebernahmeService(
			inMemoryKantenRepositoryFactory, mappingService, entityManager);

	}

	@Test
	void testAttributeUebernehmen_multipleMappingsPerKante() {
		// Arrange
		LineString linestring1 = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
			.createLineString(new Coordinate[] { new Coordinate(120.1, 230.2), new Coordinate(200.3, 330.2) });
		Map<String, Object> attribute = new HashMap<>();
		attribute.put("richtung", "1");
		attribute.put("beleuchtun", "1");
		attribute.put("belag", "10");
		attribute.put("vereinbaru", "foo");
		FeatureMapping featureMapping1 = new FeatureMapping(0L, attribute, linestring1);

		LineString linestring2 = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
			.createLineString(new Coordinate[] { new Coordinate(200.3, 330.2), new Coordinate(320.2, 430.1) });
		Map<String, Object> attribute2 = new HashMap<>();
		attribute2.put("richtung", "2");
		attribute2.put("beleuchtun", "1");
		attribute2.put("belag", "40");
		attribute2.put("vereinbaru", "bar");
		FeatureMapping featureMapping2 = new FeatureMapping(1L, attribute2, linestring2);

		LineString featureMatchedLineString1 = GeometryTestdataProvider.createLineString(new Coordinate(120, 230),
			new Coordinate(200, 310));
		LineString featureMatchedLineString2 = GeometryTestdataProvider.createLineString(new Coordinate(200, 310),
			new Coordinate(320, 430));

		Kante kante = createKante(120, 230, 320, 430, 1L);

		featureMapping1.add(new MappedGrundnetzkante(kante.getGeometry(), kante.getId(), featureMatchedLineString1));
		featureMapping2.add(new MappedGrundnetzkante(kante.getGeometry(), kante.getId(), featureMatchedLineString2));

		List<FeatureMapping> featureMappings = List.of(
			featureMapping1,
			featureMapping2);

		final List<String> attributliste = List.of("beleuchtun", "richtung", "vereinbaru", "belag");
		final AttributeImportSession session = new AttributeImportSession(
			BenutzerTestDataProvider.defaultBenutzer().build(),
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build(), attributliste,
			AttributeImportFormat.LUBW);
		session.setFeatureMappings(featureMappings);

		InMemoryKantenRepository inMemoryKantenRepository = InMemoryKantenRepositoryTestProvider.withKanten(
			Set.of(kante));

		when(inMemoryKantenRepositoryFactory.create(any(MultiPolygon.class))).thenReturn(
			inMemoryKantenRepository);

		// Act
		manuellerAttributeImportUebernahmeService.attributeUebernehmen(session.getAttribute(),
			session.getOrganisation(), session.getFeatureMappings(),
			new AttributMapperFactory(verwaltungseinheitService)
				.createMapper(session.getAttributeImportFormat()),
			new AttributeImportKonfliktProtokoll());

		// Assert
		assertThat(kante.getKantenAttributGruppe().getKantenAttribute().getBeleuchtung())
			.isEqualTo(Beleuchtung.VORHANDEN);

		assertThat(kante.isZweiseitig()).isFalse();

		assertThat(kante.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks()).isEqualTo(Richtung.IN_RICHTUNG);
		assertThat(kante.getFahrtrichtungAttributGruppe().isZweiseitig()).isFalse();

		final List<FuehrungsformAttribute> fuehrungsformAttributesLinks = sortByLineareReferenz(
			kante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks());
		assertThat(kante.getFuehrungsformAttributGruppe().isZweiseitig()).isFalse(); // => links prüfen reicht
		assertThat(fuehrungsformAttributesLinks).extracting(
				LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt)
			.usingElementComparator(LineareReferenzTestProvider.lenientComparator)
			.containsExactly(LinearReferenzierterAbschnitt.of(0, 0.4), LinearReferenzierterAbschnitt.of(0.4, 1));
		assertThat(fuehrungsformAttributesLinks.get(0).getBelagArt()).isEqualTo(BelagArt.ASPHALT);
		assertThat(fuehrungsformAttributesLinks.get(1).getBelagArt()).isEqualTo(BelagArt.WASSERGEBUNDENE_DECKE);

		final List<ZustaendigkeitAttribute> zustaendigkeitAttribute = sortByLineareReferenz(
			kante.getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute());
		assertThat(zustaendigkeitAttribute).extracting(LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt)
			.usingElementComparator(LineareReferenzTestProvider.lenientComparator)
			.containsExactly(LinearReferenzierterAbschnitt.of(0, 0.4), LinearReferenzierterAbschnitt.of(0.4, 1));
		assertThat(zustaendigkeitAttribute.get(0).getVereinbarungsKennung()).contains(VereinbarungsKennung.of("foo"));
		assertThat(zustaendigkeitAttribute.get(1).getVereinbarungsKennung()).contains(VereinbarungsKennung.of("bar"));
	}

	@Test
	void testAttributeUebernehmen_einFeatureMehrereKanten() {
		// Arrange
		LineString linestring1 = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
			.createLineString(new Coordinate[] { new Coordinate(100, 101), new Coordinate(190, 191) });
		Map<String, Object> attribute = new HashMap<>();
		attribute.put("richtung", "2");
		attribute.put("beleuchtun", "1");
		attribute.put("belag", "10");
		attribute.put("vereinbaru", "魔法を一つかける");
		FeatureMapping featureMapping1 = new FeatureMapping(0L, attribute, linestring1);

		Kante kante1 = createKante(110, 110, 150, 150, 1L);
		Kante kante2 = createKante(150, 150, 200, 200, 2L);

		MappedGrundnetzkante mappedKante1 = Mockito.mock(MappedGrundnetzkante.class);
		when(mappedKante1.getKanteId()).thenReturn(kante1.getId());
		when(mappedKante1.getLinearReferenzierterAbschnitt()).thenReturn(LinearReferenzierterAbschnitt.of(0.2, 1.));
		featureMapping1.add(mappedKante1);

		MappedGrundnetzkante mappedKante2 = Mockito.mock(MappedGrundnetzkante.class);
		when(mappedKante2.getKanteId()).thenReturn(kante2.getId());
		when(mappedKante2.getLinearReferenzierterAbschnitt()).thenReturn(LinearReferenzierterAbschnitt.of(0., 0.8));
		featureMapping1.add(mappedKante2);

		List<FeatureMapping> featureMappings = List.of(featureMapping1);

		final AttributeImportSession session = new AttributeImportSession(
			BenutzerTestDataProvider.defaultBenutzer().build(),
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build(),
			List.of("beleuchtun", "richtung", "vereinbaru", "belag"), AttributeImportFormat.LUBW);
		session.setFeatureMappings(featureMappings);

		InMemoryKantenRepository inMemoryKantenRepository = InMemoryKantenRepositoryTestProvider.withKanten(
			Set.of(kante1, kante2));

		when(inMemoryKantenRepositoryFactory.create(any(MultiPolygon.class))).thenReturn(
			inMemoryKantenRepository);

		// Act
		manuellerAttributeImportUebernahmeService.attributeUebernehmen(session.getAttribute(),
			session.getOrganisation(), session.getFeatureMappings(), new AttributMapperFactory(
				verwaltungseinheitService).createMapper(
				session.getAttributeImportFormat()),
			new AttributeImportKonfliktProtokoll());

		// Assert
		{ // kante1
			assertThat(kante1.getKantenAttributGruppe().getKantenAttribute().getBeleuchtung())
				.isEqualTo(Beleuchtung.VORHANDEN);

			assertThat(kante1.isZweiseitig()).isFalse();

			assertThat(kante1.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks()).isEqualTo(Richtung.IN_RICHTUNG);
			assertThat(kante1.getFahrtrichtungAttributGruppe().isZweiseitig()).isFalse();

			final List<FuehrungsformAttribute> fuehrungsformAttributesLinks = sortByLineareReferenz(
				kante1.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks());
			assertThat(kante1.getFuehrungsformAttributGruppe().isZweiseitig()).isFalse();
			assertThat(fuehrungsformAttributesLinks).extracting(
					LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt)
				.usingElementComparator(LineareReferenzTestProvider.lenientComparator)
				.containsExactly(LinearReferenzierterAbschnitt.of(0, 0.2), LinearReferenzierterAbschnitt.of(0.2, 1));
			assertThat(fuehrungsformAttributesLinks.get(0).getBelagArt()).isEqualTo(BelagArt.UNBEKANNT);
			assertThat(fuehrungsformAttributesLinks.get(1).getBelagArt()).isEqualTo(BelagArt.ASPHALT);

			final List<ZustaendigkeitAttribute> zustaendigkeitAttribute = sortByLineareReferenz(
				kante1.getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute());
			assertThat(zustaendigkeitAttribute).hasSize(2);
			assertThat(zustaendigkeitAttribute).extracting(
					LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt)
				.usingElementComparator(LineareReferenzTestProvider.lenientComparator)
				.containsExactly(LinearReferenzierterAbschnitt.of(0, 0.2), LinearReferenzierterAbschnitt.of(0.2, 1.));
			assertThat(zustaendigkeitAttribute.get(0).getVereinbarungsKennung()).isEmpty();
			assertThat(zustaendigkeitAttribute.get(1).getVereinbarungsKennung()).contains(
				VereinbarungsKennung.of("魔法を一つかける"));
		}

		{ // kante2
			assertThat(kante2.getKantenAttributGruppe().getKantenAttribute().getBeleuchtung())
				.isEqualTo(Beleuchtung.VORHANDEN);

			assertThat(kante2.isZweiseitig()).isFalse();

			assertThat(kante2.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks()).isEqualTo(Richtung.IN_RICHTUNG);
			assertThat(kante2.getFahrtrichtungAttributGruppe().isZweiseitig()).isFalse();

			List<FuehrungsformAttribute> fuehrungsformAttributesLinks = sortByLineareReferenz(
				kante2.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks());
			assertThat(kante2.getFuehrungsformAttributGruppe().isZweiseitig()).isFalse();
			assertThat(fuehrungsformAttributesLinks).extracting(
					LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt)
				.usingElementComparator(LineareReferenzTestProvider.lenientComparator)
				.containsExactly(LinearReferenzierterAbschnitt.of(0, 0.8), LinearReferenzierterAbschnitt.of(0.8, 1));
			assertThat(fuehrungsformAttributesLinks.get(0).getBelagArt()).isEqualTo(BelagArt.ASPHALT);
			assertThat(fuehrungsformAttributesLinks.get(1).getBelagArt()).isEqualTo(BelagArt.UNBEKANNT);

			List<ZustaendigkeitAttribute> zustaendigkeitAttribute = sortByLineareReferenz(
				kante2.getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute());
			assertThat(zustaendigkeitAttribute).extracting(
					LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt)
				.usingElementComparator(LineareReferenzTestProvider.lenientComparator)
				.containsExactly(LinearReferenzierterAbschnitt.of(0, 0.8), LinearReferenzierterAbschnitt.of(0.8, 1.));
			assertThat(zustaendigkeitAttribute.get(0).getVereinbarungsKennung()).contains(
				VereinbarungsKennung.of("魔法を一つかける"));
			assertThat(zustaendigkeitAttribute.get(1).getVereinbarungsKennung()).isEmpty();
		}
	}

	@Test
	void testAttributeUebernehmen_LUBW_zweiseitigeKante_zweiFeaturesInnerorts_RadverkehrfuehrungSelbststaendig() {
		// Arrange
		LineString linestring1 = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
			.createLineString(new Coordinate[] { new Coordinate(119.1, 230.2), new Coordinate(200.3, 330.2) });
		Map<String, Object> attribute = new HashMap<>();
		attribute.put("ST", "Sicherheitstrennstreifen innerorts mit Längsparken");
		attribute.put("BREITST", " > 1,50 m");
		attribute.put("BREITST2", null);
		attribute.put("ORTSLAGE", "Innerorts");
		FeatureMapping featureMapping1 = new FeatureMapping(0L, attribute, linestring1);

		LineString linestring2 = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
			.createLineString(new Coordinate[] { new Coordinate(120.1, 230.2), new Coordinate(201.1, 309.2) });
		Map<String, Object> attribute2 = new HashMap<>();
		attribute2.put("ST", "Kein Sicherheitstrennstreifen vorhanden");
		attribute2.put("BREITST", "Kein Sicherheitstrennstreifen, aber Bordstein (Hochbord)");
		attribute2.put("BREITST2", "60");
		attribute2.put("ORTSLAGE", "Innerorts");
		FeatureMapping featureMapping2 = new FeatureMapping(1L, attribute2, linestring2);

		LineString featureMatchedLineString1 = GeometryTestdataProvider.createLineString(new Coordinate(120, 230),
			new Coordinate(200, 310));
		LineString featureMatchedLineString2 = GeometryTestdataProvider.createLineString(new Coordinate(120, 230),
			new Coordinate(200, 310));

		Kante kante = KanteTestDataProvider.withCoordinatesAndQuelle(120, 230, 200, 310, QuellSystem.DLM)
			.isZweiseitig(true)
			.fuehrungsformAttributGruppe(FuehrungsformAttributGruppe.builder()
				.fuehrungsformAttributeLinks(
					List.of(FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
						.radverkehrsfuehrung(Radverkehrsfuehrung.GEH_RADWEG_GETRENNT_STRASSENBEGLEITEND)
						.build())
				)
				.fuehrungsformAttributeRechts(
					List.of(FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
						.radverkehrsfuehrung(Radverkehrsfuehrung.GEH_RADWEG_GETRENNT_STRASSENBEGLEITEND)
						.build())
				)
				.isZweiseitig(true)
				.build())
			.fahrtrichtungAttributGruppe(FahrtrichtungAttributGruppe.builder()
				.isZweiseitig(true)
				.build())
			.id(1L)
			.build();

		featureMapping1.add(new MappedGrundnetzkante(kante.getGeometry(), kante.getId(), featureMatchedLineString1));
		featureMapping2.add(new MappedGrundnetzkante(kante.getGeometry(), kante.getId(), featureMatchedLineString2));

		List<FeatureMapping> featureMappings = List.of(
			featureMapping1,
			featureMapping2);

		final List<String> attributliste = List.of("st");
		final AttributeImportSession session = new AttributeImportSession(
			BenutzerTestDataProvider.defaultBenutzer().build(),
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build(), attributliste,
			AttributeImportFormat.LUBW);
		session.setFeatureMappings(featureMappings);

		InMemoryKantenRepository inMemoryKantenRepository = InMemoryKantenRepositoryTestProvider.withKanten(
			Set.of(kante));

		when(inMemoryKantenRepositoryFactory.create(any(MultiPolygon.class))).thenReturn(
			inMemoryKantenRepository);

		// Act
		manuellerAttributeImportUebernahmeService.attributeUebernehmen(session.getAttribute(),
			session.getOrganisation(), session.getFeatureMappings(), new AttributMapperFactory(
				verwaltungseinheitService).createMapper(
				session.getAttributeImportFormat()),
			new AttributeImportKonfliktProtokoll());

		// Assert
		assertThat(kante.isZweiseitig()).isTrue();
		FuehrungsformAttributGruppe fag = kante.getFuehrungsformAttributGruppe();
		assertThat(fag.isZweiseitig()).isTrue();
		final List<FuehrungsformAttribute> faLinks = fag.getImmutableFuehrungsformAttributeLinks();
		assertThat(faLinks).hasSize(1);
		assertThat(faLinks.get(0).getTrennstreifenBreiteLinks()).isEmpty();
		assertThat(faLinks.get(0).getTrennstreifenBreiteRechts()).contains(Laenge.of(1.5));
		assertThat(faLinks.get(0).getTrennstreifenFormLinks()).isEmpty();
		assertThat(faLinks.get(0).getTrennstreifenFormRechts()).contains(
			TrennstreifenForm.TRENNUNG_DURCH_MARKIERUNG_ODER_BAULICHE_TRENNUNG);
		assertThat(faLinks.get(0).getTrennstreifenTrennungZuLinks()).isEmpty();
		assertThat(faLinks.get(0).getTrennstreifenTrennungZuRechts()).contains(
			TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN);

		final List<FuehrungsformAttribute> faRechts = fag.getImmutableFuehrungsformAttributeRechts();
		assertThat(faRechts).hasSize(1);
		assertThat(faRechts.get(0).getTrennstreifenBreiteLinks()).contains(Laenge.of(0.6));
		assertThat(faRechts.get(0).getTrennstreifenBreiteRechts()).isEmpty();
		assertThat(faRechts.get(0).getTrennstreifenFormLinks()).contains(
			TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART);
		assertThat(faRechts.get(0).getTrennstreifenFormRechts()).isEmpty();
		assertThat(faRechts.get(0).getTrennstreifenTrennungZuLinks()).isEmpty();
		assertThat(faRechts.get(0).getTrennstreifenTrennungZuRechts()).isEmpty();
	}

	@Test
	void testAttributeUebernehmen_LUBW_zweiseitigeKante_zweiFeaturesInnerorts_RadverkehrfuehrungSchutzstreifen() {
		// Arrange
		LineString linestring1 = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
			.createLineString(new Coordinate[] { new Coordinate(119.1, 230.2), new Coordinate(200.3, 330.2) });
		Map<String, Object> attribute = new HashMap<>();
		attribute.put("ST", "Sicherheitstrennstreifen innerorts mit Längsparken");
		attribute.put("BREITST", "> 1,50 m");
		attribute.put("BREITST2", null);
		attribute.put("ORTSLAGE", "Innerorts");
		FeatureMapping featureMapping1 = new FeatureMapping(0L, attribute, linestring1);

		LineString linestring2 = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
			.createLineString(new Coordinate[] { new Coordinate(120.1, 230.2), new Coordinate(201.1, 309.2) });
		Map<String, Object> attribute2 = new HashMap<>();
		attribute2.put("ST", "Sicherheitstrennstreifen innerorts mit Schräg-/Senkrechtparken");
		attribute2.put("BREITST", "< 1,20 m ");
		attribute2.put("BREITST2", "110");
		attribute2.put("ORTSLAGE", "Innerorts");
		FeatureMapping featureMapping2 = new FeatureMapping(1L, attribute2, linestring2);

		LineString featureMatchedLineString1 = GeometryTestdataProvider.createLineString(new Coordinate(120, 230),
			new Coordinate(200, 310));
		LineString featureMatchedLineString2 = GeometryTestdataProvider.createLineString(new Coordinate(120, 230),
			new Coordinate(200, 310));

		Kante kante = KanteTestDataProvider.withCoordinatesAndQuelle(120, 230, 200, 310, QuellSystem.DLM)
			.isZweiseitig(true)
			.fuehrungsformAttributGruppe(FuehrungsformAttributGruppe.builder()
				.fuehrungsformAttributeLinks(
					List.of(FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
						.radverkehrsfuehrung(Radverkehrsfuehrung.SCHUTZSTREIFEN)
						.build())
				)
				.fuehrungsformAttributeRechts(
					List.of(FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
						.radverkehrsfuehrung(Radverkehrsfuehrung.RADFAHRSTREIFEN)
						.build())
				)
				.isZweiseitig(true)
				.build())
			.fahrtrichtungAttributGruppe(FahrtrichtungAttributGruppe.builder()
				.isZweiseitig(true)
				.build())
			.id(1L)
			.build();

		featureMapping1.add(new MappedGrundnetzkante(kante.getGeometry(), kante.getId(), featureMatchedLineString1));
		featureMapping2.add(new MappedGrundnetzkante(kante.getGeometry(), kante.getId(), featureMatchedLineString2));

		List<FeatureMapping> featureMappings = List.of(
			featureMapping1,
			featureMapping2);

		final List<String> attributliste = List.of("st");
		final AttributeImportSession session = new AttributeImportSession(
			BenutzerTestDataProvider.defaultBenutzer().build(),
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build(), attributliste,
			AttributeImportFormat.LUBW);
		session.setFeatureMappings(featureMappings);

		InMemoryKantenRepository inMemoryKantenRepository = InMemoryKantenRepositoryTestProvider.withKanten(
			Set.of(kante));

		when(inMemoryKantenRepositoryFactory.create(any(MultiPolygon.class))).thenReturn(
			inMemoryKantenRepository);

		// Act
		manuellerAttributeImportUebernahmeService.attributeUebernehmen(session.getAttribute(),
			session.getOrganisation(), session.getFeatureMappings(), new AttributMapperFactory(
				verwaltungseinheitService).createMapper(
				session.getAttributeImportFormat()),
			new AttributeImportKonfliktProtokoll());

		// Assert
		assertThat(kante.isZweiseitig()).isTrue();
		FuehrungsformAttributGruppe fag = kante.getFuehrungsformAttributGruppe();
		assertThat(fag.isZweiseitig()).isTrue();
		final List<FuehrungsformAttribute> faLinks = fag.getImmutableFuehrungsformAttributeLinks();
		assertThat(faLinks).hasSize(1);
		assertThat(faLinks.get(0).getTrennstreifenBreiteRechts()).isEmpty();
		assertThat(faLinks.get(0).getTrennstreifenBreiteLinks()).contains(Laenge.of(1.5));
		assertThat(faLinks.get(0).getTrennstreifenFormRechts()).isEmpty();
		assertThat(faLinks.get(0).getTrennstreifenFormLinks()).contains(
			TrennstreifenForm.TRENNUNG_DURCH_MARKIERUNG_ODER_BAULICHE_TRENNUNG);
		assertThat(faLinks.get(0).getTrennstreifenTrennungZuRechts()).isEmpty();
		assertThat(faLinks.get(0).getTrennstreifenTrennungZuLinks()).contains(
			TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN);

		final List<FuehrungsformAttribute> faRechts = fag.getImmutableFuehrungsformAttributeRechts();
		assertThat(faRechts).hasSize(1);
		assertThat(faRechts.get(0).getTrennstreifenBreiteRechts()).contains(Laenge.of(1.1));
		assertThat(faRechts.get(0).getTrennstreifenBreiteLinks()).isEmpty();
		assertThat(faRechts.get(0).getTrennstreifenFormRechts()).contains(
			TrennstreifenForm.TRENNUNG_DURCH_MARKIERUNG_ODER_BAULICHE_TRENNUNG);
		assertThat(faRechts.get(0).getTrennstreifenFormLinks()).isEmpty();
		assertThat(faRechts.get(0).getTrennstreifenTrennungZuRechts()).contains(
			TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN);
		assertThat(faRechts.get(0).getTrennstreifenTrennungZuLinks()).isEmpty();
	}

	@Test
	void testAttributeUebernehmen_LUBW_zweiseitigeKante_zweiFeaturesAusserorts() {
		// Arrange
		LineString linestring1 = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
			.createLineString(new Coordinate[] { new Coordinate(119.1, 230.2), new Coordinate(200.3, 330.2) });
		Map<String, Object> attribute = new HashMap<>();
		attribute.put("ST", "Kein Sicherheitstrennstreifen vorhanden");
		attribute.put("BREITST",
			"Kein Sicherheitstrennstreifen, aber andere Abgrenzung (z.B. Rinne zw. Fahrbahn und Anlage)");
		attribute.put("BREITST2", null);
		attribute.put("ORTSLAGE", "Außerorts");
		FeatureMapping featureMapping1 = new FeatureMapping(0L, attribute, linestring1);

		LineString linestring2 = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
			.createLineString(new Coordinate[] { new Coordinate(120.1, 230.2), new Coordinate(201.1, 309.2) });
		Map<String, Object> attribute2 = new HashMap<>();
		attribute2.put("ST", "Sicherheitstrennstreifen außerorts");
		attribute2.put("BREITST", "< 1,00 m");
		attribute2.put("BREITST2", "80");
		attribute2.put("ORTSLAGE", "Außerorts");
		FeatureMapping featureMapping2 = new FeatureMapping(1L, attribute2, linestring2);

		LineString featureMatchedLineString1 = GeometryTestdataProvider.createLineString(new Coordinate(120, 230),
			new Coordinate(200, 310));
		LineString featureMatchedLineString2 = GeometryTestdataProvider.createLineString(new Coordinate(120, 230),
			new Coordinate(200, 310));

		Kante kante = KanteTestDataProvider.withCoordinatesAndQuelle(120, 230, 200, 310, QuellSystem.DLM)
			.isZweiseitig(true)
			.fuehrungsformAttributGruppe(FuehrungsformAttributGruppe.builder()
				.fuehrungsformAttributeLinks(
					List.of(FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
						.radverkehrsfuehrung(Radverkehrsfuehrung.GEH_RADWEG_GETRENNT_STRASSENBEGLEITEND)
						.build())
				)
				.fuehrungsformAttributeRechts(
					List.of(FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
						.radverkehrsfuehrung(Radverkehrsfuehrung.GEH_RADWEG_GETRENNT_STRASSENBEGLEITEND)
						.build())
				)
				.isZweiseitig(true)
				.build())
			.fahrtrichtungAttributGruppe(FahrtrichtungAttributGruppe.builder()
				.isZweiseitig(true)
				.build())
			.id(1L)
			.build();

		featureMapping1.add(new MappedGrundnetzkante(kante.getGeometry(), kante.getId(), featureMatchedLineString1));
		featureMapping2.add(new MappedGrundnetzkante(kante.getGeometry(), kante.getId(), featureMatchedLineString2));

		List<FeatureMapping> featureMappings = List.of(
			featureMapping1,
			featureMapping2);

		final List<String> attributliste = List.of("st");
		final AttributeImportSession session = new AttributeImportSession(
			BenutzerTestDataProvider.defaultBenutzer().build(),
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build(), attributliste,
			AttributeImportFormat.LUBW);
		session.setFeatureMappings(featureMappings);

		InMemoryKantenRepository inMemoryKantenRepository = InMemoryKantenRepositoryTestProvider.withKanten(
			Set.of(kante));

		when(inMemoryKantenRepositoryFactory.create(any(MultiPolygon.class))).thenReturn(
			inMemoryKantenRepository);

		// Act
		manuellerAttributeImportUebernahmeService.attributeUebernehmen(session.getAttribute(),
			session.getOrganisation(), session.getFeatureMappings(), new AttributMapperFactory(
				verwaltungseinheitService).createMapper(
				session.getAttributeImportFormat()),
			new AttributeImportKonfliktProtokoll());

		// Assert
		assertThat(kante.isZweiseitig()).isTrue();
		FuehrungsformAttributGruppe fag = kante.getFuehrungsformAttributGruppe();
		assertThat(fag.isZweiseitig()).isTrue();
		final List<FuehrungsformAttribute> faLinks = fag.getImmutableFuehrungsformAttributeLinks();
		assertThat(faLinks).hasSize(1);
		assertThat(faLinks.get(0).getTrennstreifenBreiteLinks()).isEmpty();
		assertThat(faLinks.get(0).getTrennstreifenBreiteRechts()).isEmpty();
		assertThat(faLinks.get(0).getTrennstreifenFormLinks()).contains(
			TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART);
		assertThat(faLinks.get(0).getTrennstreifenFormRechts()).isEmpty();
		assertThat(faLinks.get(0).getTrennstreifenTrennungZuLinks()).isEmpty();
		assertThat(faLinks.get(0).getTrennstreifenTrennungZuRechts()).isEmpty();

		final List<FuehrungsformAttribute> faRechts = fag.getImmutableFuehrungsformAttributeRechts();
		assertThat(faRechts).hasSize(1);
		assertThat(faRechts.get(0).getTrennstreifenBreiteLinks()).contains(Laenge.of(0.80));
		assertThat(faRechts.get(0).getTrennstreifenBreiteRechts()).isEmpty();
		assertThat(faRechts.get(0).getTrennstreifenFormLinks()).contains(
			TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN);
		assertThat(faRechts.get(0).getTrennstreifenFormRechts()).isEmpty();
		assertThat(faRechts.get(0).getTrennstreifenTrennungZuLinks()).contains(
			TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN);
		assertThat(faRechts.get(0).getTrennstreifenTrennungZuRechts()).isEmpty();
	}

	@Test
	void testAttributeUebernehmen_LUBW_zweiseitigeKante_zweiFeaturesAusserorts_RadverkehrsfuehrungPasstNicht() {
		// Arrange
		LineString linestring1 = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
			.createLineString(new Coordinate[] { new Coordinate(119.1, 230.2), new Coordinate(200.3, 330.2) });
		Map<String, Object> attribute = new HashMap<>();
		attribute.put("ST", "Kein Sicherheitstrennstreifen vorhanden");
		attribute.put("BREITST",
			"Kein Sicherheitstrennstreifen, aber andere Abgrenzung (z.B. Rinne zw. Fahrbahn und Anlage)");
		attribute.put("BREITST2", null);
		attribute.put("ORTSLAGE", "Außerorts");
		FeatureMapping featureMapping1 = new FeatureMapping(0L, attribute, linestring1);

		LineString linestring2 = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
			.createLineString(new Coordinate[] { new Coordinate(120.1, 230.2), new Coordinate(201.1, 309.2) });
		Map<String, Object> attribute2 = new HashMap<>();
		attribute2.put("ST", "Sicherheitstrennstreifen außerorts");
		attribute2.put("BREITST", "< 1,00 m");
		attribute2.put("BREITST2", "80");
		attribute2.put("ORTSLAGE", "Außerorts");
		FeatureMapping featureMapping2 = new FeatureMapping(1L, attribute2, linestring2);

		LineString featureMatchedLineString1 = GeometryTestdataProvider.createLineString(new Coordinate(120, 230),
			new Coordinate(200, 310));
		LineString featureMatchedLineString2 = GeometryTestdataProvider.createLineString(new Coordinate(120, 230),
			new Coordinate(200, 310));

		Kante kante = KanteTestDataProvider.withCoordinatesAndQuelle(120, 230, 200, 310, QuellSystem.DLM)
			.isZweiseitig(true)
			.fuehrungsformAttributGruppe(FuehrungsformAttributGruppe.builder()
				.fuehrungsformAttributeLinks(
					List.of(FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
						.radverkehrsfuehrung(Radverkehrsfuehrung.GEH_RADWEG_GETRENNT_STRASSENBEGLEITEND)
						.build())
				)
				.fuehrungsformAttributeRechts(
					List.of(FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
						.radverkehrsfuehrung(Radverkehrsfuehrung.FUEHRUNG_AUF_FAHRBAHN_ZWEISTREIFIGE_FAHRBAHN)
						.build())
				)
				.isZweiseitig(true)
				.build())
			.fahrtrichtungAttributGruppe(FahrtrichtungAttributGruppe.builder()
				.isZweiseitig(true)
				.build())
			.id(1L)
			.build();

		featureMapping1.add(new MappedGrundnetzkante(kante.getGeometry(), kante.getId(), featureMatchedLineString1));
		featureMapping2.add(new MappedGrundnetzkante(kante.getGeometry(), kante.getId(), featureMatchedLineString2));

		List<FeatureMapping> featureMappings = List.of(
			featureMapping1,
			featureMapping2);

		final List<String> attributliste = List.of("st");
		final AttributeImportSession session = new AttributeImportSession(
			BenutzerTestDataProvider.defaultBenutzer().build(),
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build(), attributliste,
			AttributeImportFormat.LUBW);
		session.setFeatureMappings(featureMappings);

		InMemoryKantenRepository inMemoryKantenRepository = InMemoryKantenRepositoryTestProvider.withKanten(
			Set.of(kante));

		when(inMemoryKantenRepositoryFactory.create(any(MultiPolygon.class))).thenReturn(
			inMemoryKantenRepository);

		// Act
		AttributeImportKonfliktProtokoll protokoll = new AttributeImportKonfliktProtokoll();
		manuellerAttributeImportUebernahmeService.attributeUebernehmen(session.getAttribute(),
			session.getOrganisation(), session.getFeatureMappings(), new AttributMapperFactory(
				verwaltungseinheitService).createMapper(
				session.getAttributeImportFormat()),
			protokoll);

		// Assert
		assertThat(kante.isZweiseitig()).isTrue();
		FuehrungsformAttributGruppe fag = kante.getFuehrungsformAttributGruppe();
		assertThat(fag.isZweiseitig()).isTrue();
		final List<FuehrungsformAttribute> faLinks = fag.getImmutableFuehrungsformAttributeLinks();
		assertThat(faLinks).hasSize(1);
		assertThat(faLinks.get(0).getTrennstreifenBreiteLinks()).isEmpty();
		assertThat(faLinks.get(0).getTrennstreifenBreiteRechts()).isEmpty();
		assertThat(faLinks.get(0).getTrennstreifenFormLinks()).contains(
			TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART);
		assertThat(faLinks.get(0).getTrennstreifenFormRechts()).isEmpty();
		assertThat(faLinks.get(0).getTrennstreifenTrennungZuLinks()).isEmpty();
		assertThat(faLinks.get(0).getTrennstreifenTrennungZuRechts()).isEmpty();

		final List<FuehrungsformAttribute> faRechts = fag.getImmutableFuehrungsformAttributeRechts();
		assertThat(faRechts).hasSize(1);
		assertThat(faRechts.get(0).getTrennstreifenBreiteLinks()).isEmpty();
		assertThat(faRechts.get(0).getTrennstreifenBreiteRechts()).isEmpty();
		assertThat(faRechts.get(0).getTrennstreifenFormLinks()).isEmpty();
		assertThat(faRechts.get(0).getTrennstreifenFormRechts()).isEmpty();
		assertThat(faRechts.get(0).getTrennstreifenTrennungZuLinks()).isEmpty();
		assertThat(faRechts.get(0).getTrennstreifenTrennungZuRechts()).isEmpty();

		assertThat(protokoll.getKantenKonfliktProtokolle()).hasSize(1);
		KantenKonfliktProtokoll kantenKonfliktProtokoll = protokoll.getKantenKonfliktProtokolle().get(0);
		assertThat(kantenKonfliktProtokoll.getKanteId()).isEqualTo(kante.getId());
		assertThat(kantenKonfliktProtokoll.getKonflikte()).hasSize(1);

		Konflikt konflikt = kantenKonfliktProtokoll.getKonflikte().stream().findFirst().get();
		assertThat(konflikt.getAttributName()).isEqualToIgnoringCase("ST");
		assertThat(konflikt.getUebernommenerWert()).startsWith(
			"Es konnten keine TrennstreifenInformationen geschrieben werden");
		assertThat(konflikt.getUebernommenerWert()).contains(
			"Radverkehrsführung: " + Radverkehrsfuehrung.FUEHRUNG_AUF_FAHRBAHN_ZWEISTREIFIGE_FAHRBAHN);
		assertThat(konflikt.getNichtUebernommeneWerte()).contains(
			"TrennstreifenForm: " + TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN,
			"TrennungZu: " + TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN,
			"TrennstreifenBreite: " + Laenge.of(0.8)
		);

	}

	@Test
	void testAttributeUebernehmen_RadVIS_einseitigeKante_einFeature() {
		// Arrange
		LineString linestring1 = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
			.createLineString(new Coordinate[] { new Coordinate(120.1, 230.2), new Coordinate(200.3, 330.2) });
		Map<String, Object> attribute = new HashMap<>();
		// KantenAttributGruppe
		attribute.put("dtv_fussve", "12");
		attribute.put("dtv_pkw", "13");
		attribute.put("dtv_radver", "198");
		attribute.put("kommentar", "Kommentäär");
		attribute.put("laenge_man", "1234,00");
		attribute.put("strassen_n", "Ein Straßenname");
		attribute.put("strassen_0", "13a");
		attribute.put("sv", "987");
		attribute.put("wege_nivea", "GEHWEG");
		attribute.put("gemeinde_n", "Ötigheim");
		attribute.put("beleuchtun", "UNBEKANNT");
		attribute.put("umfeld", "GEWERBEGEBIET");
		attribute.put("strassenka", "NAHRAEUMIG");
		attribute.put("strassenqu", "ANBAUFREIE_STRASSE");
		attribute.put("status", "IN_BAU");
		attribute.put("standards", "STARTSTANDARD_RADNETZ; ZIELSTANDARD_RADNETZ");
		// FuehrungsformAttributGruppe
		attribute.put("radverkehr", "SONDERWEG_RADWEG_SELBSTSTAENDIG");
		attribute.put("breite", "2,45");
		attribute.put("parken_typ", "LAENGS_PARKEN");
		attribute.put("parken_for", "PARKBUCHTEN");
		attribute.put("bordstein", "KOMPLETT_ABGESENKT");
		attribute.put("belag_art", "UNGEBUNDENE_DECKE");
		attribute.put("oberflaech", "NEUWERTIG");
		attribute.put("benutzungs", "VORHANDEN");
		// GeschwindigkeitsAttributGruppe
		attribute.put("ortslage", "AUSSERORTS");
		attribute.put("hoechstges", "UEBER_100_KMH");
		attribute.put("abweichend", "MAX_9_KMH");
		// ZustaendigkeitsAttributGruppe
		attribute.put("baulast_tr", "Ötigheim (GEMEINDE)");
		attribute.put("unterhalts", "Stuttgart (REGIERUNGSBEZIRK)");
		attribute.put("erhalts_zu", "Toubiz (SONSTIGES)");
		attribute.put("vereinbaru", "vereinbarung123");
		// FahrtrichtungsAttributGruppe
		attribute.put("fahrtricht", "GEGEN_RICHTUNG");

		FeatureMapping featureMapping1 = new FeatureMapping(0L, attribute, linestring1);

		LineString featureMatchedLineString1 = GeometryTestdataProvider.createLineString(new Coordinate(120, 230),
			new Coordinate(200, 310));

		Kante kante = KanteTestDataProvider.withCoordinatesAndQuelle(120, 230, 200, 310, QuellSystem.DLM)
			.id(1L)
			.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue()
				.netzklassen(Set.of(Netzklasse.RADNETZ_ALLTAG))
				.build())
			.build();

		featureMapping1.add(new MappedGrundnetzkante(kante.getGeometry(), kante.getId(), featureMatchedLineString1));

		List<FeatureMapping> featureMappings = List.of(featureMapping1);

		final List<String> attributliste = List.of(
			"dtv_fussve",
			"dtv_pkw",
			"dtv_radver",
			"kommentar",
			"laenge_man",
			"strassen_n",
			"strassen_0",
			"sv",
			"wege_nivea",
			"gemeinde_n",
			"beleuchtun",
			"umfeld",
			"strassenka",
			"strassenqu",
			"status",
			"standards",
			// FuehrungsformAttributGruppe
			"radverkehr",
			"breite",
			"parken_typ",
			"parken_for",
			"bordstein",
			"belag_art",
			"oberflaech",
			"benutzungs",
			// GeschwindigkeitsAttributGruppe
			"ortslage",
			"hoechstges",
			"abweichend",
			// ZustaendigkeitsAttributGruppe
			"baulast_tr",
			"unterhalts",
			"erhalts_zu",
			"vereinbaru",
			// FahrtrichtungsAttributGruppe
			"fahrtricht"
		);
		final AttributeImportSession session = new AttributeImportSession(
			BenutzerTestDataProvider.defaultBenutzer().build(),
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build(), attributliste,
			AttributeImportFormat.RADVIS);
		session.setFeatureMappings(featureMappings);

		InMemoryKantenRepository inMemoryKantenRepository = InMemoryKantenRepositoryTestProvider.withKanten(
			Set.of(kante));

		when(inMemoryKantenRepositoryFactory.create(any(MultiPolygon.class))).thenReturn(
			inMemoryKantenRepository);

		Gebietskoerperschaft oetigheim = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Ötigheim")
			.organisationsArt(OrganisationsArt.GEMEINDE)
			.id(45L)
			.build();
		Gebietskoerperschaft stuttgart = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Stuttgart")
			.organisationsArt(OrganisationsArt.REGIERUNGSBEZIRK)
			.id(46L)
			.build();
		Organisation toubiz = VerwaltungseinheitTestDataProvider.defaultOrganisation()
			.name("Toubiz")
			.organisationsArt(OrganisationsArt.SONSTIGES)
			.id(47L)
			.build();
		when(verwaltungseinheitService.getVerwaltungseinheitnachNameUndArt("Ötigheim", OrganisationsArt.GEMEINDE))
			.thenReturn(Optional.of(oetigheim));
		when(verwaltungseinheitService.getVerwaltungseinheitnachNameUndArt("Stuttgart",
			OrganisationsArt.REGIERUNGSBEZIRK))
			.thenReturn(Optional.of(stuttgart));
		when(verwaltungseinheitService.getVerwaltungseinheitnachNameUndArt("Toubiz",
			OrganisationsArt.SONSTIGES))
			.thenReturn(Optional.of(toubiz));

		// Act
		manuellerAttributeImportUebernahmeService.attributeUebernehmen(session.getAttribute(),
			session.getOrganisation(), session.getFeatureMappings(), new AttributMapperFactory(
				verwaltungseinheitService).createMapper(
				session.getAttributeImportFormat()),
			new AttributeImportKonfliktProtokoll());

		// Assert
		assertThat(kante.isZweiseitig()).isFalse();
		KantenAttributGruppe kag = kante.getKantenAttributGruppe();
		assertThat(kag.getIstStandards()).containsExactlyInAnyOrder(
			IstStandard.STARTSTANDARD_RADNETZ, IstStandard.ZIELSTANDARD_RADNETZ
		);
		KantenAttribute ka = kag.getKantenAttribute();
		assertThat(ka.getDtvFussverkehr().get()).isEqualTo(VerkehrStaerke.of("12"));
		assertThat(ka.getDtvPkw().get()).isEqualTo(VerkehrStaerke.of("13"));
		assertThat(ka.getDtvRadverkehr().get()).isEqualTo(VerkehrStaerke.of("198"));
		assertThat(ka.getKommentar().get()).isEqualTo(Kommentar.of("Kommentäär"));
		assertThat(ka.getLaengeManuellErfasst().get()).isEqualTo(Laenge.of("1234,00"));
		assertThat(ka.getStrassenName().get()).isEqualTo(StrassenName.of("Ein Straßenname"));
		assertThat(ka.getStrassenNummer().get()).isEqualTo(StrassenNummer.of("13a"));
		assertThat(ka.getSv().get()).isEqualTo(VerkehrStaerke.of("987"));
		assertThat(ka.getWegeNiveau().get()).isEqualTo(WegeNiveau.GEHWEG);
		assertThat(ka.getGemeinde().get()).isEqualTo(oetigheim);
		assertThat(ka.getBeleuchtung()).isEqualTo(Beleuchtung.UNBEKANNT);
		assertThat(ka.getUmfeld()).isEqualTo(Umfeld.GEWERBEGEBIET);
		assertThat(ka.getStrassenkategorieRIN().get()).isEqualTo(StrassenkategorieRIN.NAHRAEUMIG);
		assertThat(ka.getStrassenquerschnittRASt06()).isEqualTo(StrassenquerschnittRASt06.ANBAUFREIE_STRASSE);
		assertThat(ka.getStatus()).isEqualTo(Status.IN_BAU);

		FuehrungsformAttributGruppe fag = kante.getFuehrungsformAttributGruppe();
		assertThat(fag.isZweiseitig()).isFalse();
		final List<FuehrungsformAttribute> faLinks = fag.getImmutableFuehrungsformAttributeLinks();
		assertThat(faLinks).hasSize(1);
		FuehrungsformAttribute fa = faLinks.get(0);
		assertThat(fa.getRadverkehrsfuehrung()).isEqualTo(Radverkehrsfuehrung.SONDERWEG_RADWEG_SELBSTSTAENDIG);
		assertThat(fa.getBreite().get()).isEqualTo(Laenge.of(2.45));
		assertThat(fa.getParkenTyp()).isEqualTo(KfzParkenTyp.LAENGS_PARKEN);
		assertThat(fa.getParkenForm()).isEqualTo(KfzParkenForm.PARKBUCHTEN);
		assertThat(fa.getBordstein()).isEqualTo(Bordstein.KOMPLETT_ABGESENKT);
		assertThat(fa.getBelagArt()).isEqualTo(BelagArt.UNGEBUNDENE_DECKE);
		assertThat(fa.getOberflaechenbeschaffenheit()).isEqualTo(Oberflaechenbeschaffenheit.NEUWERTIG);
		assertThat(fa.getBenutzungspflicht()).isEqualTo(Benutzungspflicht.VORHANDEN);

		GeschwindigkeitAttributGruppe gag = kante.getGeschwindigkeitAttributGruppe();
		List<GeschwindigkeitAttribute> gas = gag.getImmutableGeschwindigkeitAttribute();
		assertThat(gas).hasSize(1);
		GeschwindigkeitAttribute ga = gas.get(0);
		assertThat(ga.getOrtslage().get()).isEqualTo(KantenOrtslage.AUSSERORTS);
		assertThat(ga.getHoechstgeschwindigkeit()).isEqualTo(Hoechstgeschwindigkeit.UEBER_100_KMH);
		assertThat(ga.getAbweichendeHoechstgeschwindigkeitGegenStationierungsrichtung().get())
			.isEqualTo(Hoechstgeschwindigkeit.MAX_9_KMH);

		ZustaendigkeitAttributGruppe zag = kante.getZustaendigkeitAttributGruppe();
		List<ZustaendigkeitAttribute> zas = zag.getImmutableZustaendigkeitAttribute();
		assertThat(zas).hasSize(1);
		ZustaendigkeitAttribute za = zas.get(0);
		assertThat(za.getBaulastTraeger().get()).isEqualTo(oetigheim);
		assertThat(za.getUnterhaltsZustaendiger().get()).isEqualTo(stuttgart);
		assertThat(za.getErhaltsZustaendiger().get()).isEqualTo(toubiz);
		assertThat(za.getVereinbarungsKennung().get()).isEqualTo(VereinbarungsKennung.of("vereinbarung123"));

		FahrtrichtungAttributGruppe frag = kante.getFahrtrichtungAttributGruppe();
		assertThat(frag.isZweiseitig()).isFalse();
		assertThat(frag.getFahrtrichtungLinks()).isEqualTo(Richtung.GEGEN_RICHTUNG);
		assertThat(frag.getFahrtrichtungRechts()).isEqualTo(Richtung.GEGEN_RICHTUNG);
	}

	@Test
	void testAttributeUebernehmen_RadVIS_einseitigeKante_zweiFeatures_kanteWirdZweiseitig() {
		// Arrange
		LineString linestring1 = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
			.createLineString(new Coordinate[] { new Coordinate(120.1, 230.2), new Coordinate(200.3, 330.2) });
		Map<String, Object> attribute = new HashMap<>();
		attribute.put("belag_art", "NATURSTEINPFLASTER");
		attribute.put("seite", "LINKS");
		FeatureMapping featureMapping1 = new FeatureMapping(0L, attribute, linestring1);

		LineString linestring2 = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
			.createLineString(new Coordinate[] { new Coordinate(120.1, 230.2), new Coordinate(200.3, 330.2) });
		Map<String, Object> attribute2 = new HashMap<>();
		attribute2.put("belag_art", "SONSTIGER_BELAG");
		attribute2.put("seite", "RECHTS");
		FeatureMapping featureMapping2 = new FeatureMapping(1L, attribute2, linestring2);

		LineString featureMatchedLineString1 = GeometryTestdataProvider.createLineString(new Coordinate(120, 230),
			new Coordinate(200, 310));
		LineString featureMatchedLineString2 = GeometryTestdataProvider.createLineString(new Coordinate(120, 230),
			new Coordinate(200, 310));

		Kante kante = createKante(120, 230, 200, 310, 1L);

		featureMapping1.add(new MappedGrundnetzkante(kante.getGeometry(), kante.getId(), featureMatchedLineString1));
		featureMapping2.add(new MappedGrundnetzkante(kante.getGeometry(), kante.getId(), featureMatchedLineString2));

		List<FeatureMapping> featureMappings = List.of(
			featureMapping1,
			featureMapping2);

		final List<String> attributliste = List.of("belag_art");
		final AttributeImportSession session = new AttributeImportSession(
			BenutzerTestDataProvider.defaultBenutzer().build(),
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build(), attributliste,
			AttributeImportFormat.RADVIS);
		session.setFeatureMappings(featureMappings);

		InMemoryKantenRepository inMemoryKantenRepository = InMemoryKantenRepositoryTestProvider.withKanten(
			Set.of(kante));

		when(inMemoryKantenRepositoryFactory.create(any(MultiPolygon.class))).thenReturn(
			inMemoryKantenRepository);

		// Act
		manuellerAttributeImportUebernahmeService.attributeUebernehmen(session.getAttribute(),
			session.getOrganisation(), session.getFeatureMappings(), new AttributMapperFactory(
				verwaltungseinheitService).createMapper(
				session.getAttributeImportFormat()),
			new AttributeImportKonfliktProtokoll());

		// Assert
		assertThat(kante.isZweiseitig()).isTrue();
		FuehrungsformAttributGruppe fag = kante.getFuehrungsformAttributGruppe();
		assertThat(fag.isZweiseitig()).isTrue();
		final List<FuehrungsformAttribute> faLinks = fag.getImmutableFuehrungsformAttributeLinks();
		assertThat(faLinks).hasSize(1);
		assertThat(faLinks.get(0).getBelagArt()).isEqualTo(BelagArt.NATURSTEINPFLASTER);
		final List<FuehrungsformAttribute> faRechts = fag.getImmutableFuehrungsformAttributeRechts();
		assertThat(faRechts).hasSize(1);
		assertThat(faRechts.get(0).getBelagArt()).isEqualTo(BelagArt.SONSTIGER_BELAG);
	}

	@Test
	void testAttributeUebernehmen_RadVIS_zweiseitigeKante_zweiFeatures() {
		// Arrange
		LineString linestring1 = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
			.createLineString(new Coordinate[] { new Coordinate(120.1, 230.2), new Coordinate(200.3, 330.2) });
		Map<String, Object> attribute = new HashMap<>();
		attribute.put("belag_art", "NATURSTEINPFLASTER");
		attribute.put("seite", "LINKS");
		FeatureMapping featureMapping1 = new FeatureMapping(0L, attribute, linestring1);

		LineString linestring2 = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
			.createLineString(new Coordinate[] { new Coordinate(120.1, 230.2), new Coordinate(200.3, 330.2) });
		Map<String, Object> attribute2 = new HashMap<>();
		attribute2.put("belag_art", "SONSTIGER_BELAG");
		attribute2.put("seite", "RECHTS");
		FeatureMapping featureMapping2 = new FeatureMapping(1L, attribute2, linestring2);

		LineString featureMatchedLineString1 = GeometryTestdataProvider.createLineString(new Coordinate(120, 230),
			new Coordinate(200, 310));
		LineString featureMatchedLineString2 = GeometryTestdataProvider.createLineString(new Coordinate(120, 230),
			new Coordinate(200, 310));

		Kante kante = KanteTestDataProvider.withCoordinatesAndQuelle(120, 230, 200, 310, QuellSystem.DLM)
			.isZweiseitig(true)
			.fuehrungsformAttributGruppe(FuehrungsformAttributGruppe.builder()
				.fuehrungsformAttributeLinks(
					List.of(FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
						.belagArt(BelagArt.BETON)
						.breite(Laenge.of(123))
						.build())
				)
				.fuehrungsformAttributeRechts(
					List.of(FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
						.belagArt(BelagArt.ASPHALT)
						.breite(Laenge.of(456))
						.build())
				)
				.isZweiseitig(true)
				.build())
			.fahrtrichtungAttributGruppe(FahrtrichtungAttributGruppe.builder()
				.isZweiseitig(true)
				.build())
			.id(1L)
			.build();

		featureMapping1.add(new MappedGrundnetzkante(kante.getGeometry(), kante.getId(), featureMatchedLineString1));
		featureMapping2.add(new MappedGrundnetzkante(kante.getGeometry(), kante.getId(), featureMatchedLineString2));

		List<FeatureMapping> featureMappings = List.of(
			featureMapping1,
			featureMapping2);

		final List<String> attributliste = List.of("belag_art");
		final AttributeImportSession session = new AttributeImportSession(
			BenutzerTestDataProvider.defaultBenutzer().build(),
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build(), attributliste,
			AttributeImportFormat.RADVIS);
		session.setFeatureMappings(featureMappings);

		InMemoryKantenRepository inMemoryKantenRepository = InMemoryKantenRepositoryTestProvider.withKanten(
			Set.of(kante));

		when(inMemoryKantenRepositoryFactory.create(any(MultiPolygon.class))).thenReturn(
			inMemoryKantenRepository);

		// Act
		manuellerAttributeImportUebernahmeService.attributeUebernehmen(session.getAttribute(),
			session.getOrganisation(), session.getFeatureMappings(), new AttributMapperFactory(
				verwaltungseinheitService).createMapper(
				session.getAttributeImportFormat()),
			new AttributeImportKonfliktProtokoll());

		// Assert
		assertThat(kante.isZweiseitig()).isTrue();
		FuehrungsformAttributGruppe fag = kante.getFuehrungsformAttributGruppe();
		assertThat(fag.isZweiseitig()).isTrue();
		final List<FuehrungsformAttribute> faLinks = fag.getImmutableFuehrungsformAttributeLinks();
		assertThat(faLinks).hasSize(1);
		assertThat(faLinks.get(0).getBelagArt()).isEqualTo(BelagArt.NATURSTEINPFLASTER);
		assertThat(faLinks.get(0).getBreite().get()).isEqualTo(Laenge.of(123));
		final List<FuehrungsformAttribute> faRechts = fag.getImmutableFuehrungsformAttributeRechts();
		assertThat(faRechts).hasSize(1);
		assertThat(faRechts.get(0).getBelagArt()).isEqualTo(BelagArt.SONSTIGER_BELAG);
		assertThat(faRechts.get(0).getBreite().get()).isEqualTo(Laenge.of(456));
	}

	@Test
	void testAttributeUebernehmen_RadVIS_zweiseitigeKante_dreiFeatures_eineSeiteLR() {
		// Arrange
		LineString linestring1 = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
			.createLineString(new Coordinate[] { new Coordinate(120.1, 230.2), new Coordinate(320.2, 430.1) });
		Map<String, Object> attribute = new HashMap<>();
		attribute.put("belag_art", "NATURSTEINPFLASTER");
		attribute.put("seite", "LINKS");
		FeatureMapping featureMapping1 = new FeatureMapping(0L, attribute, linestring1);

		LineString linestring2 = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
			.createLineString(new Coordinate[] { new Coordinate(120.1, 230.2), new Coordinate(200.3, 330.2) });
		Map<String, Object> attribute2 = new HashMap<>();
		attribute2.put("belag_art", "SONSTIGER_BELAG");
		attribute2.put("seite", "RECHTS");
		FeatureMapping featureMapping2 = new FeatureMapping(1L, attribute2, linestring2);

		LineString linestring3 = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
			.createLineString(new Coordinate[] { new Coordinate(200.3, 330.2), new Coordinate(320.2, 430.1) });
		Map<String, Object> attribute3 = new HashMap<>();
		attribute3.put("belag_art", "BETONSTEINPFLASTER_PLATTENBELAG");
		attribute3.put("seite", "RECHTS");
		FeatureMapping featureMapping3 = new FeatureMapping(2L, attribute3, linestring3);

		LineString featureMatchedLineString1 = GeometryTestdataProvider.createLineString(new Coordinate(120, 230),
			new Coordinate(320, 430));
		LineString featureMatchedLineString2 = GeometryTestdataProvider.createLineString(new Coordinate(120, 230),
			new Coordinate(200, 310));
		LineString featureMatchedLineString3 = GeometryTestdataProvider.createLineString(new Coordinate(200, 310),
			new Coordinate(320, 430));

		Kante kante = KanteTestDataProvider.withCoordinatesAndQuelle(120, 230, 320, 430, QuellSystem.DLM)
			.isZweiseitig(true)
			.fuehrungsformAttributGruppe(FuehrungsformAttributGruppe.builder()
				.fuehrungsformAttributeLinks(
					List.of(FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
						.belagArt(BelagArt.BETON)
						.breite(Laenge.of(123))
						.build())
				)
				.fuehrungsformAttributeRechts(
					List.of(FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
						.belagArt(BelagArt.ASPHALT)
						.breite(Laenge.of(456))
						.build())
				)
				.isZweiseitig(true)
				.build())
			.fahrtrichtungAttributGruppe(FahrtrichtungAttributGruppe.builder()
				.isZweiseitig(true)
				.build())
			.id(1L)
			.build();

		featureMapping1.add(new MappedGrundnetzkante(kante.getGeometry(), kante.getId(), featureMatchedLineString1));
		featureMapping2.add(new MappedGrundnetzkante(kante.getGeometry(), kante.getId(), featureMatchedLineString2));
		featureMapping3.add(new MappedGrundnetzkante(kante.getGeometry(), kante.getId(), featureMatchedLineString3));

		List<FeatureMapping> featureMappings = List.of(
			featureMapping1,
			featureMapping2,
			featureMapping3);

		final List<String> attributliste = List.of("belag_art");
		final AttributeImportSession session = new AttributeImportSession(
			BenutzerTestDataProvider.defaultBenutzer().build(),
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build(), attributliste,
			AttributeImportFormat.RADVIS);
		session.setFeatureMappings(featureMappings);

		InMemoryKantenRepository inMemoryKantenRepository = InMemoryKantenRepositoryTestProvider.withKanten(
			Set.of(kante));

		when(inMemoryKantenRepositoryFactory.create(any(MultiPolygon.class))).thenReturn(
			inMemoryKantenRepository);

		// Act
		manuellerAttributeImportUebernahmeService.attributeUebernehmen(session.getAttribute(),
			session.getOrganisation(), session.getFeatureMappings(), new AttributMapperFactory(
				verwaltungseinheitService).createMapper(
				session.getAttributeImportFormat()),
			new AttributeImportKonfliktProtokoll());

		// Assert
		assertThat(kante.isZweiseitig()).isTrue();
		FuehrungsformAttributGruppe fag = kante.getFuehrungsformAttributGruppe();
		assertThat(fag.isZweiseitig()).isTrue();

		final List<FuehrungsformAttribute> faLinks = fag.getImmutableFuehrungsformAttributeLinks();
		assertThat(faLinks).hasSize(1);
		assertThat(faLinks.get(0).getBelagArt()).isEqualTo(BelagArt.NATURSTEINPFLASTER);
		assertThat(faLinks.get(0).getBreite().get()).isEqualTo(Laenge.of(123));

		final List<FuehrungsformAttribute> faRechts = sortByLineareReferenz(
			fag.getImmutableFuehrungsformAttributeRechts());
		assertThat(faRechts).hasSize(2);
		assertThat(faRechts).extracting(LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt)
			.usingElementComparator(LineareReferenzTestProvider.lenientComparator)
			.containsExactly(LinearReferenzierterAbschnitt.of(0, 0.4),
				LinearReferenzierterAbschnitt.of(0.4, 1));
		assertThat(faRechts.get(0).getBelagArt()).isEqualTo(BelagArt.SONSTIGER_BELAG);
		assertThat(faRechts.get(0).getBreite().get()).isEqualTo(Laenge.of(456));
		assertThat(faRechts.get(1).getBelagArt()).isEqualTo(BelagArt.BETONSTEINPFLASTER_PLATTENBELAG);
		assertThat(faRechts.get(1).getBreite().get()).isEqualTo(Laenge.of(456));
	}

	@Test
	void testAttributeUebernehmen_RadVIS_trennstreifenErgaenzt() {
		// Arrange
		LineString linestring1 = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
			.createLineString(new Coordinate[] { new Coordinate(120.1, 230.2), new Coordinate(200.3, 330.2) });
		Map<String, Object> attribute = new HashMap<>();
		attribute.put("radverkehr", "GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND");
		attribute.put("sts_f_l", "TRENNUNG_DURCH_SPERRPFOSTEN");
		attribute.put("sts_b_l", "1,23");
		attribute.put("sts_t_l", "SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN");
		attribute.put("sts_f_r", "TRENNUNG_DURCH_GRUENSTREIFEN");
		attribute.put("sts_b_r", "2,34");
		attribute.put("sts_t_r", "SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR");

		FeatureMapping featureMapping1 = new FeatureMapping(0L, attribute, linestring1);

		LineString featureMatchedLineString1 = GeometryTestdataProvider.createLineString(new Coordinate(120, 230),
			new Coordinate(200, 310));

		FuehrungsformAttribute fuehrungsformAttribute = FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
			.radverkehrsfuehrung(Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND)
			// Keine Trennstreifen -> Sollen via Import ergänzt werden
			.build();

		Kante kante = KanteTestDataProvider.withCoordinatesAndQuelle(120, 230, 200, 310, QuellSystem.DLM)
			.id(1L)
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
					.isZweiseitig(false)
					.fuehrungsformAttributeRechts(List.of(fuehrungsformAttribute))
					.fuehrungsformAttributeLinks(List.of(fuehrungsformAttribute))
					.build()
			)
			.build();

		featureMapping1.add(new MappedGrundnetzkante(kante.getGeometry(), kante.getId(), featureMatchedLineString1));

		List<FeatureMapping> featureMappings = List.of(featureMapping1);

		final List<String> attributliste = List.of(
			"radverkehr",
			"sts_f_l",
			"sts_b_l",
			"sts_t_l",
			"sts_f_r",
			"sts_b_r",
			"sts_t_r"
		);
		final AttributeImportSession session = new AttributeImportSession(
			BenutzerTestDataProvider.defaultBenutzer().build(),
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build(), attributliste,
			AttributeImportFormat.RADVIS);
		session.setFeatureMappings(featureMappings);

		InMemoryKantenRepository inMemoryKantenRepository = InMemoryKantenRepositoryTestProvider.withKanten(
			Set.of(kante));

		when(inMemoryKantenRepositoryFactory.create(any(MultiPolygon.class))).thenReturn(
			inMemoryKantenRepository);

		// Act
		manuellerAttributeImportUebernahmeService.attributeUebernehmen(session.getAttribute(),
			session.getOrganisation(), session.getFeatureMappings(), new AttributMapperFactory(
				verwaltungseinheitService).createMapper(
				session.getAttributeImportFormat()),
			new AttributeImportKonfliktProtokoll());

		// Assert
		assertThat(kante.isZweiseitig()).isFalse();
		FuehrungsformAttributGruppe fag = kante.getFuehrungsformAttributGruppe();
		assertThat(fag.isZweiseitig()).isFalse();
		final List<FuehrungsformAttribute> faLinks = fag.getImmutableFuehrungsformAttributeLinks();
		assertThat(faLinks).hasSize(1);
		FuehrungsformAttribute fa = faLinks.get(0);
		assertThat(fa.getRadverkehrsfuehrung()).isEqualTo(Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND);
		assertThat(fa.getTrennstreifenFormLinks()).contains(TrennstreifenForm.TRENNUNG_DURCH_SPERRPFOSTEN);
		assertThat(fa.getTrennstreifenBreiteLinks()).contains(Laenge.of(1.23));
		assertThat(fa.getTrennstreifenTrennungZuLinks()).contains(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN);
		assertThat(fa.getTrennstreifenFormRechts()).contains(TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN);
		assertThat(fa.getTrennstreifenBreiteRechts()).contains(Laenge.of(2.34));
		assertThat(fa.getTrennstreifenTrennungZuRechts()).contains(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR);
	}

	@Test
	void testAttributeUebernehmen_RadVIS_trennstreifenUnverändert() {
		// Arrange
		LineString linestring1 = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
			.createLineString(new Coordinate[] { new Coordinate(120.1, 230.2), new Coordinate(200.3, 330.2) });
		Map<String, Object> attribute = new HashMap<>();
		attribute.put("radverkehr", "GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND");
		attribute.put("sts_f_l", "TRENNUNG_DURCH_SPERRPFOSTEN");
		attribute.put("sts_b_l", "1,23");
		attribute.put("sts_t_l", "SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN");
		attribute.put("sts_f_r", "TRENNUNG_DURCH_GRUENSTREIFEN");
		attribute.put("sts_b_r", "2,34");
		attribute.put("sts_t_r", "SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR");

		FeatureMapping featureMapping1 = new FeatureMapping(0L, attribute, linestring1);

		LineString featureMatchedLineString1 = GeometryTestdataProvider.createLineString(new Coordinate(120, 230),
			new Coordinate(200, 310));

		FuehrungsformAttribute fuehrungsformAttribute = FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
			.radverkehrsfuehrung(Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND)
			.trennstreifenFormLinks(TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN)
			.trennstreifenBreiteLinks(Laenge.of(1.23))
			.trennstreifenTrennungZuLinks(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN)
			.trennstreifenFormRechts(TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN)
			.trennstreifenBreiteRechts(Laenge.of(2.34))
			.trennstreifenTrennungZuRechts(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR)
			.build();

		Kante kante = KanteTestDataProvider.withCoordinatesAndQuelle(120, 230, 200, 310, QuellSystem.DLM)
			.id(1L)
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
					.isZweiseitig(false)
					.fuehrungsformAttributeRechts(List.of(fuehrungsformAttribute))
					.fuehrungsformAttributeLinks(List.of(fuehrungsformAttribute))
					.build()
			)
			.build();

		featureMapping1.add(new MappedGrundnetzkante(kante.getGeometry(), kante.getId(), featureMatchedLineString1));

		List<FeatureMapping> featureMappings = List.of(featureMapping1);

		final List<String> attributliste = List.of(
			"radverkehr",
			"sts_f_l",
			"sts_b_l",
			"sts_t_l",
			"sts_f_r",
			"sts_b_r",
			"sts_t_r"
		);
		final AttributeImportSession session = new AttributeImportSession(
			BenutzerTestDataProvider.defaultBenutzer().build(),
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build(), attributliste,
			AttributeImportFormat.RADVIS);
		session.setFeatureMappings(featureMappings);

		InMemoryKantenRepository inMemoryKantenRepository = InMemoryKantenRepositoryTestProvider.withKanten(
			Set.of(kante));

		when(inMemoryKantenRepositoryFactory.create(any(MultiPolygon.class))).thenReturn(
			inMemoryKantenRepository);

		// Act
		manuellerAttributeImportUebernahmeService.attributeUebernehmen(session.getAttribute(),
			session.getOrganisation(), session.getFeatureMappings(), new AttributMapperFactory(
				verwaltungseinheitService).createMapper(
				session.getAttributeImportFormat()),
			new AttributeImportKonfliktProtokoll());

		// Assert
		assertThat(kante.isZweiseitig()).isFalse();
		FuehrungsformAttributGruppe fag = kante.getFuehrungsformAttributGruppe();
		assertThat(fag.isZweiseitig()).isFalse();
		final List<FuehrungsformAttribute> faLinks = fag.getImmutableFuehrungsformAttributeLinks();
		assertThat(faLinks).hasSize(1);
		FuehrungsformAttribute fa = faLinks.get(0);
		assertThat(fa.getRadverkehrsfuehrung()).isEqualTo(Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND);
		assertThat(fa.getTrennstreifenFormLinks()).contains(TrennstreifenForm.TRENNUNG_DURCH_SPERRPFOSTEN);
		assertThat(fa.getTrennstreifenBreiteLinks()).contains(Laenge.of(1.23));
		assertThat(fa.getTrennstreifenTrennungZuLinks()).contains(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN);
		assertThat(fa.getTrennstreifenFormRechts()).contains(TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN);
		assertThat(fa.getTrennstreifenBreiteRechts()).contains(Laenge.of(2.34));
		assertThat(fa.getTrennstreifenTrennungZuRechts()).contains(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR);
	}

	@Test
	void testAttributeUebernehmen_RadVIS_trennstreifenEntfernt() {
		// Arrange
		LineString linestring1 = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
			.createLineString(new Coordinate[] { new Coordinate(120.1, 230.2), new Coordinate(200.3, 330.2) });
		Map<String, Object> attribute = new HashMap<>();
		attribute.put("radverkehr", "PIKTOGRAMMKETTE"); // Führungsform wird durch Import geändert

		FeatureMapping featureMapping1 = new FeatureMapping(0L, attribute, linestring1);

		LineString featureMatchedLineString1 = GeometryTestdataProvider.createLineString(new Coordinate(120, 230),
			new Coordinate(200, 310));

		FuehrungsformAttribute fuehrungsformAttribute = FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
			.radverkehrsfuehrung(Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND)
			.trennstreifenFormLinks(TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN)
			.trennstreifenBreiteLinks(Laenge.of(1.23))
			.trennstreifenTrennungZuLinks(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN)
			.trennstreifenFormRechts(TrennstreifenForm.TRENNUNG_DURCH_FAHRZEUGRUEKHALTESYSTEM)
			.trennstreifenBreiteRechts(Laenge.of(2.34))
			.trennstreifenTrennungZuRechts(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN)
			.build();

		Kante kante = KanteTestDataProvider.withCoordinatesAndQuelle(120, 230, 200, 310, QuellSystem.DLM)
			.id(1L)
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
					.isZweiseitig(false)
					.fuehrungsformAttributeRechts(List.of(fuehrungsformAttribute))
					.fuehrungsformAttributeLinks(List.of(fuehrungsformAttribute))
					.build()
			)
			.build();

		featureMapping1.add(new MappedGrundnetzkante(kante.getGeometry(), kante.getId(), featureMatchedLineString1));

		List<FeatureMapping> featureMappings = List.of(featureMapping1);

		final List<String> attributliste = List.of(
			"radverkehr"
		);
		final AttributeImportSession session = new AttributeImportSession(
			BenutzerTestDataProvider.defaultBenutzer().build(),
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build(), attributliste,
			AttributeImportFormat.RADVIS);
		session.setFeatureMappings(featureMappings);

		InMemoryKantenRepository inMemoryKantenRepository = InMemoryKantenRepositoryTestProvider.withKanten(
			Set.of(kante));

		when(inMemoryKantenRepositoryFactory.create(any(MultiPolygon.class))).thenReturn(
			inMemoryKantenRepository);

		// Act
		manuellerAttributeImportUebernahmeService.attributeUebernehmen(session.getAttribute(),
			session.getOrganisation(), session.getFeatureMappings(), new AttributMapperFactory(
				verwaltungseinheitService).createMapper(
				session.getAttributeImportFormat()),
			new AttributeImportKonfliktProtokoll());

		// Assert
		assertThat(kante.isZweiseitig()).isFalse();
		FuehrungsformAttributGruppe fag = kante.getFuehrungsformAttributGruppe();
		assertThat(fag.isZweiseitig()).isFalse();
		final List<FuehrungsformAttribute> faLinks = fag.getImmutableFuehrungsformAttributeLinks();
		assertThat(faLinks).hasSize(1);
		FuehrungsformAttribute fa = faLinks.get(0);
		assertThat(fa.getRadverkehrsfuehrung()).isEqualTo(Radverkehrsfuehrung.PIKTOGRAMMKETTE);
		assertThat(fa.getTrennstreifenFormLinks()).isEmpty();
		assertThat(fa.getTrennstreifenBreiteLinks()).isEmpty();
		assertThat(fa.getTrennstreifenTrennungZuLinks()).isEmpty();
		assertThat(fa.getTrennstreifenFormRechts()).isEmpty();
		assertThat(fa.getTrennstreifenBreiteRechts()).isEmpty();
		assertThat(fa.getTrennstreifenTrennungZuRechts()).isEmpty();
	}

	@NotNull
	private <T extends LinearReferenzierteAttribute> List<T> sortByLineareReferenz(List<T> attribute) {
		return attribute
			.stream()
			.sorted(Comparator.comparing(LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt,
				LinearReferenzierterAbschnitt.vonZuerst))
			.collect(Collectors.toList());
	}

	private Kante createKante(double x1, double y1, double x2, double y2, long id) {
		return KanteTestDataProvider.withCoordinatesAndQuelle(x1, y1, x2, y2, QuellSystem.DLM)
			.id(id)
			.build();
	}
}
