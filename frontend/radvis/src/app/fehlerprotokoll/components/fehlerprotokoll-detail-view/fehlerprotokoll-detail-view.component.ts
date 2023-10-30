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

/* eslint-disable @typescript-eslint/no-non-null-assertion */
import { ChangeDetectionStrategy, Component, EventEmitter, Input, OnChanges, Optional, Output } from '@angular/core';
import { Coordinate } from 'ol/coordinate';
import { Point } from 'ol/geom';
import { RadVisFeature } from 'src/app/shared/models/rad-vis-feature';
import { AnpassungswunschAnlegenService } from 'src/app/shared/services/anpassungswunsch-anlegen.service';
import invariant from 'tiny-invariant';
import { FehlerprotokollLayerComponent } from 'src/app/fehlerprotokoll/components/fehlerprotokoll-layer/fehlerprotokoll-layer.component';

@Component({
  selector: 'rad-fehlerprotokoll-detail-view',
  templateUrl: './fehlerprotokoll-detail-view.component.html',
  styleUrls: ['./fehlerprotokoll-detail-view.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FehlerprotokollDetailViewComponent implements OnChanges {
  @Input()
  selectedFeature!: RadVisFeature;
  @Input()
  canCreateAnpassungswunsch = false;

  @Output()
  public closePopup = new EventEmitter<void>();

  public selectedLocation!: Coordinate;
  public titel!: string;
  public beschreibung!: string;
  public entityLink!: string;
  public datum!: string;

  constructor(@Optional() public anpassungswunschAnlegenService?: AnpassungswunschAnlegenService) {}

  ngOnChanges(): void {
    invariant(this.selectedFeature);

    this.selectedLocation = (this.selectedFeature.geometry as Point).getCoordinates();

    this.titel = this.selectedFeature.attribute.find(a => a.key === 'titel')!.value;
    this.beschreibung = this.selectedFeature.attribute.find(a => a.key === 'beschreibung')!.value;
    this.entityLink = this.selectedFeature.attribute.find(a => a.key === 'entityLink')!.value;
    this.datum = this.selectedFeature.attribute.find(a => a.key === 'datum')!.value;
  }

  public onClosePopup(): void {
    this.closePopup.next();
  }

  public onAddAnpassungswunsch(): void {
    invariant(this.anpassungswunschAnlegenService);

    this.anpassungswunschAnlegenService.addAnpassungswunschFuerFehlerprotokoll(
      this.selectedLocation,
      this.beschreibung,
      FehlerprotokollLayerComponent.extractProtokollId(this.selectedFeature.attribute)
    );
  }
}
