/*
 * Copyright (c) 2024 WPS - Workplace Solutions GmbH
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

package de.wps.radvis.backend.integration.dlm.domain;

import static org.valid4j.Assertive.require;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.spatial.jts.EnvelopeAdapter;
import org.jetbrains.annotations.NotNull;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;

import de.wps.radvis.backend.common.domain.entity.FehlerprotokollEintrag;
import de.wps.radvis.backend.common.domain.service.FehlerprotokollService;
import de.wps.radvis.backend.common.domain.valueObject.FehlerprotokollTyp;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.integration.attributAbbildung.domain.KantenAttributeUebertragungService;
import de.wps.radvis.backend.integration.dlm.domain.entity.Attributluecke;
import de.wps.radvis.backend.integration.dlm.domain.entity.AttributlueckenSchliessenJobStatistik;
import de.wps.radvis.backend.integration.dlm.domain.entity.AttributlueckenSchliessenProblem;
import de.wps.radvis.backend.netz.domain.entity.FahrtrichtungAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KanteWithInitialStatesView;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
/**
 * Dieser Service dient zum Ermitteln und Schließen von Attributlücken.
 *
 * Eine Attributlücke ist eine Kante, die keinerlei Attribute hält - abgesehen von einigen, die in dieser Betrachtung
 * ignoriert werden, wie z.B. Straßenname oder DLM-ID. Diese Kanten gibt es auf natürliche Weise, aber entstehen auch
 * vereinzelt durch den DLM-Reimport.
 *
 * Die Idee beim Schließen von solchen Attributlücken: Ist am Anfang und Ende einer Lücke jeweils RadNETZ Alltag, dann
 * ist sehr wahrscheinlich auch innerhalb der Lücke RadNETZ Alltag und man kann diese Netzklasse an den Kanten der Lücke
 * setzen. Diese Argumentation gilt für fast alle Attribute und wird hier angewandt.
 *
 * Das Vorgehen ist wie folgt:
 *
 * Im ersten Schritt werden alle Lücken mittels einer Breitensuche ermittelt. Wir wissen nämlich sehr einfach, wo eine
 * Lücke anfängt (einfach da, wo eine nicht-attribuierte Kante an eine Kante mit Attributen grenzt), aber wir wissen
 * nicht, wo die Lücke aufhört. Das muss also zunächst ermittelt werden.
 *
 * Im zweiten Schritt werden Attribute der attribuierten Kanten aufgesammelt (siehe Details zu "Hilfskanten" weiter
 * unten). Die aufgesammelten Attribute werden dann auf die Lücken-Kanten übertragen, sollten die Attribute am Anfang
 * und Ende der Lücke gleich sein.
 *
 * Wichtig ist, dass pro Attribut geschaut wird. Ist also die Belagart bei allen Kanten unterschiedlich, aber die
 * Radverkehrsführung überall gleich, dann wird diese trotzdem übernommen, die Belagart aber nicht.
 */
public class AttributlueckenService implements FehlerprotokollService {

	private final KantenAttributeUebertragungService kantenAttributeUebertragungService;
	private final AttributlueckenSchliessenProblemRepository attributlueckenSchliessenProblemRepository;
	private final NetzService netzService;
	private final int maximaleLaengeInM;
	private final int maximaleLaengeInKanten;
	private final int maximaleAnzahlAdjazenterAttribuierterKanten;

	public AttributlueckenService(
		KantenAttributeUebertragungService kantenAttributeUebertragungService,
		AttributlueckenSchliessenProblemRepository attributlueckenSchliessenProblemRepository, NetzService netzService,
		int maximaleLaengeInM,
		int maximaleLaengeInKanten,
		int maximaleAnzahlAdjazenterAttribuierterKanten) {
		this.kantenAttributeUebertragungService = kantenAttributeUebertragungService;
		this.attributlueckenSchliessenProblemRepository = attributlueckenSchliessenProblemRepository;
		this.netzService = netzService;
		this.maximaleLaengeInM = maximaleLaengeInM;
		this.maximaleLaengeInKanten = maximaleLaengeInKanten;
		this.maximaleAnzahlAdjazenterAttribuierterKanten = maximaleAnzahlAdjazenterAttribuierterKanten;
	}

	/**
	 * Ermittelt für alle übergebenen Kanten die dazugehörigen Lücken.
	 *
	 * @param originalKnotenToKantenViewMap
	 *     Map von Knoten-ID zu Lücken-Kanten.
	 * @param knotenToAllKantenMap
	 *     Map von Knoten-ID zu Kante. Hier müssen alle Kanten enthalten sein, die es zu betrachten gibt (also
	 *     auch die Lücken-Kanten).
	 * @param statistik
	 *     Job Statistik.
	 * @param benutzer
	 *     Benutzer unter dem diese Aktion ausgeführt wird und ggf. Protokolleinträge erstellt werden.
	 * @return Eine Liste aller gefundenen Lücken.
	 */
	public @NotNull ArrayList<Attributluecke> ermittleLuecken(
		Map<Long, List<KanteWithInitialStatesView>> originalKnotenToKantenViewMap,
		Map<Long, List<Kante>> knotenToAllKantenMap,
		AttributlueckenSchliessenJobStatistik statistik) {
		// Die ermittleLuecken Methode hat einen Seiteneffekt, daher erstellen wir eine deep-copy der Map, um später die
		// unveränderte Map für die weitere Verarbeitung zu haben.
		Map<Long, List<KanteWithInitialStatesView>> knotenToKantenViewMap = originalKnotenToKantenViewMap.entrySet()
			.stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> new ArrayList<KanteWithInitialStatesView>(
				entry.getValue())));

		ArrayList<Attributluecke> attributluecken = new ArrayList<>();

		// Ein Lücken-End-Knoten ist ein Knoten, wo eine Lücke anfängt oder aufhört. Nach dem Motto "Alles hat ein
		// Ende, nur die Attributlücke hat zwei". Die IDs beziehen sich also auf Start- bzw. Endknoten im Lücken-Netz.
		// Nicht alle diese Knoten sind auch tatsächlich Anfänge/Enden von Lücken, manche werden beim Durchsuchen
		// gemäß gewisser Regeln ignoriert.
		List<Long> potentielleLueckenEndenKnotenIds = knotenToKantenViewMap.keySet()
			.stream()
			.filter(knotenId -> {
				Set<Long> lueckenKantenIds = knotenToKantenViewMap.get(knotenId).stream().map(k -> k.getKanteId())
					.collect(Collectors.toSet());
				long anzahlNormaleAdjazenteKanten = knotenToAllKantenMap.get(knotenId).stream().filter(
					k -> !lueckenKantenIds.contains(k.getId())).count();

				// An diesem Knoten hängen anzahlNormaleAdjazenteKanten viele normale Kanten (also Kanten, die keine
				// potentielle Lücke sind). Alle Knoten im Lücken-Netz, an denen auch andere Kanten hängen, sind
				// potentielle End-Knoten für Lücken.
				// Hier werden also auch Sackgassen-Knoten rausgefiltert, also Knoten an denen das Netz einfach vorbei
				// ist. Diese "Lücken" sind für uns uninteressant, da sie keine Lücken in unserem Verständnis
				// darstellen.
				return anzahlNormaleAdjazenteKanten > 0;
			})
			.toList();

		statistik.anzahlPotentielleLueckenEndKnoten = potentielleLueckenEndenKnotenIds.size();

		// Alle potentielle Start-Knoten durchgehen und versuchen einen passenden End-Knoten zu finden. Hat man einen
		// solchen Knoten gefunden, hat man eine zusammenhängende Lücke ermittelt.
		for (Long lueckeStartKnotenId : potentielleLueckenEndenKnotenIds) {
			// Es kann sein, dass der Knoten mittlerweile entfernt wurde, weil eine andere Lücke, die hier endet,
			// bereits
			// verarbeitet wurde. Also können wir den Knoten hier ignorieren um zu vermeiden, dass wir Lücken doppelt
			// finden (einmal in hin- und rück-Richtung) oder Lücken finden, die sich berühren (z.B. an T-Kreuzungen).
			if (!knotenToKantenViewMap.containsKey(lueckeStartKnotenId)) {
				continue;
			}

			log.trace("Ermittle Lücke von Knoten {} aus", lueckeStartKnotenId);
			List<KanteWithInitialStatesView> lueckeKantenViewsPfad = ermittleLueckenPfad(lueckeStartKnotenId,
				knotenToKantenViewMap, statistik);

			if (lueckeKantenViewsPfad.isEmpty()) {
				log.trace("Zu Knoten {} konnte kein Lücken-Ende ermittelt werden", lueckeStartKnotenId);
				continue;
			}

			Long lueckeEndKnotenId = getEndKnotenVonPfad(lueckeStartKnotenId, lueckeKantenViewsPfad);

			log.trace("Lücke gefunden von Knoten {} nach {} mit {} Kanten und {} m Länge",
				lueckeStartKnotenId,
				lueckeEndKnotenId,
				lueckeKantenViewsPfad.size(),
				lueckeKantenViewsPfad.stream().map(k -> k.getKantenLaengeInCm()).reduce(9, Integer::sum) / 100.0);

			List<Kante> adjazenteKantenAmStart = knotenToAllKantenMap.get(lueckeStartKnotenId);
			List<Kante> adjazenteKantenAmEnde = knotenToAllKantenMap.get(lueckeEndKnotenId);

			List<Kante> lueckeKantenPfad = lueckeKantenViewsPfad.stream()
				.map(kanteView -> knotenToAllKantenMap.get(kanteView.getVonKnotenId())
					.stream()
					.filter(k -> k.getId().equals(kanteView.getKanteId()))
					.findFirst()
					.get())
				.toList();

			// Lücken, die einen Kreis bilden, wollen wir ignorieren. Das hat mit der Übernahme von Attributen zu tun,
			// dass hier leicht ungewollte Attribute übernommen werden. Gerade bei zweiseitigen Kanten ist es sowieso
			// nicht möglich korrekt seitenbezogen Attribute zu übernehmen. Dieser Fall tritt z.B. bei Wendekreisen ein,
			// aber auch in Wohngebieten oder Kreuzungen.
			boolean lueckeVerbindetEndenGleicherKante = adjazenteKantenAmStart.stream()
				.anyMatch(k -> {
					boolean kanteIstAuchAmLueckenEndeAdjazent = adjazenteKantenAmEnde.contains(k);

					// Es reicht für einen Knoten (von- oder nach-Knoten) zu prüfen, ob die Kante eine potentielle
					// Lücken-Kante ist oder nicht. Für den jeweils anderen Knoten bekäme man das gleiche Ergebnis, da
					// die Map der Kanten Einträge für beide Knoten enthält.
					boolean kanteIstPotentielleLuecke = knotenToKantenViewMap.containsKey(k.getVonKnoten().getId()) &&
						knotenToKantenViewMap.get(k.getVonKnoten().getId())
							.stream()
							.anyMatch(kView -> kView.getKanteId() == k.getId());
					boolean kanteIstAttribuiert = !kanteIstPotentielleLuecke;

					// Ein Kreis bildet sich, wenn wir am Anfang und Ende die gleiche Kante haben UND sie Attribute
					// trägt. Nicht attribuierte Kanten werden in der Attributübernahme nicht betrachtet, sind daher
					// für diese Überprüfung ebenfalls irrelevant.
					return kanteIstAuchAmLueckenEndeAdjazent && kanteIstAttribuiert;
				});

			// Pfad aus Map entfernen, um sicherzustellen, dass diese Kanten nicht nochmal genommen werden. Ansonsten
			// besteht die Gefahr, dass wir die Lücke von der Gegenseite nochmal finden oder auch bei der Attribut-
			// übernahme die Attribute einer Kante zweimal schreiben und so fachliche Inkonsistenzen entstehen könnten.
			lueckeKantenPfad.forEach(kante -> removeKanteFromMap(knotenToKantenViewMap, kante));

			if (lueckeVerbindetEndenGleicherKante) {
				statistik.anzahlLueckenIgnoriertDaGleicheKanteVerbunden += 1;
				erstelleProblemProtokolleintrag(lueckeStartKnotenId);
				continue;
			}

			log.trace("Ermittle Start- und Endknoten");
			// Wir suchen explizit in den Kanten, die adjazent zum Start/Ende der Lücke sind, also MÜSSEN diese den
			// jeweiligen Knoten enthalten. Wenn nicht, schmeißen wir eine Exception
			Knoten lueckeStartKnoten = adjazenteKantenAmStart.stream()
				.map(k -> k.getVonKnoten().getId().equals(lueckeStartKnotenId) ? k.getVonKnoten() : k.getNachKnoten())
				.findFirst()
				.orElseThrow(() -> new RuntimeException("Start-Knoten " + lueckeStartKnotenId
					+ " konnte in Kanten, die an diesem Knoten anliegen, nicht gefunden werden!"));
			Knoten lueckeEndKnoten = adjazenteKantenAmEnde.stream()
				.map(k -> k.getVonKnoten().getId().equals(lueckeEndKnotenId) ? k.getVonKnoten() : k.getNachKnoten())
				.findFirst()
				.orElseThrow(() -> new RuntimeException("End-Knoten " + lueckeEndKnotenId
					+ " konnte in Kanten, die an diesem Knoten anliegen, nicht gefunden werden!"));

			attributluecken.add(
				new Attributluecke(
					lueckeStartKnoten,
					lueckeEndKnoten,
					lueckeKantenPfad,
					adjazenteKantenAmStart,
					adjazenteKantenAmEnde));

			if (attributluecken.size() % 1000 == 0) {
				log.debug("{} Lücken bisher ermittelt", attributluecken.size());
			}
		}

		// Alle Lücken entfernen, die sich berühren, also einen gemeinsamen Knoten haben. Wäre die Suche nach Lücken von
		// diesem gemeinsamen Knoten aus gestartet worden, wäre das Ergebnis nicht eindeutig gewesen und solche Lücken
		// wären gar nicht erst erzeugt worden. Startet die Suche aber in den entgegengesetzten Knoten, ist es in der
		// Breitensuche durchaus möglich solche Lücken zu finden, die sich berühren und somit nicht eindeutig sind.
		// Daher müssen wir diese nachträglich bereinigen.
		entferneSichBeruehrendeLuecken(attributluecken, statistik);

		statistik.anzahlLueckenErmittelt = attributluecken.size();
		return attributluecken;
	}

	/**
	 * Überträgt Attribute auf die übergebenen Lücken. Die Kanten, von denen Attribute übertragen werden sollen, müssen
	 * passend in den Objekten der Lücken vorhanden sein.
	 *
	 * Es wird dabei pro Attribut überprüft, ob dieses an allen adjazenten Start- und End-Kanten der Lücke gleich sind.
	 * Daher kann es sein, dass bspw. die Belagart übertragen wird, die Radverkehrsführung aber nicht.
	 */
	public void schliesseLuecken(List<Attributluecke> attributluecken,
		Map<Long, List<KanteWithInitialStatesView>> knotenToKantenViewMap) {

		int anzahlLueckenGeschlossen = 0;

		for (Attributluecke luecke : attributluecken) {
			// Von den adjazenten Kanten am Start/Ende des Lückenpfades wollen wir nur diejenigen adjazenten Kanten
			// haben, die attribuiert sind, von denen also Attribute übernommen werden könnten. Alle anderen nicht-
			// attribuierten Kanten sind für die Attributübernahme irrelevant und können ignoriert werden.
			List<Kante> adjazenteKantenAmStart = getRelevanteKantenFuerAttributuebernahme(
				luecke.getAdjazenteKantenAmStart(), knotenToKantenViewMap);
			List<Kante> adjazenteKantenAmEnde = getRelevanteKantenFuerAttributuebernahme(
				luecke.getAdjazenteKantenAmEnde(), knotenToKantenViewMap);

			/*
			 * Die Idee der Hilfskanten:
			 * 
			 * Wir müssen von den beiden Enden der Lücke jeweils die Attribute aufsammeln, die an der Lücke ankommen und
			 * nach der Lücke weiter gehen. Also wenn am Start-Knoten der Lücke alle Kanten mit "Asphalt" als Belag
			 * ankommen und alle Kanten hinter dem End-Knoten der Lücke mit "Asphalt" weiter gehen, müssen wir genau das
			 * herausfinden und dann "Asphalt" schreiben. Unterscheiden sich die Attribute können wir nichts übernehmen.
			 * 
			 * Weil Attribute aber teilw. Seitenbezogen und linear referenziert sind, bauen wir hier zwei Hilfskanten,
			 * um die Attribute aus Sicht vom Start-/End-Knoten auf diese Hilfskanten zu schreiben. Die Hilfskante AB
			 * führt vom Start- zum End-Knoten der Lücke und erhält alle Attribute, die bei allen ankommenden Kanten am
			 * Start- Knoten gleich sind. Hilfskante BA führt vom End- zum Start-Knoten der Lücke und erhält
			 * entsprechend alle gleichen Attribute der Kanten vom Ende der Lücke.
			 * 
			 * Danach schauen wir dann, welcher Attributwert innerhalb der Lücke auf welcher Seite gilt. Hierbei ist
			 * wichtig die Stationierungsrichtungen der Lücken-Kanten zu beachten, da diese nicht immer vom Start zum
			 * Ende der Lücke zeigen können, sondern auch umgekehrt oder gemischt sein können.
			 * 
			 * Wir haben mit unseren Hilfskanten also zwei Kanten, die definitiv zueinander entgegengesetzt verlaufen
			 * (also rechte Seite der AB-Kante ist linke Seite der BA-Kante usw.). Man kann dann also schauen welche
			 * Attribute auf den entgegengesetzten Seiten gleich sind und diese übernehmen. Mehr Details dazu in der
			 * entsprechenden Methode zur Attributübernahme.
			 * 
			 * Ohne Hilfskanten müsste man quasi eine Art kartesisches Produkt aus allen ankommenden und abgehenden
			 * Kanten bilden. Das wäre der komplex und sehr aufwändig, da man lineare Referenzierung,
			 * Stationierungsrichtung, Seitenbezug und Besonderheiten bei manchen Attributgruppen (z.B.
			 * GeschwindigkeitsAttributgruppe) beachten müsste. Mit den Hilfskanten hat man zwar ein doppeltes Mapping
			 * (Kanten -> Hilfskanten und Hilfskanten -> Lücken-Kanten), aber es vereinfacht ein wenig den Code.
			 */
			log.trace("Erzeuge beide Hilfskanten zwischen Knoten {} und {}", luecke.getStartKnoten(), luecke
				.getEndKnoten());
			Kante hilfskanteAB = createHilfskante(luecke.getStartKnoten(), luecke.getEndKnoten());
			Kante hilfskanteBA = createHilfskante(luecke.getEndKnoten(), luecke.getStartKnoten());

			log.trace("Übertrage gleiche Attribute auf Hilfskanten");
			kantenAttributeUebertragungService.uebertrageGleicheAttribute(adjazenteKantenAmStart, luecke
				.getStartKnoten(), hilfskanteAB, false);
			kantenAttributeUebertragungService.uebertrageGleicheAttribute(adjazenteKantenAmEnde, luecke
				.getEndKnoten(), hilfskanteBA, false);

			log.trace("Übertrage Attribute von Hilfskanten auf Lücken-Kanten");
			uebertrageAttributeVonHilfskanten(luecke, hilfskanteAB, hilfskanteBA);

			log.trace("Lücke geschlossen");
			anzahlLueckenGeschlossen++;
			if (anzahlLueckenGeschlossen % 10000 == 0) {
				log.debug("{} Lücken bisher geschlossen", anzahlLueckenGeschlossen);
			}
		}
	}

	/**
	 * Filtert die adjazenten Kanten danach, welche davon für eine Attributübernahme relevant sind. Das sind nämlich nur
	 * die Kanten mit Attributen.
	 */
	private @NotNull List<Kante> getRelevanteKantenFuerAttributuebernahme(List<Kante> adjazenteKanten,
		Map<Long, List<KanteWithInitialStatesView>> knotenToKantenViewMap) {
		return adjazenteKanten
			.stream()
			.filter(kante -> {
				// Nur Kanten mit Attribute betrachten. Heißt: Nur kanten, die _nicht_ in der Map von potentiellen
				// Lücken-Kanten enthalten sind.
				return knotenToKantenViewMap.getOrDefault(kante.getVonKnoten().getId(),
					Collections.emptyList())
					.stream()
					.noneMatch(k -> k.getKanteId() == kante.getId());
			})
			.toList();
	}

	/**
	 * Versucht das Ende einer Lücke zu finden, die beim angegebenen Knoten startet. Hierbei wird eine Breitensuche
	 * benutzt um einen Pfad vom gegebenen Start zum Ende der Lücke zu finden.
	 *
	 * @return Liste von Kanten oder null, wenn kein Pfad gefunden wurde. Die erste Kante grenzt an den übergebenen
	 *     Start-Knoten, die letzte Kante entsprechend an den gefundenen End-Knoten.
	 */
	private List<KanteWithInitialStatesView> ermittleLueckenPfad(Long startKnoten,
		Map<Long, List<KanteWithInitialStatesView>> knotenToKanteMap,
		AttributlueckenSchliessenJobStatistik statistik) {

		// Der angegebene Start-Knoten darf keine echte Sackgasse sein, da er als Sackgasse kein Anfang einer echten
		// Lücke zwischen zwei Kanten sein kann. Es handelt sich bei solchen Sackgassen also nicht um echte Lücken im
		// Netz, sondern um Endstücke, die wir hier nicht betrachten wollen.
		int knotenGradImGesamtnetz = getKnotenGrad(knotenToKanteMap, startKnoten);
		int knotenGradImLueckennetz = knotenToKanteMap.get(startKnoten).size();
		require(knotenGradImGesamtnetz > 1 || knotenGradImLueckennetz > 1);

		if (knotenGradImGesamtnetz > knotenGradImLueckennetz + maximaleAnzahlAdjazenterAttribuierterKanten) {
			// Hier gehen zu viele normale Kanten ab. Damit ist das kein hinreichend eindeutiges Ende einer Lücke.
			// Um keine falschen Lückenschlüsse zu erzeugen (z.B. Wege zwischen vollständig attribuierten größeren
			// Straßen) brechen wir hier aber ab.
			statistik.anzahlPotentielleLueckenIgnoriertDaKeineEindeutigeEndkante++;
			// Keine Protokollierung, wenn nicht explizit gewünscht. Ansonsten läuft die Datenbank noch schneller voll
			// mit Daten, die wahrscheinlich keiner anschauen wird. Von den "mehrdeutige Endkante" wird es nämlich
			// pro Job-Durchlauf mehrere hunderttausend geben.
			return Collections.emptyList();
		}

		// Queue als FIFO-Queue nutzen: Der zuerst eingefügte Pfad (Liste von Kanten-Views), also mit der längsten
		// Wartezeit in der Queue, ist der, der als erster wieder entnommen wird. Oder anders: Vorne entnehmen,
		// hinten einfügen.
		Deque<List<KanteWithInitialStatesView>> queue = new LinkedList<>();

		// Alle potentiellen Lücken-Pfade
		List<List<KanteWithInitialStatesView>> kantenPfade = new ArrayList<>();
		List<List<KanteWithInitialStatesView>> zuLangeKantenPfade = new ArrayList<>();
		List<List<KanteWithInitialStatesView>> kantenPfadeMitZuVielenKanten = new ArrayList<>();

		// Potentielle End-Knoten einer Lücke.
		final Set<Long> endKnoten = new HashSet<>();

		// Füge erste Nachfolge-Knoten vom Start-Knoten in Queue ein, da diese zuerst besucht werden sollen.
		knotenToKanteMap.get(startKnoten)
			.forEach(kante -> {
				Long successorKnotenId = kante.getOtherKnoten(startKnoten);
				List<KanteWithInitialStatesView> pfad = List.of(kante);

				// Ermittle Länge vom Start-Knoten zum Nachfolgeknoten.
				double laengePfadInM = getShortestKanteBetweenKnoten(knotenToKanteMap.get(startKnoten),
					startKnoten, successorKnotenId).getKantenLaengeInCm() / 100.0;

				// Der Pfad enthält hier nur eine Kante, daher schauen wir nur auf die Länge in Metern.
				if (laengePfadInM > maximaleLaengeInM) {
					zuLangeKantenPfade.add(pfad);
				} else {
					kantenPfade.add(pfad);
					queue.addLast(pfad);
				}
			});

		while (!queue.isEmpty()) {
			List<KanteWithInitialStatesView> pfad = queue.removeFirst();

			// Finde den letzten Knoten des Pfades, also den Knoten, hinter dem wir nach weiteren Kanten für diese
			// Lücke suchen wollen.
			Long knoten = getEndKnotenVonPfad(startKnoten, pfad);

			knotenGradImGesamtnetz = getKnotenGrad(knotenToKanteMap, knoten);
			knotenGradImLueckennetz = knotenToKanteMap.get(knoten).size();

			if (knotenGradImGesamtnetz == 1) {
				// Tatsächliche Sackgasse, hier geht es also nicht weiter. Dies stellt also keine Lücke zwischen
				// Kanten dar.
				statistik.anzahlPotentielleLueckenIgnoriertDaSackgasse++;
				erstelleProblemProtokolleintrag(startKnoten);
				continue;
			}

			require(knotenGradImGesamtnetz >= 2);

			if (knotenGradImGesamtnetz > knotenGradImLueckennetz + maximaleAnzahlAdjazenterAttribuierterKanten) {
				// Hier gehen zu viele normale Kanten ab. Damit ist das kein hinreichend eindeutiges Ende einer Lücke.
				// Um keine falschen Lückenschlüsse zu erzeugen (z.B. Wege zwischen vollständig attribuierten größeren
				// Straßen) brechen wir hier aber ab.
				statistik.anzahlPotentielleLueckenIgnoriertDaKeineEindeutigeEndkante++;
				// Keine Protokollierung, wenn nicht explizit gewünscht. Ansonsten läuft die Datenbank noch schneller
				// voll
				// mit Daten, die wahrscheinlich keiner anschauen wird. Von den "mehrdeutige Endkante" wird es nämlich
				// pro Job-Durchlauf mehrere hunderttausend geben.
				continue;
			} else if (knotenGradImGesamtnetz >= knotenGradImLueckennetz + 1) {
				// Lücke ist zu Ende, weil an diesem Knoten eine tolerable Anzahl normaler Kanten grenzt von denen ggf.
				// Attribute übernommen werden können.
				endKnoten.add(knoten);
				continue;
			}

			// Ermittle alle Folgekanten, die wir noch abarbeiten müssen. Dabei gibt es Regeln, welche Kanten wir nicht
			// mehr nehmen wollen, weil z.B. der Pfad sonst zu lang wird.
			knotenToKanteMap.get(knoten)
				.forEach(kante -> {
					Long successorKnotenId = kante.getOtherKnoten(knoten);

					// Kanten zu Knoten im aktuellen Pfad ignorieren wir, um Schleifen zu vermeiden.
					if (pfad.stream().anyMatch(
						k -> k.getVonKnotenId() == successorKnotenId || k.getNachKnotenId() == successorKnotenId)) {
						return;
					}

					// Ermittle Länge vom Start-Knoten über den aktuellen Pfad und "kante" zum Nachfolgeknoten.
					double pfadLaengeZumAktuellenKnotenInM = pfad.stream().mapToInt(k -> k.getKantenLaengeInCm()).sum()
						/ 100.0;
					double kantenLaengeZumNachfolgerInM = kante.getKantenLaengeInCm() / 100.0;
					double laengeNeuerPfadInM = pfadLaengeZumAktuellenKnotenInM + kantenLaengeZumNachfolgerInM;

					ArrayList<KanteWithInitialStatesView> neuerPfad = new ArrayList<>(pfad);
					neuerPfad.add(kante);

					if (laengeNeuerPfadInM > maximaleLaengeInM) {
						zuLangeKantenPfade.add(neuerPfad);
					} else if (neuerPfad.size() > maximaleLaengeInKanten) {
						kantenPfadeMitZuVielenKanten.add(neuerPfad);
					} else {

						kantenPfade.add(neuerPfad);
						queue.addLast(neuerPfad);
					}
				});
		}

		// Entfernt alle Pfade, die nicht an einem End-Knoten aufhören. Hiernach haben wir also nur noch Pfade, die
		// wirklich eine Lücke schließen können.
		kantenPfade.removeIf(pfad -> !endKnoten.contains(pfad.get(pfad.size() - 1).getVonKnotenId()) &&
			!endKnoten.contains(pfad.get(pfad.size() - 1).getNachKnotenId()));

		// Erstellt Protokolleinträge für die Fälle, dass die Lücke einfach zu groß war. Das ist aber nur relevant, wenn
		// tatsächlich kein normaler Pfad gefunden wurde. Haben wir einen Pfad gefunden, ist es uns egal, dass andere
		// Alternativpfade ignoriert werden, weil sie zu lang sind. Solche zu langen Alternativpfade gibt es häufiger
		// mal, wenn diese über nahegelegene nicht attribuierte Wohnstraßen oder Wald-und-Wiesen-Wege führen.
		if (kantenPfade.size() == 0) {
			if (zuLangeKantenPfade.size() > 0) {
				statistik.anzahlPotentielleLueckenIgnoriertDaAllePfadeZuLang++;
			} else if (kantenPfadeMitZuVielenKanten.size() > 0) {
				statistik.anzahlPotentielleLueckenIgnoriertDaZuVieleKantenNoetig++;
			} else {
				statistik.anzahlPotentielleLueckenIgnoriertSonstigerGrund++;
			}
			erstelleProblemProtokolleintrag(startKnoten);
			return Collections.emptyList();
		} else if (kantenPfade.size() > 1) {
			statistik.anzahlLueckenIgnoriertDaMehrdeutig++;
			erstelleProblemProtokolleintrag(startKnoten);
			return Collections.emptyList();
		}

		require(kantenPfade.size() == 1);
		return kantenPfade.get(0);
	}

	private void erstelleProblemProtokolleintrag(Long anfangKnotenDerLuecke) {
		attributlueckenSchliessenProblemRepository.save(
			new AttributlueckenSchliessenProblem(LocalDateTime.now(), netzService.getKnoten(anfangKnotenDerLuecke)));
	}

	/**
	 * Ermittelt für die übergebene Liste an Kanten und einem Start-Knoten den dazugehörigen End-Knoten. Voraussetzung
	 * ist, dass die Kanten einen geschlossenen Kantenzug bilden.
	 */
	private long getEndKnotenVonPfad(long startKnoten, List<KanteWithInitialStatesView> pfad) {
		if (pfad.size() == 1) {
			return pfad.get(0).getOtherKnoten(startKnoten);
		}

		// Ein Knoten der letzten Kante ist mit der vorletzten Kante verbunden, der andere nicht und ist damit
		// der letzte Knoten des Pfades.
		// ...--1----2----3 -> Knoten 2 wäre letzter gemeinsamer Knoten, 3 also der entgegengesetzte auf der
		// letzten Kante und den, den wir hier haben wollen.
		KanteWithInitialStatesView lastKante = pfad.get(pfad.size() - 1);
		KanteWithInitialStatesView secondLastKante = pfad.get(pfad.size() - 2);
		long lueckeEndKnotenId = secondLastKante.containsKnoten(lastKante.getVonKnotenId())
			? lastKante.getNachKnotenId()
			: lastKante.getVonKnotenId();

		return lueckeEndKnotenId;
	}

	/**
	 * Ermittelt die kürzeste Kante zwischen den beiden Knoten. Üblicherweise gibt es sowieso nur eine Kante, es kann
	 * aber auch mehrere zwischen zwei Knoten geben und dann wird die kürzere von beiden zurückgegeben.
	 *
	 * Diese Methode geht davon aus, dass es mind. eine kante zwischen den beiden Knoten gibt.
	 */
	private KanteWithInitialStatesView getShortestKanteBetweenKnoten(List<KanteWithInitialStatesView> kanten,
		Long vonKnoten, Long nachKnoten) {
		return kanten.stream()
			.filter(k -> k.getVonKnotenId() == vonKnoten && k.getNachKnotenId() == nachKnoten ||
				k.getVonKnotenId() == nachKnoten && k.getNachKnotenId() == vonKnoten)
			.min(Comparator.comparingInt(KanteWithInitialStatesView::getKantenLaengeInCm))
			.orElseThrow();
	}

	private int getKnotenGrad(Map<Long, List<KanteWithInitialStatesView>> knotenToKantenMap, Long knotenId) {
		KanteWithInitialStatesView kante = knotenToKantenMap.get(knotenId).get(0);
		return kante.getVonKnotenId() == knotenId ? kante.getVonKnotenGrad() : kante.getNachKnotenGrad();
	}

	/**
	 * Entfernt alle Lücken, die einen gemeinsamen Knoten mit einer anderen Lücke haben, diese also berühren.
	 */
	private void entferneSichBeruehrendeLuecken(List<Attributluecke> attributluecken,
		AttributlueckenSchliessenJobStatistik statistik) {

		HashMap<Long, List<Attributluecke>> knotenToLueckenMap = new HashMap<>();
		attributluecken.forEach(l -> {
			if (!knotenToLueckenMap.containsKey(l.getStartKnoten().getId())) {
				knotenToLueckenMap.put(l.getStartKnoten().getId(), new ArrayList<>());
			}
			knotenToLueckenMap.get(l.getStartKnoten().getId()).add(l);

			if (!knotenToLueckenMap.containsKey(l.getEndKnoten().getId())) {
				knotenToLueckenMap.put(l.getEndKnoten().getId(), new ArrayList<>());
			}
			knotenToLueckenMap.get(l.getEndKnoten().getId()).add(l);
		});

		Set<Long> allKnotenIds = new HashSet<>(knotenToLueckenMap.keySet());
		for (Long knotenId : allKnotenIds) {
			List<Attributluecke> luecken = knotenToLueckenMap.get(knotenId);

			if (luecken.size() == 1) {
				continue;
			}

			attributluecken.removeAll(luecken);
			statistik.anzahlLueckenIgnoriertDaGemeinsamerKnoten += luecken.size();
			attributlueckenSchliessenProblemRepository
				.save(new AttributlueckenSchliessenProblem(LocalDateTime.now(), netzService.getKnoten(knotenId)));
		}
	}

	private Kante createHilfskante(Knoten lueckeStartKnoten, Knoten lueckeEndKnoten) {
		return Kante.builder()
			.vonKnoten(lueckeStartKnoten)
			.nachKnoten(lueckeEndKnoten)
			.isZweiseitig(true)
			.dlmId(null)
			.quelle(QuellSystem.RadVis)
			.ursprungsfeatureTechnischeID("hilfskante")
			.geometry(KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createLineString(
				new Coordinate[] { lueckeStartKnoten.getPoint().getCoordinate(),
					lueckeEndKnoten.getPoint().getCoordinate() }))
			.geschwindigkeitAttributGruppe(GeschwindigkeitAttributGruppe.builder().build())
			.fuehrungsformAttributGruppe(FuehrungsformAttributGruppe.builder().isZweiseitig(true).build())
			.fahrtrichtungAttributGruppe(FahrtrichtungAttributGruppe.builder().isZweiseitig(true).build())
			.zustaendigkeitAttributGruppe(ZustaendigkeitAttributGruppe.builder().build())
			.kantenAttributGruppe(KantenAttributGruppe.builder().build())
			.build();
	}

	private void uebertrageAttributeVonHilfskanten(Attributluecke luecke, Kante hilfskanteAB, Kante hilfskanteBA) {
		Knoten lueckeStartKnoten = Knoten.builder()
			.id(-1L)
			.point(KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createPoint(new Coordinate(1, 1)))
			.build();
		Knoten lueckeEndKnoten = Knoten.builder()
			.id(-2L)
			.point(KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createPoint(new Coordinate(2, 2)))
			.build();

		hilfskanteAB.updateTopologie(lueckeStartKnoten, lueckeEndKnoten);
		hilfskanteBA.updateTopologie(lueckeEndKnoten, lueckeStartKnoten);

		Map<Kante, Boolean> kanteToLueckenRichtung = getKantenOrientierungInLuecke(luecke);
		for (Kante kante : luecke.getLueckeKantenPfad()) {
			if (kanteToLueckenRichtung.get(kante)) {
				hilfskanteAB.updateTopologie(lueckeStartKnoten, kante.getVonKnoten());
				hilfskanteBA.updateTopologie(kante.getVonKnoten(), lueckeStartKnoten);
			} else {
				hilfskanteAB.updateTopologie(kante.getVonKnoten(), lueckeEndKnoten);
				hilfskanteBA.updateTopologie(lueckeEndKnoten, kante.getVonKnoten());
			}

			kante.changeSeitenbezug(true);

			kantenAttributeUebertragungService.uebertrageGleicheAttribute(
				List.of(hilfskanteAB, hilfskanteBA),
				kante.getVonKnoten(),
				kante,
				true);

			boolean beideFuehrungsformSeitenGleich = FuehrungsformAttributGruppe.isSeitenBezugValid(
				List.of(kante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks().get(0)),
				List.of(kante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts().get(0)),
				false);
			boolean beideFahrtrichtungenGleich = FahrtrichtungAttributGruppe.isValid(
				kante.getFahrtrichtungAttributGruppe().getFahrtrichtungRechts(),
				kante.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks(),
				false);
			boolean isLueckeZweiseitig = !beideFuehrungsformSeitenGleich || !beideFahrtrichtungenGleich;
			kante.changeSeitenbezug(isLueckeZweiseitig);
		}
	}

	/**
	 * Ermittelt die Orientierung der Kanten innerhalb der Lücke. Also, ob die Stationierungsrichtung der Kante zum
	 * Anfang oder Ende der Lücke zeigt. Der Wert "true" in der Ergebnis-Map heißt hier, dass die Kante so orientiert
	 * ist, dass die Stationierungsrichtung der Kante vom Start zum Ende zeigt. Wenn man bei der Lücke also beim Start
	 * anfängt, durchläuft man diese Kante in Stationierungsrichtung (also vom von-Knoten zum nach-Knoten der Kante).
	 * Bei "false" ist es andersrum.
	 *
	 * @return Map mit "true", wenn Kante in Richtung der Lücke verläuft, "false", wenn entgegen.
	 */
	private @NotNull Map<Kante, Boolean> getKantenOrientierungInLuecke(Attributluecke luecke) {
		Map<Kante, Boolean> kanteToLueckenRichtung = new HashMap<>();
		Knoten previousKnoten = luecke.getStartKnoten();

		for (Kante kante : luecke.getLueckeKantenPfad()) {
			if (kante.getVonKnoten().equals(previousKnoten)) {
				// Kante verläuft in Richtung der Lücke
				kanteToLueckenRichtung.put(kante, true);
				previousKnoten = kante.getNachKnoten();
			} else {
				// Kante verläuft entgegen der Richtung der Lücke
				kanteToLueckenRichtung.put(kante, false);
				previousKnoten = kante.getVonKnoten();
			}
		}

		return kanteToLueckenRichtung;
	}

	/**
	 * Dies löscht die gegebene Kante aus der gegebenen Map. Heißt, die Kante wird aus der Liste vom von-Knoten und vom
	 * nach-Knoten entfernt. Ist eine Liste danach leer, wird der Eintrag entsprechend aus der Map entfernt.
	 */
	private void removeKanteFromMap(Map<Long, List<KanteWithInitialStatesView>> knotenToKanteMap,
		Kante kante) {
		Long knotenStart = kante.getVonKnoten().getId();
		Long knotenEnd = kante.getNachKnoten().getId();

		List<KanteWithInitialStatesView> kantenAdjazentZuStart = knotenToKanteMap.get(knotenStart);
		if (kantenAdjazentZuStart != null) {
			kantenAdjazentZuStart.forEach(k -> k.decreaseKnotenGrad(knotenStart));
			kantenAdjazentZuStart.removeIf(k -> k.getKanteId() == kante.getId());
			if (kantenAdjazentZuStart.isEmpty()) {
				knotenToKanteMap.remove(knotenStart);
			}
		}

		List<KanteWithInitialStatesView> kantenAdjazentZuEnd = knotenToKanteMap.get(knotenEnd);
		if (kantenAdjazentZuEnd != null) {
			kantenAdjazentZuEnd.forEach(k -> k.decreaseKnotenGrad(knotenEnd));
			kantenAdjazentZuEnd.removeIf(k -> k.getKanteId() == kante.getId());
			if (kantenAdjazentZuEnd.isEmpty()) {
				knotenToKanteMap.remove(knotenEnd);
			}
		}
	}

	@Override
	public List<? extends FehlerprotokollEintrag> getAktuelleFehlerprotokolle(FehlerprotokollTyp fehlerprotokollTyp) {
		require(fehlerprotokollTyp == FehlerprotokollTyp.ATTRIBUTLUECKEN_SCHLIESSEN);

		return attributlueckenSchliessenProblemRepository.findAttributlueckenSchliessenProblemByDatumAfter(LocalDateTime
			.now().minusDays(1));
	}

	@Override
	public List<? extends FehlerprotokollEintrag> getAktuelleFehlerprotokolleInBereich(
		FehlerprotokollTyp fehlerprotokollTyp, Envelope bereich) {
		require(fehlerprotokollTyp == FehlerprotokollTyp.ATTRIBUTLUECKEN_SCHLIESSEN);

		return attributlueckenSchliessenProblemRepository.findAttributlueckenSchliessenProblemByDatumAfterInBereich(
			LocalDateTime.now().minusDays(1),
			EnvelopeAdapter.toPolygon(bereich, KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid()));
	}
}
