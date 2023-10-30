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

import { KommentarHinzufuegenComponent } from './kommentar-hinzufuegen.component';
import { MockBuilder, MockedComponentFixture, MockRender } from 'ng-mocks';
import { ViewerModule } from 'src/app/viewer/viewer.module';

describe(KommentarHinzufuegenComponent.name, () => {
  let component: KommentarHinzufuegenComponent;
  let fixture: MockedComponentFixture<KommentarHinzufuegenComponent>;

  beforeEach(() => {
    return MockBuilder(KommentarHinzufuegenComponent, ViewerModule);
  });

  beforeEach(() => {
    fixture = MockRender(KommentarHinzufuegenComponent);
    component = fixture.point.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe(KommentarHinzufuegenComponent.prototype.send.name, () => {
    let sendEventEmitSpy: any;
    let kommentarText: string;

    beforeEach(() => {
      kommentarText = 'I bims, 1 wichtiger Mensch!!11!1!ein!elf!11';
      const formWithKommentar = {
        kommentarText,
      };
      sendEventEmitSpy = spyOn(component.sendKommentar, 'emit');

      component.formGroup.patchValue(formWithKommentar);
    });

    it('should emit send event', () => {
      component.send();

      expect(sendEventEmitSpy).toHaveBeenCalledWith(kommentarText);
      expect(component.formGroup.value.kommentarText).toBeNull();
    });
    it('should not emit send event if text is null', () => {
      component.formGroup.patchValue({ kommentarText: null });
      component.send();

      expect(sendEventEmitSpy).not.toHaveBeenCalled();
      expect(component.formGroup.value.kommentarText).toBeNull();
    });
    it('should not emit send event if text is empty', () => {
      component.formGroup.patchValue({ kommentarText: '' });
      component.send();

      expect(sendEventEmitSpy).not.toHaveBeenCalled();
      expect(component.formGroup.value.kommentarText).toEqual('');
    });
    it('should not emit send event if text is just spaces', () => {
      component.formGroup.patchValue({ kommentarText: '     ' });
      component.send();

      expect(sendEventEmitSpy).not.toHaveBeenCalled();
      expect(component.formGroup.value.kommentarText).toEqual('     ');
    });
  });
});
