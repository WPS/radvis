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
import { Leihstation } from 'src/app/viewer/leihstation/models/leihstation';
import { LEIHSTATIONEN } from 'src/app/viewer/leihstation/models/leihstation.infrastruktur';
import { LeihstationService } from 'src/app/viewer/leihstation/services/leihstation.service';
import { AbstractInfrastrukturenFilterService } from 'src/app/viewer/viewer-shared/services/abstract-infrastrukturen-filter.service';
import { FilterQueryParamsService } from 'src/app/viewer/viewer-shared/services/filter-query-params.service';
import { InfrastrukturenSelektionService } from 'src/app/viewer/viewer-shared/services/infrastrukturen-selektion.service';
import { LeihstationStatus } from 'src/app/viewer/leihstation/models/leihstation-status';
import { LeihstationQuellSystem } from 'src/app/viewer/leihstation/models/leihstation-quell-system';

@Injectable({
  providedIn: 'root',
})
export class LeihstationFilterService extends AbstractInfrastrukturenFilterService<Leihstation> {
  constructor(
    infrastrukturenSelektionService: InfrastrukturenSelektionService,
    filterQueryParamsService: FilterQueryParamsService,
    private leihstationService: LeihstationService
  ) {
    super(infrastrukturenSelektionService, LEIHSTATIONEN, filterQueryParamsService);

    this.init();
  }

  public getInfrastrukturValueForKey(item: Leihstation, key: string): string | string[] {
    switch (key) {
      case 'status':
        return item.status ? LeihstationStatus.getDisplayText(item.status) : '';
      case 'freiesAbstellen':
        return item[key] ? 'ja' : 'nein';
      case 'quellSystem':
        return LeihstationQuellSystem.getDisplayText(item.quellSystem);
      default:
        return (Object.entries(item).find(entry => entry[0] === key)?.[1] ?? '').toString();
    }
  }

  public onLeihstationDeleted(id: number): void {
    this.onAlleInfrastrukturenChanged(this.alleInfrastrukturen.filter((value: Leihstation) => value.id !== id));
  }

  protected getAll(): Promise<Leihstation[]> {
    return this.leihstationService.getAll();
  }
}
