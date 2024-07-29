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

package de.wps.radvis.backend.integration.attributAbbildung.domain.entity;

import java.util.Comparator;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Haendigkeit {

	Orientierung orientierung;
	double wahrscheinlichkeit;

	public Haendigkeit(Double raw) {
		switch ((int) Math.signum(raw)) {
		case 1:
			this.orientierung = Orientierung.LINKS;
			break;
		case -1:
			this.orientierung = Orientierung.RECHTS;
			break;
		default:
			this.orientierung = Orientierung.UNBESTIMMT;
			break;
		}
		this.wahrscheinlichkeit = Math.abs(raw);
	}

	enum Orientierung {
		LINKS,
		RECHTS,
		UNBESTIMMT;
	}

	public double getVorzeichenbehafteteWahrscheinlichkeit() {
		return orientierung.equals(Orientierung.LINKS) ? wahrscheinlichkeit : -1 * wahrscheinlichkeit;
	}

	public static final Comparator<Haendigkeit> vonLinksNachRechts = (s1, s2) -> {
		if (s1.orientierung != s2.orientierung) {
			if (s1.orientierung.equals(Orientierung.LINKS)) {
				return -1;
			} else if (s1.orientierung.equals(Orientierung.UNBESTIMMT) && s2.orientierung.equals(Orientierung.RECHTS)) {
				return -1;
			} else {
				return 1;
			}
		} else {
			if (s1.orientierung.equals(Orientierung.LINKS)) {
				return -Double.compare(s1.wahrscheinlichkeit, s2.wahrscheinlichkeit);
			} else if (s1.orientierung.equals(Orientierung.RECHTS)) {
				return Double.compare(s1.wahrscheinlichkeit, s2.wahrscheinlichkeit);
			} else {
				return 0;
			}
		}
	};
}
