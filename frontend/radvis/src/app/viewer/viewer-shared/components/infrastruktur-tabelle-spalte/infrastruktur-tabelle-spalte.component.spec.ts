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

import { MockBuilder, MockRender } from 'ng-mocks';
import { SpaltenDefinition } from 'src/app/viewer/viewer-shared/models/spalten-definition';
import { ViewerSharedModule } from 'src/app/viewer/viewer-shared/viewer-shared.module';
import { InfrastrukturTabelleSpalteComponent } from './infrastruktur-tabelle-spalte.component';

describe(InfrastrukturTabelleSpalteComponent.name, () => {
  beforeEach(() => {
    return MockBuilder(InfrastrukturTabelleSpalteComponent, ViewerSharedModule);
  });

  it('should declare MatColumnDef', () => {
    const spaltenDefinition: SpaltenDefinition = { name: 'test', displayName: 'Test' };
    const fixture = MockRender<InfrastrukturTabelleSpalteComponent>(InfrastrukturTabelleSpalteComponent, {
      spaltenDefinition: spaltenDefinition,
      getElementValueFn: (element: any, key: string) => {
        return element.toString();
      },
    });
    fixture.detectChanges();
    expect(fixture.point.componentInstance.matColumnDef).toBeDefined();
  });

  describe('expandable', () => {
    it('should return false if unset', () => {
      const spaltenDefinition: SpaltenDefinition = { name: 'test', displayName: 'Test' };
      const fixture = MockRender<InfrastrukturTabelleSpalteComponent>(InfrastrukturTabelleSpalteComponent, {
        spaltenDefinition: spaltenDefinition,
        getElementValueFn: (element: any, key: string) => {
          return element.toString();
        },
      });
      fixture.detectChanges();
      expect(fixture.point.componentInstance.expandable).toBeFalse();
    });

    it('should return false if set', () => {
      const spaltenDefinition: SpaltenDefinition = { name: 'test', displayName: 'Test', expandable: false };
      const fixture = MockRender<InfrastrukturTabelleSpalteComponent>(InfrastrukturTabelleSpalteComponent, {
        spaltenDefinition: spaltenDefinition,
        getElementValueFn: (element: any, key: string) => {
          return element.toString();
        },
      });
      fixture.detectChanges();
      expect(fixture.point.componentInstance.expandable).toBeFalse();
    });

    it('should return true if set', () => {
      const spaltenDefinition: SpaltenDefinition = { name: 'test', displayName: 'Test', expandable: true };
      const fixture = MockRender<InfrastrukturTabelleSpalteComponent>(InfrastrukturTabelleSpalteComponent, {
        spaltenDefinition: spaltenDefinition,
        getElementValueFn: (element: any, key: string) => {
          return element.toString();
        },
      });
      fixture.detectChanges();
      expect(fixture.point.componentInstance.expandable).toBeTrue();
    });
  });

  describe('width', () => {
    it('should return auto if unset', () => {
      const spaltenDefinition: SpaltenDefinition = { name: 'test', displayName: 'Test' };
      const fixture = MockRender(InfrastrukturTabelleSpalteComponent, {
        spaltenDefinition: spaltenDefinition,
        getElementValueFn: (element: any, key: string) => {
          return element.toString();
        },
      });
      fixture.detectChanges();
      expect(fixture.point.componentInstance.width).toBe('auto');
    });

    it('should return set value', () => {
      const spaltenDefinition: SpaltenDefinition = { name: 'test', displayName: 'Test', width: 'large' };
      const fixture = MockRender(InfrastrukturTabelleSpalteComponent, {
        spaltenDefinition: spaltenDefinition,
        getElementValueFn: (element: any, key: string) => {
          return element.toString();
        },
      });
      fixture.detectChanges();
      expect(fixture.point.componentInstance.width).toBe('large');
    });
  });
});
