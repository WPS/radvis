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

import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonInclude;

import de.wps.radvis.backend.furtKreuzung.domain.entity.FurtKreuzung;
import de.wps.radvis.backend.furtKreuzung.domain.valueObject.FurtKreuzungMusterloesung;
import de.wps.radvis.backend.furtKreuzung.domain.valueObject.FurtenKreuzungenKommentar;
import de.wps.radvis.backend.furtKreuzung.domain.valueObject.FurtenKreuzungenTyp;
import de.wps.radvis.backend.furtKreuzung.domain.valueObject.LichtsignalAnlageEigenschaften;
import de.wps.radvis.backend.netz.domain.valueObject.KnotenForm;
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
	private FurtKreuzungNetzBezugCommand netzbezug;
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
