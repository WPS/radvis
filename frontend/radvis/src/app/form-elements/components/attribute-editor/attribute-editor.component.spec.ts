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
import { MatToolbar } from '@angular/material/toolbar';
import { RouterTestingModule } from '@angular/router/testing';
import { MockComponent } from 'ng-mocks';
import { EditorModule } from 'src/app/editor/editor.module';
import { AttributeEditorComponent } from 'src/app/form-elements/components/attribute-editor/attribute-editor.component';

describe('AttributeEditorComponent', () => {
  let component: AttributeEditorComponent;
  let fixture: ComponentFixture<AttributeEditorComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [AttributeEditorComponent, MockComponent(MatToolbar)],
      imports: [EditorModule, RouterTestingModule],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(AttributeEditorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
