package com.danasea.backend.modules.booking.infrastructure.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
}
