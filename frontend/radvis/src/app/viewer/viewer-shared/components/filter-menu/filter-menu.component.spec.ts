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

import { fakeAsync, tick } from '@angular/core/testing';
import { FormControl } from '@angular/forms';
import { DefaultRenderComponent, MockBuilder, MockedComponentFixture, MockRender } from 'ng-mocks';
import { BehaviorSubject } from 'rxjs';
import { MassnahmeFilterService } from 'src/app/viewer/massnahme/services/massnahme-filter.service';
import { FilterMenuComponent } from 'src/app/viewer/viewer-shared/components/filter-menu/filter-menu.component';
import { FieldFilter } from 'src/app/viewer/viewer-shared/models/field-filter';
import { AbstractInfrastrukturenFilterService } from 'src/app/viewer/viewer-shared/services/abstract-infrastrukturen-filter.service';
import { ViewerModule } from 'src/app/viewer/viewer.module';
import { anything, instance, mock, verify, when } from 'ts-mockito';

describe(FilterMenuComponent.name, () => {
  let filterService: MassnahmeFilterService;
  let filterSubject: BehaviorSubject<FieldFilter[]>;

  let fixture: MockedComponentFixture<FilterMenuComponent>;
  let filterMenuComponent: DefaultRenderComponent<FilterMenuComponent>;

  beforeEach(() => {
    filterService = mock(MassnahmeFilterService);
    filterSubject = new BehaviorSubject<FieldFilter[]>([]);
    when(filterService.filter$).thenReturn(filterSubject.asObservable());

    return MockBuilder(FilterMenuComponent, ViewerModule).provide({
      provide: AbstractInfrastrukturenFilterService,
      useValue: instance(filterService),
    });
  });

  beforeEach(() => {
    fixture = MockRender(FilterMenuComponent, {
      field: 'field',
      filterControl: new FormControl(),
    } as FilterMenuComponent);
    filterMenuComponent = fixture.point.componentInstance;
    fixture.detectChanges();
  });

  it('should update Formcontrol value if FieldFilter for field changes', fakeAsync(() => {
    tick();
    expect(filterMenuComponent.filterControl.value).toEqual('');
    expect(filterMenuComponent.isAktiv).toBeFalse();
    filterSubject.next([new FieldFilter('field', 'val')]);
    tick(100);
    expect(filterMenuComponent.filterControl.value).toEqual('val');
    expect(filterMenuComponent.isAktiv).toBeTrue();
    filterSubject.next([new FieldFilter('field', 'otherVal')]);
    tick(100);
    expect(filterMenuComponent.filterControl.value).toEqual('otherVal');
    expect(filterMenuComponent.isAktiv).toBeTrue();
  }));

  it('should not trigger filtering, if filter changes', fakeAsync(() => {
    filterSubject.next([new FieldFilter('field', 'val')]);
    tick(100);
    verify(filterService.filterField(anything(), anything())).never();
    expect().nothing();
  }));

  it('should reset Formcontrol value and not be active if FieldFilter for field is cleared', fakeAsync(() => {
    tick();
    expect(filterMenuComponent.filterControl.value).toEqual('');
    expect(filterMenuComponent.isAktiv).toBeFalse();
    filterSubject.next([new FieldFilter('field', 'val')]);
    tick(100);
    expect(filterMenuComponent.filterControl.value).toEqual('val');
    expect(filterMenuComponent.isAktiv).toBeTrue();
    filterSubject.next([new FieldFilter('field', '')]);
    tick(100);
    expect(filterMenuComponent.filterControl.value).toEqual('');
    expect(filterMenuComponent.isAktiv).toBeFalse();
  }));

  it('should reset if field removed from filter', fakeAsync(() => {
    filterSubject.next([new FieldFilter('otherField', 'otherVal'), new FieldFilter('field', 'val')]);
    tick(100);
    expect(filterMenuComponent.filterControl.value).toEqual('val');
    expect(filterMenuComponent.isAktiv).toBeTrue();
    filterSubject.next([new FieldFilter('otherField', 'otherVal')]);
    tick(100);
    expect(filterMenuComponent.filterControl.value).toEqual('');
    expect(filterMenuComponent.isAktiv).toBeFalse();
  }));

  it('should do nothing if filtervalue for different field changes', fakeAsync(() => {
    tick();
    expect(filterMenuComponent.filterControl.value).toEqual('');
    expect(filterMenuComponent.isAktiv).toBeFalse();
    filterSubject.next([new FieldFilter('field', 'val')]);
    tick(100);
    expect(filterMenuComponent.filterControl.value).toEqual('val');
    expect(filterMenuComponent.isAktiv).toBeTrue();
    filterSubject.next([new FieldFilter('otherField', 'otherVal'), new FieldFilter('field', 'val')]);
    tick(100);
    expect(filterMenuComponent.filterControl.value).toEqual('val');
    expect(filterMenuComponent.isAktiv).toBeTrue();
    filterSubject.next([new FieldFilter('otherField', ''), new FieldFilter('field', 'val')]);
    tick(100);
    expect(filterMenuComponent.filterControl.value).toEqual('val');
    expect(filterMenuComponent.isAktiv).toBeTrue();
  }));
});
