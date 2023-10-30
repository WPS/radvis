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

package de.wps.radvis.backend.integration.attributAbbildung.domain.entity;

import static org.valid4j.Assertive.require;

import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;

import de.wps.radvis.backend.common.domain.entity.AbstractEntity;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
public class KantenMapping extends AbstractEntity {

	@Getter
	@Setter
	private Long grundnetzKantenId;

	@Getter
	@Enumerated(EnumType.STRING)
	private QuellSystem quellsystem;

	@Getter
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "mapped_kante")
	private List<MappedKante> abgebildeteKanten;

	public KantenMapping(Long grundnetzKantenId, QuellSystem quellsystem, List<MappedKante> abgebildeteKanten) {
		this.grundnetzKantenId = grundnetzKantenId;
		this.quellsystem = quellsystem;
		this.abgebildeteKanten = abgebildeteKanten;
	}

	public KantenMapping merge(KantenMapping other) {
		require(grundnetzKantenId.equals(other.grundnetzKantenId));
		require(quellsystem.equals(other.quellsystem));
		abgebildeteKanten.addAll(other.abgebildeteKanten);
		return this;
	}
}
