import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { createShipmentForm } from './shipment-form';
import { Booking, BookingRequest, Quote, SHIPPING_API, Tier } from './shipping-api';
import { shippingApiErrorMessage } from './shipping-api-error';

@Component({
  selector: 'shipping-page',
  standalone: true,
  imports: [CurrencyPipe, ReactiveFormsModule],
  templateUrl: './shipping-page.component.html',
  styleUrl: './shipping-page.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ShippingPageComponent implements OnInit {
  private readonly api = inject(SHIPPING_API);
  private readonly formBuilder = inject(FormBuilder).nonNullable;
  private readonly destroyRef = inject(DestroyRef);

  readonly tiers = signal<Tier[]>([]);
  readonly quote = signal<Quote | null>(null);
  readonly booking = signal<Booking | null>(null);
  readonly errorMessage = signal<string | null>(null);
  readonly isLoadingTiers = signal(false);
  readonly isQuoting = signal(false);
  readonly isBooking = signal(false);

  readonly form = createShipmentForm(this.formBuilder);

  ngOnInit(): void {
    this.isLoadingTiers.set(true);
    this.api.getTiers().pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.isLoadingTiers.set(false)),
    ).subscribe({
      next: tiers => this.tiers.set(tiers),
      error: error => this.errorMessage.set(shippingApiErrorMessage(error)),
    });
  }

  invalidateQuote(): void {
    this.quote.set(null);
    this.booking.set(null);
    this.errorMessage.set(null);
  }

  getQuote(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.errorMessage.set('Check the highlighted fields and try again.');
      return;
    }

    this.errorMessage.set(null);
    this.booking.set(null);
    this.isQuoting.set(true);
    const request = this.quoteRequest();
    this.api.getQuote(request).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.isQuoting.set(false)),
    ).subscribe({
      next: result => this.quote.set(result),
      error: error => {
        this.quote.set(null);
        this.errorMessage.set(shippingApiErrorMessage(error));
      },
    });
  }

  createBooking(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.errorMessage.set('Check the highlighted fields and try again.');
      return;
    }
    if (!this.quote()) {
      this.errorMessage.set('Calculate a quote before confirming your booking.');
      return;
    }

    const formValue = this.form.getRawValue();
    const request: BookingRequest = {
      ...this.quoteRequest(),
      paymentMethod: formValue.paymentMethod as BookingRequest['paymentMethod'],
      simulatePaymentFailure: formValue.simulatePaymentFailure,
    };
    this.errorMessage.set(null);
    this.isBooking.set(true);
    this.api.createBooking(request).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.isBooking.set(false)),
    ).subscribe({
      next: result => this.booking.set(result),
      error: error => {
        this.booking.set(null);
        this.errorMessage.set(shippingApiErrorMessage(error));
      },
    });
  }

  fieldError(field: 'weightKg' | 'distanceKm' | 'tier'): string | null {
    const control = this.form.controls[field];
    if (!control.touched || !control.errors) {
      return null;
    }
    if (control.hasError('required')) {
      return field === 'tier' ? 'Choose a delivery speed.' : 'This field is required.';
    }
    if (field === 'weightKg') {
      return 'Enter a weight from 0.1 to 50.0 kg.';
    }
    if (field === 'distanceKm') {
      return 'Enter a whole-number distance from 1 to 2,500 km.';
    }
    return 'Choose a delivery speed.';
  }

  private quoteRequest(): { weightKg: number; distanceKm: number; tier: string } {
    const value = this.form.getRawValue();
    return {
      weightKg: Number(value.weightKg),
      distanceKm: Number(value.distanceKm),
      tier: value.tier,
    };
  }

}
