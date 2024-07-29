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

package de.wps.radvis.backend.application.metrics;

import static org.valid4j.Assertive.require;

import java.util.function.Supplier;

public class MovingAverageSupplier implements Supplier<Number> {

	private Double average;
	private double factor;

	/**
	 * A Supplier for an average value.
	 * The value is calculated by multipling the given factor to the old value and adding the new value times 1-factor to it.
	 * Factor of 0.9 an old avg of 100 and a new value of 10 --> newAvg of 91
	 * Factor of 0.99 an old avg of 100 and a new value of 10 --> newAvg of 99.1
	 *
	 * @param oldValueFactor
	 *     has to be between 0 and 1, the closer to one the factor the smaller the impact of new values
	 */
	public MovingAverageSupplier(double oldValueFactor) {
		require(oldValueFactor > 0 && oldValueFactor <= 1);
		factor = oldValueFactor;
	}

	public void record(Double newRecord) {
		if (average == null) {
			average = newRecord;
		} else {
			// die beiden faktoren zusammen sind immer 1
			// faktor = 0.9, oldAvg = 100, newRecord = 10 --> (0.9 * 100) + (0.1 * 10) --> 91
			// faktor = 0.99, oldAvg = 100, newRecord = 10 --> (0.99 * 100) + (0.01 * 10) --> 99.1
			average = (factor * average) + ((1 - factor) * newRecord);
		}
	}

	@Override
	public Number get() {
		if (average == null) {
			return 0;
		}
		return (double) Math.round(average * 1000d) / 1000d;
	}
}
