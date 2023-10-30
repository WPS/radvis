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

import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, forwardRef, OnDestroy } from '@angular/core';
import { Observable } from 'rxjs';
import { ImportprotokollListView } from 'src/app/viewer/importprotokolle/models/importprotokoll-list-view';
import { ImportprotokollTyp } from 'src/app/viewer/importprotokolle/models/importprotokoll-typ';
import { IMPORTPROTOKOLLE } from 'src/app/viewer/importprotokolle/models/importprotokoll.infrastruktur';
import { ImportprotokollRoutingService } from 'src/app/viewer/importprotokolle/services/importprotokoll-routing.service';
import { ImportprotokollService } from 'src/app/viewer/importprotokolle/services/importprotokoll.service';
import { AbstractInfrastrukturenFilterService } from 'src/app/viewer/viewer-shared/services/abstract-infrastrukturen-filter.service';
import { FilterQueryParamsService } from 'src/app/viewer/viewer-shared/services/filter-query-params.service';
import { InfrastrukturenSelektionService } from 'src/app/viewer/viewer-shared/services/infrastrukturen-selektion.service';

@Component({
  selector: 'rad-importprotokolle-tabelle',
  templateUrl: './importprotokolle-tabelle.component.html',
  styleUrls: ['./importprotokolle-tabelle.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    { provide: AbstractInfrastrukturenFilterService, useExisting: forwardRef(() => ImportprotokolleTabelleComponent) },
  ],
})
export class ImportprotokolleTabelleComponent
  extends AbstractInfrastrukturenFilterService<ImportprotokollListView>
  implements OnDestroy {
  displayedColumns: string[];

  public selectedImportprotokollId$: Observable<number | null>;

  private columnMapping: Map<string, string> = new Map([
    ['importprotokollTyp', 'Typ'],
    ['importQuelle', 'Import aus'],
    ['startZeit', 'Gestartet am'],
  ]);

  constructor(
    private importprotokollService: ImportprotokollService,
    infrastrukturenSelektionService: InfrastrukturenSelektionService,
    filterQueryParamsService: FilterQueryParamsService,
    private importprotokollRoutingService: ImportprotokollRoutingService
  ) {
    super(infrastrukturenSelektionService, IMPORTPROTOKOLLE, filterQueryParamsService);
    this.selectedImportprotokollId$ = importprotokollRoutingService.selectedInfrastrukturId$;
    this.displayedColumns = Array.from(this.columnMapping.keys());
    this.init();
  }

  getHeader(key: string): string {
    return this.columnMapping.get(key) ?? '';
  }

  ngOnDestroy(): void {
    this.fetchSubscription?.unsubscribe();
  }

  onEintragSelected(importprotokoll: ImportprotokollListView): void {
    this.importprotokollRoutingService.toProtokollEintrag(importprotokoll.id, importprotokoll.importprotokollTyp);
  }

  public getInfrastrukturValueForKey(item: ImportprotokollListView, key: string): string | string[] {
    if (key === 'importprotokollTyp') {
      return ImportprotokollTyp.getDisplayName(item.importprotokollTyp);
    } else if (key === 'importQuelle') {
      return item.importQuelle;
    } else if (key === 'startZeit') {
      return new DatePipe('en-US').transform(item.startZeit, 'dd.MM.yy HH:mm') ?? '';
    }
    return '';
  }

  protected getAll(): Promise<ImportprotokollListView[]> {
    return this.importprotokollService.findAll();
  }
}
