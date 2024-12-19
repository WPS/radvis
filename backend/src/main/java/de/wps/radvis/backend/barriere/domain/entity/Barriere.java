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

package de.wps.radvis.backend.barriere.domain.entity;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED;
import static org.valid4j.Assertive.require;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.hibernate.envers.Audited;

import de.wps.radvis.backend.barriere.domain.valueObject.BarriereBegruendung;
import de.wps.radvis.backend.barriere.domain.valueObject.BarrierenForm;
import de.wps.radvis.backend.barriere.domain.valueObject.Markierung;
import de.wps.radvis.backend.barriere.domain.valueObject.Sicherung;
import de.wps.radvis.backend.barriere.domain.valueObject.VerbleibendeDurchfahrtsbreite;
import de.wps.radvis.backend.netz.domain.entity.AbstractEntityWithNetzbezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Audited
@Entity
@ToString
public class Barriere extends AbstractEntityWithNetzbezug {

	@Getter
	@Embedded
	private BarriereNetzBezug netzbezug;

	@Getter
	@ManyToOne
	@Audited(targetAuditMode = NOT_AUDITED)
	private Verwaltungseinheit verantwortlich;

	@Getter
	@Enumerated(EnumType.STRING)
	private BarrierenForm barrierenForm;

	@Enumerated(EnumType.STRING)
	private VerbleibendeDurchfahrtsbreite verbleibendeDurchfahrtsbreite;

	@Enumerated(EnumType.STRING)
	private Sicherung sicherung;

	@Enumerated(EnumType.STRING)
	private Markierung markierung;

	private BarriereBegruendung begruendung;

	@Builder(builderMethodName = "privateBuilder")
	private Barriere(
		Long id,
		Long version,
		BarriereNetzBezug netzbezug,
		Verwaltungseinheit verantwortlicheOrganisation,
		BarrierenForm barrierenForm,
		VerbleibendeDurchfahrtsbreite verbleibendeDurchfahrtsbreite,
		Sicherung sicherung,
		Markierung markierung,
		BarriereBegruendung begruendung) {
		super(id, version);
		require(netzbezug, notNullValue());
		require(verantwortlicheOrganisation, notNullValue());
		require(barrierenForm, notNullValue());

		this.version = version;
		this.netzbezug = netzbezug;
		this.verantwortlich = verantwortlicheOrganisation;
		this.barrierenForm = barrierenForm;
		this.verbleibendeDurchfahrtsbreite = verbleibendeDurchfahrtsbreite;
		this.sicherung = sicherung;
		this.markierung = markierung;
		this.begruendung = begruendung;
	}

	public Barriere(
		BarriereNetzBezug netzbezug,
		Verwaltungseinheit verantwortlicheOrganisation,
		BarrierenForm barrierenForm,
		VerbleibendeDurchfahrtsbreite verbleibendeDurchfahrtsbreite,
		Sicherung sicherung,
		Markierung markierung,
		BarriereBegruendung begruendung) {
		this(null, null, netzbezug, verantwortlicheOrganisation, barrierenForm, verbleibendeDurchfahrtsbreite,
			sicherung, markierung, begruendung);
	}

	public static Barriere.BarriereBuilder builder() {
		return privateBuilder();
	}

	public void update(
		BarriereNetzBezug netzbezug,
		Verwaltungseinheit organisation,
		BarrierenForm barrierenForm,
		VerbleibendeDurchfahrtsbreite verbleibendeDurchfahrtsbreite,
		Sicherung sicherung,
		Markierung markierung,
		BarriereBegruendung begruendung) {
		require(netzbezug, notNullValue());
		require(organisation, notNullValue());
		require(barrierenForm, notNullValue());

		this.netzbezug = netzbezug;
		this.verantwortlich = organisation;
		this.barrierenForm = barrierenForm;
		this.verbleibendeDurchfahrtsbreite = verbleibendeDurchfahrtsbreite;
		this.sicherung = sicherung;
		this.markierung = markierung;
		this.begruendung = begruendung;
	}

	public Optional<VerbleibendeDurchfahrtsbreite> getVerbleibendeDurchfahrtsbreite() {
		return Optional.ofNullable(verbleibendeDurchfahrtsbreite);
	}

	public Optional<Sicherung> getSicherung() {
		return Optional.ofNullable(sicherung);
	}

	public Optional<Markierung> getMarkierung() {
		return Optional.ofNullable(markierung);
	}

	public Optional<BarriereBegruendung> getBegruendung() {
		return Optional.ofNullable(begruendung);
	}

	public void removeKanteFromNetzbezug(Collection<Long> kantenIds) {
		require(kantenIds, notNullValue());
		netzbezug = netzbezug.withoutKanten(new HashSet<>(kantenIds));
	}

	public void removeKnotenFromNetzbezug(Collection<Long> knotenIds) {
		require(knotenIds, notNullValue());
		netzbezug = netzbezug.withoutKnoten(new HashSet<>(knotenIds));
	}

	public void ersetzeKanteInNetzbezug(Kante zuErsetzendeKante, Set<Kante> zuErsetzenDurch,
		double erlaubteAbweichung) {
		require(zuErsetzenDurch, notNullValue());
		require(!zuErsetzenDurch.isEmpty());

		netzbezug = netzbezug.withKanteErsetzt(zuErsetzendeKante, zuErsetzenDurch, erlaubteAbweichung);
	}

	/**
	 * Ersetzt Knoten anhand übergebener Abbildung
	 * 
	 * @param ersatzKnoten:
	 *     ID des zu ersetzenden Knoten -> Ersatz-Knoten
	 */
	public void ersetzeKnotenInNetzbezug(Map<Long, Knoten> ersatzKnoten) {
		netzbezug = netzbezug.withKnotenErsetzt(ersatzKnoten);
	}
}
