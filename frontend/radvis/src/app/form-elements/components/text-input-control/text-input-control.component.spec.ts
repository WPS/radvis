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

import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { UndeterminedValue } from 'src/app/form-elements/components/abstract-undetermined-form-control';
import { TextInputControlComponent } from 'src/app/form-elements/components/text-input-control/text-input-control.component';
import { MaterialDesignModule } from 'src/app/material-design.module';

describe(TextInputControlComponent.name, () => {
  let fixture: ComponentFixture<TextInputControlComponent>;
  let component: TextInputControlComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReactiveFormsModule, NoopAnimationsModule, MaterialDesignModule],
      declarations: [TextInputControlComponent],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(TextInputControlComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  let onChangeSpy: jasmine.Spy;
  beforeEach(() => {
    onChangeSpy = spyOn(component, 'onChange');
  });

  it('should process normal values', () => {
    component.writeValue('normal value');
    fixture.detectChanges();

    expect(onChangeSpy).not.toHaveBeenCalled();
    expect(component.formControl.value).toBe('normal value');
    expect(fixture.debugElement.query(By.css('input'))?.nativeElement.value).toEqual('normal value');
  });

  it('should process UndeterminedValue', () => {
    component.writeValue(new UndeterminedValue());
    fixture.detectChanges();

    expect(onChangeSpy).not.toHaveBeenCalled();
    expect(component.formControl.value).toBe('');
    expect(fixture.debugElement.query(By.css('mat-hint')).nativeElement.innerText).toEqual('Mehrere Werte ausgewählt');
  });

  it('should process null', () => {
    component.writeValue(null);
    fixture.detectChanges();

    expect(onChangeSpy).not.toHaveBeenCalled();
    expect(component.formControl.value).toBe('');
    expect(fixture.debugElement.query(By.css('input'))?.nativeElement.value).toEqual('');
  });

  it('should trim', fakeAsync(() => {
    component.formControl.setValue('      watch liz and the blue birds you cowards         ');
    tick();

    expect(onChangeSpy).toHaveBeenCalled();
    expect(onChangeSpy.calls.mostRecent().args[0]).toBe('watch liz and the blue birds you cowards');
  }));
});
