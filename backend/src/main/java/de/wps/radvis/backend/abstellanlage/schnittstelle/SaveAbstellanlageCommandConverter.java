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
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AbstellanlagenQuellSystem;
import de.wps.radvis.backend.dokument.domain.entity.DokumentListe;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitResolver;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class SaveAbstellanlageCommandConverter {
	private final VerwaltungseinheitResolver verwaltungseinheitResolver;

	public Abstellanlage convert(@Valid SaveAbstellanlageCommand command) {
		Verwaltungseinheit zustaendig = command.getZustaendigId() != null ? verwaltungseinheitResolver.resolve(command
			.getZustaendigId()) : null;

		return new Abstellanlage(
			(Point) command.getGeometrie(),
			command.getBetreiber(),
			command.getExterneId(),
			AbstellanlagenQuellSystem.RADVIS,
			zustaendig,
			command.getAnzahlStellplaetze(),
			command.getAnzahlSchliessfaecher(),
			command.getAnzahlLademoeglichkeiten(),
			command.getUeberwacht(),
			command.getAbstellanlagenOrt(),
			command.getGroessenklasse(),
			command.getStellplatzart(),
			command.getUeberdacht(),
			command.getGebuehrenProTag(),
			command.getGebuehrenProMonat(),
			command.getGebuehrenProJahr(),
			command.getBeschreibung(),
			command.getWeitereInformation(),
			command.getStatus(),
			new DokumentListe());
	}

	public void apply(Abstellanlage abstellanlage, @Valid SaveAbstellanlageCommand command) {
		Verwaltungseinheit zustaendig = command.getZustaendigId() != null ? verwaltungseinheitResolver.resolve(command
			.getZustaendigId()) : null;
		abstellanlage.update(
			(Point) command.getGeometrie(),
			command.getBetreiber(),
			command.getExterneId(),
			zustaendig,
			command.getAnzahlStellplaetze(),
			command.getAnzahlSchliessfaecher(),
			command.getAnzahlLademoeglichkeiten(),
			command.getUeberwacht(),
			command.getAbstellanlagenOrt(),
			command.getGroessenklasse(),
			command.getStellplatzart(),
			command.getUeberdacht(),
			command.getGebuehrenProTag(),
			command.getGebuehrenProMonat(),
			command.getGebuehrenProJahr(),
			command.getBeschreibung(),
			command.getWeitereInformation(),
			command.getStatus());

	}

}
