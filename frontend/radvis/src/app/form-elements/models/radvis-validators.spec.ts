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

import { AbstractControl, FormControl } from '@angular/forms';
import { RadvisValidators } from 'src/app/form-elements/models/radvis-validators';

describe('RadvisValidators', () => {
  describe(RadvisValidators.isAlphanumeric.name, () => {
    it('should return null if null', () => {
      expect(RadvisValidators.isAlphanumeric({ value: null } as AbstractControl)).toBeNull();
    });

    it('should return null if digit only', () => {
      expect(RadvisValidators.isAlphanumeric({ value: '9999999' } as AbstractControl)).toBeNull();
    });

    it('should return null if characterOnly', () => {
      expect(
        RadvisValidators.isAlphanumeric({
          value: 'abcdefghijklmnopqrstuvwxyzABCEDEFGHIJKLMNOPQRSTUVWXYZ',
        } as AbstractControl)
      ).toBeNull();
    });

    it('should not return null if non alpha numeric character is used', () => {
      expect(RadvisValidators.isAlphanumeric({ value: 'üäöÜÄÖ!"§$%&/()' } as AbstractControl)).not.toBeNull();
    });
  });

  describe(RadvisValidators.min.name, () => {
    it('should return null if exactly min', () => {
      expect(RadvisValidators.min(2000)({ value: 2000 } as AbstractControl)).toBeNull();
    });

    it('should return null if higher than min', () => {
      expect(RadvisValidators.min(2000)({ value: 9999999 } as AbstractControl)).toBeNull();
    });

    it('should not return null if lower than min', () => {
      expect(RadvisValidators.min(2000)({ value: 1999 } as AbstractControl)).not.toBeNull();
    });
  });

  describe(RadvisValidators.isPositiveInteger.name, () => {
    // valid
    it('should return null if positive integers', () => {
      expect(RadvisValidators.isPositiveInteger({ value: '9' } as AbstractControl)).toBeNull();
    });
    it('should return null if empty', () => {
      expect(RadvisValidators.isPositiveInteger({ value: '' } as AbstractControl)).toBeNull();
    });
    it('should return null if null', () => {
      expect(RadvisValidators.isPositiveInteger({ value: null } as AbstractControl)).toBeNull();
    });

    // invalid
    it('should return errors if floating points', () => {
      expect(RadvisValidators.isPositiveInteger({ value: '12.34' } as AbstractControl)).not.toBeNull();
    });
    it('should return errors if negative integer', () => {
      expect(RadvisValidators.isPositiveInteger({ value: '-9' } as AbstractControl)).not.toBeNull();
    });
    it('should return errors if negative floating point', () => {
      expect(RadvisValidators.isPositiveInteger({ value: '-9.5' } as AbstractControl)).not.toBeNull();
    });
    it('should return errors if text', () => {
      expect(RadvisValidators.isPositiveInteger({ value: 'foo' } as AbstractControl)).not.toBeNull();
    });
    it('should return errors if special chars', () => {
      expect(RadvisValidators.isPositiveInteger({ value: '#' } as AbstractControl)).not.toBeNull();
    });
  });

  describe(RadvisValidators.isPositiveFloatString.name, () => {
    // valid
    it('should return null if has comma float', () => {
      expect(RadvisValidators.isPositiveFloatString({ value: '12,34' } as AbstractControl)).toBeNull();
    });
    it('should return null if has comma float with many', () => {
      expect(RadvisValidators.isPositiveFloatString({ value: '12,123465789' } as AbstractControl)).toBeNull();
    });
    it('should return null if has dezimal number', () => {
      expect(RadvisValidators.isPositiveFloatString({ value: '123' } as AbstractControl)).toBeNull();
    });

    // invalid
    it('should return errors if has point float', () => {
      expect(RadvisValidators.isPositiveFloatString({ value: '12.34' } as AbstractControl)).not.toBeNull();
    });
    it('should return errors if text', () => {
      expect(RadvisValidators.isPositiveFloatString({ value: 'foo' } as AbstractControl)).not.toBeNull();
    });
    it('should return errors if special chars', () => {
      expect(RadvisValidators.isPositiveFloatString({ value: '#' } as AbstractControl)).not.toBeNull();
    });
    it('should return errors if <1 has no 0', () => {
      expect(RadvisValidators.isPositiveFloatString({ value: ',456' } as AbstractControl)).not.toBeNull();
    });
    it('should return errors if 0', () => {
      expect(RadvisValidators.isPositiveFloatString({ value: '0' } as AbstractControl)).not.toBeNull();
    });
  });

  describe(RadvisValidators.isNotNullOrEmpty.name, () => {
    it('should work', () => {
      expect(RadvisValidators.isNotNullOrEmpty(new FormControl(''))).not.toBeNull();
      expect(RadvisValidators.isNotNullOrEmpty(new FormControl(null))).not.toBeNull();
      expect(RadvisValidators.isNotNullOrEmpty(new FormControl(undefined))).not.toBeNull();
      expect(RadvisValidators.isNotNullOrEmpty(new FormControl('Test'))).toBeNull();
      expect(RadvisValidators.isNotNullOrEmpty(new FormControl({ blah: 1 }))).toBeNull();
    });
  });
});
