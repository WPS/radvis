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

import static org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED;

import java.util.Objects;
import java.util.Optional;

import org.hibernate.envers.Audited;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.netz.domain.valueObject.VereinbarungsKennung;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import jakarta.persistence.Embeddable;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ZustaendigkeitAttribute extends LinearReferenzierteAttribute {
	@ManyToOne
	@Audited(targetAuditMode = NOT_AUDITED)
	private Verwaltungseinheit baulastTraeger;

	@ManyToOne
	@Audited(targetAuditMode = NOT_AUDITED)
	private Verwaltungseinheit unterhaltsZustaendiger;

	@ManyToOne
	@Audited(targetAuditMode = NOT_AUDITED)
	private Verwaltungseinheit erhaltsZustaendiger;

	private VereinbarungsKennung vereinbarungsKennung;

	@Builder(builderMethodName = "privateBuilder", toBuilder = true)
	public ZustaendigkeitAttribute(LinearReferenzierterAbschnitt linearReferenzierterAbschnitt,
		Verwaltungseinheit baulastTraeger,
		Verwaltungseinheit unterhaltsZustaendiger,
		Verwaltungseinheit erhaltsZustaendiger,
		VereinbarungsKennung vereinbarungsKennung) {
		super(linearReferenzierterAbschnitt);
		this.baulastTraeger = baulastTraeger;
		this.unterhaltsZustaendiger = unterhaltsZustaendiger;
		this.erhaltsZustaendiger = erhaltsZustaendiger;
		this.vereinbarungsKennung = vereinbarungsKennung;
	}

	public static ZustaendigkeitAttributeBuilder builder() {
		return privateBuilder()
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 1));
	}

	@Override
	public ZustaendigkeitAttribute withLinearReferenzierterAbschnitt(
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt) {
		return new ZustaendigkeitAttribute(linearReferenzierterAbschnitt, baulastTraeger, unterhaltsZustaendiger,
			erhaltsZustaendiger, vereinbarungsKennung);
	}

	@Override
	public LinearReferenzierteAttribute withDefaultValuesAndLineareReferenz(
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt) {
		return ZustaendigkeitAttribute.builder().linearReferenzierterAbschnitt(linearReferenzierterAbschnitt).build();
	}

	@Override
	public ZustaendigkeitAttribute copyWithSameValues() {
		return new ZustaendigkeitAttribute(linearReferenzierterAbschnitt, baulastTraeger, unterhaltsZustaendiger,
			erhaltsZustaendiger,
			vereinbarungsKennung);
	}

	@Override
	protected Optional<ZustaendigkeitAttribute> union(LinearReferenzierteAttribute other) {
		if (!(other instanceof ZustaendigkeitAttribute)) {
			return Optional.empty();
		}

		Optional<LinearReferenzierterAbschnitt> union = linearReferenzierterAbschnitt.union(
			((ZustaendigkeitAttribute) other).linearReferenzierterAbschnitt);

		if (union.isEmpty()) {
			return Optional.empty();
		}

		if (!sindAttributeGleich((ZustaendigkeitAttribute) other)) {
			return Optional.empty();
		}

		return Optional
			.of(new ZustaendigkeitAttribute(union.get(), baulastTraeger, unterhaltsZustaendiger, erhaltsZustaendiger,
				vereinbarungsKennung));
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends LinearReferenzierteAttribute> T mergeAttributeNimmErstenNichtDefaultWert(T other,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt) {
		if (!(other instanceof ZustaendigkeitAttribute)) {
			throw new RuntimeException("Es lassen sich nur zwei Attribute der gleichen Klasse mergen");
		} else {
			return (T) mergeAttributeNimmErstenNichtDefaultWert((ZustaendigkeitAttribute) other,
				linearReferenzierterAbschnitt);
		}
	}

	private ZustaendigkeitAttribute mergeAttributeNimmErstenNichtDefaultWert(ZustaendigkeitAttribute other,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt) {
		return ZustaendigkeitAttribute.builder()
			.baulastTraeger(
				this.getBaulastTraeger().orElse(other.getBaulastTraeger().orElse(null)))
			.unterhaltsZustaendiger(this.getUnterhaltsZustaendiger()
				.orElse(other.getUnterhaltsZustaendiger().orElse(null)))
			.vereinbarungsKennung(this.getVereinbarungsKennung()
				.orElse(other.getVereinbarungsKennung().orElse(null)))
			.erhaltsZustaendiger(this.getErhaltsZustaendiger()
				.orElse(other.getErhaltsZustaendiger().orElse(null)))
			.linearReferenzierterAbschnitt(linearReferenzierterAbschnitt)
			.build();
	}

	@Override
	public boolean sindAttributeGleich(LinearReferenzierteAttribute other) {
		if (!(other instanceof ZustaendigkeitAttribute)) {
			return false;
		} else {
			return sindAttributeGleich((ZustaendigkeitAttribute) other);
		}
	}

	@Override
	public boolean widersprechenSichAttribute(LinearReferenzierteAttribute other) {
		if (!(other instanceof ZustaendigkeitAttribute)) {
			return true;
		} else {
			return widersprechenSichAttribute((ZustaendigkeitAttribute) other);
		}
	}

	public boolean widersprechenSichAttribute(ZustaendigkeitAttribute other) {
		return (!Objects.equals(baulastTraeger, other.baulastTraeger)
			&& baulastTraeger != null && other.baulastTraeger != null)
			|| (!Objects.equals(unterhaltsZustaendiger, other.unterhaltsZustaendiger)
				&& unterhaltsZustaendiger != null && other.unterhaltsZustaendiger != null)
			|| (!Objects.equals(erhaltsZustaendiger, other.erhaltsZustaendiger)
				&& erhaltsZustaendiger != null && other.erhaltsZustaendiger != null)
			|| (!Objects.equals(vereinbarungsKennung, other.vereinbarungsKennung)
				&& vereinbarungsKennung != null && other.vereinbarungsKennung != null);
	}

	public boolean sindAttributeGleich(ZustaendigkeitAttribute other) {
		return Objects.equals(baulastTraeger, other.baulastTraeger)
			&& Objects.equals(unterhaltsZustaendiger, other.unterhaltsZustaendiger)
			&& Objects.equals(erhaltsZustaendiger, other.erhaltsZustaendiger)
			&& Objects.equals(vereinbarungsKennung, other.vereinbarungsKennung);
	}

	public Optional<VereinbarungsKennung> getVereinbarungsKennung() {
		return Optional.ofNullable(vereinbarungsKennung);
	}

	public Optional<Verwaltungseinheit> getUnterhaltsZustaendiger() {
		return Optional.ofNullable(unterhaltsZustaendiger);
	}

	public Optional<Verwaltungseinheit> getErhaltsZustaendiger() {
		return Optional.ofNullable(erhaltsZustaendiger);
	}

	public Optional<Verwaltungseinheit> getBaulastTraeger() {
		return Optional.ofNullable(baulastTraeger);
	}

	@Override
	protected boolean hasOnlyDefaultAttribute() {
		return sindAttributeGleich(ZustaendigkeitAttribute.builder().build());
	}
}
