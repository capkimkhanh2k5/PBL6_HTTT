package com.danasea.backend.modules.booking.infrastructure.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.danasea.backend.modules.booking.application.usecases.CancelBookingHoldUseCase;
import com.danasea.backend.modules.booking.application.usecases.ConfirmBookingUseCase;
import com.danasea.backend.modules.booking.application.usecases.CreateBookingHoldUseCase;
import com.danasea.backend.modules.booking.domain.ports.BookingRepositoryPort;
import com.danasea.backend.modules.booking.domain.ports.InventoryLockPort;
import com.danasea.backend.modules.booking.domain.ports.ServiceSlotPort;

@Configuration
public class BookingBeans {

    @Bean
    public CreateBookingHoldUseCase createBookingHoldUseCase(
            BookingRepositoryPort bookingRepository,
            InventoryLockPort inventoryLockPort,
            ServiceSlotPort serviceSlotPort) {
        return new CreateBookingHoldUseCase(bookingRepository, inventoryLockPort, serviceSlotPort);
    }

    @Bean
    public ConfirmBookingUseCase confirmBookingUseCase(
            BookingRepositoryPort bookingRepository,
            InventoryLockPort inventoryLockPort,
            ServiceSlotPort serviceSlotPort) {
        return new ConfirmBookingUseCase(bookingRepository, inventoryLockPort, serviceSlotPort);
    }

    @Bean
    public CancelBookingHoldUseCase cancelBookingHoldUseCase(
            BookingRepositoryPort bookingRepository,
            InventoryLockPort inventoryLockPort) {
        return new CancelBookingHoldUseCase(bookingRepository, inventoryLockPort);
    }

    @Bean
    public com.danasea.backend.modules.booking.application.usecases.GetBookingDetailUseCase getBookingDetailUseCase(
            BookingRepositoryPort bookingRepository) {
        return new com.danasea.backend.modules.booking.application.usecases.GetBookingDetailUseCase(bookingRepository);
    }

    @Bean
    public com.danasea.backend.modules.booking.application.usecases.GetCustomerBookingsUseCase getCustomerBookingsUseCase(
            BookingRepositoryPort bookingRepository) {
        return new com.danasea.backend.modules.booking.application.usecases.GetCustomerBookingsUseCase(bookingRepository);
    }

    @Bean
    public com.danasea.backend.modules.booking.application.usecases.GetVendorBookingsUseCase getVendorBookingsUseCase(
            BookingRepositoryPort bookingRepository,
            com.danasea.backend.modules.booking.domain.ports.VendorLookupPort vendorLookupPort) {
        return new com.danasea.backend.modules.booking.application.usecases.GetVendorBookingsUseCase(bookingRepository, vendorLookupPort);
    }

    @Bean
    public com.danasea.backend.modules.booking.application.usecases.CancelBookingUseCase cancelBookingUseCase(
            BookingRepositoryPort bookingRepository,
            ServiceSlotPort serviceSlotPort,
            InventoryLockPort inventoryLockPort) {
        return new com.danasea.backend.modules.booking.application.usecases.CancelBookingUseCase(
                bookingRepository,
                serviceSlotPort,
                inventoryLockPort
        );
    }
}

