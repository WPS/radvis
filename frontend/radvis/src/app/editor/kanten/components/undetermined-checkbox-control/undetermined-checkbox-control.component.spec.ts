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
import { MatCheckbox, MatCheckboxModule } from '@angular/material/checkbox';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { UndeterminedCheckboxControlComponent } from 'src/app/editor/kanten/components/undetermined-checkbox-control/undetermined-checkbox-control.component';
import { UndeterminedValue } from 'src/app/form-elements/components/abstract-undetermined-form-control';

describe('UndeterminedCheckboxControlComponent', () => {
  let fixture: ComponentFixture<UndeterminedCheckboxControlComponent>;
  let component: UndeterminedCheckboxControlComponent;
  let checkbox: MatCheckbox;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReactiveFormsModule, NoopAnimationsModule, MatCheckboxModule],
      declarations: [UndeterminedCheckboxControlComponent],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(UndeterminedCheckboxControlComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    checkbox = fixture.debugElement.childNodes[0].componentInstance;
  });

  it('should not show as undetermined when checkbox is checked', () => {
    component.writeValue(new UndeterminedValue());
    fixture.detectChanges();
    expect(component.isUndetermined).toBeTrue();
    expect(checkbox.indeterminate).toBeTrue();
  });

  it('should not show as undetermined when not undetermined', () => {
    component.writeValue(true);
    fixture.detectChanges();
    expect(component.checked).toBeTrue();
    expect(checkbox.indeterminate).toBeFalse();
    expect(checkbox.checked).toBeTrue();
  });

  it('should update own state', () => {
    component.writeValue(new UndeterminedValue());
    fixture.detectChanges();
    fixture.debugElement.children[0].triggerEventHandler('change', { checked: true });
    fixture.detectChanges();

    expect(component.checked).toBeTrue();
    expect(component.isUndetermined).toBeFalse();
  });
});
