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
import { MatomoTracker } from 'ngx-matomo-client';
import { ActivatedRoute } from '@angular/router';

export abstract class AbstractEventTrackedEditor {
  constructor(
    protected matomoTracker: MatomoTracker,
    protected activatedRoute: ActivatedRoute
  ) {}

  protected trackSpeichernEvent(): void {
    const eventEmitter = this.getEventEmitter();
    this.matomoTracker.trackEvent('Editor', 'Speichern', eventEmitter);
  }

  private getEventEmitter(): string {
    const parentUrl = this.activatedRoute.parent?.snapshot.url.join('/') || '';
    const currentUrl = this.activatedRoute.snapshot.url.join('/');

    return `${parentUrl}/${currentUrl}`
      .replace(new RegExp('\\d+'), '') // remove any digits, typically IDs
      .replace(new RegExp('(viewer|editor)'), '') // remove 'viewer' or 'editor' prefixes
      .replace('//', '/') // replace double slashes with single slash
      .replace(new RegExp('^/'), '') // remove leading slashes
      .replace(new RegExp('/$'), ''); // remove trailing slashes
  }
}
