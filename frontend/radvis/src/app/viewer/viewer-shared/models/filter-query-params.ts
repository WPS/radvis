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

import { Params } from '@angular/router';
import { AbstractQueryParams } from 'src/app/shared/models/abstract-query-params';
import { FieldFilter } from 'src/app/viewer/viewer-shared/models/field-filter';

export class FilterQueryParams extends AbstractQueryParams {
  private readonly QUERY_PARAM_PREFIX = 'filter_';

  get filters(): Map<string, FieldFilter[]> {
    return this._filters;
  }

  constructor(private _filters: Map<string, FieldFilter[]>) {
    super();
  }

  public static fromRoute(params: Params): FilterQueryParams {
    const filters = new Map<string, FieldFilter[]>();

    Object.entries(params)
      .filter(([key]) => key.startsWith('filter_'))
      .forEach(([key, value]) => {
        const pathElement = this.filterKeyToPathElement(key);
        if (pathElement) {
          filters.set(pathElement, this.paramToFieldFilterArray(value));
        }
      });
    return new FilterQueryParams(filters);
  }

  public static merge(
    params: { filters: Map<string, FieldFilter[]> } | FilterQueryParams,
    into: FilterQueryParams
  ): FilterQueryParams {
    const newFilterMap = new Map<string, FieldFilter[]>(into.filters);
    for (const [pathElement, fieldFilters] of params.filters) {
      let newFieldFilters = newFilterMap.get(pathElement) ?? [];
      fieldFilters.forEach(fieldFilter => {
        newFieldFilters = newFieldFilters.filter(fF => fF.field !== fieldFilter.field);
        if (fieldFilter.value !== '') {
          newFieldFilters.push(fieldFilter);
        }
      });
      if (newFieldFilters.length > 0) {
        newFilterMap.set(pathElement, newFieldFilters);
      } else {
        newFilterMap.delete(pathElement);
      }
    }

    return new FilterQueryParams(newFilterMap);
  }

  public static filterQueryParamsEquals(
    filterQueryParams1: FilterQueryParams | null,
    filterQueryParams2: FilterQueryParams | null
  ): boolean {
    return filterQueryParams1?.toRoute() === filterQueryParams2?.toRoute();
  }

  private static paramToFieldFilterArray(param: string | undefined): FieldFilter[] {
    if (param) {
      return param
        .split(',')
        .filter(FieldFilter.isValidFilterString)
        .map(filterStr => FieldFilter.fromString(filterStr));
    }
    return [];
  }

  private static filterKeyToPathElement(key: string | undefined): string | null {
    if (key && key.includes('_')) {
      return key.split('_')[1] ?? null;
    }
    return null;
  }

  public toRouteParams(): Params {
    const params: Params = {};
    for (const [pathElement, fieldFilters] of this.filters) {
      if (fieldFilters.length > 0) {
        params[this.QUERY_PARAM_PREFIX + pathElement] = fieldFilters
          .map(fieldFilter => fieldFilter.stringify())
          .join(',');
      }
    }
    return params;
  }

  public resetInfrastrukturArt(infastrukturArt: string): void {
    this.filters.delete(infastrukturArt);
  }

  // eslint-disable-next-line prettier/prettier
  public override replaceIn(params: Params): Params {
    const cleanParams: Params = {};
    Object.keys(params).forEach(param => {
      if (!param.startsWith(this.QUERY_PARAM_PREFIX)) {
        cleanParams[param] = params[param];
      }
    });

    return super.replaceIn(cleanParams);
  }
}
