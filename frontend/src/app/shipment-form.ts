import { NonNullableFormBuilder, Validators } from '@angular/forms';

export function createShipmentForm(formBuilder: NonNullableFormBuilder) {
  return formBuilder.group({
    weightKg: [1, [Validators.required, Validators.min(0.1), Validators.max(50)]],
    distanceKm: [10, [Validators.required, Validators.min(1), Validators.max(2500), Validators.pattern(/^\d+$/)]],
    tier: ['STANDARD', Validators.required],
    paymentMethod: ['CREDIT_CARD', Validators.required],
    simulatePaymentFailure: [false],
  });
}
