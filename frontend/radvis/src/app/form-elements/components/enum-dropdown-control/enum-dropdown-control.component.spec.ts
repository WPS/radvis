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

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MockModule } from 'ng-mocks';
import { Hoechstgeschwindigkeit } from 'src/app/editor/kanten/models/hoechstgeschwindigkeit';
import { UndeterminedValue } from 'src/app/form-elements/components/abstract-undetermined-form-control';
import { EnumDropdownControlComponent } from 'src/app/form-elements/components/enum-dropdown-control/enum-dropdown-control.component';

describe('EnumDropdownControlComponent', () => {
  let fixture: ComponentFixture<EnumDropdownControlComponent>;
  let component: EnumDropdownControlComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReactiveFormsModule, MockModule(MatFormFieldModule), NoopAnimationsModule, MockModule(MatSelectModule)],
      declarations: [EnumDropdownControlComponent],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(EnumDropdownControlComponent);
    component = fixture.componentInstance;
    component.options = Hoechstgeschwindigkeit.options;
    fixture.detectChanges();
  });

  it('should hide undetermined option when not undetermined', () => {
    component.writeValue(new UndeterminedValue());

    component.formControl.patchValue(Hoechstgeschwindigkeit.UEBER_100_KMH);

    expect(component.showUndeterminedOption).toBeFalse();
  });

  it('should hide undetermined option when not undetermined', () => {
    component.writeValue(new UndeterminedValue());

    component.writeValue(Hoechstgeschwindigkeit.UEBER_100_KMH);

    expect(component.showUndeterminedOption).toBeFalse();
  });
});

describe('EnumDropdownControlComponent - embedded', () => {
  let fixture: ComponentFixture<EnumDropdownControlComponent>;
  let component: EnumDropdownControlComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReactiveFormsModule, MatFormFieldModule, NoopAnimationsModule, MatSelectModule],
      declarations: [EnumDropdownControlComponent],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(EnumDropdownControlComponent);
    component = fixture.componentInstance;
    component.options = Hoechstgeschwindigkeit.options;
    fixture.detectChanges();
  });

  it('should process UndeterminedValue', () => {
    component.writeValue(new UndeterminedValue());
    fixture.detectChanges();

    expect(component.formControl.value).toBe(component.UNDETERMINED);
    expect(component.showUndeterminedOption).toBeTrue();
    expect(fixture.debugElement.query(By.css('.mat-select-value-text'))?.nativeElement.innerText).toEqual(
      'Mehrere Werte ausgewählt'
    );
  });
});
