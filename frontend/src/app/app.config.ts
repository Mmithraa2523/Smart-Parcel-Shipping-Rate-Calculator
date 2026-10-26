import { ApplicationConfig } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { SHIPPING_API, HttpShippingApi } from './shipping-api';

export const appConfig: ApplicationConfig = {
  providers: [
    provideHttpClient(),
    { provide: SHIPPING_API, useExisting: HttpShippingApi },
  ],
};
