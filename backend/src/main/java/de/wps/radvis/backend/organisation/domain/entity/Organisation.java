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
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.organisation.domain.valueObject.OrganisationsArt;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Organisation extends Verwaltungseinheit {
	@ManyToMany
	@Getter
	private Set<Gebietskoerperschaft> zustaendigFuerBereichOf;

	public Organisation(String name, Verwaltungseinheit uebergeordneteOrganisation,
		OrganisationsArt organisationsArt, Set<Gebietskoerperschaft> zustaendigFuerBereichOf, boolean aktiv) {
		this(null, name, uebergeordneteOrganisation, organisationsArt, zustaendigFuerBereichOf, aktiv);
	}

	@Builder
	private Organisation(Long id, String name, Verwaltungseinheit uebergeordneteOrganisation,
		OrganisationsArt organisationsArt, Set<Gebietskoerperschaft> zustaendigFuerBereichOf, boolean aktiv) {
		super(id, name, uebergeordneteOrganisation, organisationsArt, aktiv);
		require(!organisationsArt.istGebietskoerperschaft());
		require(zustaendigFuerBereichOf, notNullValue());

		this.zustaendigFuerBereichOf = zustaendigFuerBereichOf;
	}

	@Override
	public Optional<MultiPolygon> getBereich() {
		return zustaendigFuerBereichOf.stream()
			.map(Verwaltungseinheit::getBereich)
			.reduce((a, b) -> {
				if (a.isPresent()) {
					if (b.isPresent()) {
						Geometry union = a.get().union(b.get());
						if (Geometry.TYPENAME_POLYGON.equals(union.getGeometryType())) {
							return Optional.of(KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
								.createMultiPolygon(new Polygon[] { (Polygon) union }));
						}
						return Optional.of((MultiPolygon) union);
					}
					return Optional.empty();
				}
				return b;
			}).orElse(Optional.empty());
	}

	public void deaktiviere() {
		this.aktiv = false;
	}

	public void aktiviere() {
		this.aktiv = true;
	}

	public void update(String name, OrganisationsArt organisationsArt,
		Set<Gebietskoerperschaft> zustaendigFuerBereichOf) {
		require(organisationsArt, notNullValue());
		require(!organisationsArt.istGebietskoerperschaft());
		require(name, notNullValue());
		require(zustaendigFuerBereichOf, notNullValue());
		this.name = name;
		this.organisationsArt = organisationsArt;
		this.zustaendigFuerBereichOf.clear();
		this.zustaendigFuerBereichOf.addAll(zustaendigFuerBereichOf);
	}
}
