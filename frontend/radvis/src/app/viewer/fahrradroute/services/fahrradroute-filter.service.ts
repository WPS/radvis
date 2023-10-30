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
import { FahrradrouteListenView } from 'src/app/viewer/fahrradroute/models/fahrradroute-listen-view';
import { FAHRRADROUTE } from 'src/app/viewer/fahrradroute/models/fahrradroute.infrastruktur';
import { FahrradrouteService } from 'src/app/viewer/fahrradroute/services/fahrradroute.service';
import { AbstractInfrastrukturenFilterService } from 'src/app/viewer/viewer-shared/services/abstract-infrastrukturen-filter.service';
import { FilterQueryParamsService } from 'src/app/viewer/viewer-shared/services/filter-query-params.service';
import { InfrastrukturenSelektionService } from 'src/app/viewer/viewer-shared/services/infrastrukturen-selektion.service';
import { FahrradrouteDetailView } from 'src/app/viewer/fahrradroute/models/fahrradroute-detail-view';

@Injectable({
  providedIn: 'root',
})
export class FahrradrouteFilterService extends AbstractInfrastrukturenFilterService<FahrradrouteListenView> {
  constructor(
    private fahrradrouteService: FahrradrouteService,
    infrastrukturenSelektionService: InfrastrukturenSelektionService,
    filterQueryParamsService: FilterQueryParamsService
  ) {
    super(infrastrukturenSelektionService, FAHRRADROUTE, filterQueryParamsService);
    this.init();
  }

  public getById(id: number): Promise<FahrradrouteDetailView> {
    return this.fahrradrouteService.getFahrradroute(id);
  }

  protected getAll(): Promise<FahrradrouteListenView[]> {
    return this.fahrradrouteService.getAll();
  }

  protected getInfrastrukturValueForKey(item: FahrradrouteListenView, key: string): string | string[] {
    return FahrradrouteListenView.getDisplayValueForKey(item, key);
  }
}
