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

package de.wps.radvis.backend.massnahme.schnittstelle.view;

import java.time.LocalDateTime;
import java.util.Set;

import org.locationtech.jts.geom.Geometry;

import de.wps.radvis.backend.benutzer.schnittstelle.BenutzerView;
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
import de.wps.radvis.backend.netz.schnittstelle.view.NetzbezugView;
import de.wps.radvis.backend.organisation.schnittstelle.VerwaltungseinheitView;
import lombok.Getter;

@Getter
public class MassnahmeEditView {

	private final Long id;
	private final Long version;
	private final Bezeichnung bezeichnung;
	private final Set<Massnahmenkategorie> massnahmenkategorien;
	private final NetzbezugView netzbezug;
	private final Umsetzungsstatus umsetzungsstatus;
	private final Boolean veroeffentlicht;
	private final Boolean planungErforderlich;
	private final Durchfuehrungszeitraum durchfuehrungszeitraum;
	private final VerwaltungseinheitView baulastZustaendiger;
	private final Prioritaet prioritaet;
	private final Kostenannahme kostenannahme;
	private final Set<Netzklasse> netzklassen;
	private final LocalDateTime letzteAenderung;
	private final BenutzerView benutzerLetzteAenderung;
	private final VerwaltungseinheitView markierungsZustaendiger;
	private final VerwaltungseinheitView unterhaltsZustaendiger;
	private final MaViSID maViSID;
	private final VerbaID verbaID;
	private final LGVFGID lgvfgid;
	private final MassnahmeKonzeptID massnahmeKonzeptID;
	private final SollStandard sollStandard;
	private final Handlungsverantwortlicher handlungsverantwortlicher;
	private final Konzeptionsquelle konzeptionsquelle;
	private final String sonstigeKonzeptionsquelle;
	private final Geometry geometry;
	private final boolean canEdit;
	private final Realisierungshilfe realisierungshilfe;

	public MassnahmeEditView(Massnahme massnahme, boolean canMassnahmeBearbeiten) {
		this.canEdit = canMassnahmeBearbeiten;
		this.id = massnahme.getId();
		this.version = massnahme.getVersion();
		this.netzbezug = new NetzbezugView(massnahme.getNetzbezug());
		this.bezeichnung = massnahme.getBezeichnung();
		this.massnahmenkategorien = massnahme.getMassnahmenkategorien();
		this.veroeffentlicht = massnahme.getVeroeffentlicht();
		this.planungErforderlich = massnahme.getPlanungErforderlich();
		this.umsetzungsstatus = massnahme.getUmsetzungsstatus();
		this.durchfuehrungszeitraum = massnahme.getDurchfuehrungszeitraum().orElse(null);
		this.baulastZustaendiger = massnahme.getBaulastZustaendiger().map(VerwaltungseinheitView::new).orElse(null);
		this.markierungsZustaendiger = massnahme.getMarkierungsZustaendiger().map(VerwaltungseinheitView::new)
			.orElse(null);
		this.unterhaltsZustaendiger = massnahme.getunterhaltsZustaendiger().map(VerwaltungseinheitView::new)
			.orElse(null);
		this.prioritaet = massnahme.getPrioritaet().orElse(null);
		this.kostenannahme = massnahme.getKostenannahme().orElse(null);
		this.netzklassen = massnahme.getNetzklassen();
		this.letzteAenderung = massnahme.getLetzteAenderung();
		this.benutzerLetzteAenderung = new BenutzerView(massnahme.getBenutzerLetzteAenderung());
		this.geometry = massnahme.berechneMittelpunkt().orElse(null);
		this.maViSID = massnahme.getMaViSID().orElse(null);
		this.verbaID = massnahme.getVerbaID().orElse(null);
		this.lgvfgid = massnahme.getLGVFGID().orElse(null);
		this.massnahmeKonzeptID = massnahme.getMassnahmeKonzeptID().orElse(null);
		this.sollStandard = massnahme.getSollStandard();
		this.handlungsverantwortlicher = massnahme.getHandlungsverantwortlicher().orElse(null);
		this.konzeptionsquelle = massnahme.getKonzeptionsquelle();
		this.sonstigeKonzeptionsquelle = massnahme.getSonstigeKonzeptionsquelle().orElse(null);
		this.realisierungshilfe = massnahme.getRealisierungshilfe().orElse(null);
	}
}
