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

package de.wps.radvis.backend.kommentar.schnittstelle.view;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.kommentar.domain.entity.Kommentar;
import de.wps.radvis.backend.kommentar.domain.entity.KommentarListe;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class KommentarView {
	@NotNull
	private final String kommentarText;

	@NotNull
	private final String benutzer;

	@NotNull
	private final LocalDateTime datum;

	@NotNull
	private final boolean fromLoggedInUser;

	public KommentarView(Kommentar kommentar, boolean fromLoggedInUser) {
		this.kommentarText = kommentar.getKommentarText();
		this.benutzer = kommentar.getBenutzer().getVollerName();
		this.datum = kommentar.getDatum();
		this.fromLoggedInUser = fromLoggedInUser;
	}

	public static List<KommentarView> convertAll(KommentarListe kommentarListe, Benutzer benutzer) {
		return kommentarListe.getKommentare()
			.stream()
			.map(kommentar -> new KommentarView(kommentar, benutzer.equals(kommentar.getBenutzer())))
			.collect(Collectors.toList());
	}
}
