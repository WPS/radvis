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
import { KantenSeite } from 'src/app/shared/models/kantenSeite';
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
      this.adjustSelectionForNewAttributgruppe(this.activeAttributGruppe!);
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

  public select(kanteId: number, additiv: boolean, kantenSeite?: KantenSeite, segmentIndex?: number): void {
    if (segmentIndex !== undefined) {
      this.selectSegment(kanteId, segmentIndex, additiv, kantenSeite);
    } else {
      if (kantenSeite) {
        this.selectKantenseite(kanteId, additiv, kantenSeite);
      } else {
        this.selectGesamteKante(kanteId, additiv);
      }
    }
  }

  public deselect(kanteId: number, kantenSeite?: KantenSeite, segmentIndex?: number): void {
    invariant(this.isKanteSelektiert(kanteId), 'Deselect für nicht selektierte Kante nicht möglich');
    if (segmentIndex !== undefined) {
      invariant(this.isKantenelementSelektiert(kanteId, segmentIndex, kantenSeite));

      if (this.isLastSelectedElement(kanteId, kantenSeite)) {
        this.deselectGesamteKante(kanteId);
      } else {
        this.deselectElement(kanteId, segmentIndex, kantenSeite);
      }
    } else {
      if (kantenSeite && this.isKantenseiteSelektiert(kanteId, KantenSeite.getGegenseite(kantenSeite))) {
        this.deselectKantenseite(kanteId, kantenSeite);
      } else {
        this.deselectGesamteKante(kanteId);
      }
    }
  }

  public adjustSelectionForSegmentInsertion(kanteId: number, segmentIndex: number, kantenSeite?: KantenSeite): void {
    invariant(this.isKanteSelektiert(kanteId));

    const bestehendeKantenSelektionIndex = this.selektion.findIndex(selektion => selektion.kante.id === kanteId);
    const bestehendeKantenSelektion = this.selektion[bestehendeKantenSelektionIndex];
    const neueSelektion = [...this.selektion];
    neueSelektion[bestehendeKantenSelektionIndex] = bestehendeKantenSelektion.insertSegment(segmentIndex, kantenSeite);

    this.selektionSubject.next(neueSelektion);
  }

  public adjustSelectionForSegmentDeletion(kanteId: number, segmentIndex: number, kantenSeite?: KantenSeite): void {
    invariant(this.isKanteSelektiert(kanteId));

    const neueSelektion = [...this.selektion];
    const bestehendeKantenSelektionIndex = this.selektion.findIndex(({ kante }) => kante.id === kanteId);
    neueSelektion[bestehendeKantenSelektionIndex] = this.selektion[bestehendeKantenSelektionIndex].deleteSegment(
      segmentIndex,
      kantenSeite
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
      kantenSelektion.updateKante(updated.find(({ id }) => id === kantenSelektion.kante.id)!)
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

  public isSelektiert(kanteId: number, kantenSeite?: KantenSeite): boolean {
    if (kantenSeite) {
      return this.isKantenseiteSelektiert(kanteId, kantenSeite);
    } else {
      return this.isKanteSelektiert(kanteId);
    }
  }

  public canDiscard(): boolean {
    return this.discardableComponent ? this.discardableComponent.canDiscard() : true;
  }

  private canDeactivate(): Promise<boolean> {
    if (this.discardableComponent) {
      return this.discardGuardService.canDeactivate(this.discardableComponent).toPromise();
    }
    return Promise.resolve(true);
  }

  private isKanteSelektiert(kanteId: number): boolean {
    return this.selektion.map(kantenSelektion => kantenSelektion.kante.id).includes(kanteId);
  }

  private isKantenseiteSelektiert(kanteId: number, kantenSeite: KantenSeite): boolean {
    const foundSelektion = this.selektion.find(selektion => selektion.kante.id === kanteId);
    if (foundSelektion) {
      return foundSelektion.istSeiteSelektiert(kantenSeite);
    } else {
      return false;
    }
  }

  private isKantenelementSelektiert(kanteId: number, segmentIndex: number, kantenSeite?: KantenSeite): boolean {
    const foundSelektion = this.selektion.find(selektion => selektion.kante.id === kanteId);
    if (foundSelektion) {
      return foundSelektion.istSegmentSelektiert(segmentIndex, kantenSeite);
    } else {
      return false;
    }
  }

  private isLastSelectedElement(kanteId: number, kantenSeite: KantenSeite | undefined): boolean {
    const currentSelektion = this.selektion.find(selektion => selektion.kante.id === kanteId);
    invariant(currentSelektion);
    if (kantenSeite !== undefined) {
      return (
        !currentSelektion.istSeiteSelektiert(KantenSeite.getGegenseite(kantenSeite)) &&
        currentSelektion.getSelectedSegmentIndices(kantenSeite).length === 1
      );
    } else {
      return (
        currentSelektion.getSelectedSegmentIndices(KantenSeite.LINKS).length === 1 &&
        currentSelektion.getSelectedSegmentIndices(KantenSeite.RECHTS).length === 1
      );
    }
  }

  private adjustSelectionForNewAttributgruppe(newAttributgruppe: AttributGruppe): void {
    const neueSelektion = this.selektion.map(kantenSelektion => {
      let neueKantenselektion: KantenSelektion;
      if (!AttributGruppe.isSeitenbezogen(newAttributgruppe) || kantenSelektion.istBeidseitigSelektiert()) {
        const segmentAnzahlLinks = Kante.getAnzahlSegmente(newAttributgruppe, kantenSelektion.kante, KantenSeite.LINKS);
        const segmentAnzahlRechts = Kante.getAnzahlSegmente(
          newAttributgruppe,
          kantenSelektion.kante,
          KantenSeite.RECHTS
        );
        neueKantenselektion = kantenSelektion.selectGesamteKante(segmentAnzahlLinks, segmentAnzahlRechts);
      } else if (kantenSelektion.istSeiteSelektiert(KantenSeite.LINKS)) {
        const segmentAnzahlLinks = Kante.getAnzahlSegmente(newAttributgruppe, kantenSelektion.kante, KantenSeite.LINKS);
        neueKantenselektion = kantenSelektion.selectSeite(KantenSeite.LINKS, segmentAnzahlLinks);
      } else {
        const segmentAnzahlRechts = Kante.getAnzahlSegmente(
          newAttributgruppe,
          kantenSelektion.kante,
          KantenSeite.RECHTS
        );
        neueKantenselektion = kantenSelektion.selectSeite(KantenSeite.RECHTS, segmentAnzahlRechts);
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
          const anzahlSegmenteLinks = Kante.getAnzahlSegmente(this.activeAttributGruppe!, newKante, KantenSeite.LINKS);
          const anzahlSegmenteRechts = Kante.getAnzahlSegmente(
            this.activeAttributGruppe!,
            newKante,
            KantenSeite.RECHTS
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

  private selectKantenseite(kanteId: number, additiv: boolean, kantenSeite: KantenSeite): void {
    const bestehendeKantenSelektionIndex = this.selektion.findIndex(selektion => selektion.kante.id === kanteId);
    if (bestehendeKantenSelektionIndex >= 0) {
      // Kante nicht nachladen
      const bestehendeKantenSelektion = this.selektion[bestehendeKantenSelektionIndex];
      const anzahlSegmente = Kante.getAnzahlSegmente(
        this.activeAttributGruppe!,
        bestehendeKantenSelektion.kante,
        kantenSeite
      );
      if (additiv) {
        const neueSelektion = [...this.selektion];
        neueSelektion[bestehendeKantenSelektionIndex] = bestehendeKantenSelektion.selectSeite(
          kantenSeite,
          anzahlSegmente,
          true
        );
        this.selektionSubject.next(neueSelektion);
      } else {
        const werdenKantenDeselektiert = this.selektion.some(kantenSelektion => kantenSelektion.kante.id !== kanteId);
        if (werdenKantenDeselektiert) {
          this.canDeactivate().then(proceed => {
            if (proceed) {
              this.selektionSubject.next([bestehendeKantenSelektion.selectSeite(kantenSeite, anzahlSegmente)]);
            }
          });
        } else {
          this.selektionSubject.next([bestehendeKantenSelektion.selectSeite(kantenSeite, anzahlSegmente)]);
        }
      }
    } else {
      // Kante nachladen
      this.canDeactivate().then(proceed => {
        if (proceed) {
          this.fetchKante(kanteId).then(newKante => {
            const anzahlSegmente = Kante.getAnzahlSegmente(this.activeAttributGruppe!, newKante, kantenSeite);
            if (additiv) {
              this.selektionSubject.next([
                ...this.resetSegmentIndices(this.selektion),
                KantenSelektion.ofSeite(newKante, kantenSeite, anzahlSegmente),
              ]);
            } else {
              this.selektionSubject.next([KantenSelektion.ofSeite(newKante, kantenSeite, anzahlSegmente)]);
            }
          });
        }
      });
    }
  }

  private deselectKantenseite(kanteId: number, kantenSeite: KantenSeite): void {
    invariant(this.isKantenseiteSelektiert(kanteId, kantenSeite));
    invariant(this.isKantenseiteSelektiert(kanteId, KantenSeite.getGegenseite(kantenSeite)));
    const bestehendeKantenSelektionIndex = this.selektion.findIndex(selektion => selektion.kante.id === kanteId);
    const neueKantenSelektion = this.selektion[bestehendeKantenSelektionIndex].deselectSeite(kantenSeite);
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
    kantenSeite: KantenSeite | undefined
  ): void {
    invariant(
      this.selektion.find(selektion => selektion.kante.id === kanteId),
      'Es können nur Elemente bereits selektierter Kanten ausgewählt werden'
    );
    const bestehendeKantenSelektionIndex = this.selektion.findIndex(selektion => selektion.kante.id === kanteId);
    const updatedSelektion = this.selektion[bestehendeKantenSelektionIndex].selectSegment(
      segmentIndex,
      kantenSeite,
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

  private deselectElement(kanteId: number, segmentIndex: number, kantenSeite?: KantenSeite): void {
    invariant(this.isKantenelementSelektiert(kanteId, segmentIndex, kantenSeite));
    const bestehendeKantenSelektionIndex = this.selektion.findIndex(selektion => selektion.kante.id === kanteId);
    const bestehendeKantenSelektion = this.selektion[bestehendeKantenSelektionIndex];
    const neueKantenSelektion = bestehendeKantenSelektion.deselectSegment(segmentIndex, kantenSeite);
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
