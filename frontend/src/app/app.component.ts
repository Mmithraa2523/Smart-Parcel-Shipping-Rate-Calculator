import { ChangeDetectionStrategy, Component } from '@angular/core';
import { ShippingPageComponent } from './shipping-page.component';

@Component({
  selector: 'shipping-root',
  standalone: true,
  imports: [ShippingPageComponent],
  template: '<shipping-page />',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppComponent {}
