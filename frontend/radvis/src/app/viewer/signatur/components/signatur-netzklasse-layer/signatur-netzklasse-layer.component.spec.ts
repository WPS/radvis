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

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Style } from 'ol/style';
import { of } from 'rxjs';
import { OlMapComponent } from 'src/app/karte/components/ol-map/ol-map.component';
import { Netzklassefilter } from 'src/app/shared/models/netzklassefilter';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { SignaturService } from 'src/app/viewer/signatur/services/signatur.service';
import { NetzAusblendenService } from 'src/app/shared/services/netz-ausblenden.service';
import { instance, mock, when } from 'ts-mockito';
import { SignaturNetzklasseLayerComponent } from './signatur-netzklasse-layer.component';

describe(SignaturNetzklasseLayerComponent.name, () => {
  let component: SignaturNetzklasseLayerComponent;
  let fixture: ComponentFixture<SignaturNetzklasseLayerComponent>;
  let netzAusblendenService: NetzAusblendenService;

  beforeEach(async () => {
    netzAusblendenService = mock(NetzAusblendenService);
    when(netzAusblendenService.kanteAusblenden$).thenReturn(of());
    when(netzAusblendenService.kanteEinblenden$).thenReturn(of());
    await TestBed.configureTestingModule({
      declarations: [SignaturNetzklasseLayerComponent],
      providers: [
        { provide: ErrorHandlingService, useValue: instance(mock(ErrorHandlingService)) },
        { provide: OlMapService, useValue: instance(mock(OlMapComponent)) },
        { provide: SignaturService, useValue: instance(mock(SignaturService)) },
        { provide: NetzAusblendenService, useValue: instance(netzAusblendenService) },
      ],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(SignaturNetzklasseLayerComponent);
    component = fixture.componentInstance;
    component.layerPrefix = 'TEST_';
    component.netzklasse = Netzklassefilter.RADNETZ;
    component.attributnamen = ['testattribut'];
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('ngOnChanges', () => {
    it('should refresh source when signature changed', () => {
      // eslint-disable-next-line @typescript-eslint/dot-notation
      const refreshSpy = spyOn(component['olLayer'].getSource(), 'refresh');
      // eslint-disable-next-line @typescript-eslint/dot-notation

      component.generatedStyleFunction = (): Style => {
        return new Style({});
      };
      component.ngOnChanges({});

      expect(refreshSpy).toHaveBeenCalled();
    });
  });
});
