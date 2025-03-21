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

package de.wps.radvis.backend.furtKreuzung.schnittstelle;

import java.util.Optional;
import java.util.Set;

import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonInclude;

import de.wps.radvis.backend.furtKreuzung.domain.entity.FurtKreuzung;
import de.wps.radvis.backend.furtKreuzung.domain.valueObject.FurtKreuzungMusterloesung;
import de.wps.radvis.backend.furtKreuzung.domain.valueObject.FurtenKreuzungenKommentar;
import de.wps.radvis.backend.furtKreuzung.domain.valueObject.FurtenKreuzungenTyp;
import de.wps.radvis.backend.furtKreuzung.domain.valueObject.LichtsignalAnlageEigenschaften;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.valueObject.Bauwerksmangel;
import de.wps.radvis.backend.netz.domain.valueObject.BauwerksmangelArt;
import de.wps.radvis.backend.netz.domain.valueObject.KnotenForm;
import de.wps.radvis.backend.netz.domain.valueObject.QuerungshilfeDetails;
import de.wps.radvis.backend.netz.schnittstelle.command.NetzbezugCommand;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@ToString
@Validated
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaveFurtKreuzungCommand {

	@NotNull
	private NetzbezugCommand netzbezug;
	@NotNull
	private Long verantwortlicheOrganisation;
	@NotNull
	private FurtenKreuzungenTyp typ;
	@NotNull
	private boolean radnetzKonform;
	private FurtenKreuzungenKommentar kommentar;
	@NotNull
	private KnotenForm knotenForm;
	Long version;
	@NotNull
	Optional<FurtKreuzungMusterloesung> furtKreuzungMusterloesung;

	@NotNull
	Optional<LichtsignalAnlageEigenschaften> lichtsignalAnlageEigenschaften;

	private QuerungshilfeDetails querungshilfeDetails;
	private Bauwerksmangel bauwerksmangel;
	private Set<BauwerksmangelArt> bauwerksmangelArt;

	@AssertTrue(message = "Details zur Querungshilfe passen nicht zur Knotenform")
	public boolean isQuerungshilfeDetailsValid() {
		// Diese Validierung kann ggf. vor der @NotNull aufgerufen werden. Damit die Fehlermeldung aber korrekt ist,
		// geben wir hier true zurück.
		if (knotenForm == null) {
			return true;
		}
		return Knoten.isQuerungshilfeDetailsValid(querungshilfeDetails, knotenForm);
	}

	@AssertTrue(message = "Keine gültige Kombination für die Werte Bauwerksmangel, Art des Bauwerksmangels und Knotenform")
	public boolean isBauwerksmangelValid() {
		// Diese Validierung kann ggf. vor der @NotNull aufgerufen werden. Damit die Fehlermeldung aber korrekt ist,
		// geben wir hier true zurück.
		if (knotenForm == null) {
			return true;
		}
		return Knoten.isBauwerksmangelValid(bauwerksmangel, bauwerksmangelArt, knotenForm);
	}

	@AssertTrue(message = "Musterlösungen können nur für RadNETZ konforme Furten/Kreuzungen vergeben werden")
	public boolean isMusterloesungNurFuerRadNetzKonformValid() {
		return FurtKreuzung.musterloesungErlaubt(furtKreuzungMusterloesung, radnetzKonform);
	}

	@AssertTrue(message = "Lichsignalanlageneigenschaften müssen und dürfen nur genau dann gesetzt sein,"
		+ "wenn die KnotenForm von der Kategorie 'Knoten mit LSA' oder 'Signalisierte Querungsstelle' ist.")
	public boolean isLichtsignalAnlageEigenschaftenFueKnotenFormErlaubtValid() {
		return FurtKreuzung.lichtsignalAnlageEigenschaftenFuerKnotenFormErlaubt(lichtsignalAnlageEigenschaften,
			knotenForm);
	}
}
