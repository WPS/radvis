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

import { HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';

@Injectable()
export class RadvisHttpInterceptor implements HttpInterceptor {
  constructor(private errorHandlingService: ErrorHandlingService) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(req).pipe(
      catchError(err => {
        if (err instanceof HttpErrorResponse && err.error instanceof Blob && err.error.type === 'application/json') {
          // https://github.com/angular/angular/issues/19888
          // When request of type Blob, the error is also in Blob instead of object of the json data
          // https://stackoverflow.com/questions/48500822/how-to-handle-error-for-response-type-blob-in-httprequest (Antwort 3)
          return new Promise<any>((resolve, reject) => {
            const reader = new FileReader();
            reader.onload = (e: Event): void => {
              try {
                const errmsg = JSON.parse((e.target as any).result);
                reject(
                  new HttpErrorResponse({
                    error: errmsg,
                    headers: err.headers,
                    status: err.status,
                    statusText: err.statusText,
                    url: err.url || undefined,
                  })
                );
              } catch (e1: unknown) {
                reject(err);
              }
            };
            reader.onerror = (): void => {
              reject(err);
            };
            reader.readAsText(err.error);
          });
        }
        return throwError(err);
      }),
      catchError(
        (error: HttpErrorResponse): Observable<any> => {
          if (error.status === 401) {
            window.location.replace(window.location.origin);
          }
          this.errorHandlingService.handleHttpError(error);
          return throwError(error);
        }
      )
    );
  }
}
