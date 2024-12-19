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

package de.wps.radvis.backend.massnahme.domain;

import static org.locationtech.jts.geom.Geometry.TYPENAME_MULTILINESTRING;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.geotools.api.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.Geometry;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.dokument.domain.entity.DokumentListe;
import de.wps.radvis.backend.kommentar.domain.entity.Kommentar;
import de.wps.radvis.backend.kommentar.domain.entity.KommentarListe;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.massnahme.domain.entity.MassnahmeNetzBezug;
import de.wps.radvis.backend.massnahme.domain.entity.MassnahmenImportProtokoll;
import de.wps.radvis.backend.massnahme.domain.valueObject.Bezeichnung;
import de.wps.radvis.backend.massnahme.domain.valueObject.Durchfuehrungszeitraum;
import de.wps.radvis.backend.massnahme.domain.valueObject.Handlungsverantwortlicher;
import de.wps.radvis.backend.massnahme.domain.valueObject.Konzeptionsquelle;
import de.wps.radvis.backend.massnahme.domain.valueObject.MassnahmeKonzeptID;
import de.wps.radvis.backend.massnahme.domain.valueObject.MassnahmenOberkategorie;
import de.wps.radvis.backend.massnahme.domain.valueObject.MassnahmenPaketId;
import de.wps.radvis.backend.massnahme.domain.valueObject.Massnahmenkategorie;
import de.wps.radvis.backend.massnahme.domain.valueObject.Prioritaet;
import de.wps.radvis.backend.massnahme.domain.valueObject.StartOderZielMassnahme;
import de.wps.radvis.backend.massnahme.domain.valueObject.Umsetzungsstatus;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.KantenAttribute;
import de.wps.radvis.backend.netz.domain.valueObject.SollStandard;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenName;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import lombok.extern.slf4j.Slf4j;

/**
 * @deprecated RAD-6071: Das Zuständigkeitsfeld ist ein Pflichtfeld. Dieser MappingService ist deprecated, da er einen
 *     veralteten Import implementiert, der nicht mehr genutzt wird. Bis dieser MappingService entfernt wird,
 *     wird hier die Unbekannte Organisation als Zustaendig gesetzt.
 */
@Deprecated
@Slf4j
public class MassnahmenMappingService {

	private final VerwaltungseinheitService verwaltungseinheitService;

	private final MassnahmenAttributeMapper mapper;

	public MassnahmenMappingService(VerwaltungseinheitService verwaltungseinheitService) {
		this.verwaltungseinheitService = verwaltungseinheitService;
		this.mapper = new MassnahmenAttributeMapper();
	}

	public List<Massnahme> createMassnahmen(SimpleFeature simpleFeature, MassnahmeNetzBezug netzbezug,
		MassnahmenImportProtokoll massnahmenImportProtokoll, Benutzer benutzer) {
		Set<Massnahmenkategorie> startMassnahmenkategorien;

		Set<Massnahmenkategorie> zielMassnahmenkategorien;
		if (((Geometry) simpleFeature.getDefaultGeometry()).getGeometryType()
			.equals(TYPENAME_MULTILINESTRING)) {
			startMassnahmenkategorien = mapper.mapStreckenStartKategorien(simpleFeature.getProperties());
			zielMassnahmenkategorien = mapper.mapStreckenZielKategorien(simpleFeature.getProperties());
		} else {
			Massnahmenkategorie startKategorie = mapper.mapPunktStartKategorie(simpleFeature.getProperties());
			startMassnahmenkategorien = startKategorie != null ? Set.of(startKategorie) : Collections.emptySet();
			Massnahmenkategorie zielKategorie = mapper.mapPunktZielKategorie(simpleFeature.getProperties());
			zielMassnahmenkategorien = zielKategorie != null ? Set.of(zielKategorie) : Collections.emptySet();
		}

		boolean startMassnahmenValide = Massnahme.hatNurEineMassnahmenkategorieProOberkategorie(
			startMassnahmenkategorien);
		boolean zielMassnahmenValide = Massnahme.hatNurEineMassnahmenkategorieProOberkategorie(
			zielMassnahmenkategorien);

		if (!startMassnahmenValide) {
			log.error("Startmaßnahmenkategorien nicht valide: {}",
				getKategorienGruppiertNachOberkategorieString(startMassnahmenkategorien));
			throw new RuntimeException("Startmaßnahmenkategorien nicht valide");
		}

		if (!zielMassnahmenValide) {
			log.error("Zielmaßnahmenkategorien nicht valide: {}",
				getKategorienGruppiertNachOberkategorieString(zielMassnahmenkategorien));
			throw new RuntimeException("Zielmaßnahmenkategorien nicht valide");
		}

		List<Massnahme> result = new ArrayList<>();
		if (startMassnahmenkategorien.isEmpty() && zielMassnahmenkategorien.isEmpty()) {
			massnahmenImportProtokoll.reportMassnahmeOhneKategorie(simpleFeature);
			return Collections.emptyList();
		} else if (startMassnahmenkategorien.isEmpty()) {
			result.add(
				createMassnahme(simpleFeature, zielMassnahmenkategorien, StartOderZielMassnahme.ZIEL,
					netzbezug, benutzer));
		} else if (zielMassnahmenkategorien.isEmpty()) {
			result.add(
				createMassnahme(simpleFeature, startMassnahmenkategorien, StartOderZielMassnahme.START,
					netzbezug, benutzer));
		} else {
			if (startMassnahmenkategorien.equals(zielMassnahmenkategorien)) {
				massnahmenImportProtokoll.reportZielUndStartMassnahmeMitIdentischenKategorien(simpleFeature);
				return result;
			}
			result.add(
				createMassnahme(simpleFeature, startMassnahmenkategorien, StartOderZielMassnahme.START,
					netzbezug, benutzer));
			result.add(
				createMassnahme(simpleFeature, zielMassnahmenkategorien, StartOderZielMassnahme.ZIEL,
					netzbezug, benutzer));
		}

		return result;
	}

	private Massnahme createMassnahme(SimpleFeature simpleFeature,
		Set<Massnahmenkategorie> massnahmenkategorien,
		StartOderZielMassnahme startOderZielMassnahme, MassnahmeNetzBezug netzbezug, Benutzer benutzer) {
		Massnahme.MassnahmeBuilder builder = Massnahme.builder().kommentarListe(new KommentarListe());

		MassnahmenOberkategorie hoechsteOberkategorie = getHoechsteStreckenOberkategorie(massnahmenkategorien);

		Bezeichnung bezeichnung = createBezeichnung(netzbezug, hoechsteOberkategorie);

		Umsetzungsstatus umsetzungsstatus = mapper.mapUmsetzungsstatus(simpleFeature.getProperties());

		if (umsetzungsstatus != Umsetzungsstatus.IDEE) {
			builder.durchfuehrungszeitraum(Durchfuehrungszeitraum.of(2019));
		}

		Prioritaet prioritaet = startOderZielMassnahme == StartOderZielMassnahme.START
			? mapper.mapStartPrioritaet(simpleFeature.getProperties())
			: mapper.mapZielPrioritaet(simpleFeature.getProperties());

		SollStandard sollStandard = startOderZielMassnahme == StartOderZielMassnahme.START
			? SollStandard.STARTSTANDARD_RADNETZ
			: SollStandard.ZIELSTANDARD_RADNETZ;

		OrganisationsArt organisationsArt = mapper.mapOrganisationsArtFuerBaulastZustaendiger(
			simpleFeature.getProperties());

		Verwaltungseinheit baulastZustaendiger;
		Geometry originalRadNETZGeometrie = (Geometry) simpleFeature.getDefaultGeometry();
		if (organisationsArt == null) {
			baulastZustaendiger = verwaltungseinheitService.getUnbekannteOrganisation();
		} else {
			String baulastString = mapper.getBaulastString(simpleFeature.getProperties());
			if (baulastString.toLowerCase().contains("bund") && !baulastString.toLowerCase().contains("dritte")) {
				baulastZustaendiger = verwaltungseinheitService.getBundesrepublikDeutschland();
			} else if (organisationsArt == OrganisationsArt.SONSTIGES) {
				baulastZustaendiger = verwaltungseinheitService.getDritter();
			} else {
				baulastZustaendiger = getOrganisation(
					organisationsArt,
					netzbezug.getGeometrie());
			}
		}

		// RAD-6071: Das Zuständigkeitsfeld ist ein Pflichtfeld. Dieser MappingService ist deprecated, da er einen
		// veralteten Import implementiert, der nicht mehr genutzt wird. Bis dieser MappingService entfernt wird,
		// wird hier die Unbekannte Organisation als Zustaendig gesetzt.
		Verwaltungseinheit zustaendiger = verwaltungseinheitService.getUnbekannteOrganisation();

		String massnahmenPaketId = MassnahmenPaketIdExtractor.getMassnahmenPaketId(simpleFeature);

		List<String> kommentarTexte = mapper.mapKommentarTexte(simpleFeature.getProperties());
		List<Kommentar> kommentare = kommentarTexte.stream()
			.map(text -> new Kommentar(text, benutzer)).collect(
				Collectors.toList());
		KommentarListe kommentarListe = new KommentarListe(kommentare);

		return builder
			.massnahmenPaketId(MassnahmenPaketId.of(massnahmenPaketId))
			.massnahmeKonzeptId(MassnahmeKonzeptID.of(massnahmenPaketId))
			.originalRadNETZGeometrie(originalRadNETZGeometrie)
			.massnahmenkategorien(massnahmenkategorien)
			.umsetzungsstatus(umsetzungsstatus)
			.dokumentListe(new DokumentListe())
			.prioritaet(prioritaet)
			.sollStandard(sollStandard)
			.baulastZustaendiger(baulastZustaendiger)
			.zustaendiger(zustaendiger)
			.netzbezug(
				new MassnahmeNetzBezug(netzbezug.getImmutableKantenAbschnittBezug(),
					netzbezug.getImmutableKantenPunktBezug(),
					netzbezug.getImmutableKnotenBezug()))
			.netzklassen(mapper.mapNetzklassen(simpleFeature.getProperties()))
			.bezeichnung(bezeichnung)
			.benutzerLetzteAenderung(benutzer)
			.kommentarListe(kommentarListe)
			.letzteAenderung(LocalDateTime.now())
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.handlungsverantwortlicher(Handlungsverantwortlicher.BAULASTTRAEGER)
			.planungErforderlich(true)
			.veroeffentlicht(false)
			.build();
	}

	private Bezeichnung createBezeichnung(MassnahmeNetzBezug netzbezug, MassnahmenOberkategorie hoechsteOberkategorie) {
		final var strassenNamenString = getStrassenNamenString(netzbezug);
		if (strassenNamenString.isEmpty()) {
			return Bezeichnung.of(hoechsteOberkategorie.toString());
		}
		return Bezeichnung.of(MessageFormat.format("{0} ({1})", hoechsteOberkategorie, strassenNamenString));
	}

	// Wir betrachten für Streckenmassnahmen nur die Kanten aus dem Netzbezug
	private String getStrassenNamenString(MassnahmeNetzBezug netzbezug) {
		List<String> strassenNamen = netzbezug.getImmutableKantenAbschnittBezug().stream()
			.map(AbschnittsweiserKantenSeitenBezug::getKante)
			.map(Kante::getKantenAttributGruppe)
			.map(KantenAttributGruppe::getKantenAttribute)
			.map(KantenAttribute::getStrassenName)
			.filter(Optional::isPresent)
			.map(Optional::get)
			.map(StrassenName::getValue)
			.sorted()
			.distinct()
			.collect(Collectors.toList());

		return String.join(", ", strassenNamen);
	}

	private MassnahmenOberkategorie getHoechsteStreckenOberkategorie(Set<Massnahmenkategorie> massnahmenkategorien) {
		return massnahmenkategorien.stream().map(Massnahmenkategorie::getMassnahmenOberkategorie)
			.max(Comparator.comparing(this::getMassnahmenOberkategoriePrioritaet)).get();

	}

	private int getMassnahmenOberkategoriePrioritaet(MassnahmenOberkategorie massnahmenOberkategorie) {
		switch (massnahmenOberkategorie) {
		case NEUBAU_STRECKE:
			return 6;
		case AUSBAU_STRECKE:
			return 5;
		case MARKIERUNG:
			return 4;
		case BELAG:
			return 3;
		case STVO_BESCHILDERUNG:
			return 2;
		case HERSTELLUNG_RANDMARKIERUNG_BELEUCHTUNG:
		case HERSTELLUNG_ABSENKUNG:
		case ABSENKEN_VON_BORDEN:
		case SICHERUNG_RADWEGANFANG_ENDE:
		case FURTEN_ERNEUERN:
		case FURTEN_HERSTELLEN:
		case SONSTIGE_BAUMASSNAHME:
			return 1;
		default:
			return 0;
		}
	}

	private String getKategorienGruppiertNachOberkategorieString(Set<Massnahmenkategorie> zielMassnahmenkategorien) {
		return zielMassnahmenkategorien.stream()
			.collect(Collectors.groupingBy(Massnahmenkategorie::getMassnahmenOberkategorie))
			.entrySet()
			.stream()
			.filter(e -> e.getValue().size() > 1)
			.map(e -> e.getKey().name() + " / " + e.getValue().stream().map(Enum::name)
				.collect(Collectors.joining(";")))
			.collect(Collectors.joining(",%n "));
	}

	private Verwaltungseinheit getOrganisation(OrganisationsArt organisationsArt, Geometry geometry) {
		List<Verwaltungseinheit> organisationenByOrganisationsArtFuerGeometrie = verwaltungseinheitService
			.getOrganisationenByOrganisationsArtFuerGeometrie(
				organisationsArt, geometry);

		if (organisationenByOrganisationsArtFuerGeometrie.isEmpty()) {
			throw new RuntimeException(
				"Keine Organisation vorhanden mit Art '" + organisationsArt + "' fuer " + geometry.toString());
		}

		return organisationenByOrganisationsArtFuerGeometrie.get(0);
	}
}
