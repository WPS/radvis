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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;
import org.springframework.data.util.Pair;

import de.wps.radvis.backend.common.domain.entity.VersionierteEntity;
import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Inheritance;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "organisation")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Inheritance
public abstract class Verwaltungseinheit extends VersionierteEntity {

	@Getter
	protected String name;

	@ManyToOne(fetch = FetchType.LAZY)
	protected Verwaltungseinheit uebergeordneteOrganisation;

	@Getter
	@Enumerated(EnumType.STRING)
	protected OrganisationsArt organisationsArt;

	@Transient
	protected PreparedGeometry bereichBuffer;

	@Getter
	protected Boolean aktiv;

	protected Verwaltungseinheit(Long id, String name,
		Verwaltungseinheit uebergeordneteOrganisation, OrganisationsArt organisationsArt, boolean aktiv) {
		super(id, null);
		require(organisationsArt, notNullValue());
		require(name, notNullValue());
		this.name = name;
		this.uebergeordneteOrganisation = uebergeordneteOrganisation;
		this.organisationsArt = organisationsArt;
		this.aktiv = aktiv;
	}

	public Optional<Verwaltungseinheit> getUebergeordneteVerwaltungseinheit() {
		return Optional.ofNullable(uebergeordneteOrganisation);
	}

	public PreparedGeometry getBereichBuffer(int bufferInMeter) {
		if (bereichBuffer == null && getBereich().isPresent()) {
			bereichBuffer = PreparedGeometryFactory.prepare(getBereich().get().buffer(bufferInMeter));
		}
		return bereichBuffer;
	}

	public Geometry getBereichBufferSimplified(int bufferInMeter, int toleranceInMeter) {
		Geometry simplifiedGeometry = TopologyPreservingSimplifier
			.simplify(getBereichBuffer(bufferInMeter).getGeometry(), toleranceInMeter);
		return simplifiedGeometry;
	}

	public abstract Optional<MultiPolygon> getBereich();

	@Override
	public String toString() {
		return String.format("Verwaltungseinheit{id=%d, name='%s', organisationsArt=%s, aktiv=%b}", id, name,
			organisationsArt, aktiv);
	}

	public static String combineNameAndArt(String name, OrganisationsArt art) {
		return name == null ? "" : String.format("%s (%s)", name, art);
	}

	public static Pair<String, OrganisationsArt> parseBezeichnung(String bezeichnung) {
		Matcher matcher = getMatcher(bezeichnung);
		// find ist wichtig, um den matching Prozess anzustoßen. Wir haben das nicht in einem if-Statement, da wir bei
		// einer Exception einfach abbrechen wollen
		matcher.find();
		return Pair.of(matcher.group(1), OrganisationsArt.fromString(matcher.group(2)));
	}

	public static Optional<Pair<String, OrganisationsArt>> parseBezeichnungWithOrgaArtAllCaps(String bezeichnung) {
		Matcher matcher = getMatcher(bezeichnung);
		// find ist wichtig, um den matching Prozess anzustoßen.
		if (!matcher.find()) {
			return Optional.empty();
		}
		try {
			OrganisationsArt oa = OrganisationsArt.valueOf(matcher.group(2));
			return Optional.of(Pair.of(matcher.group(1), oa));
		} catch (IllegalArgumentException e) {
			return Optional.empty();
		}
	}

	private static Matcher getMatcher(String bezeichnung) {
		Pattern pattern = Pattern.compile("(.*) \\((.*?)\\)$");
		Matcher matcher = pattern.matcher(bezeichnung);
		return matcher;
	}

	public String getDisplayText() {
		return combineNameAndArt(name, organisationsArt);
	}
}
