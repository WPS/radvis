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

import { Chart } from 'chart.js';
import { MockBuilder, MockedComponentFixture, MockRender } from 'ng-mocks';
import { LineString } from 'ol/geom';
import { HoehenprofilComponent } from 'src/app/viewer/fahrradroute/components/hoehenprofil/hoehenprofil.component';
import { FahrradrouteModule } from 'src/app/viewer/fahrradroute/fahrradroute.module';

// Tests wurden unvollständig aus dem RRPBW migriert!
describe(HoehenprofilComponent.name, () => {
  let component: HoehenprofilComponent;
  let fixture: MockedComponentFixture<HoehenprofilComponent>;
  let inputs: any;

  const route = new LineString([
    [0, 0, 3],
    [1, 1, 4],
    [2, 2, 5],
  ]);

  beforeEach(() => {
    return MockBuilder(HoehenprofilComponent, FahrradrouteModule);
  });

  beforeEach(() => {
    inputs = { route };
    fixture = MockRender(HoehenprofilComponent, inputs);
    component = fixture.point.componentInstance;
    fixture.detectChanges();
  });

  describe('with defaults', () => {
    it('should create', () => {
      expect(component).toBeTruthy();
    });

    describe(HoehenprofilComponent.prototype.ngOnChanges.name, () => {
      it('should fill chart Höhen', () => {
        const distanzen = component.distanzen;
        expect(component.lineChartDatasets[0].data).toEqual(
          route
            .getCoordinates()
            .map(coor => coor[2])
            .map((hoehe, index) => {
              return { x: distanzen[index], y: hoehe };
            })
        );
      });
    });

    describe('custom tooltip', () => {
      let tooltipModel: any;

      it('should be empty if no data', () => {
        const spyOnHover = spyOn(component.hoverPositionChange, 'emit');
        const dummyElement = document.createElement('div');
        document.getElementById = jasmine.createSpy('HTML Element').and.returnValue(dummyElement);
        tooltipModel = { tooltip: { opacity: 0, dataPoints: [{ raw: {} }] } };

        // @ts-ignore
        component.lineChartOptions.plugins.tooltip.external(tooltipModel);
        expect(component.currentPosition).toBeFalsy();
        expect(component.currentHoehenmeter).toBeFalsy();
        expect(spyOnHover).not.toHaveBeenCalled();
      });

      it('should have correct content', () => {
        tooltipModel = {
          tooltip: {
            opacity: 1,
            dataPoints: [
              {
                raw: { x: 25000, y: 420 },
              },
            ],
          },
        };
        const hoverSpy = spyOn(component.hoverPositionChange, 'emit');
        // @ts-ignore
        component.lineChartOptions.plugins.tooltip.external(tooltipModel);

        expect(component.currentPosition).toEqual('25 km');
        expect(component.currentHoehenmeter).toEqual(420);
        expect(hoverSpy).toHaveBeenCalledWith(tooltipModel.tooltip.dataPoints[0].raw.x);
      });
    });

    describe(HoehenprofilComponent.prototype.ngOnDestroy.name, () => {
      let spyOnChartDestroy: jasmine.Spy<() => void>;

      beforeEach(() => {
        component.chart = {
          destroy: () => void {},
        } as Chart;
        spyOnChartDestroy = spyOn(component.chart, 'destroy');
      });

      it('should destroy chart', () => {
        component.ngOnDestroy();
        expect(spyOnChartDestroy).toHaveBeenCalled();
      });
    });
  });
});
