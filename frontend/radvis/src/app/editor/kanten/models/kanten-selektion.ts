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

import { Kante } from 'src/app/editor/kanten/models/kante';
import { LinearReferenzierteAttribute } from 'src/app/editor/kanten/models/linear-referenzierte-attribute';
import { KantenSeite } from 'src/app/shared/models/kantenSeite';
import invariant from 'tiny-invariant';

export class KantenSelektion {
  private readonly _selektionLinksAlsArray: number[];
  private readonly _selektionRechtsAlsArray: number[];
  private readonly _kante: Kante;

  private constructor(
    kante: Kante,
    private _selektionLinks: Set<number>,
    private _selektionRechts: Set<number>
  ) {
    this._kante = KantenSelektion.kanteMitSortierteLinearReferenzierteAttributen(kante);
    this._selektionLinksAlsArray = Array.from(this._selektionLinks);
    this._selektionRechtsAlsArray = Array.from(this._selektionRechts);
  }

  public static ofSeite(kante: Kante, kantenSeite: KantenSeite, segmentAnzahl = 1): KantenSelektion {
    const selektion = KantenSelektion.buildArrayOfAscendingNumbers(segmentAnzahl);
    if (kantenSeite === KantenSeite.LINKS) {
      return new KantenSelektion(kante, selektion, new Set());
    } else if (kantenSeite === KantenSeite.RECHTS) {
      return new KantenSelektion(kante, new Set(), selektion);
    } else {
      throw Error('Kein valider Seitenbezug');
    }
  }

  public static ofGesamteKante(kante: Kante, segmentAnzahlLinks = 1, segmentAnzahlRechts = 1): KantenSelektion {
    const selektionLinks = KantenSelektion.buildArrayOfAscendingNumbers(segmentAnzahlLinks);
    const selektionRechts = KantenSelektion.buildArrayOfAscendingNumbers(segmentAnzahlRechts);
    return new KantenSelektion(kante, selektionLinks, selektionRechts);
  }

  private static kanteMitSortierteLinearReferenzierteAttributen(kante: Kante): Kante {
    return {
      ...kante,
      zustaendigkeitAttributGruppe: {
        ...kante.zustaendigkeitAttributGruppe,
        zustaendigkeitAttribute: [...kante.zustaendigkeitAttributGruppe.zustaendigkeitAttribute].sort(
          LinearReferenzierteAttribute.sort
        ),
      },
      geschwindigkeitAttributGruppe: {
        ...kante.geschwindigkeitAttributGruppe,
        geschwindigkeitAttribute: [...kante.geschwindigkeitAttributGruppe.geschwindigkeitAttribute].sort(
          LinearReferenzierteAttribute.sort
        ),
      },
      fuehrungsformAttributGruppe: {
        ...kante.fuehrungsformAttributGruppe,
        fuehrungsformAttributeRechts: [...kante.fuehrungsformAttributGruppe.fuehrungsformAttributeRechts].sort(
          LinearReferenzierteAttribute.sort
        ),
        fuehrungsformAttributeLinks: [...kante.fuehrungsformAttributGruppe.fuehrungsformAttributeLinks].sort(
          LinearReferenzierteAttribute.sort
        ),
      },
    };
  }

  get kante(): Kante {
    return this._kante;
  }

  private static buildArrayOfAscendingNumbers(size: number): Set<number> {
    return new Set(Array(size).keys());
  }

  private static insertAndIncrementInArray(indexToInsert: number, into: Set<number>): Set<number> {
    const result: Set<number> = new Set();
    for (const item of into.values()) {
      if (item === indexToInsert - 1) {
        // Wenn das ursprüngliche Segment selektiert war, soll das Neue auch selektiert sein
        result.add(indexToInsert);
      }
      if (item >= indexToInsert) {
        result.add(item + 1);
      } else {
        result.add(item);
      }
    }
    return result;
  }

  private static deleteAndDecrementInArray(indexToDelete: number, from: Set<number>): Set<number> {
    const result: Set<number> = new Set();
    for (const item of from.values()) {
      if (item === indexToDelete) {
        continue;
      }
      if (item > indexToDelete) {
        result.add(item - 1);
      } else {
        result.add(item);
      }
    }
    return result;
  }

  public getSelectedSegmentIndices(kantenSeite?: KantenSeite): number[] {
    if (kantenSeite) {
      if (kantenSeite === KantenSeite.LINKS) {
        return this._selektionLinksAlsArray;
      } else if (kantenSeite === KantenSeite.RECHTS) {
        return this._selektionRechtsAlsArray;
      } else {
        throw Error('Kein valider Seitenbezug');
      }
    }
    return this._selektionLinksAlsArray;
  }

  public selectSeite(kantenSeite: KantenSeite, segmentAnzahl: number, additiv = false): KantenSelektion {
    const selektion = KantenSelektion.buildArrayOfAscendingNumbers(segmentAnzahl);
    if (kantenSeite === KantenSeite.LINKS) {
      return new KantenSelektion(this._kante, selektion, additiv ? this._selektionRechts : new Set());
    } else {
      return new KantenSelektion(this._kante, additiv ? this._selektionLinks : new Set(), selektion);
    }
  }

  public selectSegment(segmentIndex: number, kantenSeite?: KantenSeite, additiv = false): KantenSelektion {
    invariant(segmentIndex >= 0, 'Segmentindex ' + segmentIndex + ' ungültig');
    if (kantenSeite) {
      if (kantenSeite === KantenSeite.LINKS) {
        const newSelektion: Set<number> = additiv ? this._selektionLinks : new Set();
        newSelektion.add(segmentIndex);
        return new KantenSelektion(this._kante, newSelektion, additiv ? this._selektionRechts : new Set());
      } else if (kantenSeite === KantenSeite.RECHTS) {
        const newSelektion: Set<number> = additiv ? this._selektionRechts : new Set();
        newSelektion.add(segmentIndex);
        return new KantenSelektion(this._kante, additiv ? this._selektionLinks : new Set(), newSelektion);
      } else {
        throw Error('Kein valider Seitenbezug');
      }
    } else {
      const newSelektion: Set<number> = additiv ? this._selektionLinks : new Set();
      newSelektion.add(segmentIndex);
      return new KantenSelektion(this._kante, newSelektion, newSelektion);
    }
  }

  public reduceSegmentAnzahl(segmentAnzahlLinks: number, segmentAnzahlRechts?: number): KantenSelektion {
    const reducedSegmentsLinks: Set<number> = new Set();
    const reducedSegmentsRechts: Set<number> = new Set();

    for (const segment of this._selektionLinks.values()) {
      if (segment < segmentAnzahlLinks) {
        reducedSegmentsLinks.add(segment);
      }
    }

    for (const segment of this._selektionRechts.values()) {
      if (segment < (segmentAnzahlRechts !== undefined ? segmentAnzahlRechts : segmentAnzahlLinks)) {
        reducedSegmentsRechts.add(segment);
      }
    }

    if (reducedSegmentsRechts.size === 0 && reducedSegmentsLinks.size === 0) {
      reducedSegmentsLinks.add(0);
      if (segmentAnzahlRechts === undefined) {
        reducedSegmentsRechts.add(0);
      }
    }

    return new KantenSelektion(this._kante, reducedSegmentsLinks, reducedSegmentsRechts);
  }

  public insertSegment(segmentIndex: number, kantenSeite?: KantenSeite): KantenSelektion {
    invariant(segmentIndex >= 0, 'Segmentindex ' + segmentIndex + ' ungültig');
    if (kantenSeite) {
      if (kantenSeite === KantenSeite.LINKS) {
        const adjustedArray = KantenSelektion.insertAndIncrementInArray(segmentIndex, this._selektionLinks);
        return new KantenSelektion(this._kante, adjustedArray, this._selektionRechts);
      } else if (kantenSeite === KantenSeite.RECHTS) {
        const adjustedArray = KantenSelektion.insertAndIncrementInArray(segmentIndex, this._selektionRechts);
        return new KantenSelektion(this._kante, this._selektionLinks, adjustedArray);
      } else {
        throw Error('Kein valider Seitenbezug');
      }
    } else {
      const adjustedArrayLeft = KantenSelektion.insertAndIncrementInArray(segmentIndex, this._selektionLinks);
      const adjustedArrayRight = KantenSelektion.insertAndIncrementInArray(segmentIndex, this._selektionRechts);
      return new KantenSelektion(this._kante, adjustedArrayLeft, adjustedArrayRight);
    }
  }

  public deleteSegment(segmentIndex: number, kantenSeite?: KantenSeite): KantenSelektion {
    invariant(segmentIndex >= 0, 'Segmentindex ' + segmentIndex + ' ungültig');
    if (kantenSeite) {
      if (kantenSeite === KantenSeite.LINKS) {
        const adjustedArray = KantenSelektion.deleteAndDecrementInArray(segmentIndex, this._selektionLinks);
        if (adjustedArray.size === 0) {
          adjustedArray.add(0);
        }
        return new KantenSelektion(this._kante, adjustedArray, this._selektionRechts);
      } else {
        const adjustedArray = KantenSelektion.deleteAndDecrementInArray(segmentIndex, this._selektionRechts);
        if (adjustedArray.size === 0) {
          adjustedArray.add(0);
        }
        return new KantenSelektion(this._kante, this._selektionLinks, adjustedArray);
      }
    } else {
      const adjustedArrayLeft = KantenSelektion.deleteAndDecrementInArray(segmentIndex, this._selektionLinks);
      const adjustedArrayRight = KantenSelektion.deleteAndDecrementInArray(segmentIndex, this._selektionRechts);
      if (adjustedArrayLeft.size === 0) {
        adjustedArrayLeft.add(0);
      }
      if (adjustedArrayRight.size === 0) {
        adjustedArrayRight.add(0);
      }
      return new KantenSelektion(this._kante, adjustedArrayLeft, adjustedArrayRight);
    }
  }

  public deselectSeite(kantenSeite: KantenSeite): KantenSelektion {
    if (kantenSeite === KantenSeite.LINKS) {
      return new KantenSelektion(this._kante, new Set(), this._selektionRechts);
    } else {
      return new KantenSelektion(this._kante, this._selektionLinks, new Set());
    }
  }

  public deselectSegment(segmentIndex: number, kantenSeite?: KantenSeite): KantenSelektion {
    invariant(segmentIndex >= 0, 'Segmentindex ' + segmentIndex + ' ungültig');
    const selektionLinks = this._selektionLinks;
    const selektionRechts = this._selektionRechts;
    if (kantenSeite === KantenSeite.LINKS || kantenSeite === undefined) {
      selektionLinks.delete(segmentIndex);
    }
    if (kantenSeite === KantenSeite.RECHTS || kantenSeite === undefined) {
      selektionRechts.delete(segmentIndex);
    }
    return new KantenSelektion(this._kante, selektionLinks, selektionRechts);
  }

  public updateKante(updatedKante: Kante): KantenSelektion {
    invariant(updatedKante.id === this._kante.id, 'Es darf nur diesselbe Kante mit neuen Werten übergeben werden');
    if (updatedKante.zweiseitig !== this.kante.zweiseitig && !this.istBeidseitigSelektiert()) {
      throw new Error('Beim Update des Seitenbezugs müssen beide Seiten selektiert werden.');
    }
    return new KantenSelektion(updatedKante, this._selektionLinks, this._selektionRechts);
  }

  public selectGesamteKante(anzahlSegmenteLinks: number, anzahlSegmenteRechts: number): KantenSelektion {
    const selektionLinks = KantenSelektion.buildArrayOfAscendingNumbers(anzahlSegmenteLinks);
    const selektionRechts = KantenSelektion.buildArrayOfAscendingNumbers(anzahlSegmenteRechts);
    return new KantenSelektion(this._kante, selektionLinks, selektionRechts);
  }

  istBeidseitigSelektiert(): boolean {
    return this.istSeiteSelektiert(KantenSeite.LINKS) && this.istSeiteSelektiert(KantenSeite.RECHTS);
  }

  istSeiteSelektiert(kantenSeite: KantenSeite): boolean {
    if (kantenSeite === KantenSeite.LINKS) {
      return this._selektionLinks.size > 0;
    } else {
      return this._selektionRechts.size > 0;
    }
  }

  istSegmentSelektiert(segmentIndex: number, kantenSeite?: KantenSeite): boolean {
    invariant(segmentIndex >= 0, 'Segmentindex ' + segmentIndex + ' ungültig');
    return this.getSelectedSegmentIndices(kantenSeite).includes(segmentIndex);
  }
}
