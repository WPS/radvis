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

import { CdkDragEnd } from '@angular/cdk/drag-drop';
import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnChanges,
  OnDestroy,
  Output,
} from '@angular/core';
import { Overlay } from 'ol';
import { Coordinate } from 'ol/coordinate';
import { OlMapService } from 'src/app/shared/services/ol-map.service';

@Component({
  selector: 'rad-ol-popup',
  templateUrl: './ol-popup.component.html',
  styleUrls: ['./ol-popup.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class OlPopupComponent implements OnDestroy, OnChanges {
  @Input()
  coordinate: Coordinate | null = null;
  @Input()
  titel = '';
  @Output()
  closePopup = new EventEmitter<void>();

  private readonly popup: Overlay;

  constructor(
    private olMapService: OlMapService,
    host: ElementRef
  ) {
    this.popup = new Overlay({
      element: host.nativeElement,
      positioning: 'bottom-center',
    });
    olMapService.addOverlay(this.popup);
  }

  ngOnChanges(): void {
    if (!this.coordinate) {
      this.popup.setPosition(undefined);
      return;
    }
    this.popup.setOffset([0, 0]);
    this.popup.setPosition(this.coordinate);
  }

  ngOnDestroy(): void {
    this.olMapService.removeOverlay(this.popup);
  }

  onDragEnded(event: CdkDragEnd): void {
    event.source.reset();
    this.popup.setOffset([this.popup.getOffset()[0] + event.distance.x, this.popup.getOffset()[1] + event.distance.y]);
  }

  onClose(): void {
    this.close();
  }

  private close(): void {
    this.popup.setPosition(undefined);
    this.closePopup.next();
  }
}
