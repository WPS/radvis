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

import { ChangeDetectorRef } from '@angular/core';
import { MockBuilder, MockedComponentFixture, MockRender } from 'ng-mocks';
import { MapBrowserEvent } from 'ol';
import { Coordinate } from 'ol/coordinate';
import { Subject } from 'rxjs';
import { OlMapComponent } from 'src/app/karte/components/ol-map/ol-map.component';
import { BedienhinweisService } from 'src/app/shared/services/bedienhinweis.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { ViewerComponent } from 'src/app/viewer/components/viewer/viewer.component';
import { PositionSelektionControlComponent } from 'src/app/viewer/viewer-shared/components/position-selektion-control/position-selektion-control.component';
import { NetzbezugAuswahlModusService } from 'src/app/shared/services/netzbezug-auswahl-modus.service';
import { ViewerModule } from 'src/app/viewer/viewer.module';
import { instance, mock, verify, when } from 'ts-mockito';

describe(PositionSelektionControlComponent.name, () => {
  let fixture: MockedComponentFixture<PositionSelektionControlComponent>;
  let component: PositionSelektionControlComponent;

  let olMapService: OlMapService;
  let olMapClick$: Subject<MapBrowserEvent<UIEvent>>;
  let bedienhinweisService: BedienhinweisService;
  let netzbezugAuswahlModusService: NetzbezugAuswahlModusService;

  beforeEach(() => {
    olMapService = mock(OlMapComponent);
    olMapClick$ = new Subject<MapBrowserEvent<UIEvent>>();
    when(olMapService.click$()).thenReturn(olMapClick$);
    bedienhinweisService = mock(BedienhinweisService);
    netzbezugAuswahlModusService = mock(ViewerComponent);

    return MockBuilder(PositionSelektionControlComponent, ViewerModule)
      .provide({
        provide: OlMapService,
        useValue: instance(olMapService),
      })
      .provide({
        provide: BedienhinweisService,
        useValue: instance(bedienhinweisService),
      })
      .provide({
        provide: NetzbezugAuswahlModusService,
        useValue: instance(netzbezugAuswahlModusService),
      });
  });

  beforeEach(() => {
    fixture = MockRender(PositionSelektionControlComponent);
    component = fixture.point.componentInstance;
    fixture.detectChanges();
  });

  it('position should not be selected', () => {
    expect(component.positionSelected).toBeFalse();
  });

  it('should return validation error', () => {
    expect(component.validate()).toEqual({ noSelection: 'Kein Ort ausgewählt' });
  });

  it('should be active by default', () => {
    expect(component.disabled).toBeFalse();
  });

  it('should not be in selectionMode by default', () => {
    expect(component.selectionMode).toBeFalse();
  });

  it('should not update position on MapClick if not in selektionMode', () => {
    expect(component.selectionMode).toBeFalse();

    olMapClick$.next({ coordinate: [42, 42] } as MapBrowserEvent);

    expect(component.position).toEqual(null);
  });

  describe(PositionSelektionControlComponent.prototype.setDisabledState.name, () => {
    beforeEach(() => {
      component.setDisabledState(true);
    });

    it('should set active to false', () => {
      expect(component.disabled).toBeTrue();
    });
  });

  describe(PositionSelektionControlComponent.prototype.ngOnDestroy.name, () => {
    it('should reset Cursor and Bedienhinweis', () => {
      component.ngOnDestroy();
      verify(olMapService.resetCursor()).once();
      verify(bedienhinweisService.hideBedienhinweis()).once();
      expect().nothing();
    });
  });

  describe('selektions modus', () => {
    beforeEach(() => {
      component.selectionMode = true;
    });

    it('should reset cursor and bedienhinweis on beenden', () => {
      component.onSelektionBeenden();
      expect(component.selectionMode).toBeFalse();
      verify(olMapService.resetCursor()).once();
      verify(bedienhinweisService.hideBedienhinweis()).once();
    });

    it('should update position on MapClick', () => {
      olMapClick$.next({ coordinate: [42, 42] } as MapBrowserEvent);

      expect(component.position).toEqual([42, 42]);
    });
  });

  describe(PositionSelektionControlComponent.prototype.writeValue.name, () => {
    let position: Coordinate;
    let detectChangesSpy: any;

    beforeEach(() => {
      position = [42, 42];
      const changeDetectorRef = fixture.debugElement.injector.get(ChangeDetectorRef);
      detectChangesSpy = spyOn(changeDetectorRef.constructor.prototype, 'markForCheck');
      component.selectionMode = true;

      component.writeValue(position);
    });

    it('should be in Selektion if no position', () => {
      component.writeValue(null);

      expect(component.selectionMode).toBeTrue();
    });

    it('should update internal netzbezug', () => {
      expect(component.position).toEqual([42, 42]);
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
});
