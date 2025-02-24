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
import { Bauwerksmangel } from 'src/app/shared/models/bauwerksmangel';
import { BauwerksmangelArt } from 'src/app/shared/models/bauwerksmangel-art';
import { QuerungshilfeDetails } from 'src/app/shared/models/querungshilfe-details';
import { SharedKnotenFormGroup } from 'src/app/shared/models/shared-knoten-form-group';

describe(SharedKnotenFormGroup.name, () => {
  let formGroup: SharedKnotenFormGroup;

  beforeEach(() => {
    formGroup = new SharedKnotenFormGroup();
  });

  describe('validierung', () => {
    describe('with required knotenform', () => {
      beforeEach(() => {
        formGroup = new SharedKnotenFormGroup(true);
      });
      it('should be valid', () => {
        formGroup.patchValue({ knotenForm: 'MINIKREISVERKEHR_NICHT_UEBERFAHRBAR' });

        expect(formGroup.valid).toBeTrue();
      });

      it('should be invalid if knotenform unset', () => {
        formGroup.reset();

        expect(formGroup.valid).toBeFalse();
      });
    });

    it('valid if knotenform unset', () => {
      formGroup.reset();

      expect(formGroup.valid).toBeTrue();
    });

    it('should be valid', () => {
      formGroup.patchValue({
        knotenForm: 'ERWEITERTE_FUSS_RADFAHRER_LSA',
        querungshilfeDetails: null,
        bauwerksmangel: { vorhanden: null, bauwerksmangelArt: null },
      });

      expect(formGroup.valid).toBeTrue();
    });

    it('should be invalid if querungshilfe details unset', () => {
      formGroup.patchValue({
        knotenForm: 'MITTELINSEL_EINFACH',
        querungshilfeDetails: null,
        bauwerksmangel: { vorhanden: null, bauwerksmangelArt: null },
      });

      expect(formGroup.valid).toBeFalse();

      formGroup.patchValue({
        querungshilfeDetails: QuerungshilfeDetails.MITTELINSEL_OK,
      });

      expect(formGroup.valid).toBeTrue();
    });

    it('should be invalid if bauwerksmangel vorhanden unset', () => {
      formGroup.patchValue({
        knotenForm: 'UEBERFUEHRUNG',
        querungshilfeDetails: null,
        bauwerksmangel: { vorhanden: null, bauwerksmangelArt: null },
      });

      expect(formGroup.valid).toBeFalse();

      formGroup.patchValue({
        bauwerksmangel: {
          vorhanden: Bauwerksmangel.UNBEKANNT,
        },
      });

      expect(formGroup.valid).toBeTrue();
    });

    it('should be invalid if bauwerksmangel art unset', () => {
      formGroup.patchValue({
        knotenForm: 'UEBERFUEHRUNG',
        querungshilfeDetails: null,
        bauwerksmangel: { vorhanden: Bauwerksmangel.VORHANDEN, bauwerksmangelArt: null },
      });

      expect(formGroup.valid).toBeFalse();

      formGroup.patchValue({
        bauwerksmangel: {
          bauwerksmangelArt: [],
        },
      });

      expect(formGroup.valid).toBeFalse();

      formGroup.patchValue({
        bauwerksmangel: {
          bauwerksmangelArt: [BauwerksmangelArt.ANDERER_MANGEL],
        },
      });

      expect(formGroup.valid).toBeTrue();
    });
  });

  describe('onChangeBauwerksmangelVorhanden', () => {
    it('should reset bauwerksmangel art', () => {
      formGroup.controls.bauwerksmangel.enable();
      formGroup.controls.bauwerksmangel.controls.bauwerksmangelArt.setValue([BauwerksmangelArt.GELAENDER_ZU_NIEDRIG]);

      formGroup.controls.bauwerksmangel.controls.vorhanden.setValue(Bauwerksmangel.VORHANDEN);

      expect(formGroup.controls.bauwerksmangel.controls.bauwerksmangelArt.value).toBeFalsy();
    });

    it('should enable bauwerksmangel art', () => {
      formGroup.controls.bauwerksmangel.enable();
      formGroup.controls.bauwerksmangel.controls.bauwerksmangelArt.disable();

      formGroup.controls.bauwerksmangel.controls.vorhanden.setValue(Bauwerksmangel.VORHANDEN);

      expect(formGroup.controls.bauwerksmangel.controls.bauwerksmangelArt.enabled).toBeTrue();
      expect(formGroup.controls.bauwerksmangel.controls.bauwerksmangelArt.valid).toBeFalse();
    });

    it('should disable bauwerksmangel art', () => {
      formGroup.controls.bauwerksmangel.enable();

      formGroup.controls.bauwerksmangel.controls.vorhanden.setValue(Bauwerksmangel.NICHT_VORHANDEN);

      expect(formGroup.controls.bauwerksmangel.controls.bauwerksmangelArt.enabled).toBeFalse();
      expect(formGroup.controls.bauwerksmangel.valid).toBeTrue();
    });
  });

  describe('onKnotenformChanged', () => {
    beforeEach(() => {
      formGroup.reset();
    });

    it('should clear querungshilfe details', () => {
      formGroup.controls.querungshilfeDetails.setValue(QuerungshilfeDetails.ANDERE_ANMERKUNG_MITTELINSEL);
      formGroup.controls.knotenForm.setValue('KOMPAKTKREISVERKEHR_FUEHRUNG_NUR_UEBER_NEBENANLAGE');

      expect(formGroup.controls.querungshilfeDetails.value).toBeFalsy();
    });

    it('should enable querungshilfe details', () => {
      formGroup.controls.querungshilfeDetails.disable();
      formGroup.controls.knotenForm.setValue('KOMPAKTKREISVERKEHR_FUEHRUNG_NUR_UEBER_NEBENANLAGE');

      expect(formGroup.controls.querungshilfeDetails.disabled).toBeFalse();
    });

    it('should disable querungshilfe details', () => {
      formGroup.controls.querungshilfeDetails.enable();
      formGroup.controls.knotenForm.setValue('UEBERFUEHRUNG');

      expect(formGroup.controls.querungshilfeDetails.enabled).toBeFalse();
    });

    it('should handle reset correct', () => {
      formGroup.controls.knotenForm.reset();

      expect(formGroup.controls.querungshilfeDetails.enabled).toBeFalse();
      expect(formGroup.controls.querungshilfeDetails.value).toBeFalsy();
      expect(formGroup.controls.bauwerksmangel.enabled).toBeFalse();
      expect(formGroup.controls.bauwerksmangel.value).toEqual({
        vorhanden: null,
        bauwerksmangelArt: null,
      });
    });

    it('should clear bauwerksmangel', () => {
      formGroup.patchValue({
        knotenForm: 'UEBERFUEHRUNG',
        bauwerksmangel: {
          vorhanden: Bauwerksmangel.VORHANDEN,
          bauwerksmangelArt: [BauwerksmangelArt.GELAENDER_ZU_NIEDRIG],
        },
      });

      formGroup.controls.knotenForm.setValue('KOMPAKTKREISVERKEHR_FUEHRUNG_NUR_UEBER_NEBENANLAGE');

      expect(formGroup.controls.bauwerksmangel.getRawValue()).toEqual({
        vorhanden: null,
        bauwerksmangelArt: null,
      });
    });

    it('should enable bauwerksmangel', () => {
      formGroup.patchValue({
        knotenForm: 'KOMPAKTKREISVERKEHR_FUEHRUNG_NUR_UEBER_NEBENANLAGE',
      });
      formGroup.controls.bauwerksmangel.disable();

      formGroup.controls.knotenForm.setValue('UEBERFUEHRUNG');

      expect(formGroup.controls.bauwerksmangel.controls.vorhanden.enabled).toBeTrue();
      expect(formGroup.controls.bauwerksmangel.controls.bauwerksmangelArt.enabled).toBeFalse();
    });

    it('should disable bauwerksmangel', () => {
      formGroup.patchValue({
        knotenForm: 'UEBERFUEHRUNG',
      });
      formGroup.controls.bauwerksmangel.enable();

      formGroup.controls.knotenForm.setValue('KOMPAKTKREISVERKEHR_FUEHRUNG_NUR_UEBER_NEBENANLAGE');

      expect(formGroup.controls.bauwerksmangel.disabled).toBeTrue();
    });
  });
});
