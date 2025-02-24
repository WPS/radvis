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
  Component,
  EventEmitter,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
} from '@angular/core';
import { RadNetzMatchingState } from 'src/app/radnetzmatching/components/radnetz-matching/radnetz-matching.component';
import { MatchingRelatedFeatureDetails } from 'src/app/radnetzmatching/models/matching-related-feature-details';
import { PrimarySelectionService } from 'src/app/radnetzmatching/services/primary-selection.service';
import { LayerId, RadVisLayer } from 'src/app/shared/models/layers/rad-vis-layer';

@Component({
  selector: 'rad-matching-feature-details',
  templateUrl: './matching-feature-details.component.html',
  styleUrls: ['./matching-feature-details.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class MatchingFeatureDetailsComponent implements OnChanges {
  @Input()
  featuresAtDisplay: MatchingRelatedFeatureDetails[] = [];
  @Input()
  allLayers: RadVisLayer[] = [];
  @Input()
  radNetzMatchingState!: RadNetzMatchingState;
  @Input()
  zugeordneteRadnetzKantenVorhanden = false;

  @Output()
  detailsClose = new EventEmitter<void>();
  @Output()
  public zuordnen = new EventEmitter<void>();
  @Output()
  public save = new EventEmitter<void>();
  @Output()
  public delete = new EventEmitter<void>();
  @Output()
  public netzfehlerErledigt = new EventEmitter<number>();

  constructor(private primarySelectionService: PrimarySelectionService) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.featuresAtDisplay && this.featuresAtDisplay != null) {
      this.primarySelectionService.primarySelectFeature({
        featureId: this.featuresAtDisplay[0].id,
        layerId: this.featuresAtDisplay[0].layerId,
      });
    }
  }

  public getLayerName(layerId: LayerId): string {
    return this.allLayers.find(l => l.id === layerId)?.bezeichnung || '';
  }

  public onClose(): void {
    this.detailsClose.emit();
  }

  public onZuordnen(): void {
    this.zuordnen.emit();
  }

  public onSave(): void {
    this.save.emit();
  }

  public onDelete(): void {
    this.delete.emit();
  }

  public onNetzfehlerErledigt(featureId: number): void {
    this.featuresAtDisplay = this.featuresAtDisplay.filter(
      matchingRelatedFeatureDetails => matchingRelatedFeatureDetails.id !== featureId
    );
    const newSelectedFeature = this.featuresAtDisplay[0];
    if (newSelectedFeature) {
      this.primarySelectionService.primarySelectFeature({
        featureId: newSelectedFeature.id,
        layerId: newSelectedFeature.layerId,
      });
    } else {
      this.detailsClose.emit();
    }
    this.netzfehlerErledigt.emit(featureId);
  }
}
