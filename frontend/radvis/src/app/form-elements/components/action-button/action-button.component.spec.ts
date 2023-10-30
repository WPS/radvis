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
import { SharedModule } from 'src/app/shared/shared.module';
import { ActionButtonComponent } from './action-button.component';

describe('ActionButtonComponent', () => {
  let component: ActionButtonComponent;
  let fixture: MockedComponentFixture<ActionButtonComponent>;

  beforeEach(async () => {
    return MockBuilder(ActionButtonComponent, SharedModule);
  });

  beforeEach(() => {
    fixture = MockRender(ActionButtonComponent);
    component = fixture.point.componentInstance;
  });

  describe('onClick', () => {
    let spy: jasmine.Spy;
    beforeEach(() => {
      component.waiting = false;
      spy = spyOn(component.action, 'next');
    });

    it('should emit action', () => {
      component.onSave();
      expect(spy).toHaveBeenCalled();
    });

    it('should not emit action if waiting', () => {
      component.waiting = true;
      component.onSave();
      expect(spy).not.toHaveBeenCalled();
    });
  });
});
