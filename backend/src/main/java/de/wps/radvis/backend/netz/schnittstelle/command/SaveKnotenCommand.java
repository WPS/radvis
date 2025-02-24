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

package de.wps.radvis.backend.netz.schnittstelle.command;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;

import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.valueObject.Bauwerksmangel;
import de.wps.radvis.backend.netz.domain.valueObject.BauwerksmangelArt;
import de.wps.radvis.backend.netz.domain.valueObject.KnotenForm;
import de.wps.radvis.backend.netz.domain.valueObject.Kommentar;
import de.wps.radvis.backend.netz.domain.valueObject.QuerungshilfeDetails;
import de.wps.radvis.backend.netz.domain.valueObject.Zustandsbeschreibung;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class SaveKnotenCommand {
	@NotNull
	private Long id;
	private Kommentar kommentar;
	private Zustandsbeschreibung zustandsbeschreibung;
	private Long gemeinde;
	@NotNull
	private Long knotenVersion;
	private KnotenForm knotenForm;
	private QuerungshilfeDetails querungshilfeDetails;
	private Bauwerksmangel bauwerksmangel;
	private Set<BauwerksmangelArt> bauwerksmangelArt;

	@AssertTrue(message = "Details zur Querungshilfe passen nicht zur Knotenform")
	public boolean isQuerungshilfeDetailsValid() {
		return Knoten.isQuerungshilfeDetailsValid(querungshilfeDetails, knotenForm);
	}

	@AssertTrue(message = "Keine gültige Kombination für die Werte Bauwerksmangel, Art des Bauwerksmangels und Knotenform")
	public boolean isBauwerksmangelValid() {
		return Knoten.isBauwerksmangelValid(bauwerksmangel, bauwerksmangelArt, knotenForm);
	}
}
