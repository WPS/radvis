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

/* eslint-disable @typescript-eslint/dot-notation */
import { KarteMenuItemComponent } from './karte-menu-item.component';
import { MockBuilder, MockedComponentFixture, MockRender } from 'ng-mocks';
import { SharedModule } from 'src/app/shared/shared.module';
import { MatMenuTrigger } from '@angular/material/menu';
import { KarteModule } from 'src/app/karte/karte.module';

describe(KarteMenuItemComponent.name, () => {
  let component: KarteMenuItemComponent;
  let fixture: MockedComponentFixture<KarteMenuItemComponent>;

  beforeEach(() => {
    return MockBuilder(KarteMenuItemComponent, [SharedModule, KarteModule]);
  });

  beforeEach(() => {
    fixture = MockRender(KarteMenuItemComponent);
    component = fixture.point.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('with menuTrigger', () => {
    beforeEach(() => {
      component['menuTrigger'] = {} as MatMenuTrigger;
    });

    describe(KarteMenuItemComponent.prototype.close.name, () => {
      beforeEach(() => {
        // Ist immer wahr
        if (!!component['menuTrigger']) {
          component['menuTrigger'].closeMenu = jasmine.createSpy();
        }
        component.close();
      });

      it('should invoke closeMenu', () => {
        expect(component['menuTrigger']?.closeMenu).toHaveBeenCalled();
      });
    });
  });
});
