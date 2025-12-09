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
import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';
import { ServicestationListView } from 'src/app/viewer/servicestation/models/servicestation-list-view';
import { ServicestationQuellSystem } from 'src/app/viewer/servicestation/models/servicestation-quell-system';
import { ServicestationStatus } from 'src/app/viewer/servicestation/models/servicestation-status';
import { ServicestationTyp } from 'src/app/viewer/servicestation/models/servicestation-typ';
import { SERVICESTATIONEN } from 'src/app/viewer/servicestation/models/servicestation.infrastruktur';
import { ServicestationService } from 'src/app/viewer/servicestation/services/servicestation.service';
import { AbstractInfrastrukturenFilterService } from 'src/app/viewer/viewer-shared/services/abstract-infrastrukturen-filter.service';
import { FilterQueryParamsService } from 'src/app/viewer/viewer-shared/services/filter-query-params.service';
import { InfrastrukturenSelektionService } from 'src/app/viewer/viewer-shared/services/infrastrukturen-selektion.service';

@Injectable({
  providedIn: 'root',
})
export class ServicestationFilterService extends AbstractInfrastrukturenFilterService<ServicestationListView> {
  constructor(
    infrastrukturenSelektionService: InfrastrukturenSelektionService,
    filterQueryParamsService: FilterQueryParamsService,
    private servicestationService: ServicestationService
  ) {
    super(infrastrukturenSelektionService, SERVICESTATIONEN, filterQueryParamsService);

    this.init();
  }

  public getInfrastrukturValueForKey(item: ServicestationListView, key: string): string | string[] {
    switch (key) {
      case 'quellSystem':
        return ServicestationQuellSystem.getDisplayText(item.quellSystem);
      case 'typ':
        return ServicestationTyp.getDisplayText(item.typ);
      case 'status':
        return ServicestationStatus.getDisplayText(item.status);
      case 'organisation':
        return Verwaltungseinheit.getDisplayName(item.organisation);
      case 'gebuehren':
      case 'luftpumpe':
      case 'kettenwerkzeug':
      case 'werkzeug':
      case 'fahrradhalterung':
      case 'radkultur':
        return item[key] ? 'ja' : 'nein';
      default:
        return (Object.entries(item).find(entry => entry[0] === key)?.[1] ?? '').toString();
    }
  }

  public onServicestationDeleted(id: number): void {
    this.onAlleInfrastrukturenChanged(
      this.alleInfrastrukturen.filter((value: ServicestationListView) => value.id !== id)
    );
  }

  protected getAll(): Promise<ServicestationListView[]> {
    return this.servicestationService.getAll();
  }
}
