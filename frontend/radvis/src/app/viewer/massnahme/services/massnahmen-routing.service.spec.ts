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

import { NavigationEnd, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { skip } from 'rxjs/operators';
import { MASSNAHMEN } from 'src/app/viewer/massnahme/models/massnahme.infrastruktur';
import { MassnahmenRoutingService } from 'src/app/viewer/massnahme/services/massnahmen-routing.service';
import { VIEWER_ROUTE } from 'src/app/viewer/viewer-shared/models/viewer-routes';
import { anything, deepEqual, instance, mock, verify, when } from 'ts-mockito';

describe(MassnahmenRoutingService.name, () => {
  let service: MassnahmenRoutingService;
  let router: Router;
  const routerEvents: Subject<NavigationEnd> = new Subject();

  beforeEach(() => {
    router = mock(Router);
    when(router.events).thenReturn(routerEvents.asObservable());
    service = new MassnahmenRoutingService(instance(router));
  });

  describe('massnahmenAttributeEditorRoute', () => {
    it('should get active subroute', () => {
      when(router.url).thenReturn(
        'http://localhost:4200/viewer/massnahmen/148506/umsetzungsstand?layers=RADVIS_NETZ&view=380000;5250000;620000;5525000&netzklassen=RADNETZ&hintergrund=&signatur=&mitVerlauf=false'
      );

      const result = service.getInfrastrukturenEditorRoute(1);

      expect(result).toEqual([VIEWER_ROUTE, MASSNAHMEN.pathElement, '1', 'umsetzungsstand']);
    });

    it('should get active subroute wihtout query params', () => {
      when(router.url).thenReturn('http://localhost:4200/viewer/massnahmen/148506/test');

      const result = service.getInfrastrukturenEditorRoute(1);

      expect(result).toEqual([VIEWER_ROUTE, MASSNAHMEN.pathElement, '1', 'test']);
    });

    it('should return nothing if no maßnahmen selected', () => {
      when(router.url).thenReturn('http://localhost:4200/viewer');

      const result = service.getInfrastrukturenEditorRoute(1);

      expect(result).toEqual([VIEWER_ROUTE, MASSNAHMEN.pathElement, '1']);
    });
  });

  describe('selectedInfrastrukturId$', () => {
    describe('initial value', () => {
      it('should be null', (done: DoneFn) => {
        const url = 'http://localhost:4200/viewer';
        when(router.url).thenReturn(url);
        service = new MassnahmenRoutingService(instance(router));
        service.selectedInfrastrukturId$.subscribe(id => {
          expect(id).toBeNull();
          done();
        });
      });

      it('should be selected id', (done: DoneFn) => {
        const url =
          'http://localhost:4200/viewer/massnahmen/148506/umsetzungsstand?layers=RADVIS_NETZ&view=380000;5250000;620000;5525000&netzklassen=RADNETZ&hintergrund=&signatur=&mitVerlauf=false';
        when(router.url).thenReturn(url);
        service = new MassnahmenRoutingService(instance(router));
        service.selectedInfrastrukturId$.subscribe(id => {
          expect(id).toEqual(148506);
          done();
        });
      });
    });

    it('should emit on navigate', (done: DoneFn) => {
      service.selectedInfrastrukturId$.pipe(skip(1)).subscribe(id => {
        expect(id).toEqual(148506);
        done();
      });
      const url1 =
        'http://localhost:4200/viewer/massnahmen/148506/umsetzungsstand?layers=RADVIS_NETZ&view=380000;5250000;620000;5525000&netzklassen=RADNETZ&hintergrund=&signatur=&mitVerlauf=false';
      when(router.url).thenReturn(url1);
      nextUrl(url1);
    });
  });

  describe('selectedInfrastrukturId', () => {
    it('should get correct massnahmenId from route if in subroute', () => {
      const url1 =
        'http://localhost:4200/viewer/massnahmen/148506/umsetzungsstand?layers=RADVIS_NETZ&view=380000;5250000;620000;5525000&netzklassen=RADNETZ&hintergrund=&signatur=&mitVerlauf=false';
      when(router.url).thenReturn(url1);
      nextUrl(url1);

      expect(service.selectedInfrastrukturId).toEqual(148506);

      const url2 =
        'http://localhost:4200/viewer/massnahmen/148507/eigenschaften?layers=RADVIS_NETZ&view=380000;5250000;620000;5525000&netzklassen=RADNETZ&hintergrund=&signatur=&mitVerlauf=false';
      when(router.url).thenReturn(url2);
      nextUrl(url2);

      expect(service.selectedInfrastrukturId).toEqual(148507);
    });

    it('should return nothing if no maßnahmen selected', () => {
      const url = 'http://localhost:4200/viewer';
      when(router.url).thenReturn(url);
      nextUrl(url);

      expect(service.selectedInfrastrukturId).toBeNull();
    });

    it('should get correct massnahmenId from route if not in subroute and without queryParams', () => {
      const url = 'http://localhost:4200/viewer/massnahmen/148506';
      when(router.url).thenReturn(url);
      nextUrl(url);

      expect(service.selectedInfrastrukturId).toEqual(148506);
    });

    it('should get correct massnahmenId from route if not in subroute but with queryParams', () => {
      const url =
        'http://localhost:4200/viewer/massnahmen/148506?layers=RADVIS_NETZ&view=380000;5250000;620000;5525000&netzklassen=RADNETZ&hintergrund=&signatur=&mitVerlauf=false';
      when(router.url).thenReturn(url);
      nextUrl(url);

      expect(service.selectedInfrastrukturId).toEqual(148506);
    });
  });

  describe('toInfrastrukturEditor', () => {
    it('should navigate to MassnahmenAttributeEditor', () => {
      const destUrl = [VIEWER_ROUTE, MASSNAHMEN.pathElement, '1'];

      service.toInfrastrukturEditor(1);

      verify(router.navigate(deepEqual(destUrl), anything())).once();
      expect().nothing();
    });
  });

  const nextUrl = (url: string): void => {
    routerEvents.next(new NavigationEnd(1, url, url));
  };
});
