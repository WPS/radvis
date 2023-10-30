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

import { ChangeDetectionStrategy, Component } from '@angular/core';
import { Observable } from 'rxjs';
import { FahrradzaehlstelleListenView } from 'src/app/viewer/fahrradzaehlstelle/models/fahrradzaehlstelle-listen-view';
import { FahrradzaehlstelleFilterService } from 'src/app/viewer/fahrradzaehlstelle/services/fahrradzaehlstelle-filter.service';
import { FahrradzaehlstelleRoutingService } from 'src/app/viewer/fahrradzaehlstelle/services/fahrradzaehlstelle-routing.service';
import { ExportFormat } from 'src/app/viewer/viewer-shared/models/export-format';
import { AbstractInfrastrukturenFilterService } from 'src/app/viewer/viewer-shared/services/abstract-infrastrukturen-filter.service';

@Component({
  selector: 'rad-fahrradzaehlstelle-tabelle',
  templateUrl: './fahrradzaehlstelle-tabelle.component.html',
  styleUrls: ['./fahrradzaehlstelle-tabelle.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: AbstractInfrastrukturenFilterService,
      useExisting: FahrradzaehlstelleFilterService,
    },
  ],
})
export class FahrradzaehlstelleTabelleComponent {
  data$: Observable<FahrradzaehlstelleListenView[]>;
  displayedColumns: string[];
  selectedID$: Observable<number | null>;
  exporting = false;
  exportFormate = [ExportFormat.CSV];
  isSmallViewport = false;
  private columnMapping: Map<string, string> = new Map([
    ['betreiberEigeneId', 'Betreiber eigene ID'],
    ['fahrradzaehlstelleGebietskoerperschaft', 'Gebietskörperschaft'],
    ['fahrradzaehlstelleBezeichnung', 'Bezeichnung'],
    ['seriennummer', 'Seriennummer'],
    ['zaehlintervall', 'Intervall'],
    ['neusterZeitstempel', 'Letzte Aktualisierung'],
  ]);

  constructor(
    public filterService: FahrradzaehlstelleFilterService,
    private routingService: FahrradzaehlstelleRoutingService
  ) {
    this.data$ = this.filterService.filteredList$;
    this.selectedID$ = this.routingService.selectedInfrastrukturId$;
    this.displayedColumns = Array.from(this.columnMapping.keys());
  }

  onChangeBreakpointState(isSmall: boolean): void {
    this.isSmallViewport = isSmall;
  }

  getHeader(key: string): string {
    return this.columnMapping.get(key) ?? '';
  }

  onSelectRecord(selectedId: number): void {
    this.routingService.toInfrastrukturEditor(selectedId);
  }

  onFilterReset(): void {
    this.filterService.reset();
  }

  public sortingDataAccessor = (item: FahrradzaehlstelleListenView, header: string): any => {
    const displayValue = FahrradzaehlstelleListenView.getSortingValueForKey(item, header);
    return Array.isArray(displayValue) ? displayValue[0] : displayValue;
  };
}