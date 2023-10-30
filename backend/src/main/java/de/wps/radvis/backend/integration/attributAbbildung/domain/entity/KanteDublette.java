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

import static org.hamcrest.Matchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.util.Objects;
import java.util.Optional;

import org.locationtech.jts.geom.LineString;

import de.wps.radvis.backend.common.domain.exception.KeineUeberschneidungException;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.LineStrings;
import lombok.Getter;

public class KanteDublette {

	@Getter
	private final Kante zielnetzKante;
	@Getter
	private final Kante quellnetzKante;

	@Getter
	private final LineString zielnetzUeberschneidung;

	public KanteDublette(Kante zielnetzKante, Kante quellnetzKante) throws KeineUeberschneidungException {
		require(zielnetzKante, notNullValue());
		require(zielnetzKante.getZugehoerigeDlmGeometrie(), notNullValue());
		require(quellnetzKante, notNullValue());
		require(quellnetzKante.getZugehoerigeDlmGeometrie(), notNullValue());

		this.zielnetzKante = zielnetzKante;
		this.quellnetzKante = quellnetzKante;

		Optional<LineString> ueberschneidung = LineStrings.calculateUeberschneidungslinestring(
			zielnetzKante.getZugehoerigeDlmGeometrie(), quellnetzKante.getZugehoerigeDlmGeometrie());

		if (ueberschneidung.isEmpty()) {
			throw new KeineUeberschneidungException("Keine Überschneidung detektierbar");

		}
		zielnetzUeberschneidung = ueberschneidung.get();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		KanteDublette that = (KanteDublette) o;

		if (Objects.equals(zielnetzKante, that.zielnetzKante)
			&& Objects.equals(quellnetzKante, that.quellnetzKante)) {
			return true;
		}

		if (Objects.equals(zielnetzKante, that.quellnetzKante)
			&& Objects.equals(quellnetzKante, that.zielnetzKante)) {
			return true;
		}

		return false;
	}

	@Override
	public int hashCode() {
		int result = zielnetzKante.hashCode();
		result = 31 * result + quellnetzKante.hashCode();
		return result;
	}

}
