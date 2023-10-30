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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;

import org.hamcrest.Matchers;
import org.hibernate.envers.Audited;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.wps.radvis.backend.common.domain.entity.VersionierteEntity;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.netz.domain.valueObject.Hoechstgeschwindigkeit;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Audited
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@ToString(callSuper = true)
public class GeschwindigkeitAttributGruppe extends VersionierteEntity {

	@JsonIgnore
	@ElementCollection
	@CollectionTable(name = "geschwindigkeit_attribut_gruppe_geschwindigkeit_attribute")
	private Set<GeschwindigkeitAttribute> geschwindigkeitAttribute;

	@Builder(builderMethodName = "privateBuilder")
	private GeschwindigkeitAttributGruppe(Long id, Long version,
		List<GeschwindigkeitAttribute> geschwindigkeitAttribute) {
		super(id, version);
		this.geschwindigkeitAttribute = new HashSet<>(geschwindigkeitAttribute);
	}

	public static GeschwindigkeitAttributGruppeBuilder builder() {
		return privateBuilder().geschwindigkeitAttribute(List.of(GeschwindigkeitAttribute.builder().build()));
	}

	public List<GeschwindigkeitAttribute> getImmutableGeschwindigkeitAttribute() {
		return Collections.unmodifiableList(new ArrayList<>(geschwindigkeitAttribute));
	}

	public void replaceGeschwindigkeitAttribute(List<GeschwindigkeitAttribute> geschwindigkeitAttribute) {
		require(geschwindigkeitAttribute, notNullValue());
		require(geschwindigkeitAttribute, Matchers.not(Matchers.empty()));
		List<LinearReferenzierterAbschnitt> lineareReferenzen = geschwindigkeitAttribute.stream()
			.map(LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt)
			.collect(Collectors.toList());
		require(LinearReferenzierterAbschnitt.segmentsCoverFullLine(lineareReferenzen),
			"Fehlerhafte Referenzen: " + lineareReferenzen);

		this.geschwindigkeitAttribute.clear();
		this.geschwindigkeitAttribute.addAll(geschwindigkeitAttribute);
	}

	public void reset() {
		replaceGeschwindigkeitAttribute(
			List.of(GeschwindigkeitAttribute.builder()
				.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.UNBEKANNT)
				.build())
		);
	}
}
