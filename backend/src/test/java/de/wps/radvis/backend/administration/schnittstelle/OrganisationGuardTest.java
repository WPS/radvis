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

package de.wps.radvis.backend.administration.schnittstelle;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import de.wps.radvis.backend.administration.domain.AdministrationService;
import de.wps.radvis.backend.administration.schnittstelle.command.SaveOrganisationCommand;
import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.BenutzerService;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.benutzer.domain.repository.BenutzerRepository;
import de.wps.radvis.backend.common.domain.MailService;
import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.organisation.domain.GebietskoerperschaftRepository;
import de.wps.radvis.backend.organisation.domain.OrganisationRepository;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitRepository;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import de.wps.radvis.backend.organisation.domain.dbView.VerwaltungseinheitDbView;
import de.wps.radvis.backend.organisation.domain.entity.Gebietskoerperschaft;
import de.wps.radvis.backend.organisation.domain.entity.Organisation;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import de.wps.radvis.backend.organisation.domain.valueObject.Mailadresse;
import jakarta.persistence.EntityNotFoundException;

class OrganisationGuardTest {

	private OrganisationGuard organisationGuard;

	@Mock
	private BenutzerResolver benutzerResolver;
	@Mock
	private Authentication authentication;
	@Mock
	private BenutzerRepository benutzerRepository;
	@Mock
	private VerwaltungseinheitRepository verwaltungseinheitRepository;
	@Mock
	private VerwaltungseinheitService verwaltungseinheitService;
	@Mock
	private AdministrationService administrationService;
	@Mock
	private GebietskoerperschaftRepository gebietskoerperschaftRepository;
	@Mock
	private OrganisationRepository organisationRepository;
	@Mock
	private MailService mailService;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		organisationGuard = new OrganisationGuard(benutzerResolver,
			new BenutzerService(benutzerRepository, new VerwaltungseinheitService(verwaltungseinheitRepository,
				gebietskoerperschaftRepository, organisationRepository,
				OrganisationsArt.BUNDESLAND, "Baden-Württemberg", new HashMap<Integer, Mailadresse>()),
				"technischerBenutzerServiceBwId", "basisUrl", mailService),
			administrationService, organisationRepository);
	}

	@Test
	void save_hasRecht() {
		Gebietskoerperschaft currentZugeordneteOrganisation = VerwaltungseinheitTestDataProvider
			.defaultGebietskoerperschaft()
			.id(26354L).build();
		Organisation currentOrganisation = VerwaltungseinheitTestDataProvider.defaultOrganisation().id(2L)
			.zustaendigFuerBereichOf(
				Set.of(currentZugeordneteOrganisation))
			.build();
		long newZugeordneteOrganisationId = 34576L;
		SaveOrganisationCommand command = SaveOrganisationCommand.builder()
			.organisationsArt(OrganisationsArt.TOURISMUSVERBAND).name("Mein Test").id(currentOrganisation.getId())
			.zustaendigFuerBereichOf(Set.of(newZugeordneteOrganisationId)).build();
		when(organisationRepository.findById(currentOrganisation.getId())).thenReturn(
			Optional.of(currentOrganisation));
		Benutzer benutzer = BenutzerTestDataProvider
			.admin(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build()).build();
		when(benutzerResolver.fromAuthentication(any())).thenReturn(
			benutzer);
		when(administrationService.getAllZuweisbareOrganisationenForBenutzer(benutzer))
			.thenReturn(List.of(new VerwaltungseinheitDbView(currentZugeordneteOrganisation),
				new VerwaltungseinheitDbView(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
					.id(newZugeordneteOrganisationId).build())));

		assertDoesNotThrow(() -> organisationGuard.save(authentication, command));
	}

	@Test
	void save_hasRecht_uebergeordnet() {
		Gebietskoerperschaft currentZugeordneteOrganisation = VerwaltungseinheitTestDataProvider
			.defaultGebietskoerperschaft()
			.id(26354L).build();
		Verwaltungseinheit uebergeordneteOrganisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.build();
		Organisation currentOrganisation = VerwaltungseinheitTestDataProvider.defaultOrganisation().id(2L)
			.zustaendigFuerBereichOf(
				Set.of(currentZugeordneteOrganisation))
			.uebergeordneteOrganisation(uebergeordneteOrganisation).build();
		long newZugeordneteOrganisationId = 34576L;
		SaveOrganisationCommand command = SaveOrganisationCommand.builder()
			.organisationsArt(OrganisationsArt.TOURISMUSVERBAND).name("Mein Test").id(currentOrganisation.getId())
			.zustaendigFuerBereichOf(Set.of(newZugeordneteOrganisationId)).build();
		when(organisationRepository.findById(currentOrganisation.getId())).thenReturn(
			Optional.of(currentOrganisation));
		Benutzer benutzer = BenutzerTestDataProvider
			.kreiskoordinator(uebergeordneteOrganisation).build();
		when(benutzerResolver.fromAuthentication(any())).thenReturn(
			benutzer);
		when(administrationService.getAllZuweisbareOrganisationenForBenutzer(benutzer))
			.thenReturn(List.of(new VerwaltungseinheitDbView(currentZugeordneteOrganisation),
				new VerwaltungseinheitDbView(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
					.id(newZugeordneteOrganisationId).build())));

		assertDoesNotThrow(() -> organisationGuard.save(authentication, command));
	}

	@Test
	void save_hasRecht_nichtUebergeordnet() {
		Gebietskoerperschaft currentZugeordneteOrganisation = VerwaltungseinheitTestDataProvider
			.defaultGebietskoerperschaft()
			.id(26354L).build();
		Verwaltungseinheit uebergeordneteOrganisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.id(6534L)
			.build();
		Organisation currentOrganisation = VerwaltungseinheitTestDataProvider.defaultOrganisation()
			.zustaendigFuerBereichOf(
				Set.of(currentZugeordneteOrganisation))
			.id(2L).uebergeordneteOrganisation(uebergeordneteOrganisation)
			.build();
		long newZugeordneteOrganisationId = 34576L;
		SaveOrganisationCommand command = SaveOrganisationCommand.builder()
			.organisationsArt(OrganisationsArt.TOURISMUSVERBAND).name("Mein Test").id(currentOrganisation.getId())
			.zustaendigFuerBereichOf(Set.of(newZugeordneteOrganisationId)).build();
		when(organisationRepository.findById(currentOrganisation.getId())).thenReturn(
			Optional.of(currentOrganisation));
		Benutzer benutzer = BenutzerTestDataProvider
			.kreiskoordinator(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(678L).build())
			.build();
		when(benutzerResolver.fromAuthentication(any())).thenReturn(
			benutzer);
		when(administrationService.getAllZuweisbareOrganisationenForBenutzer(benutzer))
			.thenReturn(List.of(new VerwaltungseinheitDbView(currentZugeordneteOrganisation),
				new VerwaltungseinheitDbView(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
					.id(newZugeordneteOrganisationId).build())));

		assertThrows(AccessDeniedException.class, () -> organisationGuard.save(authentication, command));
	}

	@Test
	void save_canZuweisen() {
		Gebietskoerperschaft currentZugeordneteOrganisation = VerwaltungseinheitTestDataProvider
			.defaultGebietskoerperschaft()
			.id(26354L).build();
		Organisation currentOrganisation = VerwaltungseinheitTestDataProvider.defaultOrganisation().id(2L)
			.zustaendigFuerBereichOf(
				Set.of(currentZugeordneteOrganisation))
			.build();
		long newZugeordneteOrganisationId = 34576L;
		SaveOrganisationCommand command = SaveOrganisationCommand.builder()
			.organisationsArt(currentOrganisation.getOrganisationsArt()).name(currentOrganisation.getName())
			.id(currentOrganisation.getId())
			.zustaendigFuerBereichOf(Set.of(newZugeordneteOrganisationId)).build();
		when(organisationRepository.findById(currentOrganisation.getId())).thenReturn(
			Optional.of(currentOrganisation));
		Benutzer benutzer = BenutzerTestDataProvider
			.radnetzErfasserinRegierungsbezirk(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build())
			.build();
		when(benutzerResolver.fromAuthentication(any())).thenReturn(benutzer);
		when(administrationService.getAllZuweisbareOrganisationenForBenutzer(benutzer))
			.thenReturn(List.of(new VerwaltungseinheitDbView(currentZugeordneteOrganisation),
				new VerwaltungseinheitDbView(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
					.id(newZugeordneteOrganisationId).build())));

		assertDoesNotThrow(() -> organisationGuard.save(authentication, command));
	}

	@Test
	void save_falscheOrgArt_throws() {
		Gebietskoerperschaft currentOrganisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.id(2L)
			.build();
		SaveOrganisationCommand command = SaveOrganisationCommand.builder()
			.organisationsArt(OrganisationsArt.TOURISMUSVERBAND).name("Mein Test").id(currentOrganisation.getId())
			.zustaendigFuerBereichOf(Set.of(34576L)).build();
		when(verwaltungseinheitService.findById(currentOrganisation.getId())).thenReturn(
			Optional.of(currentOrganisation));
		when(benutzerResolver.fromAuthentication(any())).thenReturn(
			BenutzerTestDataProvider.admin(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build())
				.build());

		assertThrows(EntityNotFoundException.class, () -> organisationGuard.save(authentication, command));
	}

	@Test
	void save_nameChanged_noRecht_throws() {
		Organisation currentOrganisation = VerwaltungseinheitTestDataProvider.defaultOrganisation().id(3L)
			.organisationsArt(OrganisationsArt.SONSTIGES).zustaendigFuerBereichOf(
				Set.of(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(26354L).build()))
			.build();
		SaveOrganisationCommand command = SaveOrganisationCommand.builder()
			.organisationsArt(OrganisationsArt.EXTERNER_DIENSTLEISTER).name(currentOrganisation.getName())
			.id(currentOrganisation.getId())
			.zustaendigFuerBereichOf(Set.of(34576L)).build();
		when(organisationRepository.findById(currentOrganisation.getId())).thenReturn(
			Optional.of(currentOrganisation));
		when(benutzerResolver.fromAuthentication(any())).thenReturn(
			BenutzerTestDataProvider
				.radnetzErfasserinRegierungsbezirk(
					VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build())
				.build());

		assertThrows(AccessDeniedException.class, () -> organisationGuard.save(authentication, command));
	}

	@Test
	void save_zustaendigkeitAdded_notZuweisbar_throws() {
		Gebietskoerperschaft currentZugeordneteOrganisation = VerwaltungseinheitTestDataProvider
			.defaultGebietskoerperschaft()
			.id(26354L).build();
		Organisation currentOrganisation = VerwaltungseinheitTestDataProvider.defaultOrganisation()
			.zustaendigFuerBereichOf(
				Set.of(currentZugeordneteOrganisation))
			.id(2L)
			.build();
		SaveOrganisationCommand command = SaveOrganisationCommand.builder()
			.organisationsArt(currentOrganisation.getOrganisationsArt()).name(currentOrganisation.getName())
			.id(currentOrganisation.getId())
			.zustaendigFuerBereichOf(Set.of(34576L, currentZugeordneteOrganisation.getId())).build();
		when(organisationRepository.findById(currentOrganisation.getId())).thenReturn(
			Optional.of(currentOrganisation));
		Benutzer benutzer = BenutzerTestDataProvider
			.radnetzErfasserinRegierungsbezirk(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build())
			.build();
		when(benutzerResolver.fromAuthentication(any())).thenReturn(benutzer);
		when(administrationService.getAllZuweisbareOrganisationenForBenutzer(benutzer))
			.thenReturn(List.of(new VerwaltungseinheitDbView(currentZugeordneteOrganisation)));

		assertThrows(AccessDeniedException.class, () -> organisationGuard.save(authentication, command));
	}
}
