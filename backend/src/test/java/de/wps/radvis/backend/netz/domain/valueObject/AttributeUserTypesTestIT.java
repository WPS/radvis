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

package de.wps.radvis.backend.netz.domain.valueObject;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.ContextConfiguration;

import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.benutzer.domain.TechnischerBenutzerConfigurationProperties;
import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.netz.NetzConfiguration;
import de.wps.radvis.backend.netz.domain.NetzConfigurationProperties;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.KantenAttribute;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.KnotenAttribute;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.repository.KnotenRepository;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Tag("group2")
@ContextConfiguration(classes = {
	NetzConfiguration.class,
	CommonConfiguration.class,
	OrganisationConfiguration.class,
	BenutzerConfiguration.class,
	GeoConverterConfiguration.class
})
@EnableConfigurationProperties(value = {
	CommonConfigurationProperties.class,
	FeatureToggleProperties.class,
	TechnischerBenutzerConfigurationProperties.class,
	PostgisConfigurationProperties.class,
	OrganisationConfigurationProperties.class,
	NetzConfigurationProperties.class
})
class AttributeUserTypesTestIT extends DBIntegrationTestIT {

	@PersistenceContext
	EntityManager entityManager;
	@Autowired
	private KantenRepository kantenRepository;

	@Autowired
	private KnotenRepository knotenRepository;

	@Test
	void testSaveAndLoad_defaultKantenAttribute() {
		// arrange
		KantenAttributGruppe kantenAttributGruppe = KantenAttributGruppe.builder().build();
		Kante kante = KanteTestDataProvider.withDefaultValues().kantenAttributGruppe(kantenAttributGruppe).build();

		// act
		long id = kantenRepository.save(kante).getId();
		entityManager.flush();
		entityManager.clear();

		// assert
		Kante kanteAusDatenbank = kantenRepository.findById(id).orElseThrow();

		assertThat(kanteAusDatenbank.getKantenAttributGruppe().getKantenAttribute())
			.isEqualTo(kantenAttributGruppe.getKantenAttribute());
	}

	@Test
	void testSaveAndLoad_KantenAttribute() {
		// arrange
		KantenAttribute kantenAttribute = KantenAttribute.builder()
			.dtvFussverkehr(VerkehrStaerke.of(20))
			.dtvPkw(VerkehrStaerke.of(30))
			.dtvRadverkehr(VerkehrStaerke.of(40))
			.kommentar(Kommentar.of("comment"))
			.umfeld(Umfeld.UNBEKANNT)
			.strassenquerschnittRASt06(StrassenquerschnittRASt06.UNBEKANNT)
			.beleuchtung(Beleuchtung.VORHANDEN)
			.status(Status.defaultWert())
			.build();
		Kante kante = KanteTestDataProvider.withDefaultValues()
			.kantenAttributGruppe(new KantenAttributGruppe(kantenAttribute, new HashSet<>(), new HashSet<>())).build();

		// act
		long id = kantenRepository.save(kante).getId();
		entityManager.flush();
		entityManager.clear();

		// assert
		Kante kanteAusDatenbank = kantenRepository.findById(id).orElseThrow();

		assertThat(kanteAusDatenbank.getKantenAttributGruppe().getKantenAttribute()).isEqualTo(kantenAttribute);
	}

	@Test
	void testSaveAndLoad_leereKnotenAttribute() {
		// arrange
		KnotenAttribute knotenAttribute = KnotenAttribute.builder().build();
		Knoten knoten = KnotenTestDataProvider.withDefaultValues().knotenAttribute(knotenAttribute).build();

		// act
		long id = knotenRepository.save(knoten).getId();
		entityManager.flush();
		entityManager.clear();

		// assert
		Knoten knotenAusDatenbank = knotenRepository.findById(id).orElseThrow();

		assertThat(knotenAusDatenbank.getKnotenAttribute()).isEqualTo(knotenAttribute);
	}

	@Test
	void testSaveAndLoad_KnotenAttribute() {
		// arrange
		KnotenAttribute.KnotenAttributeBuilder knotenAttributeBuilder = KnotenAttribute.builder();
		knotenAttributeBuilder.knotenForm(KnotenForm.ABKNICKENDE_VORFAHRT_OHNE_LSA);
		knotenAttributeBuilder.kommentar(Kommentar.of("Mein Kommentar"));

		KnotenAttribute knotenAttribute = knotenAttributeBuilder.build();

		Knoten knoten = KnotenTestDataProvider.withDefaultValues().knotenAttribute(knotenAttribute).build();

		// act
		long id = knotenRepository.save(knoten).getId();
		entityManager.flush();
		entityManager.clear();

		// assert
		Knoten knotenAusDatenbank = knotenRepository.findById(id).orElseThrow();

		assertThat(knotenAusDatenbank.getKnotenAttribute()).isEqualTo(knotenAttribute);
	}
}
