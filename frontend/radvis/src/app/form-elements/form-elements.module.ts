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

import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { ActionButtonComponent } from 'src/app/form-elements/components/action-button/action-button.component';
import { AttributeEditorComponent } from 'src/app/form-elements/components/attribute-editor/attribute-editor.component';
import { AutocompleteDropdownComponent } from 'src/app/form-elements/components/autocomplete-dropdown/autocomplete-dropdown.component';
import { AutocompleteEnumDropdownControlComponent } from 'src/app/form-elements/components/autocomplete-enum-dropdown-control/autocomplete-enum-dropdown-control.component';
import { ButtonAccessibilityDirective } from 'src/app/form-elements/components/button-accessibility.dircetive';
import { EnumDropdownControlComponent } from 'src/app/form-elements/components/enum-dropdown-control/enum-dropdown-control.component';
import { FileUploadControlComponent } from 'src/app/form-elements/components/file-upload-control/file-upload-control.component';
import { FloatInputControlComponent } from 'src/app/form-elements/components/float-input-control/float-input-control.component';
import { GroupedEnumDropdownControlComponent } from 'src/app/form-elements/components/grouped-enum-dropdown-control/grouped-enum-dropdown-control.component';
import { MultiOrganisationenDropdownControlComponent } from 'src/app/form-elements/components/multi-organisationen-dropdown-control/multi-organisationen-dropdown-control.component';
import { NumberInputControlComponent } from 'src/app/form-elements/components/number-input-control/number-input-control.component';
import { OrganisationPipe } from 'src/app/form-elements/components/organisation.pipe';
import { TextInputControlComponent } from 'src/app/form-elements/components/text-input-control/text-input-control.component';
import { ValidationErrorAnzeigeComponent } from 'src/app/form-elements/components/validation-error-anzeige/validation-error-anzeige.component';
import { MaterialDesignModule } from 'src/app/material-design.module';
import { ErrorMessagePipe } from 'src/app/form-elements/components/error-message.pipe';

const allDeclarations = [
  ValidationErrorAnzeigeComponent,
  TextInputControlComponent,
  NumberInputControlComponent,
  FloatInputControlComponent,
  GroupedEnumDropdownControlComponent,
  FileUploadControlComponent,
  EnumDropdownControlComponent,
  AutocompleteDropdownComponent,
  AutocompleteEnumDropdownControlComponent,
  AttributeEditorComponent,
  ActionButtonComponent,
  MultiOrganisationenDropdownControlComponent,
  OrganisationPipe,
  ButtonAccessibilityDirective,
  ErrorMessagePipe,
];

@NgModule({
  declarations: allDeclarations,
  imports: [CommonModule, ReactiveFormsModule, MaterialDesignModule],
  exports: [...allDeclarations, ReactiveFormsModule],
})
export class FormElementsModule {}
