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

import { LineString } from 'ol/geom';
import { isLineString, LineStringGeojson } from 'src/app/shared/models/geojson-geometrie';
import { Seitenbezug } from 'src/app/shared/models/seitenbezug';
import { Netzbezug, Segment } from 'src/app/viewer/viewer-shared/models/netzbezug';
import invariant from 'tiny-invariant';

export interface Auswahl {
  selectedIndices: number[];
  segmentierung: number[];
}

export class AbschnittSelektion {
  private _lineString: LineString;
  private _auswahlLinks!: Auswahl;
  private _auswahlRechts!: Auswahl;

  constructor(
    public readonly kanteId: number,
    public readonly geometrie: LineStringGeojson,
    public readonly segmenteLinks: Segment[],
    public readonly segmenteRechts: Segment[]
  ) {
    invariant(isLineString(geometrie));
    this._lineString = new LineString(this.geometrie.coordinates);
    this._auswahlLinks = {
      selectedIndices: [],
      segmentierung: [],
    };
    this._auswahlRechts = {
      selectedIndices: [],
      segmentierung: [],
    };
    this.calculateSegmentierung();
  }

  get lineString(): LineString {
    return this._lineString;
  }

  get istZweiseitig(): boolean {
    return !Netzbezug.sindSegmenteIdentisch(this.segmenteLinks, this.segmenteRechts);
  }

  getSelectedIndices(seitenbezug?: Seitenbezug): number[] {
    invariant(
      (!this.istZweiseitig && seitenbezug === undefined) || seitenbezug !== undefined,
      'Der seitenbezug darf hier nur undefined sein, wenn die selektion nicht zweiseitig ist'
    );

    if (seitenbezug === Seitenbezug.LINKS || seitenbezug === undefined) {
      return this._auswahlLinks.selectedIndices;
    } else {
      return this._auswahlRechts.selectedIndices;
    }
  }

  getSegmentierung(seitenbezug?: Seitenbezug): number[] {
    invariant(
      (!this.istZweiseitig && seitenbezug === undefined) || seitenbezug !== undefined,
      'Der seitenbezug darf hier nur undefined sein, wenn die selektion nicht zweiseitig ist'
    );

    if (seitenbezug === Seitenbezug.LINKS || seitenbezug === undefined) {
      return this._auswahlLinks.segmentierung;
    } else {
      return this._auswahlRechts.segmentierung;
    }
  }

  deselectSegment(index: number, seitenbezug?: Seitenbezug): void {
    invariant(
      this.segmenteLinks.length > index || seitenbezug === Seitenbezug.RECHTS,
      'Deselected Segment must be present in selektion'
    );
    invariant(
      this.segmenteRechts.length > index || seitenbezug === Seitenbezug.LINKS,
      'Deselected Segment must be present in selektion'
    );
    if (seitenbezug === Seitenbezug.LINKS || seitenbezug === undefined) {
      this.segmenteLinks[index].selected = false;
    }

    if (seitenbezug === Seitenbezug.RECHTS || seitenbezug === undefined) {
      this.segmenteRechts[index].selected = false;
    }

    this.calculateSegmentierung();
  }

  selectSegment(index: number, seitenbezug?: Seitenbezug): void {
    invariant(
      this.segmenteLinks.length > index || seitenbezug === Seitenbezug.RECHTS,
      'Selected Segment must be present in selektion'
    );
    invariant(
      this.segmenteRechts.length > index || seitenbezug === Seitenbezug.LINKS,
      'Selected Segment must be present in selektion'
    );
    if (seitenbezug === Seitenbezug.LINKS || seitenbezug === undefined) {
      this.segmenteLinks[index].selected = true;
    }

    if (seitenbezug === Seitenbezug.RECHTS || seitenbezug === undefined) {
      this.segmenteRechts[index].selected = true;
    }
    this.calculateSegmentierung();
  }

  insertSegment(lineareReferenz: number, seitenbezug?: Seitenbezug): void {
    if (seitenbezug === Seitenbezug.LINKS || seitenbezug === undefined) {
      const containingSegmentIndex = this.segmenteLinks.findIndex(
        segment => segment.von < lineareReferenz && segment.bis > lineareReferenz
      );
      const containingSegment = this.segmenteLinks.splice(containingSegmentIndex, 1)[0];
      this.segmenteLinks.push({
        von: containingSegment.von,
        bis: lineareReferenz,
        selected: containingSegment.selected,
      });
      this.segmenteLinks.push({
        von: lineareReferenz,
        bis: containingSegment.bis,
        selected: containingSegment.selected,
      });
    }

    if (seitenbezug === Seitenbezug.RECHTS || seitenbezug === undefined) {
      const containingSegmentIndex = this.segmenteRechts.findIndex(
        segment => segment.von < lineareReferenz && segment.bis > lineareReferenz
      );
      const containingSegment = this.segmenteRechts.splice(containingSegmentIndex, 1)[0];
      this.segmenteRechts.push({
        von: containingSegment.von,
        bis: lineareReferenz,
        selected: containingSegment.selected,
      });
      this.segmenteRechts.push({
        von: lineareReferenz,
        bis: containingSegment.bis,
        selected: containingSegment.selected,
      });
    }

    this.calculateSegmentierung(seitenbezug);
  }

  updateSegmentierung(newSegmentierung: number[], seitenbezug?: Seitenbezug): void {
    invariant(newSegmentierung.length === this.segmenteLinks.length + 1 || seitenbezug === Seitenbezug.RECHTS);
    invariant(newSegmentierung.length === this.segmenteRechts.length + 1 || seitenbezug === Seitenbezug.LINKS);
    if (seitenbezug === Seitenbezug.LINKS || seitenbezug === undefined) {
      for (let index = 1; index < newSegmentierung.length - 1; index++) {
        const schnittmarke = newSegmentierung[index];
        this.segmenteLinks[index - 1].bis = schnittmarke;
        this.segmenteLinks[index].von = schnittmarke;
      }
    }
    if (seitenbezug === Seitenbezug.RECHTS || seitenbezug === undefined) {
      for (let index = 1; index < newSegmentierung.length - 1; index++) {
        const schnittmarke = newSegmentierung[index];
        this.segmenteRechts[index - 1].bis = schnittmarke;
        this.segmenteRechts[index].von = schnittmarke;
      }
    }
    this.calculateSegmentierung(seitenbezug);
  }

  private calculateSegmentierung(seitenbezug?: Seitenbezug): void {
    if (seitenbezug === Seitenbezug.LINKS || seitenbezug === undefined) {
      this.calculateSegmentierungLinks();
    }
    if (seitenbezug === Seitenbezug.RECHTS || seitenbezug === undefined) {
      this.calculateSegmentierungRechts();
    }
  }

  private calculateSegmentierungLinks(): void {
    this.segmenteLinks.sort((s1, s2) => s1.von - s2.von);
    this._auswahlLinks.segmentierung = [0, ...this.segmenteLinks.map(s => s.bis)];
    this._auswahlLinks.selectedIndices = [];
    for (let index = 0; index < this.segmenteLinks.length; index++) {
      if (this.segmenteLinks[index].selected) {
        this._auswahlLinks.selectedIndices.push(index);
      }
    }
  }

  private calculateSegmentierungRechts(): void {
    this.segmenteRechts.sort((s1, s2) => s1.von - s2.von);
    this._auswahlRechts.segmentierung = [0, ...this.segmenteRechts.map(s => s.bis)];
    this._auswahlRechts.selectedIndices = [];
    for (let index = 0; index < this.segmenteRechts.length; index++) {
      if (this.segmenteRechts[index].selected) {
        this._auswahlRechts.selectedIndices.push(index);
      }
    }
  }
}
