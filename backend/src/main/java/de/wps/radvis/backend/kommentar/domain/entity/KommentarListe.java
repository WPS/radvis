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

package de.wps.radvis.backend.kommentar.domain.entity;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.util.ArrayList;
import java.util.List;

import de.wps.radvis.backend.common.domain.entity.AbstractEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import lombok.Getter;

@Entity
public class KommentarListe extends AbstractEntity {

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "kommentar_liste_id")
	@Getter
	private List<Kommentar> kommentare;

	public KommentarListe() {
		this(new ArrayList<>());
	}

	public KommentarListe(List<Kommentar> kommentare) {
		require(kommentare, notNullValue());
		this.kommentare = kommentare;
	}

	public void addKommentar(Kommentar kommentar) {
		require(kommentar, notNullValue());
		this.kommentare.add(kommentar);
	}
}
