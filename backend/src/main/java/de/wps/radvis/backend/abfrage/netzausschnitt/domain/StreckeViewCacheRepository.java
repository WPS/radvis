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

package de.wps.radvis.backend.abfrage.netzausschnitt.domain;

import java.util.Collection;
import java.util.Objects;

import de.wps.radvis.backend.abfrage.netzausschnitt.domain.exception.StreckenViewCacheNotInitializedException;
import de.wps.radvis.backend.netz.domain.entity.StreckeVonKanten;

public abstract class StreckeViewCacheRepository<Cache, StreckenTyp extends StreckeVonKanten> {
	protected static final double DISTANCE_TOLERANCE = 10.;

	protected Cache cache;
	protected Collection<StreckenTyp> streckenVonKanten;

	public boolean hasCache() {
		return Objects.nonNull(cache);
	}

	Cache getCache() {
		if (this.cache == null) {
			throw new StreckenViewCacheNotInitializedException(getCacheName());
		}
		return this.cache;
	}

	Collection<StreckenTyp> getStreckenVonKanten() {
		return this.streckenVonKanten;
	}

	void loadCache(Collection<StreckenTyp> streckenVonKanten) {
		this.streckenVonKanten = streckenVonKanten;
		this.reloadCache();
	}

	abstract void reloadCache();

	abstract String getCacheName();

	void addStrecke(StreckenTyp streckeVonKanten) {
		streckenVonKanten.add(streckeVonKanten);
	}

	void removeStrecke(StreckenTyp streckeVonKanten) {
		streckenVonKanten.remove(streckeVonKanten);
	}
}
