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

import { MatTableDataSource } from '@angular/material/table';
import { DefaultRenderComponent, MockBuilder, MockedComponentFixture, MockRender } from 'ng-mocks';
import { NetzDetailFeatureTableLink } from 'src/app/viewer/viewer-shared/models/netzdetail-feature-table-link';
import { ViewerModule } from 'src/app/viewer/viewer.module';
import { DetailFeatureTableComponent } from 'src/app/viewer/viewer-shared/components/detail-feauture-table/detail-feature-table.component';

interface TableRow {
  key: string;
  value: string | NetzDetailFeatureTableLink;
}

interface TableGroup {
  group: string;
}

describe(DetailFeatureTableComponent.name, () => {
  let component: DefaultRenderComponent<DetailFeatureTableComponent>;
  let fixture: MockedComponentFixture<DetailFeatureTableComponent>;

  let attributeForTable: MatTableDataSource<TableRow | TableGroup>;

  const gruppe1: TableGroup = { group: 'gruppe1' };
  const gruppe2: TableGroup = { group: 'gruppe2' };

  const initialAttribute: Map<string, { [key: string]: string }> = new Map();

  beforeEach(async () => {
    return MockBuilder(DetailFeatureTableComponent, ViewerModule);
  });

  beforeEach(() => {
    fixture = MockRender(DetailFeatureTableComponent, {
      attribute: initialAttribute,
      leereAttributeVisible: true,
    } as any);
    component = fixture.point.componentInstance;
    fixture.detectChanges();

    attributeForTable = fixture.point.componentInstance['attributeForTable'];

    initialAttribute.set('gruppe1', { test: 'testValue' });
    initialAttribute.set('gruppe2', { test_lr: 'testValue_lr' });
  });

  describe('attribut groups', () => {
    it('should group attributes initial', () => {
      component.attribute = initialAttribute;
      component.ngOnChanges();
      fixture.detectChanges();

      expect(attributeForTable.data).toEqual([
        gruppe1,
        { key: 'test', value: 'testValue' },
        gruppe2,
        { key: 'test_lr', value: 'testValue_lr' },
      ]);
    });

    it('should group attributes after change', () => {
      const attribute = new Map();
      attribute.set('gruppe1', {
        blubb: 'testValue',
      });
      attribute.set('gruppe2', {
        blubb_lr1: 'testValue',
        blubb_lr2: 'testValue',
      });
      component.attribute = attribute;
      component.ngOnChanges();
      fixture.detectChanges();

      expect(attributeForTable.data).toEqual([
        gruppe1,
        { key: 'blubb', value: 'testValue' },
        gruppe2,
        { key: 'blubb_lr1', value: 'testValue' },
        { key: 'blubb_lr2', value: 'testValue' },
      ]);
    });
  });

  describe('leere Attribute', () => {
    it('should filter leere Attribute', () => {
      fixture.point.componentInstance.leereAttributeVisible = true;
      const attribute = new Map();
      attribute.set('gruppe1', {
        blubb: 'testValue',
        test: '',
      });
      attribute.set('gruppe2', {
        test_lr: 'testValue_lr',
        test_lr2: '',
      });
      component.attribute = attribute;
      component.ngOnChanges();
      fixture.detectChanges();

      expect(attributeForTable.data).toEqual([
        gruppe1,
        { key: 'blubb', value: 'testValue' },
        { key: 'test', value: '' },
        gruppe2,
        { key: 'test_lr', value: 'testValue_lr' },
        { key: 'test_lr2', value: '' },
      ]);

      fixture.point.componentInstance.leereAttributeVisible = false;
      fixture.point.componentInstance.ngOnChanges();

      expect(attributeForTable.data).toEqual([
        gruppe1,
        { key: 'blubb', value: 'testValue' },
        gruppe2,
        { key: 'test_lr', value: 'testValue_lr' },
      ]);
    });

    it('should apply leere Attribute Filter on changes', () => {
      fixture.point.componentInstance.leereAttributeVisible = false;
      const attribute = new Map();
      attribute.set('gruppe1', {
        test: 'testValue',
        testLeer: '',
      });
      attribute.set('gruppe2', {
        test_lr: 'testValue_lr',
        test_lr2: '',
      });
      component.attribute = attribute;
      component.ngOnChanges();
      fixture.detectChanges();

      expect(attributeForTable.data).toEqual([
        gruppe1,
        { key: 'test', value: 'testValue' },
        gruppe2,
        { key: 'test_lr', value: 'testValue_lr' },
      ]);
    });

    it('should remove unnecessary lr group', () => {
      fixture.point.componentInstance.leereAttributeVisible = false;
      const attribute = new Map();
      attribute.set('gruppe1', {
        test: 'testValue',
        testLeer: '',
      });
      attribute.set('gruppe2', {
        test_lr2: '',
      });
      component.attribute = attribute;
      component.ngOnChanges();
      fixture.detectChanges();
      expect(attributeForTable.data).toEqual([gruppe1, { key: 'test', value: 'testValue' }]);
    });
  });
});
