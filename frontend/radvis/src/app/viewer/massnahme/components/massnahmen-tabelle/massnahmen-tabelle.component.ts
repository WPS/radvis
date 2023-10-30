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
import { FormControl } from '@angular/forms';
import { Observable } from 'rxjs';
import { RadvisValidators } from 'src/app/form-elements/models/radvis-validators';
import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';
import { BenutzerDetailsService } from 'src/app/shared/services/benutzer-details.service';
import { OrganisationenService } from 'src/app/shared/services/organisationen.service';
import { MassnahmeListenView } from 'src/app/viewer/massnahme/models/massnahme-listen-view';
import { MassnahmeFilterService } from 'src/app/viewer/massnahme/services/massnahme-filter.service';
import { MassnahmenRoutingService } from 'src/app/viewer/massnahme/services/massnahmen-routing.service';
import { ExportFormat } from 'src/app/viewer/viewer-shared/models/export-format';
import { AbstractInfrastrukturenFilterService } from 'src/app/viewer/viewer-shared/services/abstract-infrastrukturen-filter.service';
import { ExportService } from 'src/app/viewer/viewer-shared/services/export.service';

@Component({
  selector: 'rad-massnahmen-tabelle',
  templateUrl: './massnahmen-tabelle.component.html',
  styleUrls: ['./massnahmen-tabelle.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [{ provide: AbstractInfrastrukturenFilterService, useExisting: forwardRef(() => MassnahmeFilterService) }],
})
export class MassnahmenTabelleComponent {
  selectedMassnahmeID$: Observable<number | null>;
  data$: Observable<MassnahmeListenView[]>;

  exportFormate = [ExportFormat.GEOJSON, ExportFormat.SHP, ExportFormat.CSV];

  displayedColumns: string[] = [
    'massnahmeKonzeptId',
    'bezeichnung',
    'massnahmenkategorien',
    'durchfuehrungszeitraum',
    'umsetzungsstatus',
    'umsetzungsstandStatus',
    'veroeffentlicht',
    'planungErforderlich',
    'prioritaet',
    'netzklassen',
    'baulastZustaendiger',
    'markierungsZustaendiger',
    'unterhaltsZustaendiger',
    'letzteAenderung',
    'benutzerLetzteAenderung',
    'sollStandard',
    'handlungsverantwortlicher',
  ];

  public alleNonBundeslandOrganisationenOptions: Promise<Verwaltungseinheit[]>;

  public getDisplayValue = MassnahmeListenView.getDisplayValueForKey;
  public isBenutzerBerechtigtMassnahmenZuErstellen: boolean;
  public isBenutzerBerechtigtUmsetzungsstandsabfragenZuVerwalten: boolean;
  public massnahmenCreatorRoute: string;
  organisationControl: FormControl;

  public exporting = false;

  isSmallViewport = false;

  constructor(
    public massnahmeFilterService: MassnahmeFilterService,
    private massnahmenRoutingService: MassnahmenRoutingService,
    private exportService: ExportService,
    private changeDetector: ChangeDetectorRef,
    benutzerDetailsService: BenutzerDetailsService,
    organisationenService: OrganisationenService
  ) {
    this.isBenutzerBerechtigtMassnahmenZuErstellen = benutzerDetailsService.canCreateMassnahmen();
    this.isBenutzerBerechtigtUmsetzungsstandsabfragenZuVerwalten = benutzerDetailsService.canAdministrateUmsetzungsstandsabfragen();
    this.massnahmenCreatorRoute = this.massnahmenRoutingService.getCreatorRoute();
    this.selectedMassnahmeID$ = this.massnahmenRoutingService.selectedInfrastrukturId$;
    this.data$ = this.massnahmeFilterService.filteredList$;
    this.alleNonBundeslandOrganisationenOptions = organisationenService
      .getOrganisationen()
      .then(organisationen => organisationen.filter(organisation => !Verwaltungseinheit.isLandesweit(organisation)));

    this.organisationControl = new FormControl(
      this.massnahmeFilterService.organisation,
      RadvisValidators.isNotNullOrEmpty
    );

    this.organisationControl.valueChanges.subscribe(organisation => {
      this.massnahmeFilterService.organisation = organisation;
      this.massnahmeFilterService.refetchData();
    });
  }

  onChangeBreakpointState(isSmall: boolean): void {
    this.isSmallViewport = isSmall;
  }

  public onSelectRecord(id: number): void {
    this.massnahmenRoutingService.toInfrastrukturEditor(id);
  }

  onFilterReset(): void {
    this.massnahmeFilterService.reset();
  }

  public sortingDataAccessor = (item: MassnahmeListenView, header: string): any => {
    const displayValue = MassnahmeListenView.getSortingValueForKey(item, header);
    return Array.isArray(displayValue) ? displayValue[0] : displayValue;
  };

  public onExport(format: ExportFormat): void {
    const currentFilter = this.massnahmeFilterService.currentFilteredList.map(m => m.id);
    this.exporting = true;
    this.exportService.exportInfrastruktur('MASSNAHME', format, currentFilter).finally(() => {
      this.exporting = false;
      this.changeDetector.markForCheck();
    });
  }
}
