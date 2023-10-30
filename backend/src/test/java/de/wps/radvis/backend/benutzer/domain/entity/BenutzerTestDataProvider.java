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

package de.wps.radvis.backend.benutzer.domain.entity;

import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer.BenutzerBuilder;
import de.wps.radvis.backend.benutzer.domain.valueObject.BenutzerStatus;
import de.wps.radvis.backend.benutzer.domain.valueObject.Mailadresse;
import de.wps.radvis.backend.benutzer.domain.valueObject.Name;
import de.wps.radvis.backend.benutzer.domain.valueObject.Rolle;
import de.wps.radvis.backend.benutzer.domain.valueObject.ServiceBwId;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;

public class BenutzerTestDataProvider {

	public static Comparator<BenutzerDBListView> benutzerDBListViewComparator = (a, b) ->
		a.getVorname().equals(b.getVorname()) &&
			a.getNachname().equals(b.getNachname()) &&
			a.getStatus().equals(b.getStatus()) &&
			a.getOrganisation().equals(b.getOrganisation()) &&
			a.getEmail().equals(b.getEmail()) &&
			a.getId().equals(b.getId()) &&
			a.getRollen().equals(b.getRollen()) ? 0 : 1;

	public static BenutzerBuilder defaultBenutzer() {
		return radwegeErfasserinKommuneKreis(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build());
	}

	public static BenutzerBuilder radnetzErfasserinRegierungsbezirk(
		Verwaltungseinheit organisation) {
		return Benutzer.builder()
			.vorname(Name.of("defaultVorname"))
			.nachname(Name.of("defaultNachname"))
			.status(BenutzerStatus.AKTIV)
			.mailadresse(Mailadresse.of("default@mail.com"))
			.organisation(organisation)
			.rollen(Set.of(Rolle.RADVERKEHRSBEAUFTRAGTER))
			.serviceBwId(ServiceBwId.of("default"));
	}

	public static BenutzerBuilder radnetzErfasserinRegierungsbezirkInaktiv(
		Verwaltungseinheit organisation) {
		return radnetzErfasserinRegierungsbezirk(organisation).status(BenutzerStatus.INAKTIV);
	}

	public static BenutzerBuilder admin(Verwaltungseinheit organisation) {
		return Benutzer.builder()
			.vorname(Name.of("adminVorname"))
			.nachname(Name.of("adminNachname"))
			.status(BenutzerStatus.AKTIV)
			.mailadresse(Mailadresse.of("admin@mail.com"))
			.organisation(organisation)
			.rollen(Set.of(Rolle.RADVIS_ADMINISTRATOR))
			.serviceBwId(ServiceBwId.of("admin"));
	}

	public static BenutzerBuilder adminInaktiv(Verwaltungseinheit organisation) {
		return admin(organisation).status(BenutzerStatus.INAKTIV);
	}

	public static BenutzerBuilder kreiskoordinator(Verwaltungseinheit organisation) {
		return Benutzer.builder()
			.vorname(Name.of("kreisKoordinatorVorname"))
			.nachname(Name.of("kreisKoordinatorNachname"))
			.status(BenutzerStatus.AKTIV)
			.mailadresse(Mailadresse.of("kreisKoordinator@mail.com"))
			.organisation(organisation)
			.rollen(Set.of(Rolle.KREISKOORDINATOREN))
			.serviceBwId(ServiceBwId.of("kreisKoordinator"));
	}

	public static BenutzerBuilder bearbeiterinVmRadnetzAdminInaktiv(
		Verwaltungseinheit organisation) {
		return Benutzer.builder()
			.vorname(Name.of("bearbeiterinVmRadnetzAdminInaktiv"))
			.nachname(Name.of("bearbeiterinVmRadnetzAdminInaktiv"))
			.status(BenutzerStatus.INAKTIV)
			.mailadresse(Mailadresse.of("bearbeiterinVmRadnetzAdminInaktiv@mail.com"))
			.organisation(organisation)
			.rollen(Set.of(Rolle.BEARBEITERIN_VM_RADNETZ_ADMINISTRATORIN))
			.serviceBwId(ServiceBwId.of("bearbeiterinVmRadnetzAdminInaktiv"));
	}

	public static BenutzerBuilder externerDienstleister(Verwaltungseinheit organisation) {
		return Benutzer.builder().vorname(Name.of("externerDienstleisterAktiv"))
			.nachname(Name.of("externerDienstleisterAktiv"))
			.status(BenutzerStatus.AKTIV)
			.mailadresse(Mailadresse.of("externerDienstleisterAktiv@mail.com"))
			.organisation(organisation)
			.serviceBwId(ServiceBwId.of("externerDienstleisterAktiv"))
			.rollen(Set.of(Rolle.EXTERNER_DIENSTLEISTER));
	}

	public static BenutzerBuilder radwegeErfasserinKommuneKreis(Verwaltungseinheit organisation) {
		return Benutzer.builder()
			.vorname(Name.of("radwegeErfasserinKommuneKreis"))
			.nachname(Name.of("radwegeErfasserinKommuneKreis"))
			.status(BenutzerStatus.AKTIV)
			.mailadresse(Mailadresse.of("radwegeErfasserinKommuneKreis@mail.com"))
			.organisation(organisation)
			.rollen(Set.of(Rolle.RADWEGE_ERFASSERIN))
			.serviceBwId(ServiceBwId.of("radwegeErfasserinKommuneKreis"));
	}

	public static BenutzerBuilder radwegeErfasserinKommuneKreisInaktiv(
		Verwaltungseinheit organisation) {
		return radwegeErfasserinKommuneKreis(organisation).status(BenutzerStatus.INAKTIV);
	}

	public static BenutzerBuilder technischerBenutzer() {
		return Benutzer.builder()
			.rollen(Set.of(Rolle.RADVIS_BETRACHTER))
			.vorname(Name.of("RadVIS"))
			.nachname(Name.of("System"))
			.status(BenutzerStatus.INAKTIV)
			.serviceBwId(ServiceBwId.of("serviceBwIdFuerTechnischerBenutzer"))
			.mailadresse(Mailadresse.of("nicht@@vorhanden"))
			.organisation(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build());
	}

	public static BenutzerDBListView getDbListView(Benutzer benutzer) {
		return new BenutzerDBListView(
			benutzer.getId(),
			benutzer.getVorname(),
			benutzer.getNachname(),
			benutzer.getStatus(),
			benutzer.getOrganisation().getName(),
			benutzer.getOrganisation().getOrganisationsArt(),
			benutzer.getMailadresse(),
			benutzer.getRollen().stream().map(Rolle::name).collect(Collectors.joining(","))
		);
	}
}
