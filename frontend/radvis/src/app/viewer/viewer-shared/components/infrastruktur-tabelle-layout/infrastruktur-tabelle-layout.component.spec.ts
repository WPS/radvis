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
import { BreakpointObserver } from '@angular/cdk/layout';
import { MockBuilder, MockRender } from 'ng-mocks';
import { SpaltenDefinition } from 'src/app/viewer/viewer-shared/models/spalten-definition';
import { ViewerSharedModule } from 'src/app/viewer/viewer-shared/viewer-shared.module';
import { InfrastrukturTabelleLayoutComponent } from './infrastruktur-tabelle-layout.component';

describe(InfrastrukturTabelleLayoutComponent.name, () => {
  beforeEach(() => {
    return MockBuilder([InfrastrukturTabelleLayoutComponent, BreakpointObserver], ViewerSharedModule);
  });

  describe('hasFilterOnAusgeblendeteSpalten', () => {
    it('should change on select/deselect', () => {
      const spaltenDefinition: SpaltenDefinition[] = [
        { name: 'test', displayName: 'Test' },
        { name: 'test2', displayName: 'Test2' },
      ];
      const fixture = MockRender<InfrastrukturTabelleLayoutComponent>(InfrastrukturTabelleLayoutComponent, {
        spaltenDefinition,
        filteredSpalten: ['test'],
      });
      fixture.detectChanges();

      expect(fixture.point.componentInstance.hasFilterOnAusgeblendetenSpalten).toBeFalse();

      fixture.point.componentInstance.spaltenAuswahl.controls[0].controls.selected.setValue(false);

      expect(fixture.point.componentInstance.hasFilterOnAusgeblendetenSpalten).toBeTrue();

      fixture.point.componentInstance.spaltenAuswahl.controls[0].controls.selected.setValue(true);

      expect(fixture.point.componentInstance.hasFilterOnAusgeblendetenSpalten).toBeFalse();
    });
  });

  describe('spaltenAuswahl', () => {
    it('should init formarray', () => {
      const spaltenDefinition: SpaltenDefinition[] = [
        { name: 'test', displayName: 'Test' },
        { name: 'test2', displayName: 'Test2', defaultVisible: false },
      ];
      const fixture = MockRender<InfrastrukturTabelleLayoutComponent>(InfrastrukturTabelleLayoutComponent, {
        spaltenDefinition,
      });
      fixture.detectChanges();
      expect(fixture.point.componentInstance.spaltenAuswahl.length).toBe(2);
      expect(fixture.point.componentInstance.spaltenAuswahl.value).toEqual([
        { name: 'test', displayName: 'Test', selected: true },
        { name: 'test2', displayName: 'Test2', selected: false },
      ]);

      fixture.componentInstance.spaltenDefinition = [spaltenDefinition[0]];
      fixture.detectChanges();

      expect(fixture.point.componentInstance.spaltenAuswahl.length).toBe(1);
      expect(fixture.point.componentInstance.spaltenAuswahl.value).toEqual([
        { ...spaltenDefinition[0], selected: true },
      ]);
    });
    it('should show/hide spalten on select', () => {
      const spaltenDefinition: SpaltenDefinition[] = [
        { name: 'test', displayName: 'Test' },
        { name: 'test2', displayName: 'Test2' },
      ];
      const fixture = MockRender<InfrastrukturTabelleLayoutComponent>(InfrastrukturTabelleLayoutComponent, {
        spaltenDefinition,
      });
      fixture.detectChanges();

      expect(fixture.point.componentInstance.displayedColumnsWithEdit).toEqual([
        ...spaltenDefinition.map(def => def.name),
        'bearbeiten',
      ]);

      fixture.point.componentInstance.spaltenAuswahl.controls[0].controls.selected.setValue(false);

      expect(fixture.point.componentInstance.displayedColumnsWithEdit).toEqual([
        spaltenDefinition[1].name,
        'bearbeiten',
      ]);

      fixture.point.componentInstance.spaltenAuswahl.controls[0].controls.selected.setValue(true);

      expect(fixture.point.componentInstance.displayedColumnsWithEdit).toEqual([
        ...spaltenDefinition.map(def => def.name),
        'bearbeiten',
      ]);
    });
  });
});
