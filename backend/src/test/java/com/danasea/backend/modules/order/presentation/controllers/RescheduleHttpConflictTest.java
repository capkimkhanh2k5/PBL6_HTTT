package com.danasea.backend.modules.order.presentation.controllers;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.danasea.backend.modules.order.application.usecases.*;
import com.danasea.backend.shared.presentation.GlobalExceptionHandler;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

class RescheduleHttpConflictTest {
    @Test
    void conflictPreserves409AndStableCode() throws Exception {
        var confirm = mock(ConfirmSubOrderRescheduleUseCase.class);
        when(confirm.execute(any(), any(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Inventory changed"));
        var mvc =
                MockMvcBuilders.standaloneSetup(
                                new SubOrderRescheduleController(
                                        mock(GetRescheduleOptionsUseCase.class),
                                        mock(CreateRescheduleProposalUseCase.class),
                                        confirm))
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .build();
        mvc.perform(
                        post("/api/sub-orders/" + UUID.randomUUID() + "/reschedule")
                                .header("Idempotency-Key", "test-key-123")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"targetSlotId\":\""
                                                + UUID.randomUUID()
                                                + "\",\"expectedVersion\":0}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESCHEDULE_CONFLICT"));
    }
}
