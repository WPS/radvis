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

import { ClipboardModule } from '@angular/cdk/clipboard';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { NgModule } from '@angular/core';
import { MatPaginatorIntl } from '@angular/material/paginator';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RouterModule } from '@angular/router';
import { MatomoModule, provideMatomo, withRouter } from 'ngx-matomo-client';
import { FormElementsModule } from 'src/app/form-elements/form-elements.module';
import { MaterialDesignModule } from 'src/app/material-design.module';
import { AccessabilityTabCircleElementDirective } from 'src/app/shared/components/accessability-tab-circle-element.directive';
import { AccessabilityTabCircleGroupDirective } from 'src/app/shared/components/accessability-tab-circle-group.directive';
import { AccessabilityTextDirective } from 'src/app/shared/components/accessability-text.directive';
import { BedienhinweisComponent } from 'src/app/shared/components/bedienhinweis/bedienhinweis.component';
import { BenutzerNamePipe } from 'src/app/shared/components/benutzer-name.pipe';
import { CollapseDownButtonComponent } from 'src/app/shared/components/collapse-down-button/collapse-down-button.component';
import { ColorToCssPipe } from 'src/app/shared/components/color-to-css.pipe';
import { ConfirmationDialogComponent } from 'src/app/shared/components/confirmation-dialog/confirmation-dialog.component';
import { EnumDisplayPipe } from 'src/app/shared/components/enum-display.pipe';
import { ExpandLinkComponent } from 'src/app/shared/components/expandable-content/expand-link/expand-link.component';
import { ExpandableContentComponent } from 'src/app/shared/components/expandable-content/expandable-content.component';
import { HinweisDialogComponent } from 'src/app/shared/components/hinweis-dialog/hinweis-dialog.component';
import { HoverDirective } from 'src/app/shared/components/hover.directive';
import { KommazahlPipe } from 'src/app/shared/components/kommazahl.pipe';
import { LineareReferenzierungLayerComponent } from 'src/app/shared/components/lineare-referenzierung-layer/lineare-referenzierung-layer.component';
import { NetzbezugHighlightLayerComponent } from 'src/app/shared/components/netzbezug-highlight-layer/netzbezug-highlight-layer.component';
import { BearbeitungsModusToggleButtonComponent } from 'src/app/shared/components/netzbezug-selektion-control/bearbeitungs-modus-toggle-button/bearbeitungs-modus-toggle-button.component';
import { NetzbezugSelektionControlComponent } from 'src/app/shared/components/netzbezug-selektion-control/netzbezug-selektion-control.component';
import { NetzbezugSelektionLayerComponent } from 'src/app/shared/components/netzbezug-selektion-control/netzbezug-selektion-layer/netzbezug-selektion-layer.component';
import { OlPopupComponent } from 'src/app/shared/components/ol-popup/ol-popup.component';
import { OrganisationenDropdownControlComponent } from 'src/app/shared/components/organisationen-dropdown-control/organisationen-dropdown-control.component';
import { OriginalGeometrieLayerComponent } from 'src/app/shared/components/original-geometrie-layer/original-geometrie-layer.component';
import { PrintViewComponent } from 'src/app/shared/components/print-view/print-view.component';
import { RegenerateCredentialsConfirmComponent } from 'src/app/shared/components/regenerate-credentials-confirm/regenerate-credentials-confirm.component';
import { SharedKnotenFormGroupComponent } from 'src/app/shared/components/shared-knoten-form-group/shared-knoten-form-group.component';
import { SicherheitstrennstreifenAnzeigeKomplettComponent } from 'src/app/shared/components/sicherheitstrennstreifen-anzeige-komplett.component/sicherheitstrennstreifen-anzeige-komplett.component';
import { SicherheitstrennstreifenAnzeigeComponent } from 'src/app/shared/components/sicherheitstrennstreifen-anzeige/sicherheitstrennstreifen-anzeige.component';
import { SimpleLegendeAnzeigeComponent } from 'src/app/shared/components/simple-legende-anzeige/simple-legende-anzeige.component';
import { TruncateTextPipe } from 'src/app/shared/components/truncate-text.pipe';
import { VerwaltungZugangsdatenComponent } from 'src/app/shared/components/verwaltung-zugangsdaten/verwaltung-zugangsdaten.component';
import { VordefinierteExporteComponent } from 'src/app/shared/components/vordefinierte-exporte/vordefinierte-exporte.component';
import { WahlkreisDropdownControlComponent } from 'src/app/shared/components/wahlkreis-dropdown-control/wahlkreis-dropdown-control.component';
import { WarnhinweisComponent } from 'src/app/shared/components/warnhinweis/warnhinweis.component';
import { environment } from 'src/environments/environment';
import { MatPaginatorIntlLocalizationService } from './services/mat-paginator-intl-localization.service';

const allDeclarations = [
  OlPopupComponent,
  ConfirmationDialogComponent,
  KommazahlPipe,
  HoverDirective,
  HinweisDialogComponent,
  LineareReferenzierungLayerComponent,
  ColorToCssPipe,
  BenutzerNamePipe,
  EnumDisplayPipe,
  TruncateTextPipe,
  PrintViewComponent,
  OrganisationenDropdownControlComponent,
  WahlkreisDropdownControlComponent,
  SimpleLegendeAnzeigeComponent,
  VordefinierteExporteComponent,
  VerwaltungZugangsdatenComponent,
  RegenerateCredentialsConfirmComponent,
  AccessabilityTextDirective,
  SicherheitstrennstreifenAnzeigeComponent,
  SicherheitstrennstreifenAnzeigeKomplettComponent,
  AccessabilityTabCircleGroupDirective,
  AccessabilityTabCircleElementDirective,
  NetzbezugSelektionControlComponent,
  NetzbezugSelektionLayerComponent,
  NetzbezugHighlightLayerComponent,
  BearbeitungsModusToggleButtonComponent,
  BedienhinweisComponent,
  OriginalGeometrieLayerComponent,
  WarnhinweisComponent,
  ExpandableContentComponent,
  CollapseDownButtonComponent,
  SharedKnotenFormGroupComponent,
];

@NgModule({
  declarations: [allDeclarations, ExpandLinkComponent],
  exports: [
    ...allDeclarations,
    BrowserModule,
    MaterialDesignModule,
    BrowserAnimationsModule,
    RouterModule,
    FormElementsModule,
    MatomoModule,
  ],
  imports: [
    FormElementsModule,
    BrowserModule,
    MaterialDesignModule,
    BrowserAnimationsModule,
    RouterModule,
    ClipboardModule,
  ],
  providers: [
    { provide: MatPaginatorIntl, useClass: MatPaginatorIntlLocalizationService },
    provideMatomo(environment.matomoConfig, withRouter({ navigationEndComparator: 'ignoreQueryParams' })),
    provideHttpClient(withInterceptorsFromDi()),
  ],
})
export class SharedModule {}
