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

import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { NetzService } from 'src/app/editor/editor-shared/services/netz.service';
import { AttributGruppe } from 'src/app/editor/kanten/models/attribut-gruppe';
import { Kante } from 'src/app/editor/kanten/models/kante';
import { KantenSelektion } from 'src/app/editor/kanten/models/kanten-selektion';
import { NetzBearbeitungModusService } from 'src/app/editor/kanten/services/netz-bearbeitung-modus.service';
import { Seitenbezug } from 'src/app/shared/models/seitenbezug';
import { DiscardGuardService } from 'src/app/shared/services/discard-guard.service';
import { DiscardableComponent } from 'src/app/shared/services/discard.guard';
import { LadeZustandService } from 'src/app/shared/services/lade-zustand.service';
import invariant from 'tiny-invariant';

@Injectable({ providedIn: 'root' })
export class KantenSelektionService {
  selektierteKanten$: Observable<Kante[]>;
  selektion$: Observable<KantenSelektion[]>;

  private selektionSubject = new BehaviorSubject<KantenSelektion[]>([]);
  private discardableComponent: DiscardableComponent | null = null;
  private activeAttributGruppe: AttributGruppe | null = null;

  constructor(
    private netzService: NetzService,
    private ladeZustandService: LadeZustandService,
    private discardGuardService: DiscardGuardService,
    bearbeitungModusService: NetzBearbeitungModusService
  ) {
    this.selektierteKanten$ = this.selektionSubject.asObservable().pipe(map(selektion => selektion.map(s => s.kante)));
    this.selektion$ = this.selektionSubject.asObservable();
    bearbeitungModusService.getAktiveKantenGruppe().subscribe(newGruppe => {
      this.activeAttributGruppe = newGruppe;
      this.adjustSelectionForNewAttributgruppe(this.activeAttributGruppe as AttributGruppe);
    });
  }

  public get selektierteKanten(): Kante[] {
    return this.selektionSubject.value.map(s => s.kante);
  }

  public get selektion(): KantenSelektion[] {
    return this.selektionSubject.value;
  }

  public registerForDiscardGuard(discardableComponent: DiscardableComponent): void {
    this.discardableComponent = discardableComponent;
  }

  public select(kanteId: number, additiv: boolean, seitenbezug?: Seitenbezug, segmentIndex?: number): void {
    if (segmentIndex !== undefined) {
      this.selectSegment(kanteId, segmentIndex, additiv, seitenbezug);
    } else {
      if (seitenbezug) {
        this.selectKantenseite(kanteId, additiv, seitenbezug);
      } else {
        this.selectGesamteKante(kanteId, additiv);
      }
    }
  }

  public deselect(kanteId: number, seitenbezug?: Seitenbezug, segmentIndex?: number): void {
    invariant(this.isKanteSelektiert(kanteId), 'Deselect für nicht selektierte Kante nicht möglich');
    if (segmentIndex !== undefined) {
      invariant(this.isKantenelementSelektiert(kanteId, segmentIndex, seitenbezug));
      // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
      if (this.isLastSelectedElement(kanteId, seitenbezug)) {
        this.deselectGesamteKante(kanteId);
      } else {
        this.deselectElement(kanteId, segmentIndex, seitenbezug);
      }
    } else {
      if (seitenbezug && this.isKantenseiteSelektiert(kanteId, Seitenbezug.getGegenseite(seitenbezug))) {
        this.deselectKantenseite(kanteId, seitenbezug);
      } else {
        this.deselectGesamteKante(kanteId);
      }
    }
  }

  public adjustSelectionForSegmentInsertion(kanteId: number, segmentIndex: number, seitenbezug?: Seitenbezug): void {
    invariant(this.isKanteSelektiert(kanteId));

    const bestehendeKantenSelektionIndex = this.selektion.findIndex(selektion => selektion.kante.id === kanteId);
    const bestehendeKantenSelektion = this.selektion[bestehendeKantenSelektionIndex];
    const neueSelektion = [...this.selektion];
    neueSelektion[bestehendeKantenSelektionIndex] = bestehendeKantenSelektion.insertSegment(segmentIndex, seitenbezug);

    this.selektionSubject.next(neueSelektion);
  }

  public adjustSelectionForSegmentDeletion(kanteId: number, segmentIndex: number, seitenbezug?: Seitenbezug): void {
    invariant(this.isKanteSelektiert(kanteId));

    const neueSelektion = [...this.selektion];
    const bestehendeKantenSelektionIndex = this.selektion.findIndex(({ kante }) => kante.id === kanteId);
    neueSelektion[bestehendeKantenSelektionIndex] = this.selektion[bestehendeKantenSelektionIndex].deleteSegment(
      segmentIndex,
      seitenbezug
    );

    this.selektionSubject.next(neueSelektion);
  }

  public resetSelectionToConsistentState(): void {
    const neueSelektion = this.resetSegmentIndices(this.selektion);
    this.selektionSubject.next(neueSelektion);
  }

  public updateKanten(updated: Kante[]): void {
    const currentSelektionIds = this.selektionSubject.value.map(kantenSelektion => kantenSelektion.kante.id);
    const updatedKanteIds = updated.map(k => k.id);

    invariant(
      currentSelektionIds.length === updatedKanteIds.length,
      'Es müssen alle selektierten Kanten ersetzt werden'
    );
    invariant(
      currentSelektionIds.every(id => updatedKanteIds.includes(id)),
      'Kantenupdate muss mit Selektion übereinstimmen'
    );

    const updatedKantenSelektion: KantenSelektion[] = this.selektionSubject.value.map(kantenSelektion =>
      kantenSelektion.updateKante(updated.find(({ id }) => id === kantenSelektion.kante.id) as Kante)
    );

    this.selektionSubject.next(updatedKantenSelektion);
  }

  public cleanUp(reset: boolean): Promise<boolean> {
    return this.canDeactivate().then(proceed => {
      if (proceed && reset) {
        this.selektionSubject.next([]);
      }
      return proceed;
    });
  }

  public isSelektiert(kanteId: number, seitenbezug?: Seitenbezug): boolean {
    if (seitenbezug) {
      return this.isKantenseiteSelektiert(kanteId, seitenbezug);
    } else {
      return this.isKanteSelektiert(kanteId);
    }
  }

  public canDiscard(): boolean {
    return this.discardableComponent ? this.discardableComponent.canDiscard() : true;
  }

  private canDeactivate(): Promise<boolean> {
    if (this.discardableComponent) {
      return (this.discardGuardService.canDeactivate(this.discardableComponent) as Observable<boolean>).toPromise();
    }
    return Promise.resolve(true);
  }

  private isKanteSelektiert(kanteId: number): boolean {
    return this.selektion.map(kantenSelektion => kantenSelektion.kante.id).includes(kanteId);
  }

  private isKantenseiteSelektiert(kanteId: number, seitenbezug: Seitenbezug): boolean {
    const foundSelektion = this.selektion.find(selektion => selektion.kante.id === kanteId);
    if (foundSelektion) {
      return foundSelektion.istSeiteSelektiert(seitenbezug);
    } else {
      return false;
    }
  }

  private isKantenelementSelektiert(kanteId: number, segmentIndex: number, seitenbezug?: Seitenbezug): boolean {
    const foundSelektion = this.selektion.find(selektion => selektion.kante.id === kanteId);
    if (foundSelektion) {
      return foundSelektion.istSegmentSelektiert(segmentIndex, seitenbezug);
    } else {
      return false;
    }
  }

  private isLastSelectedElement(kanteId: number, seitenbezug: Seitenbezug | undefined): boolean {
    const currentSelektion = this.selektion.find(selektion => selektion.kante.id === kanteId);
    invariant(currentSelektion);
    if (seitenbezug !== undefined) {
      return (
        !currentSelektion.istSeiteSelektiert(Seitenbezug.getGegenseite(seitenbezug)) &&
        currentSelektion.getSelectedSegmentIndices(seitenbezug).length === 1
      );
    } else {
      return (
        currentSelektion.getSelectedSegmentIndices(Seitenbezug.LINKS).length === 1 &&
        currentSelektion.getSelectedSegmentIndices(Seitenbezug.RECHTS).length === 1
      );
    }
  }

  private adjustSelectionForNewAttributgruppe(newAttributgruppe: AttributGruppe): void {
    const neueSelektion = this.selektion.map(kantenSelektion => {
      let neueKantenselektion: KantenSelektion;
      if (!AttributGruppe.isSeitenbezogen(newAttributgruppe) || kantenSelektion.istBeidseitigSelektiert()) {
        const segmentAnzahlLinks = Kante.getAnzahlSegmente(newAttributgruppe, kantenSelektion.kante, Seitenbezug.LINKS);
        const segmentAnzahlRechts = Kante.getAnzahlSegmente(
          newAttributgruppe,
          kantenSelektion.kante,
          Seitenbezug.RECHTS
        );
        neueKantenselektion = kantenSelektion.selectGesamteKante(segmentAnzahlLinks, segmentAnzahlRechts);
      } else if (kantenSelektion.istSeiteSelektiert(Seitenbezug.LINKS)) {
        const segmentAnzahlLinks = Kante.getAnzahlSegmente(newAttributgruppe, kantenSelektion.kante, Seitenbezug.LINKS);
        neueKantenselektion = kantenSelektion.selectSeite(Seitenbezug.LINKS, segmentAnzahlLinks);
      } else {
        const segmentAnzahlRechts = Kante.getAnzahlSegmente(
          newAttributgruppe,
          kantenSelektion.kante,
          Seitenbezug.RECHTS
        );
        neueKantenselektion = kantenSelektion.selectSeite(Seitenbezug.RECHTS, segmentAnzahlRechts);
      }
      return neueKantenselektion;
    });
    this.selektionSubject.next(neueSelektion);
  }

  private fetchKante(kanteId: number): Promise<Kante> {
    this.ladeZustandService.startLoading();
    return this.netzService.getKanteForEdit(kanteId).finally(() => this.ladeZustandService.finishedLoading());
  }

  private selectGesamteKante(kanteId: number, additiv: boolean): void {
    this.canDeactivate().then(proceed => {
      if (proceed) {
        this.fetchKante(kanteId).then(newKante => {
          const anzahlSegmenteLinks = Kante.getAnzahlSegmente(
            this.activeAttributGruppe as AttributGruppe,
            newKante,
            Seitenbezug.LINKS
          );
          const anzahlSegmenteRechts = Kante.getAnzahlSegmente(
            this.activeAttributGruppe as AttributGruppe,
            newKante,
            Seitenbezug.RECHTS
          );
          if (additiv) {
            this.selektionSubject.next([
              ...this.resetSegmentIndices(this.selektion),
              KantenSelektion.ofGesamteKante(newKante, anzahlSegmenteLinks, anzahlSegmenteRechts),
            ]);
          } else {
            this.selektionSubject.next([
              KantenSelektion.ofGesamteKante(newKante, anzahlSegmenteLinks, anzahlSegmenteRechts),
            ]);
          }
        });
      }
    });
  }

  private selectKantenseite(kanteId: number, additiv: boolean, seitenbezug: Seitenbezug): void {
    const bestehendeKantenSelektionIndex = this.selektion.findIndex(selektion => selektion.kante.id === kanteId);
    if (bestehendeKantenSelektionIndex >= 0) {
      // Kante nicht nachladen
      const bestehendeKantenSelektion = this.selektion[bestehendeKantenSelektionIndex];
      const anzahlSegmente = Kante.getAnzahlSegmente(
        this.activeAttributGruppe as AttributGruppe,
        bestehendeKantenSelektion.kante,
        seitenbezug
      );
      if (additiv) {
        const neueSelektion = [...this.selektion];
        neueSelektion[bestehendeKantenSelektionIndex] = bestehendeKantenSelektion.selectSeite(
          seitenbezug,
          anzahlSegmente,
          true
        );
        this.selektionSubject.next(neueSelektion);
      } else {
        const werdenKantenDeselektiert = this.selektion.some(kantenSelektion => kantenSelektion.kante.id !== kanteId);
        if (werdenKantenDeselektiert) {
          this.canDeactivate().then(proceed => {
            if (proceed) {
              this.selektionSubject.next([bestehendeKantenSelektion.selectSeite(seitenbezug, anzahlSegmente)]);
            }
          });
        } else {
          this.selektionSubject.next([bestehendeKantenSelektion.selectSeite(seitenbezug, anzahlSegmente)]);
        }
      }
    } else {
      // Kante nachladen
      this.canDeactivate().then(proceed => {
        if (proceed) {
          this.fetchKante(kanteId).then(newKante => {
            const anzahlSegmente = Kante.getAnzahlSegmente(
              this.activeAttributGruppe as AttributGruppe,
              newKante,
              seitenbezug
            );
            if (additiv) {
              this.selektionSubject.next([
                ...this.resetSegmentIndices(this.selektion),
                KantenSelektion.ofSeite(newKante, seitenbezug, anzahlSegmente),
              ]);
            } else {
              this.selektionSubject.next([KantenSelektion.ofSeite(newKante, seitenbezug, anzahlSegmente)]);
            }
          });
        }
      });
    }
  }

  private deselectKantenseite(kanteId: number, seitenbezug: Seitenbezug): void {
    invariant(this.isKantenseiteSelektiert(kanteId, seitenbezug));
    invariant(this.isKantenseiteSelektiert(kanteId, Seitenbezug.getGegenseite(seitenbezug)));
    const bestehendeKantenSelektionIndex = this.selektion.findIndex(selektion => selektion.kante.id === kanteId);
    const neueKantenSelektion = this.selektion[bestehendeKantenSelektionIndex].deselectSeite(seitenbezug);
    const neueSelektion = [...this.selektion];
    neueSelektion[bestehendeKantenSelektionIndex] = neueKantenSelektion;
    this.selektionSubject.next(neueSelektion);
  }

  private deselectGesamteKante(kanteId: number): void {
    invariant(this.isKanteSelektiert(kanteId));
    this.canDeactivate().then(proceed => {
      if (proceed) {
        this.selektionSubject.next(
          this.selektionSubject.value.filter(kantenSelektion => kantenSelektion.kante.id !== kanteId)
        );
      }
    });
  }

  private selectSegment(
    kanteId: number,
    segmentIndex: number,
    additiv: boolean,
    seitenbezug: Seitenbezug | undefined
  ): void {
    invariant(
      this.selektion.find(selektion => selektion.kante.id === kanteId),
      'Es können nur Elemente bereits selektierter Kanten ausgewählt werden'
    );
    const bestehendeKantenSelektionIndex = this.selektion.findIndex(selektion => selektion.kante.id === kanteId);
    const updatedSelektion = this.selektion[bestehendeKantenSelektionIndex].selectSegment(
      segmentIndex,
      seitenbezug,
      additiv
    );
    let neueSelektion: KantenSelektion[];
    if (additiv) {
      neueSelektion = [...this.selektion];
      neueSelektion[bestehendeKantenSelektionIndex] = updatedSelektion;
      this.selektionSubject.next(neueSelektion);
    } else {
      neueSelektion = [updatedSelektion];
      if (this.selektion.length > 1) {
        this.canDeactivate().then(proceed => {
          if (proceed) {
            this.selektionSubject.next(neueSelektion);
          }
        });
      } else {
        this.selektionSubject.next(neueSelektion);
      }
    }
  }

  private deselectElement(kanteId: number, segmentIndex: number, seitenbezug?: Seitenbezug): void {
    invariant(this.isKantenelementSelektiert(kanteId, segmentIndex, seitenbezug));
    const bestehendeKantenSelektionIndex = this.selektion.findIndex(selektion => selektion.kante.id === kanteId);
    const bestehendeKantenSelektion = this.selektion[bestehendeKantenSelektionIndex];
    const neueKantenSelektion = bestehendeKantenSelektion.deselectSegment(segmentIndex, seitenbezug);
    const neueSelektion = [...this.selektion];
    neueSelektion[bestehendeKantenSelektionIndex] = neueKantenSelektion;
    this.selektionSubject.next(neueSelektion);
  }

  private resetSegmentIndices(selektion: KantenSelektion[]): KantenSelektion[] {
    return selektion.map(kantenSelektion => {
      let neueKantenselektion: KantenSelektion = kantenSelektion;
      if (this.activeAttributGruppe === AttributGruppe.ZUSTAENDIGKEIT) {
        const segmentAnzahl = kantenSelektion.kante.zustaendigkeitAttributGruppe.zustaendigkeitAttribute.length;
        neueKantenselektion = kantenSelektion.reduceSegmentAnzahl(segmentAnzahl);
      } else if (this.activeAttributGruppe === AttributGruppe.GESCHWINDIGKEIT) {
        const segmentAnzahl = kantenSelektion.kante.geschwindigkeitAttributGruppe.geschwindigkeitAttribute.length;
        neueKantenselektion = kantenSelektion.reduceSegmentAnzahl(segmentAnzahl);
      } else if (this.activeAttributGruppe === AttributGruppe.FUEHRUNGSFORM) {
        const segmentAnzahlLinks = kantenSelektion.kante.fuehrungsformAttributGruppe.fuehrungsformAttributeLinks.length;
        const segmentAnzahlRechts =
          kantenSelektion.kante.fuehrungsformAttributGruppe.fuehrungsformAttributeRechts.length;
        neueKantenselektion = kantenSelektion.reduceSegmentAnzahl(segmentAnzahlLinks, segmentAnzahlRechts);
      }
      return neueKantenselektion;
    });
  }
}
