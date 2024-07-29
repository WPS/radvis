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

import { Coordinate } from 'ol/coordinate';
import { LineString } from 'ol/geom';
import { LineStringGeojson } from 'src/app/shared/models/geojson-geometrie';
import { LineStringOperations } from 'src/app/shared/models/line-string-operations';
import { KantenSeite } from 'src/app/shared/models/kantenSeite';
import { AbschnittSelektion } from 'src/app/shared/components/netzbezug-selektion-control/netzbezug-selektion-layer/abschnitt-selektion';
import { KnotenNetzbezug } from 'src/app/shared/models/knoten-netzbezug';
import {
  AbschnittsweiserKantenSeitenBezug,
  KantenSeitenbezug,
  Netzbezug,
  Segment,
} from 'src/app/shared/models/netzbezug';
import { PunktuellerKantenNetzBezug } from 'src/app/shared/models/punktueller-kanten-netzbezug';
import invariant from 'tiny-invariant';

export class NetzbezugSelektion {
  private _kantenSeitenAbschnitte: AbschnittSelektion[];
  private _knoten: KnotenNetzbezug[];
  private _punktuelleKantenSeitenBezuege: PunktuellerKantenNetzBezug[];

  constructor(netzbezug?: Netzbezug) {
    this._kantenSeitenAbschnitte = [];
    this._knoten = [];
    this._punktuelleKantenSeitenBezuege = [];

    if (netzbezug) {
      this.parseKantenSelektion(netzbezug);
    }
    netzbezug?.knotenBezug.forEach(knoten => this.addKnotenNetzbezug(knoten));
    netzbezug?.punktuellerKantenBezug.forEach(pKB => this.addPunktuellerKantenBezug(pKB));
  }

  get kantenSeitenAbschnitte(): ReadonlyArray<AbschnittSelektion> {
    return this._kantenSeitenAbschnitte as ReadonlyArray<AbschnittSelektion>;
  }

  get punktuelleKantenSeitenBezuege(): ReadonlyArray<PunktuellerKantenNetzBezug> {
    return this._punktuelleKantenSeitenBezuege as ReadonlyArray<PunktuellerKantenNetzBezug>;
  }

  get knoten(): ReadonlyArray<KnotenNetzbezug> {
    return this._knoten as ReadonlyArray<KnotenNetzbezug>;
  }

  toNetzbezug(): Netzbezug {
    const kantenBezug: AbschnittsweiserKantenSeitenBezug[] = [];
    this._kantenSeitenAbschnitte.forEach(selektion => {
      const selectedSegmenteLinks = selektion.segmenteLinks.filter(s => s.selected);
      const selectedSegmenteRechts = selektion.segmenteRechts.filter(s => s.selected);

      const segmenteBeidseitig: Segment[] = selectedSegmenteLinks.filter(segmentLinks =>
        selectedSegmenteRechts.some(
          segmentRechts => segmentLinks.von === segmentRechts.von && segmentLinks.bis === segmentRechts.bis
        )
      );

      selectedSegmenteLinks
        .filter(
          segmentLinks =>
            !segmenteBeidseitig.some(
              segmentBeidseitig =>
                segmentLinks.von === segmentBeidseitig.von && segmentLinks.bis === segmentBeidseitig.bis
            )
        )
        .forEach(segment => {
          kantenBezug.push(this.createKantenBezug(selektion, segment, KantenSeitenbezug.LINKS));
        });

      selectedSegmenteRechts
        .filter(
          segmentRechts =>
            !segmenteBeidseitig.some(
              segmentBeidseitig =>
                segmentRechts.von === segmentBeidseitig.von && segmentRechts.bis === segmentBeidseitig.bis
            )
        )
        .forEach(segment => {
          kantenBezug.push(this.createKantenBezug(selektion, segment, KantenSeitenbezug.RECHTS));
        });

      segmenteBeidseitig.forEach(segment => {
        kantenBezug.push(this.createKantenBezug(selektion, segment, KantenSeitenbezug.BEIDSEITIG));
      });
    });

    return {
      kantenBezug,
      knotenBezug: this.knoten,
      punktuellerKantenBezug: this.punktuelleKantenSeitenBezuege,
    } as Netzbezug;
  }

  kanteSchneiden(kanteId: number, coordinate: Coordinate, kantenSeite?: KantenSeite): void {
    invariant(this.isKanteSelected(kanteId));
    const abschnittSelektion = this.getSelektionForKante(kanteId);

    const lineString = new LineString(abschnittSelektion?.geometrie.coordinates);
    const closestPoint = lineString.getClosestPoint(coordinate);
    const lineareReferenz: number = LineStringOperations.getFractionOfPointOnLineString(closestPoint, lineString);

    abschnittSelektion.insertSegment(lineareReferenz, kantenSeite);
  }

  deselectSegment(kanteId: number, index: number, kantenSeite?: KantenSeite): void {
    invariant(this.isKanteSelected(kanteId));
    const abschnittSelektion = this.getSelektionForKante(kanteId);
    abschnittSelektion.deselectSegment(index, kantenSeite);
    if (
      abschnittSelektion.getSelectedIndices(KantenSeite.LINKS).length === 0 &&
      abschnittSelektion.getSelectedIndices(KantenSeite.RECHTS).length === 0
    ) {
      this.deselectKante(kanteId);
    }
  }

  selectSegment(kanteId: number, index: number, kantenSeite?: KantenSeite): void {
    invariant(this.isKanteSelected(kanteId));
    const abschnittSelektion = this.getSelektionForKante(kanteId);
    abschnittSelektion.selectSegment(index, kantenSeite);
  }

  selectKante(kanteId: number, geometrie: LineStringGeojson, kantenSeite?: KantenSeite): void {
    invariant(!this.isKanteSelected(kanteId));
    this._kantenSeitenAbschnitte.push(
      new AbschnittSelektion(
        kanteId,
        geometrie,
        [{ von: 0, bis: 1, selected: kantenSeite === KantenSeite.LINKS || kantenSeite === undefined }],
        [{ von: 0, bis: 1, selected: kantenSeite === KantenSeite.RECHTS || kantenSeite === undefined }]
      )
    );
  }

  deselectKante(kanteId: number): void {
    const index = this._kantenSeitenAbschnitte.findIndex(selektion => selektion.kanteId === kanteId);
    this._kantenSeitenAbschnitte.splice(index, 1);
  }

  addPunktuellerKantenBezug(newPKB: PunktuellerKantenNetzBezug): boolean {
    if (this.hasPunktuellerKantenBezug(newPKB)) {
      return true;
    }
    this._punktuelleKantenSeitenBezuege.push(newPKB);
    return false;
  }

  hasPunktuellerKantenBezug(newPKB: PunktuellerKantenNetzBezug): boolean {
    return this._punktuelleKantenSeitenBezuege.filter(pKB => this.punktuellerKantenBezugEquals(pKB, newPKB)).length > 0;
  }

  removePunktuellerKantenBezug(pKBToRemove: PunktuellerKantenNetzBezug): void {
    this._punktuelleKantenSeitenBezuege = this._punktuelleKantenSeitenBezuege.filter(
      pKB => !this.punktuellerKantenBezugEquals(pKB, pKBToRemove)
    );
  }

  addKnotenNetzbezug(newKnoten: KnotenNetzbezug): void {
    invariant(!this.isKnotenSelected(newKnoten.knotenId));
    this._knoten.push(newKnoten);
  }

  removeKnoten(knotenToRemove: KnotenNetzbezug): void {
    this._knoten.splice(
      this._knoten.findIndex(k => k.knotenId === knotenToRemove.knotenId),
      1
    );
  }

  public isKnotenSelected(knotenId: number): boolean {
    return this._knoten.some(knoten => knoten.knotenId === knotenId);
  }

  public isKanteSelected(kanteId: number): boolean {
    return this._kantenSeitenAbschnitte.some(selektion => selektion.kanteId === kanteId);
  }

  public getSelektionForKante(kanteId: number): AbschnittSelektion {
    invariant(this.isKanteSelected(kanteId));
    const result = this._kantenSeitenAbschnitte.find(selektion => selektion.kanteId === kanteId);
    invariant(result);
    return result;
  }

  updateSegmentierung(kanteId: number, newSegmentierung: number[], kantenSeite?: KantenSeite): void {
    invariant(this.isKanteSelected);
    this.getSelektionForKante(kanteId).updateSegmentierung(newSegmentierung, kantenSeite);
  }

  private punktuellerKantenBezugEquals(pkB: PunktuellerKantenNetzBezug, otherPKB: PunktuellerKantenNetzBezug): boolean {
    return (
      pkB.kanteId === otherPKB.kanteId &&
      // verhindere Lineare Referenzen extrem nah beieinander
      (Math.abs(pkB.lineareReferenz - otherPKB.lineareReferenz) < 0.001 ||
        this.distance(pkB.geometrie.coordinates, otherPKB.geometrie.coordinates) < 0.5)
    );
  }

  private distance(c1: number[], c2: number[]): number {
    return Math.sqrt(Math.pow(c2[0] - c1[0], 2) + Math.pow(c2[0] - c1[0], 2));
  }

  private parseKantenSelektion(netzbezug: Netzbezug | undefined): void {
    invariant(netzbezug);
    Netzbezug.groupByKante(netzbezug.kantenBezug).forEach((kantenBezuege, kanteId) => {
      const kantenBezuegeLinks = kantenBezuege.filter(
        kBZ => kBZ.kantenSeite === KantenSeitenbezug.BEIDSEITIG || kBZ.kantenSeite === KantenSeitenbezug.LINKS
      );
      const kantenBezuegeRechts = kantenBezuege.filter(
        kBZ => kBZ.kantenSeite === KantenSeitenbezug.BEIDSEITIG || kBZ.kantenSeite === KantenSeitenbezug.RECHTS
      );

      const abschnitteLinks: Segment[] = Netzbezug.extractKantenSelektion(kantenBezuegeLinks);
      const abschnitteRechts: Segment[] = Netzbezug.extractKantenSelektion(kantenBezuegeRechts);

      this._kantenSeitenAbschnitte.push(
        new AbschnittSelektion(kanteId, kantenBezuege[0].geometrie, abschnitteLinks, abschnitteRechts)
      );
    });
  }

  private createKantenBezug(
    selektion: AbschnittSelektion,
    segment: Segment,
    kantenSeite: KantenSeitenbezug
  ): AbschnittsweiserKantenSeitenBezug {
    return {
      geometrie: selektion.geometrie,
      kanteId: selektion.kanteId,
      linearReferenzierterAbschnitt: { von: segment.von, bis: segment.bis },
      kantenSeite: kantenSeite,
    };
  }
}
