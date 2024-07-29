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

package de.wps.radvis.backend.organisation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.organisation.domain.entity.Gebietskoerperschaft;
import de.wps.radvis.backend.organisation.domain.entity.Organisation;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;

class VerwaltungseinheitServiceTest {

	private VerwaltungseinheitService service;

	@Mock
	private VerwaltungseinheitRepository repository;
	@Mock
	private GebietskoerperschaftRepository gebietskoerperschaftRepository;
	@Mock
	private OrganisationRepository organisationRepository;

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);

		this.service = new VerwaltungseinheitService(repository, gebietskoerperschaftRepository,
			organisationRepository, OrganisationsArt.BUNDESLAND, "Baden-Württemberg");
	}

	@Test
	void testGetAllOrganisationSelectViews_keineOrganisationenVorhanden() {
		// arrange

		when(repository.findAll()).thenReturn(Collections.emptyList());

		// act
		List<Verwaltungseinheit> organisationSelectViews = service.getAll();

		// assert
		assertThat(organisationSelectViews).isEmpty();
	}

	@Test
	void testGetAllOrganisationSelectViews_mehrereOrganisationenVorhanden() {
		// arrange
		Verwaltungseinheit testGemeinde = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(1L)
			.name("Meine Gemeinde").fachId(2)
			.organisationsArt(
				OrganisationsArt.GEMEINDE)
			.build();

		when(repository.findAll()).thenReturn(Collections.singletonList(testGemeinde));

		// act
		List<Verwaltungseinheit> organisationSelectViews = service.getAll();

		// assert
		assertThat(organisationSelectViews)
			.containsExactlyInAnyOrder(testGemeinde);
	}

	@Test
	void testIstUebergeordnet() {
		// arrange
		Verwaltungseinheit obersteOrganisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Oberste Organisation").uebergeordneteOrganisation(null)
			.id(1L).organisationsArt(OrganisationsArt.BUNDESLAND).fachId(1)
			.build();
		Verwaltungseinheit mittlereOrganisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Mittlere Organisation").uebergeordneteOrganisation(obersteOrganisation)
			.id(2L).organisationsArt(OrganisationsArt.KREIS).fachId(2)
			.build();
		Verwaltungseinheit untersteOrganisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Unterste Ognaisation").uebergeordneteOrganisation(mittlereOrganisation)
			.id(3L).organisationsArt(OrganisationsArt.GEMEINDE).fachId(3)
			.build();

		// act + assert
		assertThat(service.istUebergeordnet(mittlereOrganisation, untersteOrganisation)).isTrue();
		assertThat(service.istUebergeordnet(mittlereOrganisation, mittlereOrganisation)).isTrue();
		assertThat(service.istUebergeordnet(obersteOrganisation, mittlereOrganisation)).isTrue();
		assertThat(service.istUebergeordnet(obersteOrganisation, untersteOrganisation)).isTrue();
		assertThat(service.istUebergeordnet(untersteOrganisation, mittlereOrganisation)).isFalse();
		assertThat(service.istUebergeordnet(mittlereOrganisation, obersteOrganisation)).isFalse();
	}

	@Test
	void testIstUebergeordnet_externeOrganisation() {
		// arrange
		Gebietskoerperschaft obersteVerwaltungseinheit = VerwaltungseinheitTestDataProvider
			.defaultGebietskoerperschaft()
			.name("Oberste Organisation").uebergeordneteOrganisation(null)
			.id(1L).organisationsArt(OrganisationsArt.BUNDESLAND).fachId(1)
			.build();
		Organisation externeVerwaltungseinheit = VerwaltungseinheitTestDataProvider.defaultOrganisation()
			.name("Externe Organisation").uebergeordneteOrganisation(obersteVerwaltungseinheit)
			.id(2L).organisationsArt(OrganisationsArt.EXTERNER_DIENSTLEISTER)
			.build();
		Gebietskoerperschaft untersteOrganisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Gemeine Gemeinde Ognaisation").uebergeordneteOrganisation(obersteVerwaltungseinheit)
			.id(3L).organisationsArt(OrganisationsArt.GEMEINDE).fachId(3)
			.build();

		Organisation externeOrganisation = VerwaltungseinheitTestDataProvider.defaultOrganisation()
			.name(externeVerwaltungseinheit.getName())
			.uebergeordneteOrganisation(externeVerwaltungseinheit.getUebergeordneteVerwaltungseinheit().get())
			.id(externeVerwaltungseinheit.getId())
			.organisationsArt(externeVerwaltungseinheit.getOrganisationsArt())
			.zustaendigFuerBereichOf(Set.of(untersteOrganisation))
			.build();
		when(organisationRepository.findById(externeOrganisation.getId())).thenReturn(Optional.of(externeOrganisation));

		// act + assert
		assertThat(service.istUebergeordnet(externeOrganisation, untersteOrganisation)).isTrue();
		assertThat(service.istUebergeordnet(externeOrganisation, externeOrganisation)).isTrue();
		assertThat(service.istUebergeordnet(obersteVerwaltungseinheit, externeOrganisation)).isTrue();
		assertThat(service.istUebergeordnet(obersteVerwaltungseinheit, untersteOrganisation)).isTrue();
		assertThat(service.istUebergeordnet(untersteOrganisation, externeOrganisation)).isFalse();
		assertThat(service.istUebergeordnet(externeOrganisation, obersteVerwaltungseinheit)).isFalse();
	}

	@Test
	void testIstUebergeordnet_paralleleOrganisation() {
		// arrange
		Verwaltungseinheit obersteOrganisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Oberste Organisation").uebergeordneteOrganisation(null)
			.id(1L).organisationsArt(OrganisationsArt.BUNDESLAND).fachId(1)
			.build();
		Verwaltungseinheit mittlereOrganisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Mittlere Organisation").uebergeordneteOrganisation(obersteOrganisation)
			.id(2L).organisationsArt(OrganisationsArt.KREIS).fachId(2)
			.build();
		Verwaltungseinheit andereMittlereOrganisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Unterste Ognaisation").uebergeordneteOrganisation(obersteOrganisation)
			.id(3L).organisationsArt(OrganisationsArt.GEMEINDE).fachId(3)
			.build();

		// act + assert
		assertThat(service.istUebergeordnet(mittlereOrganisation, andereMittlereOrganisation)).isFalse();
		assertThat(service.istUebergeordnet(mittlereOrganisation, mittlereOrganisation)).isTrue();
		assertThat(service.istUebergeordnet(mittlereOrganisation, obersteOrganisation)).isFalse();
		assertThat(service.istUebergeordnet(obersteOrganisation, mittlereOrganisation)).isTrue();
		assertThat(service.istUebergeordnet(obersteOrganisation, andereMittlereOrganisation)).isTrue();
		assertThat(service.istUebergeordnet(obersteOrganisation, obersteOrganisation)).isTrue();
		assertThat(service.istUebergeordnet(andereMittlereOrganisation, mittlereOrganisation)).isFalse();
		assertThat(service.istUebergeordnet(andereMittlereOrganisation, obersteOrganisation)).isFalse();
		assertThat(service.istUebergeordnet(andereMittlereOrganisation, andereMittlereOrganisation)).isTrue();
	}

	@Test
	void testFindeAlleZustaendigenOrganisationen() {
		// arrange
		// die organisationen hängen zusammen
		Verwaltungseinheit obersteOrganisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Oberste Organisation").uebergeordneteOrganisation(null)
			.id(1L).organisationsArt(OrganisationsArt.BUNDESLAND).fachId(1)
			.build();
		Verwaltungseinheit mittlereOrganisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Mittlere Organisation").uebergeordneteOrganisation(obersteOrganisation)
			.id(2L).organisationsArt(OrganisationsArt.KREIS).fachId(2)
			.build();
		Verwaltungseinheit untersteOrganisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Unterste Ognaisation").uebergeordneteOrganisation(mittlereOrganisation)
			.id(3L).organisationsArt(OrganisationsArt.GEMEINDE).fachId(3)
			.build();
		// diese Organisationen haben nichts mit den anderen zu tun, hängen aber zusammen
		Verwaltungseinheit nebenOrganisationOben = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Obere Nebenorganisation").uebergeordneteOrganisation(null)
			.id(4L).organisationsArt(OrganisationsArt.BUNDESLAND).fachId(4)
			.build();
		Verwaltungseinheit nebenOrganisationUnten = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Untere Nebenorganisation").uebergeordneteOrganisation(nebenOrganisationOben)
			.id(5L).organisationsArt(OrganisationsArt.REGIERUNGSBEZIRK).fachId(5)
			.build();

		// act
		List<Verwaltungseinheit> result = service.findeAlleZustaendigenVerwaltungseinheiten(untersteOrganisation);

		// assert
		assertThat(result)
			.containsExactlyInAnyOrder(obersteOrganisation, mittlereOrganisation, untersteOrganisation)
			.doesNotContain(nebenOrganisationOben, nebenOrganisationUnten);
	}
}
