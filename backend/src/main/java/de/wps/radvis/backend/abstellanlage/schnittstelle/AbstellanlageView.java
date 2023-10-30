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

package de.wps.radvis.backend.abstellanlage.schnittstelle;

import org.locationtech.jts.geom.Point;

import de.wps.radvis.backend.abstellanlage.domain.entity.Abstellanlage;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AbstellanlagenBeschreibung;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AbstellanlagenBetreiber;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AbstellanlagenQuellSystem;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AbstellanlagenStatus;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AbstellanlagenWeitereInformation;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AnzahlLademoeglichkeiten;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AnzahlSchliessfaecher;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AnzahlStellplaetze;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.ExterneAbstellanlagenId;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.GebuehrenProJahr;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.GebuehrenProMonat;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.GebuehrenProTag;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.Groessenklasse;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.IstBikeAndRide;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.Stellplatzart;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.Ueberdacht;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.Ueberwacht;
import de.wps.radvis.backend.organisation.schnittstelle.VerwaltungseinheitView;
import lombok.Getter;

@Getter
public class AbstellanlageView {
	private final Point geometrie;
	private final AbstellanlagenBetreiber betreiber;
	private final ExterneAbstellanlagenId externeId;

	private final AbstellanlagenQuellSystem quellSystem;
	private final VerwaltungseinheitView zustaendig;
	private final AnzahlStellplaetze anzahlStellplaetze;
	private final AnzahlSchliessfaecher anzahlSchliessfaecher;
	private final AnzahlLademoeglichkeiten anzahlLademoeglichkeiten;
	private final Ueberwacht ueberwacht;
	private final IstBikeAndRide istBikeAndRide;
	private final Groessenklasse groessenklasse;
	private final Stellplatzart stellplatzart;
	private final Ueberdacht ueberdacht;
	private final GebuehrenProTag gebuehrenProTag;
	private final GebuehrenProMonat gebuehrenProMonat;
	private final GebuehrenProJahr gebuehrenProJahr;
	private final AbstellanlagenBeschreibung beschreibung;
	private final AbstellanlagenWeitereInformation weitereInformation;
	private final AbstellanlagenStatus status;

	private final Long id;
	private final Long version;
	private final boolean darfBenutzerBearbeiten;

	public AbstellanlageView(Abstellanlage abstellanlage, boolean darfBenutzerBearbeiten) {
		geometrie = abstellanlage.getGeometrie();
		betreiber = abstellanlage.getBetreiber();
		externeId = abstellanlage.getExterneId().orElse(null);
		id = abstellanlage.getId();
		version = abstellanlage.getVersion();
		this.darfBenutzerBearbeiten = darfBenutzerBearbeiten;

		quellSystem = abstellanlage.getQuellSystem();
		zustaendig = abstellanlage.getZustaendig().map(VerwaltungseinheitView::new).orElse(null);
		anzahlStellplaetze = abstellanlage.getAnzahlStellplaetze().orElse(null);
		anzahlSchliessfaecher = abstellanlage.getAnzahlSchliessfaecher().orElse(null);
		anzahlLademoeglichkeiten = abstellanlage.getAnzahlLademoeglichkeiten().orElse(null);
		ueberwacht = abstellanlage.getUeberwacht();
		istBikeAndRide = abstellanlage.getIstBikeAndRide();
		groessenklasse = abstellanlage.getGroessenklasse().orElse(null);
		stellplatzart = abstellanlage.getStellplatzart();
		ueberdacht = abstellanlage.getUeberdacht();
		gebuehrenProTag = abstellanlage.getGebuehrenProTag().orElse(null);
		gebuehrenProMonat = abstellanlage.getGebuehrenProMonat().orElse(null);
		gebuehrenProJahr = abstellanlage.getGebuehrenProJahr().orElse(null);
		beschreibung = abstellanlage.getBeschreibung().orElse(null);
		weitereInformation = abstellanlage.getWeitereInformation().orElse(null);
		status = abstellanlage.getStatus();
	}
}
