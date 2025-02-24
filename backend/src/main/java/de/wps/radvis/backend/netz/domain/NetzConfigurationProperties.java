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

package de.wps.radvis.backend.netz.domain;

import static org.valid4j.Assertive.require;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import lombok.Getter;

@ConfigurationProperties("radvis.netz")
@Getter
public class NetzConfigurationProperties {
	private final Laenge minimaleSegmentLaenge;
	private final Laenge nahegelegeneKantenDistanzInM;
	private final int kantenParallelitaetSegmente;
	private final double kantenParallelitaetToleranz;
	private final double nahegelegeneKantenMinAbgebildeteRelativeGesamtlaenge;

	@ConstructorBinding
	public NetzConfigurationProperties(double minimaleSegmentLaenge, double nahegelegeneKantenDistanzInM,
		int kantenParallelitaetSegmente, double kantenParallelitaetToleranz,
		double nahegelegeneKantenMinAbgebildeteRelativeGesamtlaenge) {
		require(minimaleSegmentLaenge > 0, "Minimale Segment Laenge muss größer 0 sein.");
		require(nahegelegeneKantenDistanzInM >= 0, "Die Distanz für nahegelegene Kanten darf nicht negativ sein.");
		require(kantenParallelitaetSegmente >= 1,
			"Die Anzahl an Segmenten zur Prüfung von parallelen Kanten muss mindestens 1 sein.");
		require(kantenParallelitaetToleranz >= 0 && kantenParallelitaetToleranz < 360,
			"Die Toleranz zur Prüfung von Parallelität zweier LineStrings muss >=0 und <360 sein.");
		require(nahegelegeneKantenMinAbgebildeteRelativeGesamtlaenge >= 0
			&& nahegelegeneKantenMinAbgebildeteRelativeGesamtlaenge <= 1,
			"Die relative Gesamtlänge der abgebildeten Kanten muss >= 0 und <=1 sein.");

		this.minimaleSegmentLaenge = Laenge.of(minimaleSegmentLaenge);
		this.nahegelegeneKantenDistanzInM = Laenge.of(nahegelegeneKantenDistanzInM);
		this.kantenParallelitaetSegmente = kantenParallelitaetSegmente;
		this.kantenParallelitaetToleranz = kantenParallelitaetToleranz;
		this.nahegelegeneKantenMinAbgebildeteRelativeGesamtlaenge = nahegelegeneKantenMinAbgebildeteRelativeGesamtlaenge;
	}
}
