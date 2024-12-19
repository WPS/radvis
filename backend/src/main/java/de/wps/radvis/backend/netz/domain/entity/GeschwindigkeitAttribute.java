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

package de.wps.radvis.backend.netz.domain.entity;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.util.Objects;
import java.util.Optional;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.netz.domain.valueObject.Hoechstgeschwindigkeit;
import de.wps.radvis.backend.netz.domain.valueObject.KantenOrtslage;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class GeschwindigkeitAttribute extends LinearReferenzierteAttribute {

	@Setter
	@Enumerated(EnumType.STRING)
	private KantenOrtslage ortslage;

	@Enumerated(EnumType.STRING)
	@Getter
	private Hoechstgeschwindigkeit hoechstgeschwindigkeit;

	@Enumerated(EnumType.STRING)
	private Hoechstgeschwindigkeit abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung;

	@Builder(builderMethodName = "privateBuilder", toBuilder = true)
	public GeschwindigkeitAttribute(
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt,
		KantenOrtslage ortslage,
		Hoechstgeschwindigkeit hoechstgeschwindigkeit,
		Hoechstgeschwindigkeit abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung) {
		super(linearReferenzierterAbschnitt);
		require(hoechstgeschwindigkeit, notNullValue());
		this.ortslage = ortslage;
		this.hoechstgeschwindigkeit = hoechstgeschwindigkeit;
		this.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung = abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung;
	}

	public Optional<KantenOrtslage> getOrtslage() {
		return Optional.ofNullable(this.ortslage);
	}

	public static GeschwindigkeitAttributeBuilder builder() {
		return privateBuilder()
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 1))
			.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.UNBEKANNT);
	}

	public Optional<Hoechstgeschwindigkeit> getAbweichendeHoechstgeschwindigkeitGegenStationierungsrichtung() {
		return Optional.ofNullable(abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung);
	}

	public GeschwindigkeitAttribute withUmgekehrterStationierungsrichtung() {
		if (getAbweichendeHoechstgeschwindigkeitGegenStationierungsrichtung().isEmpty()) {
			return new GeschwindigkeitAttribute(linearReferenzierterAbschnitt, ortslage, hoechstgeschwindigkeit, null);
		} else {
			return new GeschwindigkeitAttribute(linearReferenzierterAbschnitt, ortslage,
				abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung,
				hoechstgeschwindigkeit);
		}
	}

	@Override
	public GeschwindigkeitAttribute withLinearReferenzierterAbschnitt(
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt) {
		return new GeschwindigkeitAttribute(linearReferenzierterAbschnitt, ortslage, hoechstgeschwindigkeit,
			abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung);
	}

	@Override
	public GeschwindigkeitAttribute withDefaultValuesAndLineareReferenz(
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt) {
		return GeschwindigkeitAttribute.builder().linearReferenzierterAbschnitt(linearReferenzierterAbschnitt).build();
	}

	@Override
	public GeschwindigkeitAttribute copyWithSameValues() {
		return new GeschwindigkeitAttribute(linearReferenzierterAbschnitt, ortslage, hoechstgeschwindigkeit,
			abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung);
	}

	@Override
	public boolean sindAttributeGleich(LinearReferenzierteAttribute other) {
		if (!(other instanceof GeschwindigkeitAttribute)) {
			return false;
		} else {
			return sindAttributeGleich((GeschwindigkeitAttribute) other);
		}
	}

	@Override
	public boolean widersprechenSichAttribute(LinearReferenzierteAttribute other) {
		if (!(other instanceof GeschwindigkeitAttribute)) {
			return true;
		} else {
			return widersprechenSichAttribute((GeschwindigkeitAttribute) other);
		}
	}

	@Override
	protected Optional<? extends LinearReferenzierteAttribute> union(LinearReferenzierteAttribute other) {
		if (!(other instanceof GeschwindigkeitAttribute)) {
			return Optional.empty();
		}

		Optional<LinearReferenzierterAbschnitt> union = linearReferenzierterAbschnitt.union(
			((GeschwindigkeitAttribute) other).linearReferenzierterAbschnitt);

		if (union.isEmpty()) {
			return Optional.empty();
		}

		if (!sindAttributeGleich((GeschwindigkeitAttribute) other)) {
			return Optional.empty();
		}

		return Optional
			.of(new GeschwindigkeitAttribute(union.get(), ortslage, hoechstgeschwindigkeit,
				abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung));
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends LinearReferenzierteAttribute> T mergeAttributeNimmErstenNichtDefaultWert(T other,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt) {
		if (other instanceof GeschwindigkeitAttribute) {
			return (T) mergeAttributeNimmErstenNichtDefaultWert((GeschwindigkeitAttribute) other,
				linearReferenzierterAbschnitt);
		} else {
			throw new RuntimeException("Es lassen sich nur zwei Attribute der gleichen Klasse mergen");
		}
	}

	private GeschwindigkeitAttribute mergeAttributeNimmErstenNichtDefaultWert(GeschwindigkeitAttribute other,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt) {
		return GeschwindigkeitAttribute.builder()
			.linearReferenzierterAbschnitt(linearReferenzierterAbschnitt)
			.ortslage(
				this.getOrtslage().orElse(other.getOrtslage().orElse(null)))
			.hoechstgeschwindigkeit(this.getHoechstgeschwindigkeit())
			.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung(
				this.getAbweichendeHoechstgeschwindigkeitGegenStationierungsrichtung()
					.orElse(other.getAbweichendeHoechstgeschwindigkeitGegenStationierungsrichtung().orElse(null)))
			.build();
	}

	public boolean sindAttributeGleich(GeschwindigkeitAttribute other) {
		return Objects.equals(ortslage, other.ortslage)
			&& Objects.equals(hoechstgeschwindigkeit, other.hoechstgeschwindigkeit)
			&& Objects.equals(abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung,
				other.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung);
	}

	public boolean widersprechenSichAttribute(GeschwindigkeitAttribute other) {
		return (!Objects.equals(ortslage, other.ortslage)
			&& ortslage != null && other.ortslage != null)
			|| (!Objects.equals(hoechstgeschwindigkeit, other.hoechstgeschwindigkeit)
				&& hoechstgeschwindigkeit != null && other.hoechstgeschwindigkeit != null)
			|| (!Objects.equals(abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung,
				other.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung)
				&& abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung != null
				&& other.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung != null);
	}

	@Override
	protected boolean hasOnlyDefaultAttribute() {
		return sindAttributeGleich(GeschwindigkeitAttribute.builder().build());
	}
}
