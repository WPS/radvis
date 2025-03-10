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

import { animate, state, style, transition, trigger } from '@angular/animations';
import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  HostListener,
  Inject,
  OnDestroy,
  ViewChild,
} from '@angular/core';
import { Observable, Subscription } from 'rxjs';
import { FeatureTogglzService } from 'src/app/shared/services/feature-togglz.service';
import { ABSTELLANLAGEN } from 'src/app/viewer/abstellanlage/models/abstellanlage.infrastruktur';
import { ANPASSUNGSWUNSCH } from 'src/app/viewer/anpassungswunsch/models/anpassungswunsch.infrastruktur';
import { BARRIEREN } from 'src/app/viewer/barriere/models/barriere.infrastruktur';
import { FAHRRADROUTE } from 'src/app/viewer/fahrradroute/models/fahrradroute.infrastruktur';
import { FURTEN_KREUZUNGEN } from 'src/app/viewer/furten-kreuzungen/models/furten-kreuzungen.infrastruktur';
import { IMPORTPROTOKOLLE } from 'src/app/viewer/importprotokolle/models/importprotokoll.infrastruktur';
import { LEIHSTATIONEN } from 'src/app/viewer/leihstation/models/leihstation.infrastruktur';
import { SERVICESTATIONEN } from 'src/app/viewer/servicestation/models/servicestation.infrastruktur';
import { Infrastruktur, InfrastrukturToken } from 'src/app/viewer/viewer-shared/models/infrastruktur';
import { InfrastrukturenSelektionService } from 'src/app/viewer/viewer-shared/services/infrastrukturen-selektion.service';
import { WEGWEISENDE_BESCHILDERUNG } from 'src/app/viewer/wegweisende-beschilderung/models/wegweisende-beschilderung.infrastruktur';
import { WeitereKartenebene } from 'src/app/viewer/weitere-kartenebenen/models/weitere-kartenebene';
import { WeitereKartenebenenService } from 'src/app/viewer/weitere-kartenebenen/services/weitere-kartenebenen.service';

@Component({
  selector: 'rad-infrastrukturen-menu',
  templateUrl: './infrastrukturen-menu.component.html',
  styleUrls: ['./infrastrukturen-menu.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  animations: [
    trigger('collapseExpand', [
      state(
        'collapsed',
        style({
          transform: 'rotate(-180deg)',
        })
      ),
      state(
        'expanded',
        style({
          transform: 'rotate(0deg)',
        })
      ),
      transition('collapsed <=> expanded', [animate('0.2s 0.1s')]),
    ]),
    trigger('easeInOut', [
      state(
        'in',
        style({
          width: '2.8rem',
        })
      ),
      state(
        'out',
        style({
          width: '17rem',
        })
      ),
      transition('in <=> out', [animate('0.25s')]),
    ]),
  ],
  standalone: false,
})
export class InfrastrukturenMenuComponent implements OnDestroy {
  @ViewChild('infrastrukturenCollapseButton', { read: ElementRef })
  infrastrukturenCollapseButton: ElementRef | undefined;

  infrastrukturenCollapsed = false;
  infrastrukturenAllgemein: Infrastruktur[] = [];
  infrastrukturenAnalyse: Infrastruktur[] = [];
  weitereKartenebenen$: Observable<WeitereKartenebene[]>;
  weitereKartenebenenFeatureAktiv: boolean;
  selectedWeitereKartenebenen: WeitereKartenebene[] = [];
  selektierteInfrastrukturen$: Observable<Infrastruktur[]>;

  public subscriptions: Subscription[] = [];

  private readonly infrastrukturFeatureTogglz: Map<Infrastruktur, string> = new Map([
    [FAHRRADROUTE, FeatureTogglzService.TOGGLZ_FAHRRADROUTE],
    [ANPASSUNGSWUNSCH, FeatureTogglzService.TOGGLZ_ANPASSUNGEN],
    [IMPORTPROTOKOLLE, FeatureTogglzService.TOGGLZ_FAHRRADROUTE_IMPORTPROTOKOLLE],
    [BARRIEREN, FeatureTogglzService.TOGGLZ_BARRIEREN],
    [FURTEN_KREUZUNGEN, FeatureTogglzService.TOGGLZ_FURTEN_KREUZUNGEN],
    [SERVICESTATIONEN, FeatureTogglzService.TOGGLZ_SERVICESTATIONEN],
    [LEIHSTATIONEN, FeatureTogglzService.TOGGLZ_LEIHSTATIONEN],
    [ABSTELLANLAGEN, FeatureTogglzService.TOGGLZ_ABSTELLANLAGEN],
    [WEGWEISENDE_BESCHILDERUNG, FeatureTogglzService.TOGGLZ_WEGWEISENDE_BESCHILDERUNG],
  ]);
  infrastrukturenAnalyseDefaultVisible: boolean;

  constructor(
    private infrastrukturenSelektionService: InfrastrukturenSelektionService,
    @Inject(InfrastrukturToken) infrastrukturen: Infrastruktur[],
    public featureTogglzService: FeatureTogglzService,
    public weitereKartenebenenService: WeitereKartenebenenService
  ) {
    this.selektierteInfrastrukturen$ = this.infrastrukturenSelektionService.selektierteInfrastrukturen$;

    this.infrastrukturenAnalyseDefaultVisible = Boolean(
      this.infrastrukturenSelektionService.selektierteInfrastrukturen.find(i => i.gruppe === 'Analyse')
    );

    this.weitereKartenebenenFeatureAktiv = featureTogglzService.isToggledOn(
      FeatureTogglzService.TOGGLZ_WEITERE_KARTENEBENEN
    );
    this.weitereKartenebenen$ = this.weitereKartenebenenService.weitereKartenebenen$;
    this.subscriptions.push(
      this.weitereKartenebenenService.selectedWeitereKartenebenen$.subscribe(
        selected => (this.selectedWeitereKartenebenen = selected)
      )
    );

    infrastrukturen.forEach(infrastruktur => {
      const featureTogglzName = this.infrastrukturFeatureTogglz.get(infrastruktur);
      let toggledOn;
      if (featureTogglzName) {
        toggledOn = this.featureTogglzService.isToggledOn(featureTogglzName) ?? true;
      } else {
        toggledOn = true;
      }
      if (toggledOn) {
        this.addInfraktuktur(infrastruktur);
      }
    });
  }

  @HostListener('document:keydown.control.alt.shift.e')
  onShortcut(): void {
    this.infrastrukturenCollapseButton?.nativeElement.focus();
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  addInfraktuktur(infrastruktur: Infrastruktur): void {
    if (infrastruktur.gruppe === 'Allgemein') {
      this.infrastrukturenAllgemein.push(infrastruktur);
    } else if (infrastruktur.gruppe === 'Analyse') {
      this.infrastrukturenAnalyse.push(infrastruktur);
    }
  }

  onClickInfrastrukturen(infrastrukturen: Infrastruktur): void {
    if (this.infrastrukturenSelektionService.isSelected(infrastrukturen)) {
      this.infrastrukturenSelektionService.deselectInfrastrukturen(infrastrukturen);
    } else {
      this.infrastrukturenSelektionService.selectInfrastrukturen(infrastrukturen);
    }
  }

  onClickWeiterKartenebenen(weitereKartenebenen: WeitereKartenebene): void {
    this.weitereKartenebenenService.toggleLayerSelection(weitereKartenebenen);
  }
}
