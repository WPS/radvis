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
  AfterViewInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  Output,
  ViewChild,
} from '@angular/core';
import { MatSort } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';
import { Subscription } from 'rxjs';
import { RadNetzMatchingState } from 'src/app/radnetzmatching/components/radnetz-matching/radnetz-matching.component';
import { MatchingRelatedFeatureDetails } from 'src/app/radnetzmatching/models/matching-related-feature-details';
import { PrimarySelectionService } from 'src/app/radnetzmatching/services/primary-selection.service';
import { FeatureProperties } from 'src/app/shared/models/feature-properties';
import { QuellSystem } from 'src/app/shared/models/quell-system';
import { RadVisFeatureAttribut } from 'src/app/shared/models/rad-vis-feature-attribut';

@Component({
  selector: 'rad-matching-feature-table',
  templateUrl: './matching-feature-table.component.html',
  styleUrls: ['./matching-feature-table.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MatchingFeatureTableComponent implements OnChanges, AfterViewInit, OnDestroy, OnInit {
  @ViewChild(MatSort) sort: MatSort | null = null;

  @Input()
  public feature!: MatchingRelatedFeatureDetails;

  @Input()
  public radNetzMatchingState!: RadNetzMatchingState;

  @Input()
  public highlighted = false;

  @Input()
  public zugeordneteRadnetzKantenVorhanden = false;

  @Output()
  public zuordnen = new EventEmitter<void>();

  @Output()
  public save = new EventEmitter<void>();

  @Output()
  public delete = new EventEmitter<void>();

  @Output()
  public netzfehlerErledigt = new EventEmitter<void>();

  public RadNetzMatchingState = RadNetzMatchingState;
  public attributeForTable = new MatTableDataSource<RadVisFeatureAttribut>();
  public displayedColumns = ['key', 'value'];

  public nurLeereAttributeHinweisVisible = false;
  public leereAttributeVisible = false;

  private subscription!: Subscription;

  get featureLayerName(): string {
    return this.feature.layername;
  }

  get isNetzfehler(): boolean {
    return this.feature.isNetzfehler;
  }

  get isRadNETZ(): boolean {
    return this.featureLayerName === QuellSystem.RadNETZ;
  }

  get isDLM(): boolean {
    return this.featureLayerName === QuellSystem.DLM;
  }

  constructor(private primarySelectionService: PrimarySelectionService, private changeDetectorRef: ChangeDetectorRef) {}

  ngOnChanges(): void {
    this.attributeForTable.data = this.feature.attribute.filter(this.getFilterAttribut(this.leereAttributeVisible));
    this.nurLeereAttributeHinweisVisible = this.feature.attribute.filter(this.getFilterAttribut(false)).length === 0;
  }

  ngAfterViewInit(): void {
    this.attributeForTable.sort = this.sort;
  }

  ngOnInit(): void {
    this.subscription = this.primarySelectionService.primarySelection$.subscribe(selectedFeatureReference => {
      this.highlighted = selectedFeatureReference != null && selectedFeatureReference.featureId === this.feature.id;
      this.changeDetectorRef.markForCheck();
    });
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  public onToggleLeereAttribute(): void {
    this.leereAttributeVisible = !this.leereAttributeVisible;
    this.attributeForTable.data = this.feature.attribute.filter(this.getFilterAttribut(this.leereAttributeVisible));
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

  public onPrimarySelect(): void {
    this.primarySelectionService.primarySelectFeature({ layerId: this.feature.layerId, featureId: this.feature.id });
  }

  public onNetzfehlerErledigt(): void {
    this.netzfehlerErledigt.emit();
  }

  public getVisibilityTooltip(): string {
    return 'Leere Attribute ' + (this.leereAttributeVisible ? 'ausblenden' : 'einblenden');
  }

  private getFilterAttribut(considerLeereAttribute: boolean): (attribute: RadVisFeatureAttribut) => boolean {
    return (attribute: RadVisFeatureAttribut): boolean => {
      if (
        attribute.key === 'geometry' ||
        attribute.key === 'the_geom' ||
        attribute.key === FeatureProperties.SEITE_PROPERTY_NAME
      ) {
        return false;
      }
      if (!considerLeereAttribute) {
        return attribute.value !== '' && attribute.value !== null && attribute.value !== 'Unbekannt';
      }
      return true;
    };
  }
}
