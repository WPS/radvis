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

package de.wps.radvis.backend.dokument.domain.entity;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import de.wps.radvis.backend.common.domain.entity.AbstractEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import lombok.Getter;

@Entity
public class DokumentListe extends AbstractEntity {

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "dokument_liste_id")
	@Getter
	private List<Dokument> dokumente;

	public DokumentListe() {
		this(new ArrayList<>());
	}

	public DokumentListe(List<Dokument> dokumente) {
		require(dokumente, notNullValue());
		this.dokumente = dokumente;
	}

	public void addDokument(Dokument dokument) {
		require(dokument, notNullValue());
		this.dokumente.add(dokument);
	}

	public void addOrReplaceDokumentWithEqualDateiname(Dokument dokument, boolean isDuplicate) {
		require(dokument, notNullValue());
		if (isDuplicate) {
			Optional<Dokument> existingDokument = this.dokumente.stream()
				.filter(dok -> dok.getDateiname().equals(dokument.getDateiname()))
				.findFirst();
			existingDokument.ifPresent(existing -> this.dokumente.remove(existing));
		}
		this.dokumente.add(dokument);
	}

	public void deleteDokument(long dokumentId) {
		final var dokument = this.dokumente.stream()
			.filter(d -> d.getId() == dokumentId)
			.findFirst()
			.orElseThrow(EntityNotFoundException::new);
		this.dokumente.remove(dokument);
	}
}
