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

import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonInclude;

import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppe;
import de.wps.radvis.backend.netz.domain.valueObject.Beleuchtung;
import de.wps.radvis.backend.netz.domain.valueObject.IstStandard;
import de.wps.radvis.backend.netz.domain.valueObject.KantenOrtslage;
import de.wps.radvis.backend.netz.domain.valueObject.Kommentar;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.Status;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenquerschnittRASt06;
import de.wps.radvis.backend.netz.domain.valueObject.Umfeld;
import de.wps.radvis.backend.netz.domain.valueObject.VerkehrStaerke;
import de.wps.radvis.backend.netz.domain.valueObject.WegeNiveau;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Validated
public class SaveKanteAttributeCommand {

	@NotNull
	private Long gruppenId;
	@NotNull
	private Long gruppenVersion;
	@NotNull
	private Long kanteId;

	private KantenOrtslage ortslage;
	private WegeNiveau wegeNiveau;
	@NotNull
	private Beleuchtung beleuchtung;
	@NotNull
	private StrassenquerschnittRASt06 strassenquerschnittRASt06;
	@NotNull
	private Umfeld umfeld;
	private Laenge laengeManuellErfasst;
	private VerkehrStaerke dtvFussverkehr;
	private VerkehrStaerke dtvRadverkehr;
	private VerkehrStaerke dtvPkw;
	private VerkehrStaerke sv;
	private Kommentar kommentar;
	private Long gemeinde;
	@NotNull
	private Status status;

	private Set<Netzklasse> netzklassen;
	private Set<IstStandard> istStandards;

	@AssertTrue(message = "Netzklassen fehlerhaft: Wenn RadNETZ-IstStandards gesetzt sind, "
		+ "muss eine RadNETZ-Netzklasse gesetzt sein")
	public boolean isRadNetzIstStandardsOnlyForRadnetzTrue() {
		return KantenAttributGruppe.istStandardsAllowedForNetzklassen(netzklassen, istStandards);
	}
}
