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

package de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity;

import java.io.Serializable;
import java.util.Set;

import org.locationtech.jts.geom.LineString;

import de.wps.radvis.backend.common.domain.entity.AbstractEntity;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

@MappedSuperclass
// das Implementieren von Serializable wird für das hibernate-mapping der netzklassen benötigt,
// siehe Kommentar unten bei netzklassen
public abstract class AbstractKanteLinearReferenzierteAttributeView extends AbstractEntity implements Serializable {

	private static final long serialVersionUID = -6272570516950259068L;

	@Getter
	private LineString geometry;

	@Getter
	@Enumerated(EnumType.STRING)
	private QuellSystem quelle;

	@Getter
	private boolean isGrundnetz;

	@Getter
	@ElementCollection
	@CollectionTable(name = "kanten_attribut_gruppe_netzklassen",
		// Wir setzen hier den referencedColumnName auf eine non-primary-key column. Daher müssen wir
		// a) eine Property hinzufügen, die auf diese Column gemappt wird (kantenAttributgruppeId, siehe unten)
		// b) die enthaltende Klasse Serializable implementieren lassen siehe
		// https://docs.jboss.org/hibernate/stable/annotations/reference/en/html/entity.html#entity-mapping-association
		joinColumns = @JoinColumn(name = "kanten_attribut_gruppe_id", referencedColumnName = "kanten_attributgruppe_id"))
	@Column(name = "netzklasse")
	@Enumerated(EnumType.STRING)
	private Set<Netzklasse> netzklassen;

	// Wird für das hibernate-mapping der netzklassen benötigt, siehe Kommentar oben bei netzklassen
	@Column(name = "kanten_attributgruppe_id")
	private Long kantenAttributgruppeId;

	public LineString getSegment(LinearReferenzierterAbschnitt linearReferenzierterAbschnitt) {
		return linearReferenzierterAbschnitt.toSegment(geometry);
	}
}
