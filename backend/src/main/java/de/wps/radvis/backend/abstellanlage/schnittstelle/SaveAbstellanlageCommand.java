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

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import org.locationtech.jts.geom.Geometry;

import de.wps.radvis.backend.abstellanlage.domain.valueObject.AbstellanlagenBeschreibung;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AbstellanlagenBetreiber;
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
import lombok.Getter;

@Getter
public class SaveAbstellanlageCommand {
	@NotNull
	private Geometry geometrie;
	@NotNull
	private AbstellanlagenBetreiber betreiber;
	private ExterneAbstellanlagenId externeId;
	private Long zustaendigId;
	private AnzahlStellplaetze anzahlStellplaetze;
	private AnzahlSchliessfaecher anzahlSchliessfaecher;
	private AnzahlLademoeglichkeiten anzahlLademoeglichkeiten;
	@NotNull
	private Ueberwacht ueberwacht;
	@NotNull
	private IstBikeAndRide istBikeAndRide;
	private Groessenklasse groessenklasse;
	@NotNull
	private Stellplatzart stellplatzart;
	@NotNull
	private Ueberdacht ueberdacht;
	private GebuehrenProTag gebuehrenProTag;
	private GebuehrenProMonat gebuehrenProMonat;
	private GebuehrenProJahr gebuehrenProJahr;
	private AbstellanlagenBeschreibung beschreibung;
	private AbstellanlagenWeitereInformation weitereInformation;
	@NotNull
	private AbstellanlagenStatus status;

	private Long version;

	@AssertTrue
	public boolean isGeometrieValid() {
		return geometrie.getGeometryType().equals(Geometry.TYPENAME_POINT);
	}

	@AssertTrue
	public boolean isGroessenklasseNurBeiBikeAndRide() {
		return istBikeAndRide.getValue() || groessenklasse == null;
	}
}
