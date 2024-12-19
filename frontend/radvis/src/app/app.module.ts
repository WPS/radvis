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

import { HTTP_INTERCEPTORS } from '@angular/common/http';
import { APP_INITIALIZER, ErrorHandler, NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { AdministrationModule } from 'src/app/administration/administration.module';
import { AuswertungModule } from 'src/app/auswertung/auswertung.module';
import { SystembenachrichtigungComponent } from 'src/app/components/systembenachrichtigung/systembenachrichtigung.component';
import { EditorModule } from 'src/app/editor/editor.module';
import { FehlerprotokollService } from 'src/app/fehlerprotokoll/services/fehlerprotokoll.service';
import { FreischaltungModule } from 'src/app/freischaltung/freischaltung.module';
import { GlobalErrorHandler } from 'src/app/global-error-handler';
import { ImportModule } from 'src/app/import/import.module';
import { HintergrundLayerService } from 'src/app/karte/services/hintergrund-layer.service';
import { RadnetzMatchingModule } from 'src/app/radnetzmatching/radnetz-matching.module';
import { RadvisHttpInterceptor } from 'src/app/radvis-http-interceptor';
import { RegistrierungModule } from 'src/app/registrierung/registrierung.module';
import { BenutzerDetailsService } from 'src/app/shared/services/benutzer-details.service';
import { FeatureTogglzService } from 'src/app/shared/services/feature-togglz.service';
import { SharedModule } from 'src/app/shared/shared.module';
import { RoutingProfileService } from 'src/app/viewer/fahrradroute/services/routing-profile.service';
import { SignaturService } from 'src/app/viewer/signatur/services/signatur.service';
import { ViewerModule } from 'src/app/viewer/viewer.module';
import { VordefinierteLayerService } from 'src/app/viewer/weitere-kartenebenen/services/vordefinierte-layer.service';
import { WeitereKartenebenenService } from 'src/app/viewer/weitere-kartenebenen/services/weitere-kartenebenen.service';
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { InfoModule } from './info/info.module';

const initBenutzerDetails = (benutzerDetailsService: BenutzerDetailsService) => (): Promise<any> =>
  benutzerDetailsService.fetchBenutzerDetails();

const initFeatureTogglz = (featureTogglzService: FeatureTogglzService) => (): Promise<any> =>
  featureTogglzService.fetchTogglz();

const initWeitereKartenebenen = (weitereKartenebenenService: WeitereKartenebenenService) => (): Promise<any> =>
  weitereKartenebenenService.initWeitereKartenebenen();

const initCustomRoutingProfiles = (routingProfileService: RoutingProfileService) => (): Promise<any> =>
  routingProfileService.initCustomRoutingProfiles();

const initVordefinierteLayer = (vordefinierteLayerService: VordefinierteLayerService) => (): Promise<any> =>
  vordefinierteLayerService.initPredefinedLayer();

const initHintergrundLayers = (hintergrundLayerService: HintergrundLayerService) => (): Promise<any> =>
  hintergrundLayerService.initLayers();

const initSignaturen = (signaturService: SignaturService) => (): Promise<any> => signaturService.initSignaturen();

const initFehlerprotokollTypen = (fehlerprotokollService: FehlerprotokollService) => (): Promise<any> =>
  fehlerprotokollService.initFehlerprotokollTypen();

// Bitte dran denken: Alle APP_INITIALIZER-Endpunkte müssen im Backend in SecurityConfiguration.internalApiFilterChain()
// als "authenticated" hinzugefügt werden, sonst funktioniert die Anmeldung mit nicht registrierten Benutzern nicht!
@NgModule({
  declarations: [AppComponent, SystembenachrichtigungComponent],
  imports: [
    BrowserModule,
    RegistrierungModule,
    FreischaltungModule,
    ViewerModule,
    RadnetzMatchingModule,
    EditorModule,
    ImportModule,
    SharedModule,
    AdministrationModule,
    AuswertungModule,
    // AppRoutingModule muss nach allen Modules geladen werden,
    // die Routing mitbringen, da die default Fehler-Route enthalten ist
    AppRoutingModule,
    InfoModule,
  ],
  bootstrap: [AppComponent],
  providers: [
    {
      provide: HTTP_INTERCEPTORS,
      useClass: RadvisHttpInterceptor,
      multi: true,
    },
    {
      provide: ErrorHandler,
      useClass: GlobalErrorHandler,
    },
    {
      provide: APP_INITIALIZER,
      multi: true,
      useFactory: initBenutzerDetails,
      deps: [BenutzerDetailsService],
    },
    {
      provide: APP_INITIALIZER,
      multi: true,
      useFactory: initVordefinierteLayer,
      deps: [VordefinierteLayerService],
    },
    {
      provide: APP_INITIALIZER,
      multi: true,
      useFactory: initHintergrundLayers,
      deps: [HintergrundLayerService],
    },
    {
      provide: APP_INITIALIZER,
      multi: true,
      useFactory: initFehlerprotokollTypen,
      deps: [FehlerprotokollService],
    },
    {
      provide: APP_INITIALIZER,
      multi: true,
      useFactory: initFeatureTogglz,
      deps: [FeatureTogglzService],
    },
    {
      provide: APP_INITIALIZER,
      multi: true,
      useFactory: initWeitereKartenebenen,
      deps: [WeitereKartenebenenService],
    },
    {
      provide: APP_INITIALIZER,
      multi: true,
      useFactory: initCustomRoutingProfiles,
      deps: [RoutingProfileService],
    },
    {
      provide: APP_INITIALIZER,
      multi: true,
      useFactory: initSignaturen,
      deps: [SignaturService],
    },
  ],
})
export class AppModule {}
