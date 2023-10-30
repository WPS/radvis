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

package de.wps.radvis.backend.netz.domain.valueObject.provider;

import java.util.Comparator;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;

public class LineareReferenzTestProvider {
	public static final Comparator<LinearReferenzierterAbschnitt> lenientComparator = comparatorWithTolerance(0.05);

	public static Comparator<LinearReferenzierterAbschnitt> comparatorWithTolerance(double tolerance) {
		return (LinearReferenzierterAbschnitt LR1, LinearReferenzierterAbschnitt LR2) -> {
			if (Math.abs(LR1.getVonValue() - LR2.getVonValue()) < tolerance
				&& Math.abs(LR1.getBisValue() - LR2.getBisValue()) < tolerance) {
				return 0;
			}
			return LinearReferenzierterAbschnitt.vonZuerst.compare(LR1, LR2);
		};
	}

}
