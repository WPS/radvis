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

import { HttpClient } from '@angular/common/http';
import { Component, ElementRef, HostListener, ViewChild } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, IsActiveMatchOptions, NavigationError, Params, Router } from '@angular/router';
import { Observable } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { AdministrationRoutingService } from 'src/app/administration/services/administration-routing.service';
import { EditorRoutingService } from 'src/app/editor/editor-shared/services/editor-routing.service';
import { ImportRoutes } from 'src/app/import/models/import-routes';
import { MapQueryParamsService } from 'src/app/karte/services/map-query-params.service';
import { RadnetzMatchingRoutingService } from 'src/app/radnetzmatching/services/radnetz-matching-routing.service';
import { VerwaltungZugangsdatenComponent } from 'src/app/shared/components/verwaltung-zugangsdaten/verwaltung-zugangsdaten.component';
import { VordefinierteExporteComponent } from 'src/app/shared/components/vordefinierte-exporte/vordefinierte-exporte.component';
import { MapQueryParams } from 'src/app/shared/models/map-query-params';
import { BenutzerDetailsService } from 'src/app/shared/services/benutzer-details.service';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { FeatureTogglzService } from 'src/app/shared/services/feature-togglz.service';
import { LadeZustandService } from 'src/app/shared/services/lade-zustand.service';
import { ManualRoutingService } from 'src/app/shared/services/manual-routing.service';
import { VIEWER_ROUTE } from 'src/app/viewer/viewer-shared/models/viewer-routes';
import { ViewerRoutingService } from 'src/app/viewer/viewer-shared/services/viewer-routing.service';
import { InfoComponent } from './info/info.component';

@Component({
  selector: 'rad-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
})
export class AppComponent {
  @ViewChild('viewerLink')
  viewerLink: ElementRef | undefined;

  public viewerRoute = VIEWER_ROUTE;
  public radnetzMatchingRoute = RadnetzMatchingRoutingService.RADNETZ_MATCHING_ROUTE;
  public editorRoute = EditorRoutingService.EDITOR_ROUTE;
  public importRoute = ImportRoutes.IMPORT_ROUTE;

  public administrationBenutzerRoute = AdministrationRoutingService.ADMINISTRATION_BENUTZER_ROUTE;
  public administrationOrganisationRoute = AdministrationRoutingService.ADMINISTRATION_ORGANISATION_ROUTE;
  public auswertungRoute = 'auswertung';
  public viewerQueryParams$: Observable<Params>;

  public isBenutzerRegistriert?: boolean;
  public isBenutzerAktiv?: boolean;
  public isBenutzerOrgaUndNutzerVerwalter: boolean;
  public canBenutzerEditBereicheOfOrganisation: boolean;
  public isBenutzerRadNETZQualitaetsSicherIn: boolean;
  public benutzerName?: string;
  public benutzerVorname?: string;
  public benutzerOrganisation?: string;
  public ladend$: Observable<boolean>;
  public canBenutzerEdit: boolean;
  public hasBenutzerImportRecht: boolean;
  routerLinkActiveOptions: IsActiveMatchOptions = {
    paths: 'subset',
    queryParams: 'ignored',
    fragment: 'ignored',
    matrixParams: 'ignored',
  };

  constructor(
    private router: Router,
    activatedRoute: ActivatedRoute,
    mapQueryParamsService: MapQueryParamsService,
    ladeZustandService: LadeZustandService,
    private benutzerDetailsService: BenutzerDetailsService,
    private http: HttpClient,
    private dialog: MatDialog,
    private manualRoutingService: ManualRoutingService,
    private featureTogglzService: FeatureTogglzService,
    errorHandlingService: ErrorHandlingService
  ) {
    this.viewerQueryParams$ = activatedRoute.queryParams.pipe(
      map(currentParams => {
        return {
          ...currentParams,
          ...MapQueryParams.merge(
            mapQueryParamsService.mapQueryParamsSnapshot,
            ViewerRoutingService.DEFAULT_QUERY_PARAMS
          ).toRouteParams(),
        };
      })
    );
    this.ladend$ = ladeZustandService.isLoading$;
    this.isBenutzerRegistriert = this.benutzerDetailsService.istAktuellerBenutzerRegistriert();
    this.isBenutzerAktiv = this.benutzerDetailsService.istAktuellerBenutzerAktiv();
    this.isBenutzerOrgaUndNutzerVerwalter = this.benutzerDetailsService.istAktuellerBenutzerOrgaUndNutzerVerwalter();
    this.canBenutzerEditBereicheOfOrganisation =
      this.benutzerDetailsService.canEditZustaendigkeitsBereichOfOrganisation();
    this.isBenutzerRadNETZQualitaetsSicherIn =
      this.benutzerDetailsService.istAktuellerBenutzerRadNETZQualitaetsSicherInOrAdmin();
    this.benutzerName = benutzerDetailsService.aktuellerBenutzerNachname();
    this.benutzerVorname = benutzerDetailsService.aktuellerBenutzerVorname();
    this.benutzerOrganisation = benutzerDetailsService.aktuellerBenutzerOrganisationName();
    this.canBenutzerEdit = benutzerDetailsService.canEdit();
    this.hasBenutzerImportRecht = benutzerDetailsService.canBenutzerImport();
    router.events.pipe(filter(e => e instanceof NavigationError)).subscribe(e => {
      errorHandlingService.handleError(
        (e as NavigationError).error,
        'Aufgerufene Route konnte nicht geöffnet werden: ' + (e as NavigationError).url.split('?')[0]
      );
      this.router.navigateByUrl('/').then();
    });
  }

  @HostListener('document:keydown.control.alt.shift.n')
  onShortcut(): void {
    this.viewerLink?.nativeElement.focus();
  }

  get isOrganisationenBearbeitenToggleOn(): boolean {
    return this.featureTogglzService.isToggledOn(FeatureTogglzService.TOGGLZ_ORGANISATIONEN_ERSTELLEN_UND_BEARBEITEN);
  }

  get isVordefinierteExporteToggleOn(): boolean {
    return this.featureTogglzService.isToggledOn(FeatureTogglzService.TOGGLZ_VORDEFINIERTE_EXPORTE);
  }

  get isBasicAuthZugangsdatenVerwaltenToggleOn(): boolean {
    return this.featureTogglzService.isToggledOn(FeatureTogglzService.TOGGLZ_BASIC_AUTH_VERWALTEN_ANZEIGEN);
  }

  openInfoDialog(): void {
    this.dialog.open(InfoComponent);
  }

  openVordefinierteExporte(): void {
    this.dialog.open(VordefinierteExporteComponent);
  }

  openManual(): void {
    this.manualRoutingService.openManual();
  }

  openVerwaltungZugangsdaten(): void {
    this.dialog.open(VerwaltungZugangsdatenComponent);
  }
}
