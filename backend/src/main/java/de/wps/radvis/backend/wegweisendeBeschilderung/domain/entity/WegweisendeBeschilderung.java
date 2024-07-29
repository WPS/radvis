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

package de.wps.radvis.backend.wegweisendeBeschilderung.domain.entity;

import static org.hamcrest.Matchers.notNullValue;
import static org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED;
import static org.valid4j.Assertive.require;

import java.util.Objects;

import org.hibernate.envers.Audited;
import org.locationtech.jts.geom.Geometry;

import de.wps.radvis.backend.common.domain.entity.VersionierteEntity;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.valueObject.Defizit;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.valueObject.Gemeinde;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.valueObject.Kreis;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.valueObject.Land;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.valueObject.PfostenNr;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.valueObject.PfostenTyp;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.valueObject.Pfostendefizit;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.valueObject.Pfostenzustand;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.valueObject.WegweiserTyp;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.valueObject.Zustandsbewertung;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Audited
@Entity
@Getter
@NoArgsConstructor
public class WegweisendeBeschilderung extends VersionierteEntity {
	private PfostenNr pfostenNr;
	private Geometry geometrie;
	private WegweiserTyp wegweiserTyp;
	private PfostenTyp pfostenTyp;
	private Zustandsbewertung zustandsbewertung;
	private Defizit defizit;
	private Pfostenzustand pfostenzustand;
	private Pfostendefizit pfostendefizit;
	private Gemeinde gemeinde;
	private Kreis kreis;
	private Land land;

	@ManyToOne
	@Audited(targetAuditMode = NOT_AUDITED)
	@NonNull
	private Verwaltungseinheit zustaendigeVerwaltungseinheit;

	public WegweisendeBeschilderung(PfostenNr pfostenNr, Geometry geometrie, WegweiserTyp wegweiserTyp,
		PfostenTyp pfostenTyp, Zustandsbewertung zustandsbewertung, Defizit defizit, Pfostenzustand pfostenzustand,
		Pfostendefizit pfostendefizit, Gemeinde gemeinde, Kreis kreis, Land land,
		Verwaltungseinheit zustaendigeVerwaltungseinheit) {
		this(null, null, pfostenNr, geometrie, wegweiserTyp, pfostenTyp, zustandsbewertung, defizit, pfostenzustand,
			pfostendefizit, gemeinde, kreis, land, zustaendigeVerwaltungseinheit);
	}

	/**
	 * Wird nur in Tests verwendet
	 */
	@Builder
	private WegweisendeBeschilderung(Long id, Long version, PfostenNr pfostenNr, Geometry geometrie,
		WegweiserTyp wegweiserTyp, PfostenTyp pfostenTyp, Zustandsbewertung zustandsbewertung, Defizit defizit,
		Pfostenzustand pfostenzustand, Pfostendefizit pfostendefizit, Gemeinde gemeinde, Kreis kreis, Land land,
		Verwaltungseinheit zustaendigeVerwaltungseinheit) {
		super(id, version);
		require(pfostenNr, notNullValue());
		require(geometrie, notNullValue());
		require(zustaendigeVerwaltungseinheit, notNullValue());
		this.pfostenNr = pfostenNr;
		this.geometrie = geometrie;
		this.wegweiserTyp = wegweiserTyp;
		this.pfostenTyp = pfostenTyp;
		this.zustandsbewertung = zustandsbewertung;
		this.defizit = defizit;
		this.pfostenzustand = pfostenzustand;
		this.pfostendefizit = pfostendefizit;
		this.gemeinde = gemeinde;
		this.kreis = kreis;
		this.land = land;
		this.zustaendigeVerwaltungseinheit = zustaendigeVerwaltungseinheit;
	}

	/**
	 * Aktualisiert die wesentlichen Attribute der Beschilderung. Sollte also nur aufgerufen werden, wenn {@link
	 * #isDifferentTo(WegweisendeBeschilderung)} entsprechend {@code true} zurück gibt.
	 *
	 * @return Die Beschilderung auf der diese Methode aufgerufen wurde (sprich {@code this}) und deren Attribute
	 *     aktualisiert wurden.
	 */
	public WegweisendeBeschilderung update(WegweisendeBeschilderung ersatz) {
		this.geometrie = ersatz.geometrie;
		this.wegweiserTyp = ersatz.wegweiserTyp;
		this.pfostenTyp = ersatz.pfostenTyp;
		this.zustandsbewertung = ersatz.zustandsbewertung;
		this.defizit = ersatz.defizit;
		this.pfostenzustand = ersatz.pfostenzustand;
		this.pfostendefizit = ersatz.pfostendefizit;
		this.gemeinde = ersatz.gemeinde;
		this.kreis = ersatz.kreis;
		this.land = ersatz.land;
		return this;
	}

	/**
	 * Überprüft, ob die übergebene wegweisende Beschilderung sich von dieser unterscheidet und zwar in einer Art und
	 * Weise, dass diese Beschilderung durch die übergebene aktualisiert werden sollte (s. {@link
	 * #update(WegweisendeBeschilderung)}).
	 * <p>
	 * Es wird also <b>nicht</b> verglichen: Id, Version, Verwaltungseinheit und Pfosten-Nr.
	 */
	public boolean isDifferentTo(WegweisendeBeschilderung other) {
		// Bewusst keine Überprüfung der Entity-Id, da wir Pfosten und keine Entities miteinander vergleichen wollen.
		require(Objects.equals(this.getPfostenNr(), other.getPfostenNr()));

		if (other == this) {
			return false;
		}

		if (!Objects.equals(this.getGeometrie(), other.getGeometrie())) {
			return true;
		}
		if (!Objects.equals(this.getWegweiserTyp(), other.getWegweiserTyp())) {
			return true;
		}
		if (!Objects.equals(this.getPfostenTyp(), other.getPfostenTyp())) {
			return true;
		}
		if (!Objects.equals(this.getZustandsbewertung(), other.getZustandsbewertung())) {
			return true;
		}
		if (!Objects.equals(this.getDefizit(), other.getDefizit())) {
			return true;
		}
		if (!Objects.equals(this.getPfostenzustand(), other.getPfostenzustand())) {
			return true;
		}
		if (!Objects.equals(this.getPfostendefizit(), other.getPfostendefizit())) {
			return true;
		}
		if (!Objects.equals(this.getGemeinde(), other.getGemeinde())) {
			return true;
		}
		if (!Objects.equals(this.getKreis(), other.getKreis())) {
			return true;
		}
		if (!Objects.equals(this.getLand(), other.getLand())) {
			return true;
		}

		return false;
	}
}
