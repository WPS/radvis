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

package de.wps.radvis.backend.organisation.domain.entity;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.util.Optional;

import jakarta.annotation.Nullable;
import jakarta.persistence.Entity;

import org.locationtech.jts.geom.MultiPolygon;

import de.wps.radvis.backend.organisation.domain.valueObject.OrganisationsArt;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Gebietskoerperschaft extends Verwaltungseinheit {

	@Nullable
	private MultiPolygon bereich;

	@Deprecated
	@Getter
	private Boolean istQualitaetsgesichert;

	@Getter
	private Integer fachId;

	public Gebietskoerperschaft(Integer fachId, String name, Verwaltungseinheit uebergeordneteOrganisation,
		OrganisationsArt organisationsArt, MultiPolygon bereich, boolean aktiv) {
		this(null, fachId, name, uebergeordneteOrganisation, organisationsArt, bereich, Boolean.FALSE, aktiv);
	}

	@Builder
	private Gebietskoerperschaft(Long id, Integer fachId, String name, Verwaltungseinheit uebergeordneteOrganisation,
		OrganisationsArt organisationsArt, MultiPolygon bereich, boolean istQualitaetsgesichert, boolean aktiv) {
		super(id, name, uebergeordneteOrganisation, organisationsArt, aktiv);
		require(organisationsArt.istGebietskoerperschaft());
		require(bereich, notNullValue());
		this.bereich = bereich;
		this.istQualitaetsgesichert = istQualitaetsgesichert;
		this.fachId = fachId;
	}

	public void markOrganisationAsQualitaetsgesichert() {
		require(OrganisationsArt.KREIS.equals(organisationsArt), "OrganisationsArt ist nicht Kreis.");
		this.istQualitaetsgesichert = true;
	}

	public Optional<MultiPolygon> getBereich() {
		return Optional.ofNullable(bereich);
	}

}
