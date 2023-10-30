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
})
export class WegweisendeBeschilderungTabelleComponent {
  selectedBeschilderungsId$: Observable<number | null>;

  displayedColumns: string[] = [
    'pfostenNr',
    'wegweiserTyp',
    'pfostenTyp',
    'zustandsbewertung',
    'pfostenzustand',
    'pfostendefizit',
    'gemeinde',
    'kreis',
    'land',
    'zustaendig',
  ];

  data$: Observable<WegweisendeBeschilderungListenView[]>;
  isSmallViewport = false;

  constructor(
    public wegweisendeBeschilderungFilterService: WegweisendeBeschilderungFilterService,
    private wegweisendeBeschilderungRoutingService: WegweisendeBeschilderungRoutingService
  ) {
    this.data$ = this.wegweisendeBeschilderungFilterService.filteredList$;
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
