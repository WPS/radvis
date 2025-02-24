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
  EventEmitter,
  Input,
  Optional,
  Output,
} from '@angular/core';
import { Coordinate } from 'ol/coordinate';
import { AnpassungswunschAnlegenService } from 'src/app/shared/services/anpassungswunsch-anlegen.service';
import invariant from 'tiny-invariant';
import { MatomoTracker } from 'ngx-matomo-client';

@Component({
  selector: 'rad-fehlerprotokoll-detail-view',
  templateUrl: './fehlerprotokoll-detail-view.component.html',
  styleUrls: ['./fehlerprotokoll-detail-view.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class FehlerprotokollDetailViewComponent {
  @Input()
  canCreateAnpassungswunsch = false;

  @Output()
  public closePopup = new EventEmitter<void>();

  public selectedLocation: Coordinate | null = null;
  public titel = '';
  public beschreibung = '';
  public entityLink = '';
  public datum = '';
  public id: string | undefined;

  constructor(
    private changeDetector: ChangeDetectorRef,
    private matomoTracker: MatomoTracker,
    @Optional() public anpassungswunschAnlegenService?: AnpassungswunschAnlegenService
  ) {}

  public showFehlerprotokoll(
    location: Coordinate,
    id: string,
    titel: string,
    beschreibung: string,
    entityLink: string,
    datum: string
  ): void {
    this.selectedLocation = location;
    this.titel = titel;
    this.beschreibung = beschreibung;
    this.entityLink = entityLink;
    this.datum = datum;
    this.id = id;

    this.changeDetector.markForCheck();
  }

  public onClosePopup(): void {
    this.closePopup.next();
  }

  public onAddAnpassungswunsch(): void {
    invariant(this.anpassungswunschAnlegenService);
    invariant(this.selectedLocation);
    invariant(this.id);

    this.matomoTracker.trackEvent('Editor', 'Speichern', 'anpassungen/newFromFehlerprotokoll');

    this.anpassungswunschAnlegenService.addAnpassungswunschFuerFehlerprotokoll(
      this.selectedLocation,
      this.beschreibung,
      this.id
    );
  }
}
