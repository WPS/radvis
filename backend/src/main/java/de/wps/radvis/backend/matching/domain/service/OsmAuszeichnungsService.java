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

package de.wps.radvis.backend.matching.domain.service;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import de.topobyte.osm4j.core.access.OsmIterator;
import de.topobyte.osm4j.core.access.OsmOutputStream;
import de.topobyte.osm4j.core.model.iface.EntityContainer;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmRelation;
import de.topobyte.osm4j.core.model.iface.OsmTag;
import de.topobyte.osm4j.core.model.impl.Tag;
import de.topobyte.osm4j.core.model.impl.Way;
import de.topobyte.osm4j.pbf.seq.PbfIterator;
import de.topobyte.osm4j.pbf.seq.PbfWriter;
import de.wps.radvis.backend.matching.domain.entity.PbfJobStatistik;
import de.wps.radvis.backend.netz.domain.dbView.KanteOsmMatchWithAttribute;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.valueObject.BelagArt;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.Oberflaechenbeschaffenheit;
import de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OsmAuszeichnungsService {

	public static final Set<Netzklasse> RELEVANTE_NETZKLASSEN = Set.of(Netzklasse.RADNETZ_ALLTAG,
		Netzklasse.RADNETZ_FREIZEIT);

	public static final Map<String, String> RELEVANTE_BELAGARTEN_TO_OSMVALUE = new HashMap<>() {{
		put(BelagArt.ASPHALT.name(), "asphalt");
		put(BelagArt.BETON.name(), "concrete");
		put(BelagArt.NATURSTEINPFLASTER.name(), "cobblestone");
		put(BelagArt.BETONSTEINPFLASTER_PLATTENBELAG.name(), "paving_stones");
		put(BelagArt.WASSERGEBUNDENE_DECKE.name(), "compacted");
		put(BelagArt.UNGEBUNDENE_DECKE.name(), "unpaved");
		put(BelagArt.SONSTIGER_BELAG.name(), "other");
	}};

	private final KantenRepository kantenRepository;

	private final Double minimaleUeberdeckungFuerAttributAuszeichnung;

	public OsmAuszeichnungsService(KantenRepository kantenRepository,
		Double minimaleUeberdeckungFuerAttributAuszeichnung) {
		this.kantenRepository = kantenRepository;
		require(kantenRepository, notNullValue());
		require(minimaleUeberdeckungFuerAttributAuszeichnung, notNullValue());
		require(minimaleUeberdeckungFuerAttributAuszeichnung >= 0,
			"minimaleUeberdeckungFuerNetzklassenAuszeichnung %s >= 0", minimaleUeberdeckungFuerAttributAuszeichnung);

		this.minimaleUeberdeckungFuerAttributAuszeichnung = minimaleUeberdeckungFuerAttributAuszeichnung;
	}

	public PbfJobStatistik reicherePbfAn(File inputFile, File outputFile) throws IOException {
		PbfJobStatistik statistik = new PbfJobStatistik();

		log.info("Erstelle Map von OsmWayId auf Set<flacheKante>");
		Map<Long, Set<KanteOsmMatchWithAttribute>> osmWayToKanten = new HashMap<>();
		kantenRepository.getKanteOsmMatchesWithOsmAttributes(minimaleUeberdeckungFuerAttributAuszeichnung)
			.forEach(kanteMapping -> osmWayToKanten.computeIfAbsent(kanteMapping.getOsmWayId(),
				k -> new HashSet<>()).add(kanteMapping));
		log.info("Anzahl osmWayIds als keys in der Map: " + osmWayToKanten.keySet().size());

		File tempOutputFile = File.createTempFile(outputFile.getName(), "temp");
		log.info("Starte schreiben einer neuen osm pbf (erstmal in temp): " + tempOutputFile.getAbsoluteFile());
		try (InputStream input = new FileInputStream(inputFile);
			OutputStream output = new FileOutputStream(tempOutputFile)) {

			OsmIterator iterator = new PbfIterator(input, true);
			OsmOutputStream osmOutput = new PbfWriter(output, true);
			int i = 0;
			for (EntityContainer container : iterator) {
				switch (container.getType()) {
				default:
				case Node:
					statistik.anzahlNodes++;
					osmOutput.write((OsmNode) container.getEntity());
					break;
				case Way:
					statistik.anzahlWays++;
					Way osmWay = (Way) container.getEntity();

					if (osmWayToKanten.containsKey(osmWay.getId())) {
						// Es kann sein, dass es mehrere Kanten mit 0 bis 1 gibt, dann muessen wir uns eine raussuchen
						// -> hier koennte man auch nach bestimmten Kriterien die Kante raussuchen
						// aber fuers erste wird einfach mit findFirst eine beliebige genommen.
						KanteOsmMatchWithAttribute flattenedKante = osmWayToKanten.get(osmWay.getId()).stream()
							.findFirst()
							.get();

						// Wichtig:
						// Im CustomKantenRepositoryImpl wird bereits gefiltert welche Kanten relevant sind
						// und welche nicht. Ergänzt man ein neues Attribut zur Auszeichnung, muss man es
						// dort ebenfalls hinzufügen.
						String netzklassenString = flattenedKante.getNetzklassen().orElse("");
						tagNetzklassen(osmWay, netzklassenString, statistik);
						tagBelagart(osmWay, flattenedKante, statistik);

						String breiteValue = flattenedKante.getBreite()
							.map(breite -> String.format(Locale.ROOT, "%.2f", breite))
							.orElse("");
						addTag(osmWay, "width", breiteValue, "", statistik);
						addTag(osmWay, "cycleway", flattenedKante.getRadverkehrsfuehrung(),
							Radverkehrsfuehrung.UNBEKANNT.name(), statistik);
						addTag(osmWay, "surface:condition", flattenedKante.getOberflaechenbeschaffenheit(),
							Oberflaechenbeschaffenheit.UNBEKANNT.name(),
							statistik);
						addTag(osmWay, "status", flattenedKante.getStatus(), null, statistik);

						statistik.anzahlWaysTagged++;
					}

					osmOutput.write(osmWay);
					break;
				case Relation:
					statistik.anzahlRelations++;
					osmOutput.write((OsmRelation) container.getEntity());
					break;
				}

				i++;
				if (i % 1000000 == 0) {
					log.info("Es wurden bisher {} OsmEntities bearbeitet, davon {} Ways wovon {} angereichert wurden.",
						i,
						statistik.anzahlWays, statistik.anzahlWaysTagged);
				}
			}
			log.info("Es wurden insgesamt {} OsmEntities bearbeitet, davon {} Ways wovon {} angereichert wurden.", i,
				statistik.anzahlWays, statistik.anzahlWaysTagged);

			osmOutput.complete();
		}

		Files.move(
			Paths.get(tempOutputFile.getAbsolutePath()),
			Paths.get(outputFile.getAbsolutePath()),
			StandardCopyOption.REPLACE_EXISTING);

		return statistik;
	}

	private void tagNetzklassen(Way osmWay, String netzklassenString, PbfJobStatistik statistik) {
		if (netzklassenString.isEmpty()) {
			return;
		}

		Set<Netzklasse> netzklassen = Arrays
			.stream(netzklassenString.split(";"))
			.map(Netzklasse::valueOf)
			.filter(RELEVANTE_NETZKLASSEN::contains)
			.collect(Collectors.toSet());

		if (!netzklassen.isEmpty()) {
			addTag(osmWay, "netzklassen", netzklassen.stream().map(Enum::name).collect(Collectors.joining(";")),
				null, statistik);
		}
	}

	private void tagBelagart(Way osmWay, KanteOsmMatchWithAttribute flattenedKante, PbfJobStatistik statistik) {
		if (RELEVANTE_BELAGARTEN_TO_OSMVALUE.containsKey(flattenedKante.getBelagArt())) {
			addTag(osmWay, "surface", RELEVANTE_BELAGARTEN_TO_OSMVALUE.get(flattenedKante.getBelagArt()), null, statistik);
		}
	}

	private void addTag(Way osmWay, String keySuffix, String value, String ignoreValue, PbfJobStatistik statistik) {
		if (value.equals(ignoreValue)) {
			return;
		}

		List<OsmTag> tags = new ArrayList<>(osmWay.getTags());
		tags.add(new Tag("radvis:" + keySuffix, value));
		osmWay.setTags(tags);
	}
}
