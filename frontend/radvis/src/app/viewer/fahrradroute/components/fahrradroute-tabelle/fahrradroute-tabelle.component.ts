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

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, forwardRef } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { Observable } from 'rxjs';
import { skip, take } from 'rxjs/operators';
import { BenutzerDetailsService } from 'src/app/shared/services/benutzer-details.service';
import { RoutingProfileVerwaltenDialogComponent } from 'src/app/viewer/fahrradroute/components/routing-profile-verwalten-dialog/routing-profile-verwalten-dialog.component';
import { FahrradrouteListenView } from 'src/app/viewer/fahrradroute/models/fahrradroute-listen-view';
import { FahrradrouteFilterService } from 'src/app/viewer/fahrradroute/services/fahrradroute-filter.service';
import { FahrradrouteProfilService } from 'src/app/viewer/fahrradroute/services/fahrradroute-profil.service';
import { FahrradrouteRoutingService } from 'src/app/viewer/fahrradroute/services/fahrradroute-routing.service';
import { ExportFormat } from 'src/app/viewer/viewer-shared/models/export-format';
import { AbstractInfrastrukturenFilterService } from 'src/app/viewer/viewer-shared/services/abstract-infrastrukturen-filter.service';
import { ExportService } from 'src/app/viewer/viewer-shared/services/export.service';

@Component({
  selector: 'rad-fahrradroute-tabelle',
  templateUrl: './fahrradroute-tabelle.component.html',
  styleUrls: ['./fahrradroute-tabelle.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    { provide: AbstractInfrastrukturenFilterService, useExisting: forwardRef(() => FahrradrouteFilterService) },
  ],
})
export class FahrradrouteTabelleComponent {
  selectedFahrradrouteID$: Observable<number | null>;

  displayedColumns: string[] = [
    'name',
    'kategorie',
    'fahrradrouteTyp',
    'verantwortlicheOrganisation',
    'anstiegAbstieg',
  ];

  data$: Observable<FahrradrouteListenView[]>;

  exportFormate = [ExportFormat.GEOJSON, ExportFormat.SHP, ExportFormat.GEOPKG];

  public getDisplayValue = FahrradrouteListenView.getDisplayValueForKey;

  public fahrradroutenCreatorRoute: string;
  public isBenutzerBerechtigtFahrradroutenZuErstellen: boolean;
  public isBenutzerAdmin: boolean;

  public exporting = false;
  public showRoutenProfil$: Observable<boolean>;

  isSmallViewport = false;

  constructor(
    public fahrradrouteFilterService: FahrradrouteFilterService,
    private fahrradrouteRoutingService: FahrradrouteRoutingService,
    private exportService: ExportService,
    private changeDetector: ChangeDetectorRef,
    benutzerDetailsService: BenutzerDetailsService,
    private fahrradrouteProfilService: FahrradrouteProfilService,
    private dialog: MatDialog
  ) {
    this.data$ = this.fahrradrouteFilterService.filteredList$;

    this.selectedFahrradrouteID$ = this.fahrradrouteRoutingService.selectedInfrastrukturId$;

    this.isBenutzerBerechtigtFahrradroutenZuErstellen = benutzerDetailsService.canCreateFahrradrouten();
    this.isBenutzerAdmin = benutzerDetailsService.istAktuellerBenutzerAdmin();

    this.fahrradroutenCreatorRoute = this.fahrradrouteRoutingService.getCreatorRoute();

    this.showRoutenProfil$ = this.fahrradrouteProfilService.showCurrentRouteProfile$;
  }

  onChangeBreakpointState(isSmall: boolean): void {
    this.isSmallViewport = isSmall;
  }

  public onSelectRecord(id: number): void {
    this.fahrradrouteRoutingService.toInfrastrukturEditor(id);
  }

  onFilterReset(): void {
    this.fahrradrouteFilterService.reset();
  }

  public onExport(format: ExportFormat): void {
    const currentFilter = this.fahrradrouteFilterService.currentFilteredList.map(m => m.id);
    this.exporting = true;
    this.exportService.exportInfrastruktur('FAHRRADROUTE', format, currentFilter).finally(() => {
      this.exporting = false;
      this.changeDetector.markForCheck();
    });
  }

  onShowHoehenprofil(id: number): void {
    if (id === this.fahrradrouteRoutingService.getIdFromRoute()) {
      this.fahrradrouteProfilService.showCurrentRouteProfile();
    } else {
      this.fahrradrouteProfilService.currentRouteProfil$
        // Da wir in diesem Fall für das korrekte Profil auf das Laden der neuen Fahrradroute im Editor
        // warten müssen und hinter currentRouteProfil$ ein BehaviourSubject steht, wird der erste Wert,
        // der aus dem Observable kommt übersprungen (das wäre noch das Profil der zuletzt angezeigten Route oder null)
        // Dieses Pattern bitte nicht wiederverwenden! Ist hier nur nötig, weil der Editor nicht wissen kann, ob das
        // Profil beim Laden einer neuen Route angezeigt werden soll. Leider hat der Editor nicht die komplette Hoheit
        // über die Anzeige des Profils, sondern nur über deren Inhalt.
        .pipe(skip(1), take(1))
        .subscribe(() => this.fahrradrouteProfilService.showCurrentRouteProfile());
    }
    this.onSelectRecord(id);
  }

  hasLineStringGeometry(element: FahrradrouteListenView): boolean {
    return element.geometry?.type === 'LineString';
  }

  onCloseRoutenProfil(): void {
    this.fahrradrouteProfilService.hideCurrentRouteProfile();
  }

  onRoutingProfileVerwalten(): void {
    this.dialog.open(RoutingProfileVerwaltenDialogComponent, {
      width: '800px',
      disableClose: true,
    });
  }
}
