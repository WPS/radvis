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

import { Component, ViewChild } from '@angular/core';
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatListModule } from '@angular/material/list';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatToolbarModule } from '@angular/material/toolbar';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MockComponent, MockModule } from 'ng-mocks';
import { LineString } from 'ol/geom';
import { KanteGrundgeometrieLayerComponent } from 'src/app/editor/editor-shared/components/kante-grundgeometrie-layer/kante-grundgeometrie-layer.component';
import { AutocorrectingNumberInputControlComponent } from 'src/app/editor/kanten/components/autocorrecting-number-input-control/autocorrecting-number-input-control.component';
import { LinearReferenzierterAbschnittControlComponent } from 'src/app/editor/kanten/components/lineare-referenz-control/linear-referenzierter-abschnitt-control.component';
import { LineareReferenzierungLayerComponent } from 'src/app/shared/components/lineare-referenzierung-layer/lineare-referenzierung-layer.component';
import { LineStringGeojson } from 'src/app/shared/models/geojson-geometrie';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { instance, mock } from 'ts-mockito';

describe(LinearReferenzierterAbschnittControlComponent.name, () => {
  let component: LinearReferenzierterAbschnittControlComponent;
  let fixture: ComponentFixture<LinearReferenzierterAbschnittControlComponent>;
  const eventMock = mock(Event);
  const notifyUserServiceMock = mock(NotifyUserService);

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        ReactiveFormsModule,
        MockModule(MatFormFieldModule),
        MockModule(MatInputModule),
        MockModule(MatToolbarModule),
        MockModule(MatIconModule),
        MockModule(MatListModule),
        MockModule(MatCheckboxModule),
        NoopAnimationsModule,
      ],
      declarations: [
        LinearReferenzierterAbschnittControlComponent,
        MockComponent(AutocorrectingNumberInputControlComponent),
        MockComponent(LineareReferenzierungLayerComponent),
        MockComponent(KanteGrundgeometrieLayerComponent),
      ],
      providers: [
        {
          provide: NotifyUserService,
          useValue: instance(notifyUserServiceMock),
        },
      ],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(LinearReferenzierterAbschnittControlComponent);
    component = fixture.componentInstance;
    component.lineString = new LineString([
      [0, 0],
      [0, 10],
    ]);
    fixture.detectChanges();
  });

  describe('write value', () => {
    it('should not trigger valueChanges', () => {
      const spy = spyOn(component, 'onChange');
      component.writeValue([{ von: 0, bis: 1 }]);
      component.writeValue(null);
      expect(spy).not.toHaveBeenCalled();
    });

    it('should fill form correct', () => {
      const spy = spyOn(component, 'onChange');
      const inputValue = [{ von: 0, bis: 1 }];
      component.writeValue(inputValue);
      component.lineareReferenzenForm.updateValueAndValidity();
      expect(spy).toHaveBeenCalled();
      expect(spy.calls.mostRecent().args[0]).toEqual(inputValue);
    });

    it('should sort', () => {
      const inputValue = [
        { von: 0.5, bis: 1 },
        { von: 0, bis: 0.5 },
      ];
      component.writeValue(inputValue);
      expect(component.lineareReferenzenForm.getRawValue()).toEqual([0, 5, 10]);
    });

    it('should update relativeSegmentPoints', () => {
      const inputValue = [
        { von: 0.5, bis: 1 },
        { von: 0, bis: 0.5 },
      ];
      component.writeValue(inputValue);
      expect(component.relativeSegmentPoints).toEqual([0, 0.5, 1]);
    });

    it('should update previousControlValues', () => {
      const inputValue = [
        { von: 0.5, bis: 1 },
        { von: 0, bis: 0.5 },
      ];
      component.writeValue(inputValue);
      expect(component.previousControlValues).toEqual([0, 5, 10]);
    });
  });

  describe('minUndMaxWertDerMetermarkenBeruecksichtigen', () => {
    it('should correct values lower than local lower bound', () => {
      const spy = spyOn(component, 'onChange');
      component.writeValue([
        { von: 0.0, bis: 0.32 },
        { von: 0.32, bis: 0.66 },
        { von: 0.66, bis: 1.0 },
      ]);
      component.lineareReferenzenForm.setValue([0, 3.2, 1.0, 10.0]);
      fixture.detectChanges();
      expect(spy).toHaveBeenCalled();
      expect(spy.calls.mostRecent().args[0]).toEqual([
        { von: 0.0, bis: 0.32 },
        { von: 0.32, bis: 0.321 },
        { von: 0.321, bis: 1.0 },
      ]);
    });

    it('should correct values larger than local upper bound', () => {
      const spy = spyOn(component, 'onChange');
      component.writeValue([
        { von: 0.0, bis: 0.33 },
        { von: 0.33, bis: 0.66 },
        { von: 0.66, bis: 1.0 },
      ]);
      component.lineareReferenzenForm.get('2')?.setValue(12.3);
      expect(spy).toHaveBeenCalled();
      expect(spy.calls.mostRecent().args[0]).toEqual([
        { von: 0.0, bis: 0.33 },
        { von: 0.33, bis: 0.999 },
        { von: 0.999, bis: 1.0 },
      ]);
    });
  });

  describe('onCutSegment', () => {
    it('should cut in half', () => {
      const spy = spyOn(component, 'onChange');
      component.writeValue([{ von: 0, bis: 1 }]);
      component.onCutSegment(0, instance(eventMock));
      expect(spy).toHaveBeenCalled();
      expect(spy.calls.mostRecent().args[0]).toEqual([
        { von: 0, bis: 0.5 },
        { von: 0.5, bis: 1 },
      ]);
    });

    it('should keep correct disabled state', () => {
      component.writeValue([
        { von: 0, bis: 0.5 },
        { von: 0.5, bis: 1 },
      ]);
      component.onCutSegment(1, instance(eventMock));
      expect(component.lineareReferenzenForm.controls.length).toBe(4);
      expect(component.lineareReferenzenForm.at(0).disabled).toBeTrue();
      expect(component.lineareReferenzenForm.at(1).disabled).toBeFalse();
      expect(component.lineareReferenzenForm.at(2).disabled).toBeFalse();
      expect(component.lineareReferenzenForm.at(3).disabled).toBeTrue();
    });
  });

  describe('onDeleteSegment', () => {
    it('should merge segments correctly', () => {
      const spy = spyOn(component, 'onChange');
      component.writeValue([
        { von: 0, bis: 0.5 },
        { von: 0.5, bis: 0.7 },
        { von: 0.7, bis: 1 },
      ]);
      component.onDeleteSegment(1, instance(eventMock));
      expect(spy).toHaveBeenCalled();
      expect(spy.calls.mostRecent().args[0]).toEqual([
        { von: 0, bis: 0.7 },
        { von: 0.7, bis: 1 },
      ]);
    });
  });

  describe('onSegmentierungAufKarteChanged', () => {
    it('should update form', () => {
      component.writeValue([
        { von: 0, bis: 0.5 },
        { von: 0.5, bis: 1 },
      ]);
      component.onSegmentierungAufKarteChanged([0, 0.2, 1]);
      expect(component.lineareReferenzenForm.getRawValue()).toEqual([0, 2, 10]);
    });
  });

  describe('meterToRelativeNumberConverting', () => {
    it('should respect cm for relative digit number', fakeAsync(() => {
      const spy = spyOn(component, 'onChange');

      component.lineString = new LineString([
        [0, 0],
        [0, 400],
      ]);
      component.writeValue([
        { von: 0, bis: 0.5 },
        { von: 0.5, bis: 1 },
      ]);

      component.lineareReferenzenForm.setValue([0, 500, 400]);
      tick();

      expect(spy).toHaveBeenCalled();
      expect(spy.calls.mostRecent().args[0]).not.toEqual([
        { von: 0, bis: 1 },
        { von: 1, bis: 1 },
      ]);
    }));
  });
});

describe(LinearReferenzierterAbschnittControlComponent.name + ' - integration', () => {
  let component: LinearReferenzierterAbschnittControlComponent;
  let fixture: ComponentFixture<LinearReferenzierterAbschnittControlComponent>;
  const eventMock = mock(Event);

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        ReactiveFormsModule,
        MatFormFieldModule,
        MatInputModule,
        MatToolbarModule,
        MatIconModule,
        MatListModule,
        MatSnackBarModule,
        MatCheckboxModule,
        NoopAnimationsModule,
      ],
      declarations: [
        LinearReferenzierterAbschnittControlComponent,
        AutocorrectingNumberInputControlComponent,
        MockComponent(LineareReferenzierungLayerComponent),
        MockComponent(KanteGrundgeometrieLayerComponent),
      ],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(LinearReferenzierterAbschnittControlComponent);
    component = fixture.componentInstance;
    component.lineString = new LineString([
      [0, 0],
      [0, 10],
    ]);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeDefined();
  });

  describe('onCutSegment', () => {
    it('should update disabled state', () => {
      component.writeValue([{ von: 0, bis: 1 }]);
      component.onCutSegment(0, instance(eventMock));

      fixture.detectChanges();

      const allInputs = fixture.debugElement.queryAll(By.css('input:not([type="checkbox"])'));
      expect(allInputs[0].properties.disabled).toBeFalse();
      expect(allInputs[1].properties.disabled).toBeTrue();
    });
  });

  describe('input correction', () => {
    it('should correct values smaller than local lower bound', () => {
      const spy = spyOn(component, 'onChange');
      component.writeValue([
        { von: 0, bis: 0.25 },
        { von: 0.25, bis: 0.5 },
        { von: 0.5, bis: 0.75 },
        { von: 0.75, bis: 1.0 },
      ]);
      fixture.detectChanges();

      const allInputs = fixture.debugElement.queryAll(By.css('input:not([type="checkbox"])'));
      allInputs[1].nativeElement.value = '1.23';
      (allInputs[1].nativeElement as HTMLElement).dispatchEvent(new Event('input'));
      (allInputs[1].nativeElement as HTMLElement).dispatchEvent(new Event('blur'));

      expect(spy).toHaveBeenCalled();
      expect(spy.calls.mostRecent().args[0]).toEqual([
        { von: 0, bis: 0.25 },
        { von: 0.25, bis: 0.251 },
        { von: 0.251, bis: 0.75 },
        { von: 0.75, bis: 1.0 },
      ]);
      fixture.detectChanges();

      expect(allInputs[0].nativeElement.value).toBe('2.5');
      expect(allInputs[1].nativeElement.value).toBe('2.51');
      expect(allInputs[2].nativeElement.value).toBe('7.5');
      expect(allInputs[3].nativeElement.value).toBe('10');
    });

    it('should correct values larger than local upper bound', () => {
      const spy = spyOn(component, 'onChange');
      component.writeValue([
        { von: 0, bis: 0.25 },
        { von: 0.25, bis: 0.5 },
        { von: 0.5, bis: 0.75 },
        { von: 0.75, bis: 1.0 },
      ]);
      fixture.detectChanges();

      const allInputs = fixture.debugElement.queryAll(By.css('input:not([type="checkbox"])'));
      allInputs[1].nativeElement.value = '9.3';
      (allInputs[1].nativeElement as HTMLElement).dispatchEvent(new Event('input'));
      (allInputs[1].nativeElement as HTMLElement).dispatchEvent(new Event('blur'));

      expect(spy).toHaveBeenCalled();
      expect(spy.calls.mostRecent().args[0]).toEqual([
        { von: 0, bis: 0.25 },
        { von: 0.25, bis: 0.749 },
        { von: 0.749, bis: 0.75 },
        { von: 0.75, bis: 1.0 },
      ]);
      fixture.detectChanges();

      expect(allInputs[0].nativeElement.value).toBe('2.5');
      expect(allInputs[1].nativeElement.value).toBe('7.49');
      expect(allInputs[2].nativeElement.value).toBe('7.5');
      expect(allInputs[3].nativeElement.value).toBe('10');
    });
  });
});

@Component({
  template:
    '<rad-linear-referenzierter-abschnitt-control [geometrie]="geometrie"></rad-linear-referenzierter-abschnitt-control>',
  standalone: false,
})
export class LineareReferenzControlTestWrapperComponent {
  @ViewChild(LinearReferenzierterAbschnittControlComponent)
  component!: LinearReferenzierterAbschnittControlComponent;

  geometrie: LineStringGeojson = { coordinates: [], type: 'LineString' };
}

describe(LinearReferenzierterAbschnittControlComponent.name + ' - embedded', () => {
  let component: LinearReferenzierterAbschnittControlComponent;
  let wrapper: LineareReferenzControlTestWrapperComponent;
  let fixture: ComponentFixture<LineareReferenzControlTestWrapperComponent>;
  const notifyUserServiceMock = mock(NotifyUserService);

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        ReactiveFormsModule,
        MockModule(MatFormFieldModule),
        MockModule(MatInputModule),
        MockModule(MatToolbarModule),
        MockModule(MatIconModule),
        MockModule(MatListModule),
        MockModule(MatCheckboxModule),
        NoopAnimationsModule,
      ],
      declarations: [
        LinearReferenzierterAbschnittControlComponent,
        LineareReferenzControlTestWrapperComponent,
        MockComponent(AutocorrectingNumberInputControlComponent),
        MockComponent(LineareReferenzierungLayerComponent),
      ],
      providers: [
        {
          provide: NotifyUserService,
          useValue: instance(notifyUserServiceMock),
        },
      ],
    }).compileComponents();
  });

  const expectedCoordinates = [
    [0, 1],
    [10, 10],
  ];

  beforeEach(() => {
    fixture = TestBed.createComponent(LineareReferenzControlTestWrapperComponent);
    wrapper = fixture.componentInstance;
    wrapper.geometrie = {
      coordinates: expectedCoordinates,
      type: 'LineString',
    };
    fixture.detectChanges();
    component = wrapper.component;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should update linestring from geometrie', () => {
    expect(component.lineString.getCoordinates()).toEqual(expectedCoordinates);
  });
});
