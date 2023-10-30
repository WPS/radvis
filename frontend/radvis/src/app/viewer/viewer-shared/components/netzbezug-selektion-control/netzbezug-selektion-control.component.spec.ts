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

/* eslint-disable @typescript-eslint/dot-notation */

import { ChangeDetectorRef } from '@angular/core';
import { DefaultRenderComponent, MockBuilder, MockedComponentFixture, MockRender } from 'ng-mocks';
import { OlMapComponent } from 'src/app/karte/components/ol-map/ol-map.component';
import { BedienhinweisService } from 'src/app/karte/services/bedienhinweis.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { MASSNAHMEN } from 'src/app/viewer/massnahme/models/massnahme.infrastruktur';
import { NetzbezugSelektionControlComponent } from 'src/app/viewer/viewer-shared/components/netzbezug-selektion-control/netzbezug-selektion-control.component';
import { Netzbezug } from 'src/app/viewer/viewer-shared/models/netzbezug';
import { defaultNetzbezug } from 'src/app/viewer/viewer-shared/models/netzbezug-test-data-provider.spec';
import { ViewerModule } from 'src/app/viewer/viewer.module';
import { anything, capture, instance, mock, reset, verify } from 'ts-mockito';

describe(NetzbezugSelektionControlComponent.name, () => {
  let fixture: MockedComponentFixture<NetzbezugSelektionControlComponent>;
  let component: NetzbezugSelektionControlComponent;

  let olMapService: OlMapService;
  let bedienhinweisService: BedienhinweisService;

  beforeEach(() => {
    olMapService = mock(OlMapComponent);
    bedienhinweisService = mock(BedienhinweisService);

    return MockBuilder(NetzbezugSelektionControlComponent, ViewerModule)
      .provide({
        provide: OlMapService,
        useValue: instance(olMapService),
      })
      .provide({
        provide: BedienhinweisService,
        useValue: instance(bedienhinweisService),
      });
  });

  beforeEach(() => {
    const params = ({
      layerId: MASSNAHMEN.name,
    } as unknown) as DefaultRenderComponent<NetzbezugSelektionControlComponent>;
    fixture = MockRender(NetzbezugSelektionControlComponent, params);
    component = fixture.point.componentInstance;
    fixture.detectChanges();
  });

  it('netzbezug should not be selected', () => {
    expect(component.netzbezugSelected).toBeFalse();
  });

  it('should return validation error', () => {
    expect(component.validate()).toEqual({ noSelection: 'Kein Netzbezug ausgewählt' });
  });

  it('should be active by default', () => {
    expect(component.disabled).toBeFalse();
  });

  it('should not be in pointOnKanteSelectionMode by default', () => {
    expect(component.pointSelectionMode).toBeFalse();
  });

  it('should not be in selectionMode by default', () => {
    expect(component.selectionMode).toBeFalse();
  });

  it('should not have zweiseitigeNetzanzeige toggled by default', () => {
    expect(component.zweiseitigeNetzanzeige).toBeFalse();
  });

  describe(NetzbezugSelektionControlComponent.prototype.setDisabledState.name, () => {
    beforeEach(() => {
      component.setDisabledState(true);
    });

    it('should set active to false', () => {
      expect(component.disabled).toBeTrue();
    });
  });

  describe(NetzbezugSelektionControlComponent.prototype.ngOnDestroy.name, () => {
    it('should reset Cursor and Bedienhinweis', () => {
      component.ngOnDestroy();
      verify(olMapService.resetCursor()).once();
      verify(bedienhinweisService.hideBedienhinweis()).once();
      expect().nothing();
    });
  });

  it('toggle punkt auswahl', () => {
    component.selectionMode = true;
    component.onTogglePunktModus();

    expect(component.pointSelectionMode).toBeTrue();
    verify(olMapService.setCursor(anything())).once();
    expect(capture(olMapService.setCursor).last()[0]).toEqual('point-selection-cursor');

    component.onTogglePunktModus();

    expect(component.pointSelectionMode).toBeFalse();
    verify(olMapService.resetCursor()).once();
  });

  it('toggle zweiseitigeNetzanzeige auswahl', () => {
    component.selectionMode = true;
    component.onToggleZweiseitigeNetzanzeige();

    expect(component.zweiseitigeNetzanzeige).toBeTrue();

    component.onToggleZweiseitigeNetzanzeige();

    expect(component.zweiseitigeNetzanzeige).toBeFalse();
  });

  describe('selektions modus', () => {
    it('should reset cursor and bedienhinweis on beenden', () => {
      component.selectionMode = true;
      component.onSelektionBeenden();
      expect(component.selectionMode).toBeFalse();
      expect(component.zweiseitigeNetzanzeige).toBeFalse();
      verify(olMapService.resetCursor()).once();
      verify(bedienhinweisService.hideBedienhinweis()).once();
    });

    it('should start in Standard Modus', () => {
      component.onSelektionStarten();
      component.onToggleSchereModus();
      component.onSelektionBeenden();

      reset(bedienhinweisService);
      reset(olMapService);
      component.onSelektionStarten();

      expect(component.selectionMode).toBeTrue();
      expect(component.schereSelectionMode).toBeFalse();
      verify(bedienhinweisService.hideBedienhinweis()).never();
      verify(bedienhinweisService.showBedienhinweis(component['STANDARD_BEDIEN_HINWEIS'])).once();
      verify(olMapService.resetCursor()).once();
    });
  });

  describe(NetzbezugSelektionControlComponent.prototype.writeValue.name, () => {
    let netzbezug: Netzbezug;
    let detectChangesSpy: any;

    beforeEach(() => {
      // Arrange
      netzbezug = defaultNetzbezug;
      const changeDetectorRef = fixture.debugElement.injector.get(ChangeDetectorRef);
      detectChangesSpy = spyOn(changeDetectorRef.constructor.prototype, 'markForCheck');
      component.selectionMode = true;

      // Act
      component.writeValue(netzbezug);
    });

    it('should be in Selektion if no netzbezug', () => {
      component.writeValue(null);

      expect(component.selectionMode).toBeTrue();
    });

    it('should update internal netzbezug', () => {
      expect(component.netzbezug).toEqual(defaultNetzbezug);
    });

    it('should reset selected modes', () => {
      expect(component.selectionMode).toBeFalse();
      verify(bedienhinweisService.hideBedienhinweis()).once();
      verify(olMapService.resetCursor()).once();
    });

    it('should mark for changes', () => {
      expect(detectChangesSpy).toHaveBeenCalled();
    });
  });

  describe('bearbeitungs modus', () => {
    it('should be mutual exclusive', () => {
      component.onToggleSchereModus();
      expect(component.schereSelectionMode).toBeTrue();
      verify(olMapService.setCursor('schere-cursor')).once();
      verify(bedienhinweisService.showBedienhinweis(component['SCHERE_BEDIEN_HINWEIS'])).once();

      component.onTogglePunktModus();
      expect(component.schereSelectionMode).toBeFalse();
      verify(olMapService.setCursor('point-selection-cursor')).once();
      verify(bedienhinweisService.showBedienhinweis(component['POINT_BEDIEN_HINWEIS'])).once();
      expect(component.pointSelectionMode).toBeTrue();
    });

    it('should be reset onEscape', () => {
      component.onToggleSchereModus();
      expect(component.schereSelectionMode).toBeTrue();
      verify(olMapService.setCursor('schere-cursor')).once();
      verify(bedienhinweisService.showBedienhinweis(component['SCHERE_BEDIEN_HINWEIS'])).once();

      component.onEscape();
      expect(component.schereSelectionMode).toBeFalse();
      verify(olMapService.resetCursor()).once();
      verify(bedienhinweisService.showBedienhinweis(component['STANDARD_BEDIEN_HINWEIS'])).once();
    });

    it('should point untoggle', () => {
      component.onTogglePunktModus();
      expect(component.pointSelectionMode).toBeTrue();
      verify(olMapService.setCursor('point-selection-cursor')).once();
      verify(bedienhinweisService.showBedienhinweis(component['POINT_BEDIEN_HINWEIS'])).once();

      component.onTogglePunktModus();
      expect(component.pointSelectionMode).toBeFalse();
      verify(olMapService.resetCursor()).once();
      verify(bedienhinweisService.showBedienhinweis(component['STANDARD_BEDIEN_HINWEIS'])).once();
    });

    it('should schere untoggle', () => {
      component.onToggleSchereModus();
      expect(component.schereSelectionMode).toBeTrue();
      verify(olMapService.setCursor('schere-cursor')).once();
      verify(bedienhinweisService.showBedienhinweis(component['SCHERE_BEDIEN_HINWEIS'])).once();

      component.onToggleSchereModus();
      expect(component.schereSelectionMode).toBeFalse();
      verify(olMapService.resetCursor()).once();
      verify(bedienhinweisService.showBedienhinweis(component['STANDARD_BEDIEN_HINWEIS'])).once();
    });
  });
});
