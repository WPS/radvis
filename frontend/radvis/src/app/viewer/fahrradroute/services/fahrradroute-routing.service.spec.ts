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
import { FAHRRADROUTE } from 'src/app/viewer/fahrradroute/models/fahrradroute.infrastruktur';
import { VIEWER_ROUTE } from 'src/app/viewer/viewer-shared/models/viewer-routes';
import { anything, deepEqual, instance, mock, verify, when } from 'ts-mockito';
import { FahrradrouteRoutingService } from './fahrradroute-routing.service';

describe(FahrradrouteRoutingService.name, () => {
  let service: FahrradrouteRoutingService;
  let router: Router;
  const routerEvents: Subject<NavigationEnd> = new Subject();

  beforeEach(() => {
    router = mock(Router);
    when(router.events).thenReturn(routerEvents.asObservable());
    service = new FahrradrouteRoutingService(instance(router));
  });

  describe('getInfrastrukturenEditorRoute', () => {
    it('should not get active subroute', () => {
      when(router.url).thenReturn(
        'http://localhost:4200/viewer/fahrradrouten/148506/subroute?layers=RADVIS_NETZ&view=380000;5250000;620000;5525000&netzklassen=RADNETZ&hintergrund=&signatur=&mitVerlauf=false'
      );

      const result = service.getInfrastrukturenEditorRoute(1);

      expect(result).toEqual([VIEWER_ROUTE, FAHRRADROUTE.pathElement, '1']);
    });

    it('should not get active subroute wihtout query params', () => {
      when(router.url).thenReturn('http://localhost:4200/viewer/fahrradrouten/148506/test');

      const result = service.getInfrastrukturenEditorRoute(1);

      expect(result).toEqual([VIEWER_ROUTE, FAHRRADROUTE.pathElement, '1']);
    });

    it('should return nothing if no fahrradroute selected', () => {
      when(router.url).thenReturn('http://localhost:4200/viewer');

      const result = service.getInfrastrukturenEditorRoute(1);

      expect(result).toEqual([VIEWER_ROUTE, FAHRRADROUTE.pathElement, '1']);
    });
  });

  describe('selectedInfrastrukturId$', () => {
    describe('initial value', () => {
      it('should be null', (done: DoneFn) => {
        const url = 'http://localhost:4200/viewer';
        when(router.url).thenReturn(url);
        service = new FahrradrouteRoutingService(instance(router));
        service.selectedInfrastrukturId$.subscribe(id => {
          expect(id).toBeNull();
          done();
        });
      });

      it('should be selected id', (done: DoneFn) => {
        const url =
          'http://localhost:4200/viewer/fahrradrouten/148506?layers=RADVIS_NETZ&view=380000;5250000;620000;5525000&netzklassen=RADNETZ&hintergrund=&signatur=&mitVerlauf=false';
        when(router.url).thenReturn(url);
        service = new FahrradrouteRoutingService(instance(router));
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
        'http://localhost:4200/viewer/fahrradrouten/148506/subroute?layers=RADVIS_NETZ&view=380000;5250000;620000;5525000&netzklassen=RADNETZ&hintergrund=&signatur=&mitVerlauf=false';
      when(router.url).thenReturn(url1);
      nextUrl(url1);
    });
  });

  describe('selectedInfrastrukturId', () => {
    // Bis jetzt sind mW. keine Subroutes für Fahrradrouten vorgesehen. Testfälle trotzdem beibehalten?
    it('should get correct fahrradroutenId from route if in subroute', () => {
      const url1 =
        'http://localhost:4200/viewer/fahrradrouten/148506/subroute1?layers=RADVIS_NETZ&view=380000;5250000;620000;5525000&netzklassen=RADNETZ&hintergrund=&signatur=&mitVerlauf=false';
      when(router.url).thenReturn(url1);
      nextUrl(url1);

      expect(service.selectedInfrastrukturId).toEqual(148506);

      const url2 =
        'http://localhost:4200/viewer/fahrradrouten/148507/subroute2?layers=RADVIS_NETZ&view=380000;5250000;620000;5525000&netzklassen=RADNETZ&hintergrund=&signatur=&mitVerlauf=false';
      when(router.url).thenReturn(url2);
      nextUrl(url2);

      expect(service.selectedInfrastrukturId).toEqual(148507);
    });

    it('should return nothing if no fahrradrouten selected', () => {
      const url = 'http://localhost:4200/viewer';
      when(router.url).thenReturn(url);
      nextUrl(url);

      expect(service.selectedInfrastrukturId).toBeNull();
    });

    it('should get correct fahrradroutenId from route if not in subroute and without queryParams', () => {
      const url = 'http://localhost:4200/viewer/fahrradrouten/148506';
      when(router.url).thenReturn(url);
      nextUrl(url);

      expect(service.selectedInfrastrukturId).toEqual(148506);
    });

    it('should get correct fahrradroutenId from route if not in subroute but with queryParams', () => {
      const url =
        'http://localhost:4200/viewer/fahrradrouten/148506?layers=RADVIS_NETZ&view=380000;5250000;620000;5525000&netzklassen=RADNETZ&hintergrund=&signatur=&mitVerlauf=false';
      when(router.url).thenReturn(url);
      nextUrl(url);

      expect(service.selectedInfrastrukturId).toEqual(148506);
    });
  });

  describe('toInfrastrukturEditor', () => {
    it('should navigate to fahrradrouteDetailView', () => {
      const destUrl = [VIEWER_ROUTE, FAHRRADROUTE.pathElement, '1'];

      service.toInfrastrukturEditor(1);

      verify(router.navigate(deepEqual(destUrl), anything())).once();
      expect().nothing();
    });
  });

  const nextUrl = (url: string): void => {
    routerEvents.next(new NavigationEnd(1, url, url));
  };
});
