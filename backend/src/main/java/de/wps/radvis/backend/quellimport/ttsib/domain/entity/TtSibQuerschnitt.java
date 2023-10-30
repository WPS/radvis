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

package de.wps.radvis.backend.quellimport.ttsib.domain.entity;

import static org.valid4j.Assertive.require;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;

import de.wps.radvis.backend.quellimport.ttsib.domain.KeinMittelstreifenException;
import de.wps.radvis.backend.quellimport.ttsib.domain.valueObject.TtSibEinordnung;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Entity
@NoArgsConstructor
@Slf4j
public class TtSibQuerschnitt extends TtSibAbstractEntity {
	@Getter
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "tt_sib_querschnitt_id", nullable = false)
	private Set<TtSibStreifen> ttSibStreifen = new HashSet<>();

	public void addStreifen(TtSibStreifen neuerTtSibStreifen) {
		require(
			!neuerTtSibStreifen.getEinordnung().equals(TtSibEinordnung.MITTE)
				|| this.ttSibStreifen.stream().noneMatch(streifen -> streifen.getEinordnung().equals(
					TtSibEinordnung.MITTE)),
			"Es gibt bereits einen Streifen M");

		require(this.ttSibStreifen.stream().noneMatch(
			streifen -> streifen.getEinordnung().equals(neuerTtSibStreifen.getEinordnung()) && streifen.getNr()
				.equals(neuerTtSibStreifen.getNr())),
			String
				.format("Streifen mit TtSibEinordnung %s hat bereits die Nummer %d", neuerTtSibStreifen.getEinordnung(),
					neuerTtSibStreifen.getNr()));

		this.ttSibStreifen.add(neuerTtSibStreifen);
	}

	public Set<TtSibTeilabschnitt.Radwegstreifenversatz> ermittleStreifenversatze() throws KeinMittelstreifenException {
		// Versichere, dass die Sortierung im TreeSet der Streifennummerierung enspricht
		TtSibStreifen mittelstreifen = null;
		Comparator<TtSibStreifen> nachStreifennummerComparator = Comparator.comparing(TtSibStreifen::getNr);
		TreeSet<TtSibStreifen> linkeStreifenSet = new TreeSet<>(nachStreifennummerComparator);
		TreeSet<TtSibStreifen> rechteStreifenSet = new TreeSet<>(nachStreifennummerComparator);
		for (TtSibStreifen streifen : this.ttSibStreifen) {
			if (streifen.getEinordnung() == TtSibEinordnung.RECHTS) {
				rechteStreifenSet.add(streifen);
			} else if (streifen.getEinordnung() == TtSibEinordnung.LINKS) {
				linkeStreifenSet.add(streifen);
			} else if (streifen.getEinordnung() == TtSibEinordnung.MITTE) {
				mittelstreifen = streifen;
			}
		}
		if (mittelstreifen == null) {
			throw new KeinMittelstreifenException("Es wurde kein Mittelstreifen gefunden für Querschnitt " + id);
		}

		Set<TtSibTeilabschnitt.Radwegstreifenversatz> streifenversatze = new HashSet<>();
		int startversatz = mittelstreifen.getBreiteVon() / 2;
		int endversatz = mittelstreifen.getBreiteBis() / 2;
		streifenversatze.addAll(this.berechneStreifenversatzeProRichtung(linkeStreifenSet, startversatz, endversatz));
		streifenversatze.addAll(this.berechneStreifenversatzeProRichtung(rechteStreifenSet, startversatz, endversatz));

		// Sollte der Mittelstreifen der Radweg sein, ist der Abstand zur Mitte 0
		if (mittelstreifen.isRadwegStreifen()) {
			streifenversatze.add(new TtSibTeilabschnitt.Radwegstreifenversatz(0, 0, mittelstreifen.getEinordnung()));
		}

		return streifenversatze;
	}

	private Set<TtSibTeilabschnitt.Radwegstreifenversatz> berechneStreifenversatzeProRichtung(
		TreeSet<TtSibStreifen> streifenSet, int startversatz, int endversatz) {
		int streifenversatzStart = startversatz;
		int streifenversatzEnde = endversatz;

		Set<TtSibTeilabschnitt.Radwegstreifenversatz> streifenversatze = new HashSet<>();
		int i = 1;
		for (TtSibStreifen streifen : streifenSet) {
			if (streifen.getNr() != i) {
				log.warn("Keine lückenlose Nummerierung der Streifen vorhanden, Streifen-ID: " + streifen.getId()
					+ ". Ignoriere alle nachfolgenden Streifen für Querschnitt " + this.id);
				break;
			}

			if (streifen.isRadwegStreifen()) {
				// Die Linie soll genau auf der Mitte des Radstreifens dargestellt werden
				int abstandZurMitteInCmStart = streifenversatzStart + (streifen.getBreiteVon() / 2);
				int abstandZurMitteInCmEnde = streifenversatzEnde + (streifen.getBreiteBis() / 2);

				streifenversatze
					.add(new TtSibTeilabschnitt.Radwegstreifenversatz(abstandZurMitteInCmStart, abstandZurMitteInCmEnde,
						streifen.getEinordnung()));
			}

			// Auch weitere Streifen berücksichtigen, weil es eventuell noch weitere Rad-geignete-Streifen gibt
			if (streifen.getBreiteVon() != null && streifen.getBreiteBis() != null) {
				streifenversatzStart += streifen.getBreiteVon();
				streifenversatzEnde += streifen.getBreiteBis();
			}

			i++;
		}

		return streifenversatze;
	}

}
