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

import { ChangeDetectionStrategy, Component, ElementRef, HostListener, ViewChild } from '@angular/core';
import { Observable } from 'rxjs';
import { EditorLayerZindexConfig } from 'src/app/editor/editor-shared/models/editor-layer-zindex-config';
import { EditorRoutingService } from 'src/app/editor/editor-shared/services/editor-routing.service';
import { AttributGruppe } from 'src/app/editor/kanten/models/attribut-gruppe';
import { KantenSelektionService } from 'src/app/editor/kanten/services/kanten-selektion.service';
import { NetzBearbeitungModusService } from 'src/app/editor/kanten/services/netz-bearbeitung-modus.service';
import { FeatureTogglzService } from 'src/app/shared/services/feature-togglz.service';

@Component({
  selector: 'rad-kanten-tool',
  templateUrl: './kanten-tool.component.html',
  styleUrls: ['./kanten-tool.component.scss', '../../../editor-tool-styles.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class KantenToolComponent {
  @ViewChild('toolContainer', { read: ElementRef })
  toolContainer: ElementRef | undefined;

  AttributGruppe = AttributGruppe;

  public kantenCreatorRoute: string;
  public isKantenCreatorAktiv: Observable<boolean>;
  public fehlerprotokolleEnabled: boolean;
  public fehlerprotokollZIndex = EditorLayerZindexConfig.FEHLERPROTOKOLL_LAYER;
  aktiveKantenGruppe$: Observable<AttributGruppe | null>;

  constructor(
    public editorRoutingService: EditorRoutingService,
    private bearbeitungsModusService: NetzBearbeitungModusService,
    private kantenSelektionService: KantenSelektionService,
    featureTogglzService: FeatureTogglzService
  ) {
    this.kantenCreatorRoute = this.editorRoutingService.getKantenCreatorRoute();
    this.isKantenCreatorAktiv = this.bearbeitungsModusService.isKantenCreatorAktiv();
    this.aktiveKantenGruppe$ = bearbeitungsModusService.getAktiveKantenGruppe();
    this.fehlerprotokolleEnabled = featureTogglzService.fehlerprotokoll;
  }

  @HostListener('document:keydown.control.alt.shift.d')
  onShortcut(): void {
    this.toolContainer?.nativeElement.querySelector('button, [role="button"]')?.focus();
  }

  public getRouteForSubmenu(group: AttributGruppe): string {
    return this.editorRoutingService.getAttributGruppeRoute(group);
  }

  public get kanteSelektiert(): boolean {
    return this.kantenSelektionService.selektion.length > 0;
  }
}
