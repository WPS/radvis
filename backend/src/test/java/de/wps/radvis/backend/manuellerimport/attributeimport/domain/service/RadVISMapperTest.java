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
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.entity.KantenMapping;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.valueObject.Haendigkeit;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.valueObject.KantenKonfliktProtokoll;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.valueObject.MappedFeature;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.Konflikt;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.provider.FuehrungsformAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.FuehrungsformAttributeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.valueObject.Beschilderung;
import de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung;
import de.wps.radvis.backend.organisation.domain.OrganisationsartUndNameNichtEindeutigException;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;

class RadVISMapperTest {
	@Mock
	VerwaltungseinheitService verwaltungseinheitService;
	private RadVISMapper radVISMapper;

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);
		radVISMapper = new RadVISMapper(verwaltungseinheitService);
	}

	@Test
	void isAttributWertValid_shouldBeImplementedForAllUnterstuetzteAttribute() {
		assertThat(RadVISMapper.UNTERSTUETZTE_ATTRIBUTE).allSatisfy(attr -> {
			assertThatNoException().isThrownBy(() -> radVISMapper.isAttributWertValid(attr, ""));
		});
	}

	@Test
	void getRadVisAttributName_shouldBeImplementedForAllUnterstuetzteAttribute() {
		assertThat(RadVISMapper.UNTERSTUETZTE_ATTRIBUTE).allSatisfy(attr -> {
			assertThatNoException().isThrownBy(() -> radVISMapper.getRadVisAttributName(attr));
		});
	}

	@Test
	void sortAttribute_radverkehrsfuehrungFirst() {
		assertThat(
			List.of("beschilder", "radverkehr", "absenkung").stream().sorted(radVISMapper::sortAttribute).toList())
				.containsExactly("radverkehr", "beschilder", "absenkung");
	}

	@Test
	void mapGemeinde_OrganisationNichtEindeutig() throws OrganisationsartUndNameNichtEindeutigException {
		// arrange
		Kante kante = KanteTestDataProvider.withDefaultValues().id(2l).build();
		MappingService mappingService = new MappingService();

		KantenMapping kantenMapping = new KantenMapping(kante);
		Map<String, Object> shpProperties = new HashMap<>();
		String attributname = "gemeinde_n";
		String gemeindeName = "Altdorf";
		shpProperties.put(attributname, gemeindeName);
		kantenMapping.add(MappedFeature.of(kante.getGeometry(), shpProperties,
			LinearReferenzierterAbschnitt.of(0, 1), Haendigkeit.of(kante.getGeometry(), kante.getGeometry()),
			Seitenbezug.LINKS));

		when(
			verwaltungseinheitService.hasVerwaltungseinheitNachNameUndArt(eq(gemeindeName),
				eq(OrganisationsArt.GEMEINDE)))
					.thenReturn(true);
		OrganisationsartUndNameNichtEindeutigException organisationsartUndNameNichtEindeutigException = new OrganisationsartUndNameNichtEindeutigException(
			gemeindeName, OrganisationsArt.GEMEINDE);
		when(
			verwaltungseinheitService.getVerwaltungseinheitNachNameUndArt(eq(gemeindeName),
				eq(OrganisationsArt.GEMEINDE)))
					.thenThrow(organisationsartUndNameNichtEindeutigException);

		// act
		KantenKonfliktProtokoll kantenKonfliktProtokoll = new KantenKonfliktProtokoll(kante.getId(),
			kante.getGeometry());
		mappingService.map(radVISMapper, attributname, kantenMapping, kantenKonfliktProtokoll);

		// assert
		assertThat(kantenKonfliktProtokoll.getKonflikte()).isNotEmpty();
		Konflikt konflikt = kantenKonfliktProtokoll.getKonflikte().iterator().next();
		assertThat(konflikt.getBemerkung()).isEqualTo(organisationsartUndNameNichtEindeutigException.getMessage());
		assertThat(konflikt.getAttributName()).isEqualTo(attributname);
		assertThat(konflikt.getNichtUebernommeneWerte()).isNotEmpty();
		assertThat(konflikt.getNichtUebernommeneWerte().iterator().next())
			.isEqualTo(gemeindeName);
	}

	@Test
	void mapBeschilderungAndRadverkehrsfuehrung_zwischenzeitlicherValidierungskonflikt_beideAttributeImportiert() {
		// arrange
		Kante kante = KanteTestDataProvider.withDefaultValues()
			.id(2l)
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppeTestDataProvider.withAttribute(FuehrungsformAttributeTestDataProvider
					.withGrundnetzDefaultwerte().radverkehrsfuehrung(Radverkehrsfuehrung.BETRIEBSWEG_FORST)
					.beschilderung(Beschilderung.HAUPTZEICHEN_KFZ_VERBOTEN)).build())
			.build();
		MappingService mappingService = new MappingService();

		KantenMapping kantenMapping = new KantenMapping(kante);
		Map<String, Object> shpProperties = new HashMap<>();
		shpProperties.put("radverkehr", Radverkehrsfuehrung.BEGEGNUNBSZONE.name());
		shpProperties.put("beschilder", Beschilderung.ABGESPERRT.name());
		kantenMapping.add(MappedFeature.of(kante.getGeometry(), shpProperties,
			LinearReferenzierterAbschnitt.of(0, 1), Haendigkeit.of(kante.getGeometry(), kante.getGeometry()),
			Seitenbezug.LINKS));

		// act
		KantenKonfliktProtokoll kantenKonfliktProtokoll = new KantenKonfliktProtokoll(kante.getId(),
			kante.getGeometry());
		// Vom FE kommen die Attribute in alphabetischer Reihenfolge und werden im ManuellerAttributeImportService
		// derart sortiert:
		List.of("beschilder", "radverkehr").stream().sorted(radVISMapper::sortAttribute)
			.forEach((attr) -> mappingService.map(radVISMapper, attr, kantenMapping, kantenKonfliktProtokoll));

		// assert
		assertThat(kantenMapping.getKante().getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks()
			.get(0).getBeschilderung()).isEqualTo(Beschilderung.ABGESPERRT);
		assertThat(kantenMapping.getKante().getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks()
			.get(0).getRadverkehrsfuehrung()).isEqualTo(Radverkehrsfuehrung.BEGEGNUNBSZONE);
	}

	@Test
	void mapBeschilderung_RadverkehrsfuehrungNichtImportiert() {
		// arrange
		Kante kante = KanteTestDataProvider.withDefaultValues()
			.id(2l)
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppeTestDataProvider.withAttribute(FuehrungsformAttributeTestDataProvider
					.withGrundnetzDefaultwerte().radverkehrsfuehrung(Radverkehrsfuehrung.BEGEGNUNBSZONE)
					.beschilderung(Beschilderung.GEHWEG_MIT_VZ_239)).build())
			.build();
		MappingService mappingService = new MappingService();

		KantenMapping kantenMapping = new KantenMapping(kante);
		Map<String, Object> shpProperties = new HashMap<>();
		shpProperties.put("radverkehr", Radverkehrsfuehrung.MEHRZWECKSTREIFEN_BEIDSEITIG.name());
		shpProperties.put("beschilder", Beschilderung.ABGESPERRT.name());
		kantenMapping.add(MappedFeature.of(kante.getGeometry(), shpProperties,
			LinearReferenzierterAbschnitt.of(0, 1), Haendigkeit.of(kante.getGeometry(), kante.getGeometry()),
			Seitenbezug.LINKS));

		// act
		KantenKonfliktProtokoll kantenKonfliktProtokoll = new KantenKonfliktProtokoll(kante.getId(),
			kante.getGeometry());
		mappingService.map(radVISMapper, "beschilder", kantenMapping, kantenKonfliktProtokoll);

		// assert
		assertThat(kantenMapping.getKante().getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks()
			.get(0).getBeschilderung()).isEqualTo(Beschilderung.ABGESPERRT);
		assertThat(kantenMapping.getKante().getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks()
			.get(0).getRadverkehrsfuehrung()).isEqualTo(Radverkehrsfuehrung.BEGEGNUNBSZONE);
	}

	@Test
	void mapRadverkehrsfuehrung_BeschilderungNichtImportiert() {
		// arrange
		Kante kante = KanteTestDataProvider.withDefaultValues()
			.id(2l)
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppeTestDataProvider.withAttribute(FuehrungsformAttributeTestDataProvider
					.withGrundnetzDefaultwerte().radverkehrsfuehrung(Radverkehrsfuehrung.BEGEGNUNBSZONE)
					.beschilderung(Beschilderung.GEHWEG_MIT_VZ_239)).build())
			.build();
		MappingService mappingService = new MappingService();

		KantenMapping kantenMapping = new KantenMapping(kante);
		Map<String, Object> shpProperties = new HashMap<>();
		shpProperties.put("radverkehr", Radverkehrsfuehrung.MEHRZWECKSTREIFEN_BEIDSEITIG.name());
		shpProperties.put("beschilder", Beschilderung.ABGESPERRT.name());
		kantenMapping.add(MappedFeature.of(kante.getGeometry(), shpProperties,
			LinearReferenzierterAbschnitt.of(0, 1), Haendigkeit.of(kante.getGeometry(), kante.getGeometry()),
			Seitenbezug.LINKS));

		// act
		KantenKonfliktProtokoll kantenKonfliktProtokoll = new KantenKonfliktProtokoll(kante.getId(),
			kante.getGeometry());
		mappingService.map(radVISMapper, "radverkehr", kantenMapping, kantenKonfliktProtokoll);

		// assert
		assertThat(kantenMapping.getKante().getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks()
			.get(0).getBeschilderung()).isEqualTo(Beschilderung.GEHWEG_MIT_VZ_239);
		assertThat(kantenMapping.getKante().getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks()
			.get(0).getRadverkehrsfuehrung()).isEqualTo(Radverkehrsfuehrung.MEHRZWECKSTREIFEN_BEIDSEITIG);
	}

	@Test
	void mapBeschilderung_validateRadverkehrsfuehrung_valid() {
		// arrange
		Kante kante = KanteTestDataProvider.withDefaultValues()
			.id(2l)
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppeTestDataProvider.withAttribute(FuehrungsformAttributeTestDataProvider
					.withGrundnetzDefaultwerte().radverkehrsfuehrung(Radverkehrsfuehrung.BETRIEBSWEG_FORST)
					.beschilderung(Beschilderung.UNBEKANNT)).build())
			.build();
		MappingService mappingService = new MappingService();

		KantenMapping kantenMapping = new KantenMapping(kante);
		Map<String, Object> shpProperties = new HashMap<>();
		shpProperties.put("beschilder", Beschilderung.HAUPTZEICHEN_KFZ_VERBOTEN.name());
		kantenMapping.add(MappedFeature.of(kante.getGeometry(), shpProperties,
			LinearReferenzierterAbschnitt.of(0, 1), Haendigkeit.of(kante.getGeometry(), kante.getGeometry()),
			Seitenbezug.LINKS));

		// act
		KantenKonfliktProtokoll kantenKonfliktProtokoll = new KantenKonfliktProtokoll(kante.getId(),
			kante.getGeometry());
		mappingService.map(radVISMapper, "beschilder", kantenMapping, kantenKonfliktProtokoll);

		// assert
		assertThat(kantenKonfliktProtokoll.getKonflikte()).isEmpty();
		assertThat(kantenMapping.getKante().getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks()
			.get(0).getBeschilderung()).isEqualTo(Beschilderung.HAUPTZEICHEN_KFZ_VERBOTEN);
	}

	@Test
	void mapBeschilderung_validateRadverkehrsfuehrung_invalid() {
		// arrange
		Kante kante = KanteTestDataProvider.withDefaultValues()
			.id(2l)
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppeTestDataProvider.withAttribute(FuehrungsformAttributeTestDataProvider
					.withGrundnetzDefaultwerte()
					.beschilderung(Beschilderung.UNBEKANNT)
					.radverkehrsfuehrung(Radverkehrsfuehrung.EINBAHNSTRASSE_OHNE_FREIGABE_RADVERKEHR_MEHR_WENIGER_30))
					.build())
			.build();
		MappingService mappingService = new MappingService();

		KantenMapping kantenMapping = new KantenMapping(kante);
		Map<String, Object> shpProperties = new HashMap<>();
		shpProperties.put("beschilder", Beschilderung.HAUPTZEICHEN_KFZ_VERBOTEN.name());
		kantenMapping.add(MappedFeature.of(kante.getGeometry(), shpProperties,
			LinearReferenzierterAbschnitt.of(0, 1), Haendigkeit.of(kante.getGeometry(), kante.getGeometry()),
			Seitenbezug.BEIDSEITIG));

		// act
		KantenKonfliktProtokoll kantenKonfliktProtokoll = new KantenKonfliktProtokoll(kante.getId(),
			kante.getGeometry());
		mappingService.map(radVISMapper, "beschilder", kantenMapping, kantenKonfliktProtokoll);

		// assert
		assertThat(kantenKonfliktProtokoll.getKonflikte()).isNotEmpty();
		Konflikt konflikt = kantenKonfliktProtokoll.getKonflikte().iterator().next();
		assertThat(konflikt.getAttributName()).isEqualTo("beschilder");
		assertThat(konflikt.getNichtUebernommeneWerte())
			.containsExactly(Beschilderung.HAUPTZEICHEN_KFZ_VERBOTEN.name());
		assertThat(kantenMapping.getKante().getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks()
			.get(0).getBeschilderung()).isEqualTo(Beschilderung.UNBEKANNT);
	}

	@Test
	void mapBeschilderung_seitenbezogen_validateRadverkehrsfuehrung_invalid() {
		// arrange
		Kante kante = KanteTestDataProvider.withDefaultValues()
			.id(2l)
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppeTestDataProvider.withAttribute(FuehrungsformAttributeTestDataProvider
					.withGrundnetzDefaultwerte()
					.beschilderung(Beschilderung.UNBEKANNT)
					.radverkehrsfuehrung(Radverkehrsfuehrung.EINBAHNSTRASSE_OHNE_FREIGABE_RADVERKEHR_MEHR_WENIGER_30))
					.build())
			.build();
		MappingService mappingService = new MappingService();

		KantenMapping kantenMapping = new KantenMapping(kante);
		Map<String, Object> shpProperties = new HashMap<>();
		shpProperties.put("beschilder", Beschilderung.HAUPTZEICHEN_KFZ_VERBOTEN.name());
		kantenMapping.add(MappedFeature.of(kante.getGeometry(), shpProperties,
			LinearReferenzierterAbschnitt.of(0, 1), Haendigkeit.of(kante.getGeometry(), kante.getGeometry()),
			Seitenbezug.LINKS));

		// act
		KantenKonfliktProtokoll kantenKonfliktProtokoll = new KantenKonfliktProtokoll(kante.getId(),
			kante.getGeometry());
		mappingService.map(radVISMapper, "beschilder", kantenMapping, kantenKonfliktProtokoll);

		// assert
		assertThat(kantenKonfliktProtokoll.getKonflikte()).isNotEmpty();
		Konflikt konflikt = kantenKonfliktProtokoll.getKonflikte().iterator().next();
		assertThat(konflikt.getAttributName()).isEqualTo("beschilder");
		assertThat(konflikt.getNichtUebernommeneWerte())
			.containsExactly(Beschilderung.HAUPTZEICHEN_KFZ_VERBOTEN.name());
		assertThat(konflikt.getSeitenbezug()).isEqualTo(Seitenbezug.LINKS);
		assertThat(kantenMapping.getKante().getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks()
			.get(0).getBeschilderung()).isEqualTo(Beschilderung.UNBEKANNT);
	}

	@Test
	void mapRadverkehrsfuehrung_validateBeschilderung_valid() {
		// arrange
		Kante kante = KanteTestDataProvider.withDefaultValues()
			.id(2l)
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppeTestDataProvider.withAttribute(FuehrungsformAttributeTestDataProvider
					.withGrundnetzDefaultwerte().radverkehrsfuehrung(Radverkehrsfuehrung.BETRIEBSWEG_FORST)
					.beschilderung(Beschilderung.UNBEKANNT)).build())
			.build();
		MappingService mappingService = new MappingService();

		KantenMapping kantenMapping = new KantenMapping(kante);
		Map<String, Object> shpProperties = new HashMap<>();
		shpProperties.put("radverkehr", Radverkehrsfuehrung.BEGEGNUNBSZONE.name());
		kantenMapping.add(MappedFeature.of(kante.getGeometry(), shpProperties,
			LinearReferenzierterAbschnitt.of(0, 1), Haendigkeit.of(kante.getGeometry(), kante.getGeometry()),
			Seitenbezug.LINKS));

		// act
		KantenKonfliktProtokoll kantenKonfliktProtokoll = new KantenKonfliktProtokoll(kante.getId(),
			kante.getGeometry());
		mappingService.map(radVISMapper, "radverkehr", kantenMapping, kantenKonfliktProtokoll);

		// assert
		assertThat(kantenKonfliktProtokoll.getKonflikte()).isEmpty();
		assertThat(kantenMapping.getKante().getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks()
			.get(0).getRadverkehrsfuehrung()).isEqualTo(Radverkehrsfuehrung.BEGEGNUNBSZONE);
	}

	@Test
	void mapRadverkehrsfuehrung_seitenbezogen_validateBeschilderung_invalid() {
		// arrange
		Kante kante = KanteTestDataProvider.withDefaultValues()
			.id(2l)
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppeTestDataProvider.withAttribute(FuehrungsformAttributeTestDataProvider
					.withGrundnetzDefaultwerte().radverkehrsfuehrung(Radverkehrsfuehrung.BETRIEBSWEG_FORST)
					.beschilderung(Beschilderung.HAUPTZEICHEN_KFZ_VERBOTEN)).build())
			.build();
		MappingService mappingService = new MappingService();

		KantenMapping kantenMapping = new KantenMapping(kante);
		Map<String, Object> shpProperties = new HashMap<>();
		shpProperties.put("radverkehr", Radverkehrsfuehrung.BEGEGNUNBSZONE.name());
		kantenMapping.add(MappedFeature.of(kante.getGeometry(), shpProperties,
			LinearReferenzierterAbschnitt.of(0, 1), Haendigkeit.of(kante.getGeometry(), kante.getGeometry()),
			Seitenbezug.LINKS));

		// act
		KantenKonfliktProtokoll kantenKonfliktProtokoll = new KantenKonfliktProtokoll(kante.getId(),
			kante.getGeometry());
		mappingService.map(radVISMapper, "radverkehr", kantenMapping, kantenKonfliktProtokoll);

		// assert
		assertThat(kantenKonfliktProtokoll.getKonflikte()).isNotEmpty();
		Konflikt konflikt = kantenKonfliktProtokoll.getKonflikte().iterator().next();
		assertThat(konflikt.getAttributName()).isEqualTo("radverkehr");
		assertThat(konflikt.getNichtUebernommeneWerte())
			.containsExactly(Beschilderung.HAUPTZEICHEN_KFZ_VERBOTEN.name());
		assertThat(konflikt.getSeitenbezug()).isEqualTo(Seitenbezug.LINKS);
		assertThat(konflikt.getBemerkung()).isNotBlank();
		assertThat(kantenMapping.getKante().getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks()
			.get(0).getBeschilderung()).isEqualTo(Beschilderung.UNBEKANNT);
	}

	@Test
	void mapRadverkehrsfuehrung_validateBeschilderung_invalid() {
		// arrange
		Kante kante = KanteTestDataProvider.withDefaultValues()
			.id(2l)
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppeTestDataProvider.withAttribute(FuehrungsformAttributeTestDataProvider
					.withGrundnetzDefaultwerte().radverkehrsfuehrung(Radverkehrsfuehrung.BETRIEBSWEG_FORST)
					.beschilderung(Beschilderung.HAUPTZEICHEN_KFZ_VERBOTEN)).build())
			.build();
		MappingService mappingService = new MappingService();

		KantenMapping kantenMapping = new KantenMapping(kante);
		Map<String, Object> shpProperties = new HashMap<>();
		shpProperties.put("radverkehr", Radverkehrsfuehrung.BEGEGNUNBSZONE.name());
		kantenMapping.add(MappedFeature.of(kante.getGeometry(), shpProperties,
			LinearReferenzierterAbschnitt.of(0, 1), Haendigkeit.of(kante.getGeometry(), kante.getGeometry()),
			Seitenbezug.BEIDSEITIG));

		// act
		KantenKonfliktProtokoll kantenKonfliktProtokoll = new KantenKonfliktProtokoll(kante.getId(),
			kante.getGeometry());
		mappingService.map(radVISMapper, "radverkehr", kantenMapping, kantenKonfliktProtokoll);

		// assert
		assertThat(kantenKonfliktProtokoll.getKonflikte()).isNotEmpty();
		Konflikt konflikt = kantenKonfliktProtokoll.getKonflikte().iterator().next();
		assertThat(konflikt.getAttributName()).isEqualTo("radverkehr");
		assertThat(konflikt.getNichtUebernommeneWerte())
			.containsExactly(Beschilderung.HAUPTZEICHEN_KFZ_VERBOTEN.name());
		assertThat(konflikt.getBemerkung()).isNotBlank();
		assertThat(kantenMapping.getKante().getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks()
			.get(0).getBeschilderung()).isEqualTo(Beschilderung.UNBEKANNT);
	}
}
