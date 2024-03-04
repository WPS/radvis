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
import de.wps.radvis.backend.massnahme.domain.dbView.MassnahmeListenDbView;
import de.wps.radvis.backend.massnahme.domain.valueObject.Bezeichnung;
import de.wps.radvis.backend.massnahme.domain.valueObject.Durchfuehrungszeitraum;
import de.wps.radvis.backend.massnahme.domain.valueObject.Handlungsverantwortlicher;
import de.wps.radvis.backend.massnahme.domain.valueObject.MassnahmeKonzeptID;
import de.wps.radvis.backend.massnahme.domain.valueObject.Massnahmenkategorie;
import de.wps.radvis.backend.massnahme.domain.valueObject.Prioritaet;
import de.wps.radvis.backend.massnahme.domain.valueObject.UmsetzungsstandStatus;
import de.wps.radvis.backend.massnahme.domain.valueObject.Umsetzungsstatus;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.SollStandard;
import de.wps.radvis.backend.organisation.schnittstelle.VerwaltungseinheitView;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MassnahmeListenView {
	private final Long id;
	private final Bezeichnung bezeichnung;
	private final MassnahmeKonzeptID massnahmeKonzeptId;
	private final Set<Massnahmenkategorie> massnahmenkategorien;
	private final Umsetzungsstatus umsetzungsstatus;
	private final UmsetzungsstandStatus umsetzungsstandStatus;
	private final Boolean veroeffentlicht;
	private final Boolean planungErforderlich;
	private final Durchfuehrungszeitraum durchfuehrungszeitraum;
	private final VerwaltungseinheitView baulastZustaendiger;
	private final Prioritaet prioritaet;
	private final Set<Netzklasse> netzklassen;
	private final LocalDateTime letzteAenderung;
	private final BenutzerView benutzerLetzteAenderung;
	private final VerwaltungseinheitView zustaendiger;
	private final VerwaltungseinheitView unterhaltsZustaendiger;
	private final SollStandard sollStandard;
	private final Handlungsverantwortlicher handlungsverantwortlicher;
	private final Geometry geometry;

	public MassnahmeListenView(MassnahmeListenDbView massnahmeListenDBView) {
		this(massnahmeListenDBView.getId(),
			massnahmeListenDBView.getBezeichnung(),
			massnahmeListenDBView.getMassnahmeKonzeptId(),
			massnahmeListenDBView.getMassnahmenkategorien(),
			massnahmeListenDBView.getUmsetzungsstatus(),
			massnahmeListenDBView.getUmsetzungsstandStatus(),
			massnahmeListenDBView.getVeroeffentlicht(),
			massnahmeListenDBView.getPlanungErforderlich(),
			massnahmeListenDBView.getDurchfuehrungszeitraum(),
			massnahmeListenDBView.getBaulastId() == null ? null
				: new VerwaltungseinheitView(
				massnahmeListenDBView.getBaulastId(),
				massnahmeListenDBView.getBaulastName(),
				massnahmeListenDBView.getBaulastOrganisationsArt(),
				massnahmeListenDBView.getBaulastUebergeordneteOrganisationId(),
				// Wir setzen aktiv einfach auf true, weil wir die DBView nicht um das aktiv Flag erweitern.
				true),
			massnahmeListenDBView.getPrioritaet(),
			massnahmeListenDBView.getNetzklassen(),
			massnahmeListenDBView.getLetzteAenderung(),
			massnahmeListenDBView.getBenutzerLetzteAenderungId() == null ? null
				: new BenutzerView(
				massnahmeListenDBView.getBenutzerLetzteAenderungVorname(),
				massnahmeListenDBView.getBenutzerLetzteAenderungNachname()),
			massnahmeListenDBView.getZustaendigId() == null ? null
				: new VerwaltungseinheitView(
				massnahmeListenDBView.getZustaendigId(),
				massnahmeListenDBView.getZustaendigName(),
				massnahmeListenDBView.getZustaendigOrganisationsArt(),
				massnahmeListenDBView.getZustaendigUebergeordneteOrganisationId(),
				true),
			massnahmeListenDBView.getUnterhaltId() == null ? null
				: new VerwaltungseinheitView(
				massnahmeListenDBView.getUnterhaltId(),
				massnahmeListenDBView.getUnterhaltName(),
				massnahmeListenDBView.getUnterhaltOrganisationsArt(),
				massnahmeListenDBView.getUnterhaltUebergeordneteOrganisationId(),
				true),
			massnahmeListenDBView.getSollStandard(),
			massnahmeListenDBView.getHandlungsverantwortlicher(),
			massnahmeListenDBView.getDisplayGeometry());
	}
}
