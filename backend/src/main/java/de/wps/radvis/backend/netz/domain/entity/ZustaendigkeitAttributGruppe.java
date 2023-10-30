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
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Audited
@NoArgsConstructor
public class ZustaendigkeitAttributGruppe extends VersionierteEntity {

	@JsonIgnore
	@ElementCollection
	@CollectionTable(name = "zustaendigkeit_attribut_gruppe_zustaendigkeit_attribute")
	Set<ZustaendigkeitAttribute> zustaendigkeitAttribute;

	public ZustaendigkeitAttributGruppe(List<ZustaendigkeitAttribute> zustaendigkeitAttribute) {
		require(zustaendigkeitAttribute, notNullValue());
		require(zustaendigkeitAttribute, Matchers.not(Matchers.empty()));
		require(LinearReferenzierterAbschnitt.segmentsCoverFullLine(zustaendigkeitAttribute.stream()
			.map(LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt)
			.collect(Collectors.toList())));

		this.zustaendigkeitAttribute = new HashSet<>(zustaendigkeitAttribute);
	}

	@Builder(builderMethodName = "privateBuilder")
	private ZustaendigkeitAttributGruppe(Long id, List<ZustaendigkeitAttribute> zustaendigkeitAttribute, Long version) {
		super(id, version);
		this.zustaendigkeitAttribute = new HashSet<>(zustaendigkeitAttribute);
	}

	public static ZustaendigkeitAttributGruppeBuilder builder() {
		return privateBuilder().zustaendigkeitAttribute(List.of(ZustaendigkeitAttribute.builder().build()));
	}

	public List<ZustaendigkeitAttribute> getImmutableZustaendigkeitAttribute() {
		return Collections.unmodifiableList(new ArrayList<>(zustaendigkeitAttribute));
	}

	public Set<ZustaendigkeitAttribute> getImmutableZustaendigkeitAttributeSet() {
		return Collections.unmodifiableSet(zustaendigkeitAttribute);
	}

	public void replaceZustaendigkeitAttribute(List<ZustaendigkeitAttribute> zustaendigkeitAttribute) {
		require(zustaendigkeitAttribute, notNullValue());
		require(zustaendigkeitAttribute, Matchers.not(Matchers.empty()));
		List<LinearReferenzierterAbschnitt> lineareReferenzen = zustaendigkeitAttribute.stream()
			.map(LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt)
			.collect(Collectors.toList());
		require(LinearReferenzierterAbschnitt.segmentsCoverFullLine(lineareReferenzen),
			"Fehlerhafte Referenzen: " + lineareReferenzen);

		this.zustaendigkeitAttribute.clear();
		this.zustaendigkeitAttribute.addAll(zustaendigkeitAttribute);
	}

	public void reset() {
		this.replaceZustaendigkeitAttribute(
			List.of(ZustaendigkeitAttribute.builder().build()));
	}
}
