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

import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  Input,
  OnDestroy,
  OnInit,
  QueryList,
  ViewChild,
  ViewChildren,
} from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatMenuTrigger } from '@angular/material/menu';
import { of, Subscription, zip } from 'rxjs';
import { concatMap, distinctUntilChanged, map } from 'rxjs/operators';
import { FehlerprotokollLayerComponent } from 'src/app/fehlerprotokoll/components/fehlerprotokoll-layer/fehlerprotokoll-layer.component';
import { FehlerprotokollTyp } from 'src/app/fehlerprotokoll/models/fehlerprotokoll-typ';
import { FehlerprotokollView } from 'src/app/fehlerprotokoll/models/fehlerprotokoll-view';
import {
  FehlerprotokollLoader,
  FehlerprotokollSelectionService,
} from 'src/app/fehlerprotokoll/services/fehlerprotokoll-selection.service';
import { FehlerprotokollService } from 'src/app/fehlerprotokoll/services/fehlerprotokoll.service';
import { KonsistenzregelService } from 'src/app/fehlerprotokoll/services/konsistenzregel.service';
import { RadvisValidators } from 'src/app/form-elements/models/radvis-validators';
import { Konsistenzregel } from 'src/app/shared/models/konsistenzregel';
import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';
import { BenutzerDetailsService } from 'src/app/shared/services/benutzer-details.service';
import { FeatureTogglzService } from 'src/app/shared/services/feature-togglz.service';
import { MenuEventService } from 'src/app/shared/services/menu-event.service';
import { OrganisationenService } from 'src/app/shared/services/organisationen.service';
import { MatCheckbox } from '@angular/material/checkbox';
import { AccessabilityTabCircleGroupDirective } from 'src/app/shared/components/accessability-tab-circle-group.directive';

@Component({
  selector: 'rad-fehlerprotokoll-auswahl',
  templateUrl: './fehlerprotokoll-auswahl.component.html',
  styleUrls: ['./fehlerprotokoll-auswahl.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FehlerprotokollAuswahlComponent implements OnDestroy, OnInit {
  @Input()
  public zoom: number = Number.MAX_VALUE;

  @ViewChild(MatMenuTrigger)
  private menuTrigger: MatMenuTrigger | null = null;

  @ViewChildren('konsistenzregelMenuItems')
  private konsistenzregelMenuCheckboxes: QueryList<MatCheckbox> | undefined;

  @ViewChild('konsistenzregelMenuInnerContainer', { read: AccessabilityTabCircleGroupDirective })
  private konsistenzregelMenuInnerContainer: AccessabilityTabCircleGroupDirective | null = null;

  public fehlerprotokolleOptions: FehlerprotokollTyp[] = FehlerprotokollTyp.getAll();
  public alleOrganisationenOptions: Promise<Verwaltungseinheit[]>;
  organisationControl: FormControl;
  netzklassenImportControl: FormControl;
  attributeImportControl: FormControl;

  alleRegelGruppen = new Map<string, Konsistenzregel[]>();

  private subscriptions: Subscription[] = [];

  constructor(
    organisationenService: OrganisationenService,
    benutzerDetailsService: BenutzerDetailsService,
    private fehlerprotokollSelectionService: FehlerprotokollSelectionService,
    private fehlerprotokollService: FehlerprotokollService,
    private konsistenzregelService: KonsistenzregelService,
    private changeDetector: ChangeDetectorRef,
    private menuEventService: MenuEventService,
    private featureTogglzService: FeatureTogglzService
  ) {
    this.alleOrganisationenOptions = organisationenService.getOrganisationen();
    if (this.fehlerprotokollSelectionService.selectedOrganisation == null) {
      this.fehlerprotokollSelectionService.selectedOrganisation =
        benutzerDetailsService.aktuellerBenutzerOrganisation() || null;
    }
    this.organisationControl = new FormControl(
      this.fehlerprotokollSelectionService.selectedOrganisation,
      RadvisValidators.isNotNullOrEmpty
    );

    this.organisationControl.statusChanges
      .pipe(
        map(() => this.organisationControl.valid),
        distinctUntilChanged()
      )
      .subscribe(isValid => {
        if (!isValid) {
          this.disableManuellerImportCheckboxes();
        } else {
          this.enableManuellerImportCheckboxes();
        }
      });

    this.organisationControl.valueChanges.subscribe(organisation => {
      this.fehlerprotokollSelectionService.selectedOrganisation = organisation;
      this.updateFehlerprotokollLoader();
    });

    this.netzklassenImportControl = new FormControl(this.fehlerprotokollSelectionService.netzklassenImportSelected);
    this.netzklassenImportControl.valueChanges.subscribe(checked => {
      this.fehlerprotokollSelectionService.netzklassenImportSelected = checked;
      this.updateFehlerprotokollLoader();
    });

    this.attributeImportControl = new FormControl(this.fehlerprotokollSelectionService.attributeImportSelected);
    this.attributeImportControl.valueChanges.subscribe(checked => {
      this.fehlerprotokollSelectionService.attributeImportSelected = checked;
      this.updateFehlerprotokollLoader();
    });

    this.subscriptions.push(
      this.menuEventService.menuClosed$.subscribe(() => {
        this.closeMenu();
      })
    );
  }

  ngOnInit(): void {
    this.konsistenzregelService.getAllKonsistenzRegel().subscribe(regeln => {
      regeln.forEach(regel =>
        this.alleRegelGruppen.has(regel.regelGruppe)
          ? this.alleRegelGruppen.get(regel.regelGruppe)?.push(regel)
          : this.alleRegelGruppen.set(regel.regelGruppe, [regel])
      );
      this.changeDetector.detectChanges();
      // Da die Optionen erst deutlich nach dem AfterViewInit gerendert werden
      // müssen die Listener für die Keyboardnavigation erneut angemeldet werden
      this.konsistenzregelMenuInnerContainer?.refresh();
    });
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  get isAnythingSelected(): boolean {
    return (
      this.selectedFehlerprotokolle.length > 0 ||
      this.selectedKonsistenzregelVerletzungsTypen.length > 0 ||
      ((this.fehlerprotokollSelectionService.attributeImportSelected ||
        this.fehlerprotokollSelectionService.netzklassenImportSelected) &&
        this.fehlerprotokollSelectionService.selectedOrganisation != null)
    );
  }

  get areFehlerprotokolleVisibleOnZoomlevel(): boolean {
    return this.zoom > FehlerprotokollLayerComponent.MIN_ZOOM;
  }

  get selectedFehlerprotokolle(): FehlerprotokollTyp[] {
    return this.fehlerprotokollSelectionService.selectedFehlerprotokollTypen;
  }

  get selectedKonsistenzregelVerletzungsTypen(): string[] {
    return this.fehlerprotokollSelectionService.selectedKonsistenzregelVerletzungsTypen;
  }

  get isKonsistenzregelnToggleOn(): boolean {
    return this.featureTogglzService.isToggledOn(FeatureTogglzService.TOGGLZ_KONSISTENZREGELN);
  }

  onFehlerprotokollClicked(clickedFehlerprotokoll: FehlerprotokollTyp): void {
    if (this.selectedFehlerprotokolle.includes(clickedFehlerprotokoll)) {
      this.fehlerprotokollSelectionService.selectedFehlerprotokollTypen = this.selectedFehlerprotokolle.filter(
        fehlerprotokoll => fehlerprotokoll !== clickedFehlerprotokoll
      );
    } else {
      this.fehlerprotokollSelectionService.selectedFehlerprotokollTypen.push(clickedFehlerprotokoll);
    }

    this.updateFehlerprotokollLoader();
  }

  onRegelClicked(clickedRegel: Konsistenzregel): void {
    const selectedTypen = this.fehlerprotokollSelectionService.selectedKonsistenzregelVerletzungsTypen;
    if (selectedTypen.includes(clickedRegel.verletzungsTyp)) {
      this.fehlerprotokollSelectionService.selectedKonsistenzregelVerletzungsTypen = selectedTypen.filter(
        verletzungsTyp => verletzungsTyp !== clickedRegel.verletzungsTyp
      );
    } else {
      selectedTypen.push(clickedRegel.verletzungsTyp);
    }

    this.updateFehlerprotokollLoader();
  }

  closeMenu(): void {
    this.menuTrigger?.closeMenu();
  }

  onKonsistenzregelMenuOpened(): void {
    // eslint-disable-next-line no-underscore-dangle
    this.konsistenzregelMenuCheckboxes?.get(0)?._elementRef.nativeElement.querySelector('input').focus();
  }

  private updateFehlerprotokollLoader(): void {
    const fehlerprotokollLoader: FehlerprotokollLoader = extent => {
      const alleFehlerprotokolleObservables = [
        this.fehlerprotokollService.getFehlerprotokolle(
          this.fehlerprotokollSelectionService.selectedFehlerprotokollTypen,
          extent
        ),
      ];
      if (this.fehlerprotokollSelectionService.selectedOrganisation) {
        alleFehlerprotokolleObservables.push(
          this.fehlerprotokollService.getFehlerFromManuellerImport(
            this.fehlerprotokollSelectionService.selectedOrganisation,
            this.fehlerprotokollSelectionService.netzklassenImportSelected,
            this.fehlerprotokollSelectionService.attributeImportSelected,
            extent
          )
        );
      }

      if (this.fehlerprotokollSelectionService.selectedKonsistenzregelVerletzungsTypen.length > 0) {
        alleFehlerprotokolleObservables.push(
          this.konsistenzregelService.getAlleVerletzungenForTypen(
            this.fehlerprotokollSelectionService.selectedKonsistenzregelVerletzungsTypen,
            extent
          )
        );
      }

      return zip(...alleFehlerprotokolleObservables).pipe(
        concatMap(fehlerprotokollViews => of(([] as FehlerprotokollView[]).concat(...fehlerprotokollViews)))
      );
    };

    this.fehlerprotokollSelectionService.fehlerprotokollLoader$.next(fehlerprotokollLoader);
  }

  private disableManuellerImportCheckboxes(): void {
    this.attributeImportControl.disable({ emitEvent: false });
    this.netzklassenImportControl.disable({ emitEvent: false });
  }

  private enableManuellerImportCheckboxes(): void {
    this.attributeImportControl.enable({ emitEvent: false });
    this.netzklassenImportControl.enable({ emitEvent: false });
  }
}
