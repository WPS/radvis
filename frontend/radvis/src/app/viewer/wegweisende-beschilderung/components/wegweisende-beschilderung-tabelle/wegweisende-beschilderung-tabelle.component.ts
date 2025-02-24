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

import { ChangeDetectionStrategy, Component, forwardRef } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { SpaltenDefinition } from 'src/app/viewer/viewer-shared/models/spalten-definition';
import { AbstractInfrastrukturenFilterService } from 'src/app/viewer/viewer-shared/services/abstract-infrastrukturen-filter.service';
import { WegweisendeBeschilderungListenView } from 'src/app/viewer/wegweisende-beschilderung/models/wegweisende-beschilderung-listen-view';
import { WegweisendeBeschilderungFilterService } from 'src/app/viewer/wegweisende-beschilderung/services/wegweisende-beschilderung-filter.service';
import { WegweisendeBeschilderungRoutingService } from 'src/app/viewer/wegweisende-beschilderung/services/wegweisende-beschilderung-routing.service';

@Component({
  selector: 'rad-wegweisende-beschilderung-tabelle',
  templateUrl: './wegweisende-beschilderung-tabelle.component.html',
  styleUrls: ['./wegweisende-beschilderung-tabelle.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: AbstractInfrastrukturenFilterService,
      useExisting: forwardRef(() => WegweisendeBeschilderungFilterService),
    },
  ],
  standalone: false,
})
export class WegweisendeBeschilderungTabelleComponent {
  selectedBeschilderungsId$: Observable<number | null>;

  spaltenDefinition: SpaltenDefinition[] = [
    { name: 'pfostenNr', displayName: 'Pfostennummer' },
    { name: 'wegweiserTyp', displayName: 'Wegweisertyp', width: 'large' },
    { name: 'pfostenTyp', displayName: 'Pfostentyp', width: 'large' },
    { name: 'zustandsbewertung', displayName: 'Zustandsbewertung' },
    { name: 'pfostenzustand', displayName: 'Pfostenzustand' },
    { name: 'pfostendefizit', displayName: 'Pfostendefizit', width: 'large' },
    { name: 'gemeinde', displayName: 'Gemeinde', width: 'large' },
    { name: 'kreis', displayName: 'Kreis', width: 'large' },
    { name: 'land', displayName: 'Land', width: 'large' },
    { name: 'zustaendig', displayName: 'Zuständige Organisation' },
  ];

  data$: Observable<WegweisendeBeschilderungListenView[]>;
  isSmallViewport = false;
  filteredSpalten$: Observable<string[]>;

  constructor(
    public wegweisendeBeschilderungFilterService: WegweisendeBeschilderungFilterService,
    private wegweisendeBeschilderungRoutingService: WegweisendeBeschilderungRoutingService
  ) {
    this.data$ = this.wegweisendeBeschilderungFilterService.filteredList$;
    this.filteredSpalten$ = this.wegweisendeBeschilderungFilterService.filter$.pipe(
      map(filteredFields => filteredFields.map(f => f.field))
    );
    this.selectedBeschilderungsId$ = wegweisendeBeschilderungRoutingService.selectedInfrastrukturId$;
  }

  onChangeBreakpointState(isSmall: boolean): void {
    this.isSmallViewport = isSmall;
  }

  public onSelectRecord(id: number): void {
    this.wegweisendeBeschilderungRoutingService.toInfrastrukturEditor(id);
  }

  onFilterReset(): void {
    this.wegweisendeBeschilderungFilterService.reset();
  }
}
