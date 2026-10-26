import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ShippingPageComponent } from './shipping-page.component';
import { Booking, Quote, SHIPPING_API, ShippingApi, Tier } from './shipping-api';

const tiers: Tier[] = [{
  code: 'STANDARD',
  name: 'Standard',
  deliveryEstimate: '3-5 business days',
  baseFee: 5,
  maximumDistanceKm: null,
}];
const quote: Quote = {
  tier: 'STANDARD',
  deliveryEstimate: '3-5 business days',
  baseFee: 5,
  weightCharge: 1.2,
  distanceCharge: 0.5,
  total: 6.7,
};
const booking: Booking = {
  bookingId: 'booking-test-123',
  status: 'CONFIRMED',
  tier: 'STANDARD',
  deliveryEstimate: '3-5 business days',
  total: 6.7,
  paymentReference: 'SIM-test',
};

describe('ShippingPageComponent', () => {
  let api: ShippingApi;

  beforeEach(async () => {
    api = {
      getTiers: vi.fn(() => of(tiers)),
      getQuote: vi.fn(() => of(quote)),
      createBooking: vi.fn(() => of(booking)),
    };
    await TestBed.configureTestingModule({
      imports: [ShippingPageComponent],
      providers: [{ provide: SHIPPING_API, useValue: api }],
    }).compileComponents();
  });

  it('renders available delivery tiers through the replaceable API adapter', async () => {
    const fixture = TestBed.createComponent(ShippingPageComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(api.getTiers).toHaveBeenCalledOnce();
    expect(fixture.nativeElement.textContent).toContain('Standard');
    expect(fixture.nativeElement.querySelector('[aria-label="Parcel weight in kilograms"]')).not.toBeNull();
  });

  it('marks invalid form fields and does not request an invalid quote', () => {
    const fixture = TestBed.createComponent(ShippingPageComponent);
    const component = fixture.componentInstance;
    component.form.controls.weightKg.setValue(0.1);
    component.form.controls.distanceKm.setValue(1);
    expect(component.form.valid).toBe(true);

    component.form.controls.distanceKm.setValue(2500);
    expect(component.form.valid).toBe(true);
    component.form.controls.distanceKm.setValue(2501);
    expect(component.form.valid).toBe(false);
    component.form.controls.distanceKm.setValue(10);
    component.form.controls.weightKg.setValue(51);

    component.getQuote();
    fixture.detectChanges();

    expect(component.form.controls.weightKg.touched).toBe(true);
    expect(api.getQuote).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('0.1 to 50.0 kg');
  });

  it('displays the server quote and sends valid normalized parcel details', async () => {
    const fixture = TestBed.createComponent(ShippingPageComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    component.form.controls.weightKg.setValue(2.5);
    component.form.controls.distanceKm.setValue(100);

    component.getQuote();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(api.getQuote).toHaveBeenCalledWith({
      weightKg: 2.5,
      distanceKm: 100,
      tier: 'STANDARD',
    });
    expect(fixture.nativeElement.textContent).toContain('$6.70');
    expect(fixture.nativeElement.textContent).toContain('Base rate');
  });

  it('creates a booking after a successful quote and displays its reference', async () => {
    const fixture = TestBed.createComponent(ShippingPageComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    component.form.controls.distanceKm.setValue(10);
    component.getQuote();
    fixture.detectChanges();
    component.createBooking();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(api.createBooking).toHaveBeenCalledWith({
      weightKg: 1,
      distanceKm: 10,
      tier: 'STANDARD',
      paymentMethod: 'CREDIT_CARD',
      simulatePaymentFailure: false,
    });
    expect(fixture.nativeElement.textContent).toContain('Booking confirmed.');
    expect(fixture.nativeElement.textContent).toContain('booking-test-123');
  });

  it('requires a quote before booking and displays payment decline errors', async () => {
    const fixture = TestBed.createComponent(ShippingPageComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    component.createBooking();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Calculate a quote before confirming');
    expect(api.createBooking).not.toHaveBeenCalled();

    api.createBooking = vi.fn(() => throwError(() => new HttpErrorResponse({
      status: 402,
      error: { code: 'PAYMENT_DECLINED', message: 'Simulated payment declined.' },
    })));
    component.getQuote();
    fixture.detectChanges();
    component.createBooking();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Simulated payment declined.');
    expect(component.booking()).toBeNull();
  });

  it('shows predictable Same-Day business rejections returned by the API', async () => {
    const fixture = TestBed.createComponent(ShippingPageComponent);
    fixture.detectChanges();
    fixture.componentInstance.form.controls.tier.setValue('SAME_DAY');
    fixture.componentInstance.form.controls.distanceKm.setValue(101);
    api.getQuote = vi.fn(() => throwError(() => new HttpErrorResponse({
      status: 422,
      error: {
        code: 'SAME_DAY_DISTANCE_EXCEEDED',
        message: 'Same-Day delivery is available only for distances up to 100 km.',
      },
    })));

    fixture.componentInstance.getQuote();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Same-Day delivery is available only for distances up to 100 km.');
    expect(fixture.componentInstance.quote()).toBeNull();
  });

  it('uses the injected replaceable adapter rather than constructing a transport client', () => {
    const alternate: ShippingApi = {
      getTiers: vi.fn(() => of(tiers)),
      getQuote: vi.fn(() => of({ ...quote, total: 8 })),
      createBooking: vi.fn(() => of(booking)),
    };
    TestBed.overrideProvider(SHIPPING_API, { useValue: alternate });
    const fixture = TestBed.createComponent(ShippingPageComponent);
    fixture.detectChanges();
    fixture.componentInstance.getQuote();
    expect(alternate.getQuote).toHaveBeenCalledOnce();
    expect(fixture.componentInstance.quote()?.total).toBe(8);
  });
});
