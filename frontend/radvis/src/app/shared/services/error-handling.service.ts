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

import { HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';

@Injectable({
  providedIn: 'root',
})
export class ErrorHandlingService {
  public static readonly DEFAULT_SERVER_MESSAGE = 'Es ist ein unbekannter Server-Fehler aufgetreten.';
  private readonly defaultUserMessage = 'Es ist ein unerwarteter Fehler aufgetreten';

  constructor(private notifyUserService: NotifyUserService) {}

  public handleError(error: Error, userMessage: string = this.defaultUserMessage): void {
    console.error(error);
    this.notifyUserService.warn(userMessage);
  }

  public handleHttpError(error: HttpErrorResponse): void {
    this.notifyUserService.warn(this.createMessageByStatusCode(error));
  }

  private createMessageByStatusCode(error: HttpErrorResponse): string {
    switch (error?.status) {
      case 0:
        return 'Es ist ein Verbindungsfehler aufgetreten.';
      case 409:
        return error?.error;
      case 403:
      case 400:
      case 503:
        return error?.error?.message ?? error.error.detail;
      default:
        return ErrorHandlingService.DEFAULT_SERVER_MESSAGE;
    }
  }
}
