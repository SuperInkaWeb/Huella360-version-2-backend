package com.vet_saas.modules.delivery.service;

import com.vet_saas.core.exceptions.types.BusinessException;
import com.vet_saas.modules.delivery.model.Delivery;
import com.vet_saas.modules.delivery.model.DeliveryStatus;
import com.vet_saas.modules.delivery.repository.DeliveryRepository;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.repository.Lock;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    @Mock
    private DeliveryRepository deliveryRepository;

    @InjectMocks
    private DeliveryService deliveryService;

    @Test
    void aceptarPedido_leeElDeliveryConBloqueoPesimista() throws Exception {
        Method method = DeliveryRepository.class.getMethod("findByIdForUpdate", Long.class);

        assertEquals(LockModeType.PESSIMISTIC_WRITE, method.getAnnotation(Lock.class).value());
    }

    @Test
    void aceptarPedido_siOtraTransaccionYaLoAsigno_rechazaLaSolicitud() {
        Delivery deliveryYaAsignado = Delivery.builder()
                .id(10L)
                .estado(DeliveryStatus.REPARTIDOR_ASIGNADO)
                .build();
        when(deliveryRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(deliveryYaAsignado));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> deliveryService.aceptarPedido(10L, 200L)
        );

        assertEquals("El pedido ya no está disponible para asignación", exception.getMessage());
        verify(deliveryRepository).findByIdForUpdate(10L);
        verify(deliveryRepository, never()).save(deliveryYaAsignado);
    }
}
