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

import { PredefinedLayerMenuComponent } from 'src/app/viewer/weitere-kartenebenen/components/predefined-layer-menu/predefined-layer-menu.component';
import { MockBuilder, MockedComponentFixture, MockRender } from 'ng-mocks';
import { PredefinedKartenMenu } from 'src/app/viewer/weitere-kartenebenen/models/predefined-karten-menu';
import { SaveWeitereKartenebeneCommand } from 'src/app/viewer/weitere-kartenebenen/models/save-weitere-kartenebene-command';
import { ViewerModule } from 'src/app/viewer/viewer.module';

describe(PredefinedLayerMenuComponent.name, () => {
  let component: PredefinedLayerMenuComponent;
  let fixture: MockedComponentFixture<PredefinedLayerMenuComponent, any>;

  beforeEach(() => {
    return MockBuilder(PredefinedLayerMenuComponent, ViewerModule);
  });

  beforeEach(() => {
    fixture = MockRender(PredefinedLayerMenuComponent, { menu: [] });
    component = fixture.point.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('with predefined karten menus', () => {
    let menuItems: PredefinedKartenMenu[];

    beforeEach(() => {
      menuItems = [
        new PredefinedKartenMenu('super-item', [
          new PredefinedKartenMenu({ id: 123, name: 'foo-layer' } as SaveWeitereKartenebeneCommand, []),
        ]),
        new PredefinedKartenMenu({ id: 234, name: 'bar-layer' } as SaveWeitereKartenebeneCommand, []),
      ];

      component.menu = menuItems;
    });

    it('should determine correct layer items', () => {
      const layerItems = component.getLayerItems();

      expect(layerItems).toHaveSize(1);
      expect(layerItems[0].id).toEqual(234);
    });

    it('should determine correct submenu items', () => {
      const submenuItems = component.getSubMenuItems();

      expect(submenuItems).toHaveSize(1);
      expect(submenuItems[0].item).toEqual('super-item');
      expect(submenuItems[0].children).toHaveSize(1);
      expect((submenuItems[0].children[0].item as SaveWeitereKartenebeneCommand).id).toEqual(123);
    });
  });
});
