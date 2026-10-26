import { HttpErrorResponse } from '@angular/common/http';

export function shippingApiErrorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    const message: unknown = error.error?.message;
    if (typeof message === 'string' && message.length > 0) {
      return message;
    }
  }
  return 'Shipping is temporarily unavailable. Please try again.';
}
