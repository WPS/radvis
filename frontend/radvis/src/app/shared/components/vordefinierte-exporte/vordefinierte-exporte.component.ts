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

import { HttpClient, HttpHeaders } from '@angular/common/http';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input } from '@angular/core';
import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { FileHandlingService } from 'src/app/shared/services/file-handling.service';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { OrganisationenService } from 'src/app/shared/services/organisationen.service';

@Component({
  selector: 'rad-vordefinierte-exporte',
  templateUrl: './vordefinierte-exporte.component.html',
  styleUrls: ['./vordefinierte-exporte.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class VordefinierteExporteComponent {
  private static readonly FILETYPE_SHAPE_ZIP = '(Shape-ZIP)';
  private static readonly FILETYPE_PBF = '(PBF)';

  @Input()
  isFetching = false;

  alleGebietskoerperschaften$: Promise<Verwaltungseinheit[]>;

  readonly geoserverBaseUrl = '/api/geoserver/saml/radvis/wfs?service=WFS&version=1.0.0&request=GetFeature';
  readonly geoserverBaseUrlShapezip = this.geoserverBaseUrl + '&outputFormat=SHAPE-ZIP';
  readonly geoserverBaseUrlGeoPackage = this.geoserverBaseUrl + '&outputFormat=gpkg';

  public exportLinksDaten = [
    {
      url: this.geoserverBaseUrlShapezip + '&typeName=radvis:abstellanlage',
      title: 'Abstellanlagen',
      filetype: VordefinierteExporteComponent.FILETYPE_SHAPE_ZIP,
    },
    {
      url: this.geoserverBaseUrlShapezip + '&typeName=radvis:anpassungswunsch',
      title: 'Anpassungswünsche',
      filetype: VordefinierteExporteComponent.FILETYPE_SHAPE_ZIP,
    },
    {
      url: this.geoserverBaseUrlShapezip + '&typeName=radvis:barriere_points,radvis:barriere_lines',
      title: 'Barrieren',
      filetype: VordefinierteExporteComponent.FILETYPE_SHAPE_ZIP,
    },
    {
      url: this.geoserverBaseUrlShapezip + '&typeName=radvis:fahrradroute',
      title: 'Fahrradrouten',
      filetype: VordefinierteExporteComponent.FILETYPE_SHAPE_ZIP,
    },
    {
      url: this.geoserverBaseUrlShapezip + '&typeName=radvis:fahrradzaehlstelle',
      title: 'Fahrradzählstellen',
      filetype: VordefinierteExporteComponent.FILETYPE_SHAPE_ZIP,
    },
    {
      url: this.geoserverBaseUrlShapezip + '&typeName=radvis:fehlerprotokoll',
      title: 'Fehlerprotokolle exkl. Konsistenzfehler',
      filetype: VordefinierteExporteComponent.FILETYPE_SHAPE_ZIP,
    },
    {
      url:
        this.geoserverBaseUrlShapezip +
        '&typeName=radvis:geoserver_furt_kreuzung_lines_view,radvis:geoserver_furt_kreuzung_points_view',
      title: 'Furten & Kreuzungen',
      filetype: VordefinierteExporteComponent.FILETYPE_SHAPE_ZIP,
    },
    {
      url: this.geoserverBaseUrlShapezip + '&typeName=radvis:geoserver_fahrradroute_import_diff_materialized_view',
      title: 'Importprotokolle (Unterschiede)',
      filetype: VordefinierteExporteComponent.FILETYPE_SHAPE_ZIP,
    },
    {
      url: this.geoserverBaseUrlShapezip + '&typeName=radvis:geoserver_konsistenzregel_verletzung_view',
      title: 'Konsistenzregel-Verletzungen',
      filetype: VordefinierteExporteComponent.FILETYPE_SHAPE_ZIP,
    },
    {
      url: this.geoserverBaseUrlShapezip + '&typeName=radvis:leihstationen',
      title: 'Leihstation',
      filetype: VordefinierteExporteComponent.FILETYPE_SHAPE_ZIP,
    },
    {
      url: this.geoserverBaseUrlShapezip + '&typeName=radvis:massnahmen_points,radvis:massnahmen_lines',
      title: 'Maßnahmen',
      filetype: VordefinierteExporteComponent.FILETYPE_SHAPE_ZIP,
    },
    {
      url: this.geoserverBaseUrlShapezip + '&typeName=radvis:servicestation',
      title: 'Servicestationen',
      filetype: VordefinierteExporteComponent.FILETYPE_SHAPE_ZIP,
    },
    {
      url: this.geoserverBaseUrlShapezip + '&typeName=radvis:wegweisende_beschilderung',
      title: 'Wegweisende Beschilderung',
      filetype: VordefinierteExporteComponent.FILETYPE_SHAPE_ZIP,
    },
  ];

  public exportLinksNetz = [
    {
      url:
        this.geoserverBaseUrlShapezip +
        '&typeName=radvis%3Aradvisnetz_klassifiziert&cql_filter=netzklassen%20like%20%27%25RADNETZ_FREIZEIT%25%27%20OR%20netzklassen%20like%20%27%25RADNETZ_ALLTAG%25%27%20',
      title: 'RadNETZ-Freizeit & RadNETZ-Alltag',
      filetype: VordefinierteExporteComponent.FILETYPE_SHAPE_ZIP,
    },
    {
      url: 'https://radroutenplaner-bw.de/data/routing/data-export.pbf',
      title: 'Angereichertes OSM-Netz',
      filetype: VordefinierteExporteComponent.FILETYPE_PBF,
    },
  ];

  constructor(
    private organisationenService: OrganisationenService,
    private http: HttpClient,
    private fileHandlingService: FileHandlingService,
    private errorHandlingService: ErrorHandlingService,
    private notifyUserService: NotifyUserService,
    private changeDetector: ChangeDetectorRef
  ) {
    this.alleGebietskoerperschaften$ = organisationenService.getGebietskoerperschaften();
  }

  ladeHerunter(value: Verwaltungseinheit | null): Promise<void> | null {
    this.isFetching = true;
    return this.organisationenService
      .getBereichVonOrganisationAlsString(value?.id)
      .then(bereich => {
        const body = new URLSearchParams();
        body.set('CQL_FILTER', 'INTERSECTS(geometry,' + bereich + ')');
        const headers = new HttpHeaders().set('Content-Type', 'application/x-www-form-urlencoded');
        const url = this.geoserverBaseUrlGeoPackage + '&typeName=radvis:radvisnetz_kante_abschnitte';

        return this.http
          .post<Blob>(url, body, {
            headers,
            observe: 'response',
            responseType: 'blob' as 'json',
          })
          .toPromise()
          .then(res => {
            if (res.body) {
              const filename = res.headers.get('content-disposition')?.split('=')[1] ?? '';
              try {
                this.fileHandlingService.downloadInBrowser(res.body, filename);
              } catch (err) {
                console.log(err);
                this.notifyUserService.warn('Die heruntergeladene Datei konnte nicht gespeichert werden');
              }
            } else {
              this.notifyUserService.warn('Die heruntergeladene Datei konnte nicht gespeichert werden');
            }
          })
          .catch(err => this.errorHandlingService.handleError(err, 'Die Datei konnte nicht heruntergeladen werden'));
      })
      .finally(() => {
        this.isFetching = false;
        this.changeDetector.markForCheck();
      })
      .catch(err => this.errorHandlingService.handleError(err, 'Die Datei konnte nicht heruntergeladen werden'));
  }
}
