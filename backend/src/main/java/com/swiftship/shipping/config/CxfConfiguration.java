package com.swiftship.shipping.config;

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import com.swiftship.shipping.api.BookingQueryResource;
import com.swiftship.shipping.api.BookingResource;
import com.swiftship.shipping.api.QuoteResource;
import com.swiftship.shipping.api.ShippingExceptionMapper;
import com.swiftship.shipping.api.TierResource;
import com.swiftship.shipping.api.ValidationExceptionMapper;
import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.validation.JAXRSBeanValidationInInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class CxfConfiguration {
    @Bean
    public Server jaxRsServer(
            Bus bus,
            QuoteResource quoteResource,
            TierResource tierResource,
            BookingResource bookingResource,
            BookingQueryResource bookingQueryResource,
            ShippingExceptionMapper shippingExceptionMapper,
            ValidationExceptionMapper validationExceptionMapper) {
        JAXRSServerFactoryBean factory = new JAXRSServerFactoryBean();
        factory.setBus(bus);
        factory.setAddress("/");
        factory.setServiceBeans(List.of(quoteResource, tierResource, bookingResource, bookingQueryResource));
        factory.setProviders(List.of(new JacksonJsonProvider(), shippingExceptionMapper, validationExceptionMapper));
        factory.setInInterceptors(List.of(new JAXRSBeanValidationInInterceptor()));
        return factory.create();
    }
}
