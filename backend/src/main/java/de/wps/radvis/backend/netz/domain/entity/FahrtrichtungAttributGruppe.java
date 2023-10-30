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

package de.wps.radvis.backend.netz.domain.entity;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import org.hibernate.envers.Audited;

import de.wps.radvis.backend.common.domain.entity.VersionierteEntity;
import de.wps.radvis.backend.netz.domain.valueObject.Richtung;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Entity
@Audited
@NoArgsConstructor
public class FahrtrichtungAttributGruppe extends VersionierteEntity {

	@Enumerated(EnumType.STRING)
	@NonNull
	@Getter
	private Richtung fahrtrichtungLinks;

	@Enumerated(EnumType.STRING)
	@NonNull
	@Getter
	private Richtung fahrtrichtungRechts;

	@Getter
	boolean isZweiseitig;

	public FahrtrichtungAttributGruppe(Richtung richtungFuerBeideSeiten, boolean isZweiseitig) {
		require(isValid(richtungFuerBeideSeiten, richtungFuerBeideSeiten, isZweiseitig));

		this.fahrtrichtungLinks = richtungFuerBeideSeiten;
		this.fahrtrichtungRechts = richtungFuerBeideSeiten;
		this.isZweiseitig = isZweiseitig;
	}

	public FahrtrichtungAttributGruppe(Richtung fahrtrichtungLinks, Richtung fahrtrichtungRechts,
		boolean isZweiseitig) {
		require(isValid(fahrtrichtungLinks, fahrtrichtungRechts, isZweiseitig));

		this.fahrtrichtungLinks = fahrtrichtungLinks;
		this.fahrtrichtungRechts = fahrtrichtungRechts;
		this.isZweiseitig = isZweiseitig;
	}

	@Builder(builderMethodName = "privateBuilder")
	public FahrtrichtungAttributGruppe(Long id, Long version, Richtung fahrtrichtungLinks, Richtung fahrtrichtungRechts,
		boolean isZweiseitig) {
		super(id, version);

		require(isValid(fahrtrichtungLinks, fahrtrichtungRechts, isZweiseitig));

		this.fahrtrichtungLinks = fahrtrichtungLinks;
		this.fahrtrichtungRechts = fahrtrichtungRechts;
		this.isZweiseitig = isZweiseitig;
	}

	public static boolean isValid(Richtung fahrtrichtungLinks, Richtung fahrtrichtungRechts, boolean isZweiseitig) {
		if (fahrtrichtungLinks == null)
			return false;
		if (fahrtrichtungRechts == null)
			return false;
		if (!isZweiseitig) {
			return fahrtrichtungLinks.equals(fahrtrichtungRechts);
		} else {
			return true;
		}
	}

	public static FahrtrichtungAttributGruppeBuilder builder() {
		return privateBuilder().fahrtrichtungRechts(Richtung.UNBEKANNT).fahrtrichtungLinks(Richtung.UNBEKANNT)
			.isZweiseitig(false);
	}

	public void setRichtung(Richtung links, Richtung rechts) {
		require(isValid(links, rechts, isZweiseitig()));

		this.fahrtrichtungLinks = links;
		this.fahrtrichtungRechts = rechts;
	}

	public void setRichtung(Richtung richtung) {
		require(richtung, notNullValue());

		this.fahrtrichtungLinks = richtung;
		this.fahrtrichtungRechts = richtung;
	}

	public void changeSeitenbezug(boolean isZweiseitig) {
		this.isZweiseitig = isZweiseitig;

		if (!isZweiseitig) {
			fahrtrichtungRechts = fahrtrichtungLinks;
		}
	}

	public void reset() {
		this.fahrtrichtungLinks = Richtung.UNBEKANNT;
		this.fahrtrichtungRechts = Richtung.UNBEKANNT;
		this.isZweiseitig = false;
	}

	public Richtung befahrbarIn() {
		if (!isZweiseitig()) {
			switch (getFahrtrichtungLinks()) {
			case UNBEKANNT:
			case BEIDE_RICHTUNGEN:
				return Richtung.BEIDE_RICHTUNGEN;
			default:
				return getFahrtrichtungLinks();
			}
		} else {
			List<Richtung> fahrtrichtungenLinksUndRechts = List.of(getFahrtrichtungLinks(),
				getFahrtrichtungRechts());
			if (fahrtrichtungenLinksUndRechts.contains(Richtung.BEIDE_RICHTUNGEN)
				|| fahrtrichtungenLinksUndRechts.contains(Richtung.UNBEKANNT)) {
				return Richtung.BEIDE_RICHTUNGEN;
			} else if (fahrtrichtungenLinksUndRechts.contains(Richtung.IN_RICHTUNG)
				&& fahrtrichtungenLinksUndRechts.contains(Richtung.GEGEN_RICHTUNG)) {
				return Richtung.BEIDE_RICHTUNGEN;
			} else if (fahrtrichtungenLinksUndRechts.contains(Richtung.IN_RICHTUNG)) {
				return Richtung.IN_RICHTUNG;
			} else {
				return Richtung.GEGEN_RICHTUNG;
			}
		}
	}
}
