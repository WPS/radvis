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

package de.wps.radvis.backend.massnahme.schnittstelle;

import java.util.Set;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonInclude;

import de.wps.radvis.backend.massnahme.domain.valueObject.Konzeptionsquelle;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.massnahme.domain.valueObject.Bezeichnung;
import de.wps.radvis.backend.massnahme.domain.valueObject.Durchfuehrungszeitraum;
import de.wps.radvis.backend.massnahme.domain.valueObject.Handlungsverantwortlicher;
import de.wps.radvis.backend.massnahme.domain.valueObject.Kostenannahme;
import de.wps.radvis.backend.massnahme.domain.valueObject.LGVFGID;
import de.wps.radvis.backend.massnahme.domain.valueObject.MaViSID;
import de.wps.radvis.backend.massnahme.domain.valueObject.MassnahmeKonzeptID;
import de.wps.radvis.backend.massnahme.domain.valueObject.Massnahmenkategorie;
import de.wps.radvis.backend.massnahme.domain.valueObject.Prioritaet;
import de.wps.radvis.backend.massnahme.domain.valueObject.Realisierungshilfe;
import de.wps.radvis.backend.massnahme.domain.valueObject.Umsetzungsstatus;
import de.wps.radvis.backend.massnahme.domain.valueObject.VerbaID;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.SollStandard;
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
public class SaveMassnahmeCommand {

	@NotNull
	private Long id;
	@NotNull
	private Long version;
	@NotNull
	private Bezeichnung bezeichnung;
	@NotNull
	@NotEmpty
	private Set<Massnahmenkategorie> massnahmenkategorien;
	@NotNull
	private NetzbezugCommand netzbezug;
	@NotNull
	private Umsetzungsstatus umsetzungsstatus;
	@NotNull
	private Boolean veroeffentlicht;

	private Boolean planungErforderlich;
	private Durchfuehrungszeitraum durchfuehrungszeitraum;
	private Long baulastZustaendigerId;
	private MaViSID maViSID;
	private VerbaID verbaID;
	private LGVFGID lgvfgid;
	private MassnahmeKonzeptID massnahmeKonzeptID;
	private Set<Netzklasse> netzklassen;
	private Prioritaet prioritaet;
	private Kostenannahme kostenannahme;
	private Long unterhaltsZustaendigerId;
	private Long markierungsZustaendigerId;
	private SollStandard sollStandard;
	private Handlungsverantwortlicher handlungsverantwortlicher;
	private Konzeptionsquelle konzeptionsquelle;
	private String sonstigeKonzeptionsquelle;
	private Realisierungshilfe realisierungshilfe;


	@AssertTrue(message = "Durchführungszeitpunkt und Zuständiger-Baulast sind ab Status 'Planung' ein Pflichtfeld.")
	public boolean isRequiredAbUmsetzungsstatusPlanung() {
		return Massnahme.pflichtFelderAbPlanung(umsetzungsstatus, baulastZustaendigerId, durchfuehrungszeitraum,
			handlungsverantwortlicher);
	}

	@AssertTrue(message = "Sonstige Konzeptionsquelle ist ein Pflichtfeld, wenn Konzeptionsquelle 'Sonstige' ist.")
	public boolean isSonstigeKonzeptionsquelleNichtLeerWennSonstigeKonzeptionsquelle() {
		return Massnahme.sonstigeKonzeptionsquelleNichtLeerWennSonstigeKonzeptionsquelle(konzeptionsquelle,
			sonstigeKonzeptionsquelle);
	}

	@AssertTrue(message = "Nur eine Massnahmenkategorie pro Oberkategorie erlaubt.")
	public boolean isNurEineMassnahmenkategorieProOberkategorie() {
		return Massnahme.hatNurEineMassnahmenkategorieProOberkategorie(massnahmenkategorien);
	}
}
