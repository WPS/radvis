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

describe(FieldFilter.name, () => {
  describe('stringify / from String', () => {
    const strEncoded = 'field:' + encodeURIComponent('.:;/()&%$"![]{}=ß?`\\');
    const fieldFilter = new FieldFilter('field', '.:;/()&%$"![]{}=ß?`\\');

    it('should encode special chars', () => {
      const stringified = fieldFilter.stringify();
      expect(stringified).toEqual(strEncoded);
    });

    it('should decode special chars', () => {
      expect(FieldFilter.fromString(strEncoded)).toEqual(fieldFilter);
    });

    it('should be bidirectional', () => {
      expect(FieldFilter.fromString(strEncoded).stringify()).toEqual(strEncoded);
      expect(FieldFilter.fromString(fieldFilter.stringify())).toEqual(fieldFilter);
    });
  });
});
