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

package de.wps.radvis.backend.netz.schnittstelle.view;

import java.util.Set;

import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppe;
import de.wps.radvis.backend.netz.domain.valueObject.Beleuchtung;
import de.wps.radvis.backend.netz.domain.valueObject.IstStandard;
import de.wps.radvis.backend.netz.domain.valueObject.Kommentar;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.Status;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenName;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenNummer;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenkategorieRIN;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenquerschnittRASt06;
import de.wps.radvis.backend.netz.domain.valueObject.Umfeld;
import de.wps.radvis.backend.netz.domain.valueObject.VerkehrStaerke;
import de.wps.radvis.backend.netz.domain.valueObject.WegeNiveau;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.schnittstelle.VerwaltungseinheitView;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class KantenAttributGruppeEditView {

	private Long id;
	private Long version;

	private WegeNiveau wegeNiveau;
	private Beleuchtung beleuchtung;
	private Umfeld umfeld;
	private StrassenkategorieRIN strassenkategorieRIN;
	private StrassenquerschnittRASt06 strassenquerschnittRASt06;
	private Laenge laengeManuellErfasst;
	private VerkehrStaerke dtvFussverkehr;
	private VerkehrStaerke dtvRadverkehr;
	private VerkehrStaerke dtvPkw;
	private VerkehrStaerke sv;
	private Kommentar kommentar;
	private StrassenName strassenName;
	private StrassenNummer strassenNummer;
	private Status status;
	private VerwaltungseinheitView gemeinde;
	private VerwaltungseinheitView landkreis;
	private Set<Netzklasse> netzklassen;
	private Set<IstStandard> istStandards;

	public KantenAttributGruppeEditView(KantenAttributGruppe kantenAttributGruppe) {
		this.id = kantenAttributGruppe.getId();
		this.version = kantenAttributGruppe.getVersion();
		this.wegeNiveau = kantenAttributGruppe.getKantenAttribute().getWegeNiveau().orElse(null);
		this.beleuchtung = kantenAttributGruppe.getKantenAttribute().getBeleuchtung();
		this.umfeld = kantenAttributGruppe.getKantenAttribute().getUmfeld();
		this.strassenkategorieRIN = kantenAttributGruppe.getKantenAttribute().getStrassenkategorieRIN().orElse(null);
		this.strassenquerschnittRASt06 = kantenAttributGruppe.getKantenAttribute().getStrassenquerschnittRASt06();
		this.laengeManuellErfasst = kantenAttributGruppe.getKantenAttribute().getLaengeManuellErfasst().orElse(null);
		this.dtvFussverkehr = kantenAttributGruppe.getKantenAttribute().getDtvFussverkehr().orElse(null);
		this.dtvRadverkehr = kantenAttributGruppe.getKantenAttribute().getDtvRadverkehr().orElse(null);
		this.dtvPkw = kantenAttributGruppe.getKantenAttribute().getDtvPkw().orElse(null);
		this.sv = kantenAttributGruppe.getKantenAttribute().getSv().orElse(null);
		this.kommentar = kantenAttributGruppe.getKantenAttribute().getKommentar().orElse(null);
		this.strassenName = kantenAttributGruppe.getKantenAttribute().getStrassenName().orElse(null);
		this.strassenNummer = kantenAttributGruppe.getKantenAttribute().getStrassenNummer().orElse(null);
		this.status = kantenAttributGruppe.getKantenAttribute().getStatus();
		kantenAttributGruppe.getKantenAttribute().getGemeinde()
			.ifPresent(b -> gemeinde = new VerwaltungseinheitView(b));
		kantenAttributGruppe.getKantenAttribute().getGemeinde()
			.flatMap(Verwaltungseinheit::getUebergeordneteVerwaltungseinheit)
			.ifPresent(c -> landkreis = new VerwaltungseinheitView(c));
		this.netzklassen = kantenAttributGruppe.getNetzklassen();
		this.istStandards = kantenAttributGruppe.getIstStandards();
	}

}
