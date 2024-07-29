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
import invariant from 'tiny-invariant';

@Injectable({
  providedIn: 'root',
})
export class NetzAusblendenService {
  private ausgeblendeteKantenMap = new Map<number, number>();
  private ausgeblendeteKnotenMap = new Map<number, number>();

  private kanteAusblenden$$ = new Subject<number>();
  private kanteEinblenden$$ = new Subject<number>();
  private knotenAusblenden$$ = new Subject<number>();
  private knotenEinblenden$$ = new Subject<number>();

  get kanteAusblenden$(): Observable<number> {
    return this.kanteAusblenden$$.asObservable();
  }

  get kanteEinblenden$(): Observable<number> {
    return this.kanteEinblenden$$.asObservable();
  }

  get knotenAusblenden$(): Observable<number> {
    return this.knotenAusblenden$$.asObservable();
  }

  get knotenEinblenden$(): Observable<number> {
    return this.knotenEinblenden$$.asObservable();
  }

  get ausgeblendeteKnoten(): number[] {
    const result: number[] = [];
    this.ausgeblendeteKnotenMap.forEach((value, key) => {
      result.push(key);
    });
    return result;
  }

  get ausgeblendeteKanten(): number[] {
    const result: number[] = [];
    this.ausgeblendeteKantenMap.forEach((value, key) => {
      result.push(key);
    });
    return result;
  }

  public kanteAusblenden(id: number): void {
    if (!this.ausgeblendeteKantenMap.has(id)) {
      this.kanteAusblenden$$.next(id);
    }
    this.ausgeblendeteKantenMap.set(id, (this.ausgeblendeteKantenMap.get(id) || 0) + 1);
  }

  public kanteEinblenden(id: number): void {
    const currentNumber = this.ausgeblendeteKantenMap.get(id);
    invariant(currentNumber);
    if (currentNumber > 1) {
      this.ausgeblendeteKantenMap.set(id, currentNumber - 1);
    } else {
      this.ausgeblendeteKantenMap.delete(id);
      this.kanteEinblenden$$.next(id);
    }
  }

  public knotenAusblenden(id: number): void {
    if (!this.ausgeblendeteKnotenMap.has(id)) {
      this.knotenAusblenden$$.next(id);
    }
    this.ausgeblendeteKnotenMap.set(id, (this.ausgeblendeteKnotenMap.get(id) || 0) + 1);
  }

  public knotenEinblenden(id: number): void {
    const currentNumber = this.ausgeblendeteKnotenMap.get(id);
    invariant(currentNumber);
    if (currentNumber > 1) {
      this.ausgeblendeteKnotenMap.set(id, currentNumber - 1);
    } else {
      this.ausgeblendeteKnotenMap.delete(id);
      this.knotenEinblenden$$.next(id);
    }
  }
}
