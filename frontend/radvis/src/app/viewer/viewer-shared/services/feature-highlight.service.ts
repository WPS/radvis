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

import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { RadVisFeature } from 'src/app/shared/models/rad-vis-feature';

@Injectable({
  providedIn: 'root',
})
export class FeatureHighlightService {
  private highlightedFeature$$ = new Subject<RadVisFeature>();
  private unhighlightedFeature$$ = new Subject<RadVisFeature>();

  public highlight(f: RadVisFeature): void {
    this.highlightedFeature$$.next(f);
  }

  public unhighlight(f: RadVisFeature): void {
    this.unhighlightedFeature$$.next(f);
  }

  public get highlightedFeature$(): Observable<RadVisFeature> {
    return this.highlightedFeature$$.asObservable();
  }

  public get unhighlightedFeature$(): Observable<RadVisFeature> {
    return this.unhighlightedFeature$$.asObservable();
  }
}
