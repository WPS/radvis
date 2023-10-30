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

package de.wps.radvis.backend.administration.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.organisation.domain.OrganisationRepository;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitRepository;
import de.wps.radvis.backend.organisation.domain.dbView.VerwaltungseinheitDbView;
import de.wps.radvis.backend.organisation.domain.entity.Gebietskoerperschaft;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;

public class AdministrationServiceTest {
	@Mock
	private VerwaltungseinheitRepository repository;
	@Mock
	private OrganisationRepository organisationRepository;
	AdministrationService service;

	@BeforeEach
	public void setup() {
		MockitoAnnotations.openMocks(this);
		service = new AdministrationService(repository, organisationRepository);
	}

	@Test
	public void getAllZuweisbareForBenutzer_admin_allGebietskoerperschaften() {
		Gebietskoerperschaft gebietskoerperschaft = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.build();
		when(repository.findAllAsView()).thenReturn(
			List.of(new VerwaltungseinheitDbView(gebietskoerperschaft),
				new VerwaltungseinheitDbView(VerwaltungseinheitTestDataProvider.defaultOrganisation().build())));

		List<VerwaltungseinheitDbView> allZuweisbareForBenutzer = service.getAllZuweisbareOrganisationenForBenutzer(
			BenutzerTestDataProvider.admin(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build())
				.build());

		assertThat(allZuweisbareForBenutzer).containsExactly(new VerwaltungseinheitDbView(gebietskoerperschaft));
	}

	@Test
	public void getAllZuweisbareForBenutzer_koordinator_untergeordneteGebietskoerperschaften() {
		Gebietskoerperschaft gebietskoerperschaft = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.build();
		when(repository.findAllUntergeordnet(any())).thenReturn(
			List.of(gebietskoerperschaft,
				VerwaltungseinheitTestDataProvider.defaultOrganisation().build()));

		Benutzer benutzer = BenutzerTestDataProvider
			.kreiskoordinator(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build()).build();
		List<VerwaltungseinheitDbView> allZuweisbareForBenutzer = service.getAllZuweisbareOrganisationenForBenutzer(
			benutzer);

		verify(repository).findAllUntergeordnet(eq(benutzer.getOrganisation()));
		assertThat(allZuweisbareForBenutzer).containsExactly(new VerwaltungseinheitDbView(gebietskoerperschaft));
	}

	@Test
	public void getAllZuweisbareForBenutzer_bearbeiterGebietskoerperschaft_ownOrg() {
		Gebietskoerperschaft gebietskoerperschaft = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.build();

		Benutzer benutzer = BenutzerTestDataProvider
			.radwegeErfasserinKommuneKreis(gebietskoerperschaft).build();
		List<VerwaltungseinheitDbView> allZuweisbareForBenutzer = service.getAllZuweisbareOrganisationenForBenutzer(
			benutzer);

		assertThat(allZuweisbareForBenutzer).containsExactly(new VerwaltungseinheitDbView(gebietskoerperschaft));
	}

	@Test
	public void getAllZuweisbareForBenutzer_bearbeiterOrg_noOrg() {
		Benutzer benutzer = BenutzerTestDataProvider
			.radwegeErfasserinKommuneKreis(VerwaltungseinheitTestDataProvider.defaultOrganisation().build()).build();
		List<VerwaltungseinheitDbView> allZuweisbareForBenutzer = service.getAllZuweisbareOrganisationenForBenutzer(
			benutzer);

		assertThat(allZuweisbareForBenutzer).isEmpty();
	}

	@Test
	public void getAllZuweisbareForBenutzer_externerBearbeiter_noOrg() {
		Benutzer benutzer = BenutzerTestDataProvider
			.externerDienstleister(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build()).build();
		List<VerwaltungseinheitDbView> allZuweisbareForBenutzer = service.getAllZuweisbareOrganisationenForBenutzer(
			benutzer);

		assertThat(allZuweisbareForBenutzer).isEmpty();
	}
}
