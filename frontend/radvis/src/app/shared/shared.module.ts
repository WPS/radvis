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

import { HttpClientModule } from '@angular/common/http';
import { NgModule } from '@angular/core';
import { MatPaginatorIntl } from '@angular/material/paginator';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RouterModule } from '@angular/router';
import { FormElementsModule } from 'src/app/form-elements/form-elements.module';
import { MaterialDesignModule } from 'src/app/material-design.module';
import { BenutzerNamePipe } from 'src/app/shared/components/benutzer-name.pipe';
import { ColorToCssPipe } from 'src/app/shared/components/color-to-css.pipe';
import { ConfirmationDialogComponent } from 'src/app/shared/components/confirmation-dialog/confirmation-dialog.component';
import { EnumDisplayPipe } from 'src/app/shared/components/enum-display.pipe';
import { HinweisDialogComponent } from 'src/app/shared/components/hinweis-dialog/hinweis-dialog.component';
import { HoverDirective } from 'src/app/shared/components/hover.directive';
import { KommazahlPipe } from 'src/app/shared/components/kommazahl.pipe';
import { LineareReferenzierungLayerComponent } from 'src/app/shared/components/lineare-referenzierung-layer/lineare-referenzierung-layer.component';
import { OlPopupComponent } from 'src/app/shared/components/ol-popup/ol-popup.component';
import { OrganisationPipe } from 'src/app/shared/components/organisation.pipe';
import { OrganisationenDropdownControlComponent } from 'src/app/shared/components/organisationen-dropdown-control/organisationen-dropdown-control.component';
import { PrintViewComponent } from 'src/app/shared/components/print-view/print-view.component';
import { TruncateTextPipe } from 'src/app/shared/components/truncate-text.pipe';
import { MatPaginatorIntlLocalizationService } from './services/mat-paginator-intl-localization.service';
import { SimpleLegendeAnzeigeComponent } from 'src/app/shared/components/simple-legende-anzeige/simple-legende-anzeige.component';
import { VordefinierteExporteComponent } from 'src/app/shared/components/vordefinierte-exporte/vordefinierte-exporte.component';
import { AccessabilityTextDirective } from 'src/app/shared/components/accessability-text.directive';
import { SicherheitstrennstreifenAnzeigeComponent } from 'src/app/shared/components/sicherheitstrennstreifen-anzeige/sicherheitstrennstreifen-anzeige.component';
import { SicherheitstrennstreifenAnzeigeKomplettComponent } from 'src/app/shared/components/sicherheitstrennstreifen-anzeige-komplett.component/sicherheitstrennstreifen-anzeige-komplett.component';
import { AccessabilityTabCircleGroupDirective } from 'src/app/shared/components/accessability-tab-circle-group.directive';
import { AccessabilityTabCircleElementDirective } from 'src/app/shared/components/accessability-tab-circle-element.directive';

const allDeclarations = [
  OlPopupComponent,
  ConfirmationDialogComponent,
  KommazahlPipe,
  HoverDirective,
  HinweisDialogComponent,
  LineareReferenzierungLayerComponent,
  ColorToCssPipe,
  BenutzerNamePipe,
  OrganisationPipe,
  EnumDisplayPipe,
  TruncateTextPipe,
  PrintViewComponent,
  OrganisationenDropdownControlComponent,
  SimpleLegendeAnzeigeComponent,
  VordefinierteExporteComponent,
  AccessabilityTextDirective,
  SicherheitstrennstreifenAnzeigeComponent,
  SicherheitstrennstreifenAnzeigeKomplettComponent,
  AccessabilityTabCircleGroupDirective,
  AccessabilityTabCircleElementDirective,
];

@NgModule({
  declarations: allDeclarations,
  imports: [
    FormElementsModule,
    BrowserModule,
    HttpClientModule,
    MaterialDesignModule,
    BrowserAnimationsModule,
    RouterModule,
  ],
  exports: [
    ...allDeclarations,
    BrowserModule,
    HttpClientModule,
    MaterialDesignModule,
    BrowserAnimationsModule,
    RouterModule,
    FormElementsModule,
  ],
  providers: [{ provide: MatPaginatorIntl, useClass: MatPaginatorIntlLocalizationService }],
})
export class SharedModule {}
