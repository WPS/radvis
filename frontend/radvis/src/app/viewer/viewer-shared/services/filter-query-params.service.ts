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

import { Inject, Injectable } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable } from 'rxjs';
import { distinctUntilChanged, map } from 'rxjs/operators';
import { AbstractQueryParamsService } from 'src/app/shared/services/abstract-query-params.service';
import { FieldFilter } from 'src/app/viewer/viewer-shared/models/field-filter';
import { FilterQueryParams } from 'src/app/viewer/viewer-shared/models/filter-query-params';
import { Infrastruktur, InfrastrukturToken } from 'src/app/viewer/viewer-shared/models/infrastruktur';

@Injectable({
  providedIn: 'root',
})
export class FilterQueryParamsService extends AbstractQueryParamsService<FilterQueryParams> {
  filter$: Observable<Map<Infrastruktur, FieldFilter[]>>;

  constructor(
    private activatedRoute: ActivatedRoute,
    router: Router,
    @Inject(InfrastrukturToken) private infrastrukturen: Infrastruktur[]
  ) {
    super(router);
    this.filter$ = this.activatedRoute.queryParams.pipe(
      map(params => FilterQueryParams.fromRoute(params)),
      distinctUntilChanged((prev, curr) => FilterQueryParams.filterQueryParamsEquals(prev, curr)),
      map(filterQueryParams => this.filterQueryParamsToMap(filterQueryParams))
    );
  }

  public get filterQueryParamsSnapshot(): FilterQueryParams {
    return FilterQueryParams.fromRoute(this.activatedRoute.snapshot.queryParams);
  }

  public update(filters: FieldFilter[], infrastruktur: Infrastruktur): void {
    const filterMap = new Map<string, FieldFilter[]>();
    filterMap.set(infrastruktur.pathElement, filters);
    const filterQueryParams = FilterQueryParams.merge({ filters: filterMap }, this.filterQueryParamsSnapshot);
    this.updateInUrl(filterQueryParams);
  }

  reset(infrastrukturArt: Infrastruktur): void {
    const filterQueryParamsSnapshot = this.filterQueryParamsSnapshot;
    filterQueryParamsSnapshot.resetInfrastrukturArt(infrastrukturArt.pathElement);
    this.updateInUrl(filterQueryParamsSnapshot);
  }

  private filterQueryParamsToMap(filterQueryParams: FilterQueryParams): Map<Infrastruktur, FieldFilter[]> {
    const result = new Map<Infrastruktur, FieldFilter[]>();
    for (const [pathElement, fieldFilters] of filterQueryParams.filters) {
      const infrastruktur = this.infrastrukturen.find(infra => infra.pathElement === pathElement);
      if (infrastruktur && fieldFilters.length > 0) {
        result.set(infrastruktur, fieldFilters);
      }
    }
    return result;
  }
}
