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

import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { benutzerAktivGuard } from 'src/app/shared/services/benutzer-aktiv.guard';
import { benutzerRegistriertGuard } from 'src/app/shared/services/benutzer-registriert.guard';
import { discardGuard } from 'src/app/shared/services/discard.guard';
import { AbstellanlageEditorComponent } from 'src/app/viewer/abstellanlage/components/abstellanlage-editor/abstellanlage-editor.component';
import { AbstellanlageToolComponent } from 'src/app/viewer/abstellanlage/components/abstellanlage-tool/abstellanlage-tool.component';
import { ABSTELLANLAGEN } from 'src/app/viewer/abstellanlage/models/abstellanlage.infrastruktur';
import { AbstellanlageRoutingService } from 'src/app/viewer/abstellanlage/services/abstellanlage-routing.service';
import { abstellanlageResolver } from 'src/app/viewer/abstellanlage/services/abstellanlage.resolver';
import { AnpassungenEditorComponent } from 'src/app/viewer/anpassungswunsch/components/anpassungen-editor/anpassungen-editor.component';
import { AnpassungswunschToolComponent } from 'src/app/viewer/anpassungswunsch/components/anpassungswunsch-tool/anpassungswunsch-tool.component';
import { ANPASSUNGSWUNSCH } from 'src/app/viewer/anpassungswunsch/models/anpassungswunsch.infrastruktur';
import { AnpassungenRoutingService } from 'src/app/viewer/anpassungswunsch/services/anpassungen-routing.service';
import { BarriereEditorComponent } from 'src/app/viewer/barriere/components/barriere-editor/barriere-editor.component';
import { BARRIEREN } from 'src/app/viewer/barriere/models/barriere.infrastruktur';
import { barriereResolver } from 'src/app/viewer/barriere/services/barriere.resolver';
import { ViewerComponent } from 'src/app/viewer/components/viewer/viewer.component';
import { FahrradrouteAttributeEditorComponent } from 'src/app/viewer/fahrradroute/components/fahrradroute-attribute-editor/fahrradroute-attribute-editor.component';
import { FahrradroutenCreatorComponent } from 'src/app/viewer/fahrradroute/components/fahrradrouten-creator/fahrradrouten-creator.component';
import { FAHRRADROUTE } from 'src/app/viewer/fahrradroute/models/fahrradroute.infrastruktur';
import { FahrradrouteRoutingService } from 'src/app/viewer/fahrradroute/services/fahrradroute-routing.service';
import { fahrradrouteResolver } from 'src/app/viewer/fahrradroute/services/fahrradroute.resolver';
import { FahrradzaehlstelleToolComponent } from 'src/app/viewer/fahrradzaehlstelle/components/fahrradzaehlstelle-tool/fahrradzaehlstelle-tool.component';
import { FAHRRADZAEHLSTELLE } from 'src/app/viewer/fahrradzaehlstelle/models/fahrradzaehlstelle.infrastruktur';
import { FahrradzaehlstelleRoutingService } from 'src/app/viewer/fahrradzaehlstelle/services/fahrradzaehlstelle-routing.service';
import { fahrradzaehlstelleResolver } from 'src/app/viewer/fahrradzaehlstelle/services/fahrradzaehlstelle.resolver';
import { FurtenKreuzungenEditorComponent } from 'src/app/viewer/furten-kreuzungen/components/furten-kreuzungen-editor/furten-kreuzungen-editor.component';
import { FURTEN_KREUZUNGEN } from 'src/app/viewer/furten-kreuzungen/models/furten-kreuzungen.infrastruktur';
import { furtKreuzungResolver } from 'src/app/viewer/furten-kreuzungen/services/furt-kreuzung.resolver';
import { FahrradrouteImportDetailViewComponent } from 'src/app/viewer/importprotokolle/components/fahrradroute-import-detail-view/fahrradroute-import-detail-view.component';
import { WegweiserImportDetailViewComponent } from 'src/app/viewer/importprotokolle/components/wegweiser-import-detail-view/wegweiser-import-detail-view.component';
import { ImportprotokollTyp } from 'src/app/viewer/importprotokolle/models/importprotokoll-typ';
import { IMPORTPROTOKOLLE } from 'src/app/viewer/importprotokolle/models/importprotokoll.infrastruktur';
import { fahrradrouteImportprotokollResolver } from 'src/app/viewer/importprotokolle/services/fahrradroute-importprotokoll.resolver';
import { wegweiserImportprotokollResolver } from 'src/app/viewer/importprotokolle/services/wegweiser-importprotokoll.resolver';
import { LeihstationEditorComponent } from 'src/app/viewer/leihstation/components/leihstation-editor/leihstation-editor.component';
import { LEIHSTATIONEN } from 'src/app/viewer/leihstation/models/leihstation.infrastruktur';
import { LeihstationRoutingService } from 'src/app/viewer/leihstation/services/leihstation-routing.service';
import { leihstationResolver } from 'src/app/viewer/leihstation/services/leihstation.resolver';
import { MassnahmenCreatorComponent } from 'src/app/viewer/massnahme/components/massnahmen-creator/massnahmen-creator.component';
import { MassnahmenToolComponent } from 'src/app/viewer/massnahme/components/massnahmen-tool/massnahmen-tool.component';
import { MASSNAHMEN } from 'src/app/viewer/massnahme/models/massnahme.infrastruktur';
import { MassnahmenRoutingService } from 'src/app/viewer/massnahme/services/massnahmen-routing.service';
import { massnahmenToolResolver } from 'src/app/viewer/massnahme/services/massnahmen-tool.resolver';
import { NETZ_DETAIL_ROUTES } from 'src/app/viewer/netz-details/netz-details.routes';
import { ServicestationEditorComponent } from 'src/app/viewer/servicestation/components/servicestation-editor/servicestation-editor.component';
import { ServicestationToolComponent } from 'src/app/viewer/servicestation/components/servicestation-tool/servicestation-tool.component';
import { SERVICESTATIONEN } from 'src/app/viewer/servicestation/models/servicestation.infrastruktur';
import { ServicestationRoutingService } from 'src/app/viewer/servicestation/services/servicestation-routing.service';
import { servicestationResolver } from 'src/app/viewer/servicestation/services/servicestation.resolver';
import { VIEWER_ROUTE } from 'src/app/viewer/viewer-shared/models/viewer-routes';
import { WegweisendeBeschilderungDetailViewComponent } from 'src/app/viewer/wegweisende-beschilderung/components/wegweisende-beschilderung-detail-view/wegweisende-beschilderung-detail-view.component';
import { WEGWEISENDE_BESCHILDERUNG } from 'src/app/viewer/wegweisende-beschilderung/models/wegweisende-beschilderung.infrastruktur';
import { wegweisendeBeschilderungResolver } from 'src/app/viewer/wegweisende-beschilderung/services/wegweisende-beschilderung.resolver';
import { WeitereKartenebenenDetailViewComponent } from 'src/app/viewer/weitere-kartenebenen/components/weitere-kartenebenen-detail-view/weitere-kartenebenen-detail-view.component';
import { WeitereKartenebenenRoutingService } from 'src/app/viewer/weitere-kartenebenen/services/weitere-kartenebenen-routing.service';
import { weitereKartenebenenGuard } from 'src/app/viewer/weitere-kartenebenen/services/weitere-kartenebenen.guard';
import { weitereKartenebenenResolver } from 'src/app/viewer/weitere-kartenebenen/services/weitere-kartenebenen.resolver';

const routes: Routes = [
  {
    path: VIEWER_ROUTE,
    component: ViewerComponent,
    canActivate: [benutzerRegistriertGuard, benutzerAktivGuard],
    children: [
      {
        path: `${MASSNAHMEN.pathElement}/${MassnahmenRoutingService.CREATOR}`,
        component: MassnahmenCreatorComponent,
        canDeactivate: [discardGuard],
      },
      {
        path: `${MASSNAHMEN.pathElement}/:id`,
        component: MassnahmenToolComponent,
        children: MassnahmenRoutingService.getChildRoutes(),
        resolve: { massnahme: massnahmenToolResolver },
      },
      {
        path: `${FAHRRADROUTE.pathElement}/${FahrradrouteRoutingService.CREATOR}`,
        component: FahrradroutenCreatorComponent,
        canDeactivate: [discardGuard],
      },
      {
        path: `${FAHRRADROUTE.pathElement}/:id`,
        component: FahrradrouteAttributeEditorComponent,
        resolve: { fahrradrouteDetailView: fahrradrouteResolver },
        canDeactivate: [discardGuard],
      },
      ...NETZ_DETAIL_ROUTES,
      {
        path: `${IMPORTPROTOKOLLE.pathElement}/${ImportprotokollTyp.FAHRRADROUTE}/:id`,
        component: FahrradrouteImportDetailViewComponent,
        resolve: { protokollEintrag: fahrradrouteImportprotokollResolver },
      },
      {
        path: `${IMPORTPROTOKOLLE.pathElement}/${ImportprotokollTyp.WEGWEISENDE_BESCHILDERUNG}/:id`,
        component: WegweiserImportDetailViewComponent,
        resolve: { protokollEintrag: wegweiserImportprotokollResolver },
      },
      {
        path: `${ANPASSUNGSWUNSCH.pathElement}/${AnpassungenRoutingService.CREATOR_ROUTE}`,
        component: AnpassungenEditorComponent,
        data: {
          isCreator: true,
        },
        canDeactivate: [discardGuard],
      },
      {
        path: `${ANPASSUNGSWUNSCH.pathElement}/:id`,
        component: AnpassungswunschToolComponent,
        children: AnpassungenRoutingService.getChildRoutes(),
      },
      {
        path: `${FURTEN_KREUZUNGEN.pathElement}/new`,
        component: FurtenKreuzungenEditorComponent,
        data: {
          isCreator: true,
        },
        canDeactivate: [discardGuard],
      },
      {
        path: `${FURTEN_KREUZUNGEN.pathElement}/:id`,
        component: FurtenKreuzungenEditorComponent,
        data: {
          isCreator: false,
        },
        resolve: { furtKreuzung: furtKreuzungResolver },
        canDeactivate: [discardGuard],
      },
      {
        path: `${BARRIEREN.pathElement}/new`,
        component: BarriereEditorComponent,
        data: {
          isCreator: true,
        },
        canDeactivate: [discardGuard],
      },
      {
        path: `${BARRIEREN.pathElement}/:id`,
        component: BarriereEditorComponent,
        data: {
          isCreator: false,
        },
        resolve: { barriere: barriereResolver },
        canDeactivate: [discardGuard],
      },
      {
        path: `${WeitereKartenebenenRoutingService.ROUTE_LAYER}/:layerId/${WeitereKartenebenenRoutingService.ROUTE_FEATURE}/:featureId`,
        component: WeitereKartenebenenDetailViewComponent,
        resolve: { feature: weitereKartenebenenResolver },
        canActivate: [weitereKartenebenenGuard],
      },
      {
        path: `${WEGWEISENDE_BESCHILDERUNG.pathElement}/:id`,
        component: WegweisendeBeschilderungDetailViewComponent,
        resolve: { wegweisendeBeschilderung: wegweisendeBeschilderungResolver },
      },
      {
        path: `${ABSTELLANLAGEN.pathElement}/${AbstellanlageRoutingService.CREATOR_ROUTE}`,
        component: AbstellanlageEditorComponent,
        data: { isCreator: true },
        canDeactivate: [discardGuard],
      },
      {
        path: `${ABSTELLANLAGEN.pathElement}/:id`,
        component: AbstellanlageToolComponent,
        children: AbstellanlageRoutingService.getChildRoutes(),
        resolve: { abstellanlage: abstellanlageResolver },
      },
      {
        path: `${SERVICESTATIONEN.pathElement}/${ServicestationRoutingService.CREATOR_ROUTE}`,
        component: ServicestationEditorComponent,
        data: { isCreator: true },
        canDeactivate: [discardGuard],
      },
      {
        path: `${SERVICESTATIONEN.pathElement}/:id`,
        component: ServicestationToolComponent,
        children: ServicestationRoutingService.getChildRoutes(),
        resolve: { servicestation: servicestationResolver },
      },
      {
        path: `${LEIHSTATIONEN.pathElement}/${LeihstationRoutingService.CREATOR_ROUTE}`,
        component: LeihstationEditorComponent,
        data: { isCreator: true },
        canDeactivate: [discardGuard],
      },
      {
        path: `${LEIHSTATIONEN.pathElement}/:id`,
        component: LeihstationEditorComponent,
        data: { isCreator: false },
        resolve: { leihstation: leihstationResolver },
        canDeactivate: [discardGuard],
      },
      {
        path: `${FAHRRADZAEHLSTELLE.pathElement}/:id`,
        component: FahrradzaehlstelleToolComponent,
        children: FahrradzaehlstelleRoutingService.getChildRoutes(),
        resolve: { fahrradzaehlstelleDetailView: fahrradzaehlstelleResolver },
      },
    ],
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class ViewerRoutingModule {}
