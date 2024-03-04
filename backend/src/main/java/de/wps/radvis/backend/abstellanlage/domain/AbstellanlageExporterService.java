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

package de.wps.radvis.backend.abstellanlage.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import de.wps.radvis.backend.abstellanlage.domain.entity.Abstellanlage;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AbstellanlagenBeschreibung;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AbstellanlagenWeitereInformation;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AnzahlLademoeglichkeiten;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AnzahlSchliessfaecher;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AnzahlStellplaetze;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.GebuehrenProJahr;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.GebuehrenProMonat;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.GebuehrenProTag;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.Groessenklasse;
import de.wps.radvis.backend.common.domain.service.ExporterService;
import de.wps.radvis.backend.common.domain.valueObject.ExportData;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;

public class AbstellanlageExporterService implements ExporterService {

	private final AbstellanlageRepository abstellanlageRepository;

	public AbstellanlageExporterService(AbstellanlageRepository abstellanlageRepository) {
		this.abstellanlageRepository = abstellanlageRepository;
	}

	@Override
	public List<ExportData> export(List<Long> ids) {
		return StreamSupport.stream(abstellanlageRepository.findAllById(ids).spliterator(), false)
			.map(abstellanlage -> {
				Map<String, String> attribute = new LinkedHashMap<>();

				attribute.put(Abstellanlage.CsvHeader.RAD_VIS_ID, abstellanlage.getId().toString());
				attribute.put(Abstellanlage.CsvHeader.POSITION_X_UTM32_N,
					String.valueOf(abstellanlage.getGeometrie().getCoordinate().x).replace('.', ','));
				attribute.put(Abstellanlage.CsvHeader.POSITION_Y_UTM32_N,
					String.valueOf(abstellanlage.getGeometrie().getCoordinate().y).replace('.', ','));
				attribute.put(Abstellanlage.CsvHeader.BETREIBER, abstellanlage.getBetreiber().getValue());
				attribute.put(Abstellanlage.CsvHeader.EXTERNE_ID,
					abstellanlage.getExterneId().isPresent() ? abstellanlage.getExterneId().get().getValue() : "");
				attribute.put(Abstellanlage.CsvHeader.QUELLSYSTEM,
					abstellanlage.getQuellSystem().toString());
				attribute.put(Abstellanlage.CsvHeader.ZUSTAENDIG_IN_RAD_VIS,
					abstellanlage.getZustaendig()
						.map(t -> Verwaltungseinheit.combineNameAndArt(t.getName(), t.getOrganisationsArt()))
						.orElse(""));

				attribute.put(Abstellanlage.CsvHeader.ANZAHL_STELLPLAETZE, abstellanlage.getAnzahlStellplaetze()
					.map(AnzahlStellplaetze::getValue).map(Object::toString)
					.orElse(""));

				attribute.put(Abstellanlage.CsvHeader.ANZAHL_SCHLIESSFAECHER,
					abstellanlage.getAnzahlSchliessfaecher().map(AnzahlSchliessfaecher::getValue).map(Objects::toString)
						.orElse(""));
				attribute.put(Abstellanlage.CsvHeader.ANZAHL_LADEMOEGLICHKEITEN,
					abstellanlage.getAnzahlLademoeglichkeiten().map(AnzahlLademoeglichkeiten::getValue)
						.map(Objects::toString).orElse(""));
				attribute.put(Abstellanlage.CsvHeader.UEBERWACHT, abstellanlage.getUeberwacht().toString());
				attribute.put(Abstellanlage.CsvHeader.ABSTELLANLAGEN_ORT, abstellanlage.getAbstellanlagenOrt()
					.toString());
				attribute.put(Abstellanlage.CsvHeader.GROESSENKLASSE, abstellanlage.getGroessenklasse().map(
					Groessenklasse::toString).orElse(""));
				attribute.put(Abstellanlage.CsvHeader.STELLPLATZART, abstellanlage.getStellplatzart().toString());
				attribute.put(Abstellanlage.CsvHeader.UEBERDACHT,
					abstellanlage.getUeberdacht() != null && abstellanlage.getUeberdacht().getValue() ? "Ja" :
						"Nein");
				attribute.put(Abstellanlage.CsvHeader.GEBUEHREN_PRO_TAG,
					abstellanlage.getGebuehrenProTag().map(GebuehrenProTag::getValue).map(Objects::toString)
						.orElse(""));
				attribute.put(Abstellanlage.CsvHeader.GEBUEHREN_PRO_MONAT,
					abstellanlage.getGebuehrenProMonat().map(GebuehrenProMonat::getValue).map(Objects::toString)
						.orElse(""));
				attribute.put(Abstellanlage.CsvHeader.GEBUEHREN_PRO_JAHR,
					abstellanlage.getGebuehrenProJahr().map(GebuehrenProJahr::getValue).map(Objects::toString)
						.orElse(""));
				attribute.put(Abstellanlage.CsvHeader.BESCHREIBUNG, abstellanlage.getBeschreibung().map(
					AbstellanlagenBeschreibung::getValue).map(Objects::toString).orElse(""));
				attribute.put(Abstellanlage.CsvHeader.WEITERE_INFORMATION,
					abstellanlage.getWeitereInformation().map(AbstellanlagenWeitereInformation::getValue)
						.map(Objects::toString).orElse(""));
				attribute.put(Abstellanlage.CsvHeader.ABSTELLANLAGEN_STATUS, abstellanlage.getStatus().toString());

				return new ExportData(abstellanlage.getGeometrie(), attribute);
			}).collect(Collectors.toList());
	}

	@Override
	public String getDateinamenPrefix() {
		return "abstellanlagen";
	}

}
