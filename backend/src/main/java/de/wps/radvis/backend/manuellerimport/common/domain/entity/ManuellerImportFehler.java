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

package de.wps.radvis.backend.manuellerimport.common.domain.entity;

import static org.valid4j.Assertive.require;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.Point;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.common.domain.FrontendLinks;
import de.wps.radvis.backend.common.domain.entity.AbstractEntity;
import de.wps.radvis.backend.common.domain.entity.FehlerprotokollEintrag;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.ImportTyp;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.Konflikt;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.ManuellerImportFehlerursache;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Getter
public class ManuellerImportFehler extends AbstractEntity implements FehlerprotokollEintrag {
	@Getter(AccessLevel.NONE)
	@ManyToOne
	private Kante kante;
	@Getter(AccessLevel.NONE)
	private Geometry originalGeometrie;
	@Enumerated(EnumType.STRING)
	private ImportTyp importTyp;
	private LocalDateTime importZeitpunkt;
	@ManyToOne
	private Benutzer benutzer;
	@ManyToOne
	private Verwaltungseinheit organisation;
	@Enumerated(EnumType.STRING)
	private ManuellerImportFehlerursache fehlerursache;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "manueller_import_fehler_konflikt")
	private Set<Konflikt> konflikte;

	private Geometry iconPosition;

	private String titel;

	private String beschreibung;

	private String entityLink;

	private ManuellerImportFehler(Kante kante, Geometry originalGeometrie, ImportTyp importTyp,
		LocalDateTime importZeitpunkt,
		Benutzer benutzer, Verwaltungseinheit organisation, ManuellerImportFehlerursache fehlerursache,
		Set<Konflikt> konflikte) {
		require(kante != null || originalGeometrie != null, "Braucht geometrischen Bezug");
		require(konflikte == null || fehlerursache == ManuellerImportFehlerursache.ATTRIBUTE_NICHT_EINDEUTIG,
			"Konflikte können nur gesetzt werden, wenn auch eine Fehlerursache gesetzt ist.");
		this.kante = kante;
		this.originalGeometrie = originalGeometrie;
		this.importTyp = importTyp;
		this.importZeitpunkt = importZeitpunkt;
		this.benutzer = benutzer;
		this.organisation = organisation;
		this.fehlerursache = fehlerursache;
		this.konflikte = konflikte;

		this.iconPosition = kante != null ? kante.getGeometry().getCentroid() : originalGeometrie.getCentroid();
		this.titel = ManuellerImportFehler.generateTitel(importTyp);
		this.beschreibung = ManuellerImportFehler.generateBeschreibung(kante != null ? kante.getId() : null, konflikte,
			fehlerursache);
		this.entityLink = ManuellerImportFehler.generateEntityLink(kante != null ? kante.getId() : null);
	}

	/**
	 * Matchingfehler
	 *
	 * @param originalGeometrie
	 * @param importTyp
	 * @param importZeitpunkt
	 * @param benutzer
	 * @param organisation
	 */
	public ManuellerImportFehler(Geometry originalGeometrie, ImportTyp importTyp,
		LocalDateTime importZeitpunkt,
		Benutzer benutzer, Verwaltungseinheit organisation) {
		this(null, originalGeometrie, importTyp, importZeitpunkt, benutzer, organisation,
			ManuellerImportFehlerursache.KEIN_MATCHING, null);
	}

	/**
	 * Attribute nicht eindeutig
	 *
	 * @param kante
	 * @param importZeitpunkt
	 * @param benutzer
	 * @param organisation
	 * @param konflikte
	 */
	public ManuellerImportFehler(Kante kante,
		LocalDateTime importZeitpunkt,
		Benutzer benutzer, Verwaltungseinheit organisation,
		Set<Konflikt> konflikte) {
		this(kante, null, ImportTyp.ATTRIBUTE_UEBERNEHMEN, importZeitpunkt, benutzer, organisation,
			ManuellerImportFehlerursache.ATTRIBUTE_NICHT_EINDEUTIG, konflikte);
	}

	@Override
	public MultiPoint getIconPosition() {
		return new MultiPoint(new Point[] { (Point) iconPosition }, iconPosition.getFactory());
	}

	public Optional<Kante> getKante() {
		return Optional.ofNullable(kante);
	}

	public Optional<Set<Konflikt>> getKonflikte() {
		return Optional.ofNullable(konflikte);
	}

	public Optional<Geometry> getOriginalGeometrie() {
		return Optional.ofNullable(originalGeometrie);
	}

	/**
	 * Achtung: Wenn moeglich den Optional<Geometry> getter von originalGeometrie verwenden, da die originalGeometrie
	 * beim ManuellerImportFehler nullable ist.
	 */
	public Geometry getOriginalGeometry() {
		return this.originalGeometrie;
	}

	public LocalDateTime getDatum() {
		return this.getImportZeitpunkt();
	}

	public static String generateTitel(ImportTyp importTyp) {
		return String.format("Fehler beim manuellen Import (%s)",
			importTyp.equals(ImportTyp.ATTRIBUTE_UEBERNEHMEN) ? "Attribute" : "Netzklasse");
	}

	public static String generateBeschreibung(Long kanteid, Set<Konflikt> konflikte,
		ManuellerImportFehlerursache fehlerursache) {
		StringBuilder stringBuilder = new StringBuilder();
		if (fehlerursache.equals(ManuellerImportFehlerursache.KEIN_MATCHING)) {
			stringBuilder.append("Das Feature konnte nicht auf das Netz abgebildet werden.");
		} else {
			if (kanteid != null) {
				stringBuilder.append(String.format("Für die Kante %d ", kanteid));
			} else {
				stringBuilder.append("Für eine Kante ");
			}
			stringBuilder
				.append(String.format(
					"konnte(n) %d Attribut(e) aus der Shape nicht vollständig übernommen werden:\n",
					konflikte.size()));
			int i = 1;
			for (Konflikt konflikt : konflikte) {
				stringBuilder.append(i++).append(".\n");
				stringBuilder.append("Bemerkung: ").append(konflikt.getBemerkung()).append("\n");
				stringBuilder.append("Attributname: ").append(konflikt.getAttributName()).append("\n");
				stringBuilder.append("Übernommener Wert: ").append(konflikt.getUebernommenerWert()).append("\n");
				stringBuilder.append("Nicht übernommen: ")
					.append(String.join(", ", konflikt.getNichtUebernommeneWerte())).append("\n");
				stringBuilder.append("Seite: ").append(konflikt.getSeitenbezug()).append("\n");
				stringBuilder.append("Abschnitt: ").append(konflikt.getLinearReferenzierterAbschnitt().toString())
					.append("\n\n");
			}
		}
		return stringBuilder.toString();
	}

	public static String generateEntityLink(Long kanteId) {
		if (kanteId != null) {
			return FrontendLinks.kanteDetailView(kanteId);
		}

		return null;
	}

	public void removeDeletedKante(Geometry deletedKanteGeometry) {
		Long kanteId = kante.getId();
		this.kante = null;
		if (this.originalGeometrie == null) {
			this.originalGeometrie = deletedKanteGeometry;
		}
		this.beschreibung = "HINWEIS: Die betroffene Kante mit Id " + kanteId + " existiert nicht mehr!\n"
			+ this.beschreibung;
	}
}