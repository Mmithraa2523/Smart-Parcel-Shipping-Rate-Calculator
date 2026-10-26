import { HttpClient } from '@angular/common/http';
import { InjectionToken, Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface Tier {
  code: string;
  name: string;
  deliveryEstimate: string;
  baseFee: number;
  maximumDistanceKm: number | null;
}

export interface QuoteRequest {
  weightKg: number;
  distanceKm: number;
  tier: string;
}

export interface Quote {
  tier: string;
  deliveryEstimate: string;
  baseFee: number;
  weightCharge: number;
  distanceCharge: number;
  total: number;
}

export type PaymentMethod = 'CREDIT_CARD' | 'CORPORATE_ACCOUNT_BILLING';

export interface BookingRequest extends QuoteRequest {
  paymentMethod: PaymentMethod;
  simulatePaymentFailure: boolean;
}

export interface Booking {
  bookingId: string;
  status: string;
  tier: string;
  deliveryEstimate: string;
  total: number;
  paymentReference: string;
}

export interface ShippingApi {
  getTiers(): Observable<Tier[]>;
  getQuote(request: QuoteRequest): Observable<Quote>;
  createBooking(request: BookingRequest): Observable<Booking>;
}

export const SHIPPING_API = new InjectionToken<ShippingApi>('SHIPPING_API');

@Injectable({ providedIn: 'root' })
export class HttpShippingApi implements ShippingApi {
  private readonly http = inject(HttpClient);

  getTiers(): Observable<Tier[]> {
    return this.http.get<Tier[]>('/api/tiers');
  }

  getQuote(request: QuoteRequest): Observable<Quote> {
    return this.http.post<Quote>('/api/quotes', request);
  }

  createBooking(request: BookingRequest): Observable<Booking> {
    return this.http.post<Booking>('/api/bookings', request);
  }
}
