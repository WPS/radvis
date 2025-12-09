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

import { LocationStrategy } from '@angular/common';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting, TestRequest } from '@angular/common/http/testing';
import { DebugElement } from '@angular/core';
import { fakeAsync, tick } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatOption } from '@angular/material/core';
import { By } from '@angular/platform-browser';
import { MockBuilder, MockedComponentFixture, MockRender } from 'ng-mocks';
import { MatomoTracker } from 'ngx-matomo-client';
import { MaterialDesignModule } from 'src/app/material-design.module';
import { OrganisationenDropdownControlComponent } from 'src/app/shared/components/organisationen-dropdown-control/organisationen-dropdown-control.component';
import { VordefinierteExporteComponent } from 'src/app/shared/components/vordefinierte-exporte/vordefinierte-exporte.component';
import { defaultGemeinden } from 'src/app/shared/models/organisation-test-data-provider.spec';
import { FileHandlingService } from 'src/app/shared/services/file-handling.service';
import { OrganisationenService } from 'src/app/shared/services/organisationen.service';
import { SharedModule } from 'src/app/shared/shared.module';
import { anything, instance, mock, verify } from 'ts-mockito';

describe(VordefinierteExporteComponent.name, () => {
  let component: VordefinierteExporteComponent;
  let fixture: MockedComponentFixture<VordefinierteExporteComponent>;
  let http: HttpTestingController;
  let fileHandlingService: FileHandlingService;
  let matomoTracker: MatomoTracker;

  const firstDefaultGemeindeOption = {
    option: {
      value: defaultGemeinden[0],
    } as MatOption,
  } as MatAutocompleteSelectedEvent;

  beforeEach(() => {
    fileHandlingService = mock(FileHandlingService);
    matomoTracker = mock(MatomoTracker);
    return MockBuilder(VordefinierteExporteComponent, SharedModule)
      .keep(ReactiveFormsModule)
      .keep(OrganisationenService)
      .keep(OrganisationenDropdownControlComponent)
      .keep(MaterialDesignModule)
      .keep(MatButtonModule)
      .keep(LocationStrategy)
      .provide(provideHttpClient(withInterceptorsFromDi()))
      .provide(provideHttpClientTesting())
      .provide({ provide: FileHandlingService, useValue: instance(fileHandlingService) })
      .provide({ provide: MatomoTracker, useValue: instance(matomoTracker) });
  });

  beforeEach(() => {
    fixture = MockRender(VordefinierteExporteComponent);
    component = fixture.point.componentInstance;
    fixture.detectChanges();
    http = fixture.debugElement.injector.get(HttpTestingController);
    http.expectOne('/api/organisationen/gebietskoerperschaften').flush(defaultGemeinden);
  });
  describe('Netz', () => {
    it('should show disabled Download Netz Button', () => {
      const downloadNetzButton = getButtonWithText('Download Netz');
      expect(downloadNetzButton).not.toBeNull();
      expect(downloadNetzButton?.nativeElement.disabled).toBeTrue();
    });

    describe('with Gebietskörperschaft selected', () => {
      beforeEach(fakeAsync(() => {
        const organisationenDropDown = getOrgaDropDown();

        organisationenDropDown.onOptionSelected(firstDefaultGemeindeOption);

        tick();
        fixture.detectChanges();

        expect(organisationenDropDown.selectedOrganisation).toEqual(defaultGemeinden[0]);
      }));

      it('should download Netz', fakeAsync(() => {
        const downloadNetzButton = getButtonWithText('Download Netz')!;
        expect(downloadNetzButton?.nativeElement.disabled).toBeFalse();

        downloadNetzButton?.nativeElement.click();
        expectBereichApiCallAndDisabledButton(downloadNetzButton);
        expect(getButtonWithText('Download Maßnahmen')!.nativeElement.disabled).toBeFalse();

        const request = http.expectOne(
          component.geoserverBaseUrlGeoPackage + '&typeName=radvis:radvisnetz_kante_abschnitte'
        );
        expect(downloadNetzButton?.nativeElement.disabled).toBeTrue();
        expectGeoserverRequestWithCorrectCQLFilter(request);

        request.flush(new Blob(), getHeadersForFilename('testname.zip'));

        tick();
        fixture.detectChanges();
        // noinspection JSVoidFunctionReturnValueUsed
        verify(fileHandlingService.downloadInBrowser(anything(), 'testname.zip')).once();
        // noinspection JSVoidFunctionReturnValueUsed
        verify(matomoTracker.trackEvent('Vordefinierte Exporte', 'Download', 'Netz für Gebietskörperschaft')).once();
        expect(downloadNetzButton?.nativeElement.disabled).toBeFalse();
      }));
    });
  });

  describe('Massnahmen', () => {
    let downloadMassnahmenButton: DebugElement;

    beforeEach(() => {
      downloadMassnahmenButton = getButtonWithText('Download Maßnahmen')!;
    });

    it('should show disabled Download Massnahmen Button', () => {
      expect(downloadMassnahmenButton).not.toBeNull();
      expect(downloadMassnahmenButton.nativeElement.disabled).toBeTrue();
    });

    describe('with Gebietskörperschaft selected', () => {
      beforeEach(fakeAsync(() => {
        const organisationenDropDown = getOrgaDropDown();
        organisationenDropDown.onOptionSelected(firstDefaultGemeindeOption);

        tick();
        fixture.detectChanges();

        expect(organisationenDropDown.selectedOrganisation).toEqual(defaultGemeinden[0]);

        expect(downloadMassnahmenButton.nativeElement.disabled).toBeFalse();

        downloadMassnahmenButton.nativeElement.click();
      }));

      it('should download Massnahmen as CSV', fakeAsync(() => {
        getButtonWithText('CSV')?.nativeElement.click();
        expectBereichApiCallAndDisabledButton(downloadMassnahmenButton);
        expect(getButtonWithText('Download Netz')!.nativeElement.disabled).toBeFalse();

        const request = http.expectOne(
          component.geoserverBaseUrlCsv + '&typeName=radvis:geoserver_massnahmen_erweitert_view'
        );

        expect(downloadMassnahmenButton.nativeElement.disabled).toBeTrue();
        expectGeoserverRequestWithCorrectCQLFilter(request);

        request.flush(new Blob(), getHeadersForFilename('testname.csv'));

        tick();
        fixture.detectChanges();
        // noinspection JSVoidFunctionReturnValueUsed
        verify(fileHandlingService.downloadInBrowser(anything(), 'testname.csv')).once();
        // noinspection JSVoidFunctionReturnValueUsed
        verifyMatomoTrackerNotCalled();
        expect(downloadMassnahmenButton.nativeElement.disabled).toBeFalse();
      }));

      it('should download Massnahmen as GEOPACKAGE', fakeAsync(() => {
        getButtonWithText('GeoPackage')?.nativeElement.click();
        expectBereichApiCallAndDisabledButton(downloadMassnahmenButton);
        expect(getButtonWithText('Download Netz')!.nativeElement.disabled).toBeFalse();

        const request = http.expectOne(
          component.geoserverBaseUrlGeoPackage + '&typeName=radvis:geoserver_massnahmen_erweitert_view'
        );

        expect(downloadMassnahmenButton.nativeElement.disabled).toBeTrue();
        expectGeoserverRequestWithCorrectCQLFilter(request);

        request.flush(new Blob(), getHeadersForFilename('testname.gpkg'));

        tick();
        fixture.detectChanges();
        // noinspection JSVoidFunctionReturnValueUsed
        verify(fileHandlingService.downloadInBrowser(anything(), 'testname.gpkg')).once();
        verifyMatomoTrackerNotCalled();
        expect(downloadMassnahmenButton.nativeElement.disabled).toBeFalse();
      }));
    });
  });

  function getHeadersForFilename(filename: string): { headers: { [p: string]: string } } {
    return {
      headers: {
        'content-disposition': `attachment; filename=${filename}`,
      },
    };
  }

  const bereichAlsStringDummy = 'bereichAlsStringDummy';

  function expectBereichApiCallAndDisabledButton(button: DebugElement): void {
    fixture.detectChanges();

    expect(button.nativeElement.disabled).toBeTrue();
    http.expectOne(`/api/organisationen/bereichAlsString/${defaultGemeinden[0].id}`).flush(bereichAlsStringDummy);

    tick();
    fixture.detectChanges();
    expect(button.nativeElement.disabled).toBeTrue();
  }

  function verifyMatomoTrackerNotCalled(): void {
    // noinspection JSVoidFunctionReturnValueUsed
    verify(matomoTracker.trackEvent(anything(), anything())).never();
    // noinspection JSVoidFunctionReturnValueUsed
    verify(matomoTracker.trackEvent(anything(), anything(), anything())).never();
    // noinspection JSVoidFunctionReturnValueUsed
    verify(matomoTracker.trackEvent(anything(), anything(), anything(), anything())).never();
    // noinspection JSVoidFunctionReturnValueUsed
    verify(matomoTracker.trackEvent(anything(), anything(), anything(), anything(), anything())).never();
  }

  function getOrgaDropDown(): OrganisationenDropdownControlComponent {
    return fixture.debugElement.query(By.directive(OrganisationenDropdownControlComponent))
      .componentInstance as OrganisationenDropdownControlComponent;
  }

  function getButtonWithText(text: string): DebugElement | null {
    return (
      fixture.debugElement.queryAll(By.css('button')).find(button => button.nativeElement.textContent.includes(text)) ??
      null
    );
  }

  function expectGeoserverRequestWithCorrectCQLFilter(request: TestRequest): void {
    expect(request.request.responseType).toEqual('blob');
    expect(request.request.headers.get('Content-Type')).toEqual('application/x-www-form-urlencoded');
    expect(request.request.method).toEqual('POST');
    expect((request.request.body as URLSearchParams).get('CQL_FILTER')).toEqual(
      `INTERSECTS(geometry,${bereichAlsStringDummy})`
    );
  }
});
