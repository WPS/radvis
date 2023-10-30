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
import { EditorModule } from 'src/app/editor/editor.module';
import { FreischaltungModule } from 'src/app/freischaltung/freischaltung.module';
import { GlobalErrorHandler } from 'src/app/global-error-handler';
import { RadnetzMatchingModule } from 'src/app/radnetzmatching/radnetz-matching.module';
import { RadvisHttpInterceptor } from 'src/app/radvis-http-interceptor';
import { RegistrierungModule } from 'src/app/registrierung/registrierung.module';
import { BenutzerDetailsService } from 'src/app/shared/services/benutzer-details.service';
import { FeatureTogglzService } from 'src/app/shared/services/feature-togglz.service';
import { SharedModule } from 'src/app/shared/shared.module';
import { WeitereKartenebenenService } from 'src/app/viewer/weitere-kartenebenen/services/weitere-kartenebenen.service';
import { ViewerModule } from 'src/app/viewer/viewer.module';
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { InfoModule } from './info/info.module';
import { RoutingProfileService } from 'src/app/viewer/fahrradroute/services/routing-profile.service';

const initBenutzerDetails = (benutzerDetailsService: BenutzerDetailsService) => (): Promise<any> =>
  benutzerDetailsService.fetchBenutzerDetails();

const initFeatureTogglz = (featureTogglzService: FeatureTogglzService) => (): Promise<any> =>
  featureTogglzService.fetchTogglz();

const initWeitereKartenebenen = (weitereKartenebenenService: WeitereKartenebenenService) => (): Promise<any> =>
  weitereKartenebenenService.initWeitereKartenebenen();

const initCustomRoutingProfiles = (routingProfileService: RoutingProfileService) => (): Promise<any> =>
  routingProfileService.initCustomRoutingProfiles();

@NgModule({
  declarations: [AppComponent],
  imports: [
    BrowserModule,
    RegistrierungModule,
    FreischaltungModule,
    ViewerModule,
    RadnetzMatchingModule,
    EditorModule,
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
  ],
})
export class AppModule {}
