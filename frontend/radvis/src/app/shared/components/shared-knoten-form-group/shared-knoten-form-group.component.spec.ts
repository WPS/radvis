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

import { MockBuilder, MockedComponentFixture, MockRender } from 'ng-mocks';
import { BauwerksmangelArt } from 'src/app/shared/models/bauwerksmangel-art';
import { QuerungshilfeDetails } from 'src/app/shared/models/querungshilfe-details';
import { SharedKnotenFormGroup } from 'src/app/shared/models/shared-knoten-form-group';
import { SharedModule } from 'src/app/shared/shared.module';
import { SharedKnotenFormGroupComponent } from './shared-knoten-form-group.component';

describe(SharedKnotenFormGroupComponent.name, () => {
  let fixture: MockedComponentFixture<SharedKnotenFormGroupComponent>;
  let component: SharedKnotenFormGroupComponent;
  let formGroup: SharedKnotenFormGroup;

  beforeEach(() => {
    return MockBuilder(SharedKnotenFormGroupComponent, SharedModule);
  });

  beforeEach(() => {
    formGroup = new SharedKnotenFormGroup();
    fixture = MockRender(SharedKnotenFormGroupComponent, {
      sharedKnotenFormGroup: formGroup,
    } as SharedKnotenFormGroupComponent);
    component = fixture.point.componentInstance;
    fixture.detectChanges();
  });

  describe('onInit', () => {
    it('should update querungshilfe enum options', () => {
      formGroup.controls.knotenForm.setValue('KOMPAKTKREISVERKEHR_FUEHRUNG_NUR_UEBER_NEBENANLAGE');
      fixture = MockRender(
        SharedKnotenFormGroupComponent,
        {
          sharedKnotenFormGroup: formGroup,
        } as SharedKnotenFormGroupComponent,
        { reset: true }
      );
      fixture.detectChanges();

      expect(fixture.point.componentInstance.querungshilfeDetailsOptions).toEqual(
        QuerungshilfeDetails.getOptionsForKnotenform('KOMPAKTKREISVERKEHR_FUEHRUNG_NUR_UEBER_NEBENANLAGE')
      );
    });

    it('should update bauwerksmangel enum options', () => {
      formGroup.controls.knotenForm.setValue('UEBERFUEHRUNG');
      fixture = MockRender(
        SharedKnotenFormGroupComponent,
        {
          sharedKnotenFormGroup: formGroup,
        } as SharedKnotenFormGroupComponent,
        { reset: true }
      );
      fixture.detectChanges();

      expect(component.bauwerksmangelArtOptions).toEqual(BauwerksmangelArt.getOptionsForKnotenform('UEBERFUEHRUNG'));
    });
  });

  describe('onKnotenformChanged', () => {
    beforeEach(() => {
      formGroup.reset();
    });

    it('should update querungshilfe details options', () => {
      component.querungshilfeDetailsOptions = [];
      component.sharedKnotenFormGroup.controls.knotenForm.setValue(
        'KOMPAKTKREISVERKEHR_FUEHRUNG_NUR_UEBER_NEBENANLAGE'
      );

      expect(component.querungshilfeDetailsOptions).toEqual(
        QuerungshilfeDetails.getOptionsForKnotenform('KOMPAKTKREISVERKEHR_FUEHRUNG_NUR_UEBER_NEBENANLAGE')
      );
    });

    it('should handle reset correct', () => {
      component.sharedKnotenFormGroup.controls.knotenForm.reset();

      expect(component.querungshilfeDetailsOptions).toEqual([]);
      expect(component.bauwerksmangelArtOptions).toEqual([]);
    });

    it('should reset bauwerksmangel art options', () => {
      component.bauwerksmangelArtOptions = [];

      component.sharedKnotenFormGroup.controls.knotenForm.setValue('UEBERFUEHRUNG');

      expect(component.bauwerksmangelArtOptions).toEqual(BauwerksmangelArt.getOptionsForKnotenform('UEBERFUEHRUNG'));
    });
  });
});
