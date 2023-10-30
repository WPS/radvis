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

import { FieldFilter } from 'src/app/viewer/viewer-shared/models/field-filter';
import { FilterQueryParams } from 'src/app/viewer/viewer-shared/models/filter-query-params';

describe(FilterQueryParams.name, () => {
  describe('route params conversion', () => {
    it('should be bidirectional if empty', () => {
      const leereParams = new FilterQueryParams(new Map<string, FieldFilter[]>());
      const asParams = leereParams.toRouteParams();
      expect(FilterQueryParams.fromRoute(asParams)).toEqual(leereParams);
    });

    it('should be bidirectional if not empty', () => {
      const map = new Map<string, FieldFilter[]>();
      map.set('infrastructure', [new FieldFilter('field', 'value')]);
      map.set('otherInfrastructure', [new FieldFilter('otherfield', 'othervalue')]);
      const volleParams = new FilterQueryParams(map);
      const asParams = volleParams.toRouteParams();
      expect(FilterQueryParams.fromRoute(asParams)).toEqual(volleParams);
    });
  });

  describe('reset', () => {
    it('should work', () => {
      const map = new Map<string, FieldFilter[]>();
      const infrastrukturArt1 = 'infrastructure';
      map.set(infrastrukturArt1, [new FieldFilter('field', 'value')]);
      const infrastrukturArt2 = 'otherInfrastructure';
      map.set(infrastrukturArt2, [new FieldFilter('otherfield', 'othervalue')]);
      const filterQueryParams = new FilterQueryParams(map);

      filterQueryParams.resetInfrastrukturArt(infrastrukturArt1);

      expect(filterQueryParams.filters.get(infrastrukturArt1)).toBeUndefined();
      expect(filterQueryParams.filters.get(infrastrukturArt2)).toBeDefined();
    });
  });

  describe('equals', () => {
    it('should give true for both null', () => {
      const result = FilterQueryParams.filterQueryParamsEquals(null, null);

      expect(result).toEqual(true);
    });

    it('should give false for only one of them null', () => {
      const map = new Map<string, FieldFilter[]>();
      map.set('infrastructure', [new FieldFilter('field', 'value')]);
      map.set('otherInfrastructure', [new FieldFilter('otherfield', 'othervalue')]);

      const result1 = FilterQueryParams.filterQueryParamsEquals(new FilterQueryParams(map), null);
      const result2 = FilterQueryParams.filterQueryParamsEquals(null, new FilterQueryParams(map));

      expect(result1).toEqual(false);
      expect(result2).toEqual(false);
    });

    it('should give false for different Params', () => {
      const map1 = new Map<string, FieldFilter[]>();
      map1.set('infrastructure', [new FieldFilter('field', 'value')]);
      map1.set('otherInfrastructure', [new FieldFilter('otherfield', 'othervalue')]);
      const filterQueryParams1 = new FilterQueryParams(map1);

      const map2 = new Map<string, FieldFilter[]>();
      map2.set('infrastructure', [new FieldFilter('field', 'value')]);
      map2.set('otherInfrastructure', [new FieldFilter('otherfield', 'othervalue')]);
      map2.set('yetAnotherInfrastructure', [new FieldFilter('otherfield2', 'othervalue2')]);
      const filterQueryParams2 = new FilterQueryParams(map2);

      const result = FilterQueryParams.filterQueryParamsEquals(filterQueryParams1, filterQueryParams2);

      expect(result).toEqual(false);
    });

    it('should give true for same params in different objects', () => {
      const map1 = new Map<string, FieldFilter[]>();
      map1.set('infrastructure', [new FieldFilter('field', 'value')]);
      map1.set('otherInfrastructure', [new FieldFilter('otherfield', 'othervalue')]);
      const filterQueryParams1 = new FilterQueryParams(map1);

      const map2 = new Map<string, FieldFilter[]>();
      map2.set('infrastructure', [new FieldFilter('field', 'value')]);
      map2.set('otherInfrastructure', [new FieldFilter('otherfield', 'othervalue')]);
      const filterQueryParams2 = new FilterQueryParams(map2);

      const result = FilterQueryParams.filterQueryParamsEquals(filterQueryParams1, filterQueryParams2);

      expect(result).toEqual(true);
    });
  });
});
