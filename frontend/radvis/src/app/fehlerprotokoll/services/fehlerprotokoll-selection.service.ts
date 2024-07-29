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
import { Extent } from 'ol/extent';
import { BehaviorSubject, Observable, combineLatest, of } from 'rxjs';
import { distinctUntilChanged, map } from 'rxjs/operators';
import { FehlerprotokollTyp } from 'src/app/fehlerprotokoll/models/fehlerprotokoll-typ';
import { FehlerprotokollView } from 'src/app/fehlerprotokoll/models/fehlerprotokoll-view';
import { Konsistenzregel } from 'src/app/shared/models/konsistenzregel';
import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';

export type FehlerprotokollLoader = (extent: Extent) => Observable<FehlerprotokollView[]>;

@Injectable({
  providedIn: 'root',
})
export class FehlerprotokollSelectionService {
  private readonly DEFAULT_MIN_ZOOM = 11.5;
  public fehlerprotokollLoader$: BehaviorSubject<FehlerprotokollLoader> = new BehaviorSubject<FehlerprotokollLoader>(
    () => of([])
  );
  private selectedFehlerprotokollTypen$$ = new BehaviorSubject<FehlerprotokollTyp[]>([]);
  private selectedKonsistenzregelVerletzungen$$: BehaviorSubject<string[]> = new BehaviorSubject<string[]>([]);

  public selectedOrganisation: Verwaltungseinheit | null = null;
  public netzklassenImportSelected = false;
  public attributeImportSelected = false;

  public get selectedKonsistenzregelVerletzungen(): string[] {
    return [...this.selectedKonsistenzregelVerletzungen$$.value];
  }

  public get selectedFehlerprotokollTypen(): FehlerprotokollTyp[] {
    return [...this.selectedFehlerprotokollTypen$$.value];
  }

  public get minZoom$(): Observable<number> {
    return combineLatest([this.selectedFehlerprotokollTypen$$, this.selectedKonsistenzregelVerletzungen$$]).pipe(
      map(([protokolle, regeln]) => this.aggregateMinZoom(protokolle, regeln)),
      distinctUntilChanged()
    );
  }

  public get minZoom(): number {
    return this.aggregateMinZoom(this.selectedFehlerprotokollTypen, this.selectedKonsistenzregelVerletzungen);
  }

  public selectFehlerprotokoll(fehlerprotokollTyp: FehlerprotokollTyp): void {
    this.selectedFehlerprotokollTypen$$.next([...this.selectedFehlerprotokollTypen, fehlerprotokollTyp]);
  }

  public unselectFehlerprotokoll(fehlerprotokollTyp: FehlerprotokollTyp): void {
    this.selectedFehlerprotokollTypen$$.next(this.selectedFehlerprotokollTypen.filter(f => f !== fehlerprotokollTyp));
  }

  public selectKonsistenzregel(konsistenzregel: Konsistenzregel): void {
    this.selectedKonsistenzregelVerletzungen$$.next([
      ...this.selectedKonsistenzregelVerletzungen,
      konsistenzregel.verletzungsTyp,
    ]);
  }

  public unselectKonsistenzregel(konsistenzregel: Konsistenzregel): void {
    this.selectedKonsistenzregelVerletzungen$$.next(
      this.selectedKonsistenzregelVerletzungen.filter(k => k !== konsistenzregel.verletzungsTyp)
    );
  }

  private aggregateMinZoom(
    selectedFehlerprotokollTypen: FehlerprotokollTyp[],
    selectedKonsistenzregelVerletzungsTypen: string[]
  ): number {
    let zoomFehlerprotokolle = this.DEFAULT_MIN_ZOOM;
    if (selectedFehlerprotokollTypen.length > 0) {
      zoomFehlerprotokolle = selectedFehlerprotokollTypen.map(f => f.minZoom).reduce((a, b) => Math.max(a, b), 0);
    }

    if (selectedKonsistenzregelVerletzungsTypen.length > 0) {
      return Math.max(zoomFehlerprotokolle, this.DEFAULT_MIN_ZOOM);
    }

    return zoomFehlerprotokolle;
  }
}
