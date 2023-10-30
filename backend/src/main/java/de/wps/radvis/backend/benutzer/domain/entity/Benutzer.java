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

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.wps.radvis.backend.benutzer.domain.valueObject.BenutzerStatus;
import de.wps.radvis.backend.benutzer.domain.valueObject.Mailadresse;
import de.wps.radvis.backend.benutzer.domain.valueObject.Name;
import de.wps.radvis.backend.benutzer.domain.valueObject.Recht;
import de.wps.radvis.backend.benutzer.domain.valueObject.Rolle;
import de.wps.radvis.backend.benutzer.domain.valueObject.ServiceBwId;
import de.wps.radvis.backend.common.domain.entity.VersionierteEntity;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Getter
public class Benutzer extends VersionierteEntity {

	@Setter
	private Name vorname;

	@Setter
	private Name nachname;

	@Setter
	@Enumerated(EnumType.STRING)
	private BenutzerStatus status;

	@ManyToOne(fetch = FetchType.EAGER)
	@Setter
	private Verwaltungseinheit organisation;

	@Setter
	private Mailadresse mailadresse;

	private ServiceBwId serviceBwId;

	@Getter
	@Setter
	@JsonIgnore
	@ElementCollection(fetch = FetchType.EAGER)
	@Enumerated(EnumType.STRING)
	private Set<Rolle> rollen;

	public Benutzer(Name vorname, Name nachname, BenutzerStatus status,
		Verwaltungseinheit organisation, Mailadresse mailadresse, ServiceBwId serviceBwId,
		Set<Rolle> rollen) {
		this(null, null, vorname, nachname, status, organisation, mailadresse, serviceBwId, rollen);
	}

	@Builder
	private Benutzer(Long id, Long version, Name vorname, Name nachname, BenutzerStatus status,
		Verwaltungseinheit organisation, Mailadresse mailadresse, ServiceBwId serviceBwId, Set<Rolle> rollen) {
		super(id, version);
		require(vorname, notNullValue());
		require(nachname, notNullValue());
		require(status, notNullValue());
		require(organisation, notNullValue());
		require(mailadresse, notNullValue());
		require(serviceBwId, notNullValue());
		require(rollen, notNullValue());
		require(!rollen.isEmpty(), "Einem Benutzer muss eine Rolle zugewiesen sein.");

		this.vorname = vorname;
		this.nachname = nachname;
		this.status = status;
		this.organisation = organisation;
		this.mailadresse = mailadresse;
		this.serviceBwId = serviceBwId;
		this.rollen = rollen;
	}

	public Set<Recht> getRechte() {
		return rollen.stream().flatMap(rolle -> Arrays.stream(rolle.getRechte())).collect(
			Collectors.toSet());
	}

	public boolean hatRecht(Recht recht) {
		require(recht, notNullValue());

		return getRechte().contains(recht);
	}

	public String getVollerName() {
		return getVorname() + " " + getNachname();
	}
}
