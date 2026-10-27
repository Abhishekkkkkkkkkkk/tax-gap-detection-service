package com.avega.tgd.service;

import com.avega.tgd.domain.entity.ExceptionEntity;
import com.avega.tgd.domain.enums.Severity;
import com.avega.tgd.repository.ExceptionRepository;
import com.avega.tgd.service.impl.ExceptionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Exception Service Unit Tests")
class ExceptionServiceTest {

    @Mock
    private ExceptionRepository exceptionRepository;

    private ExceptionService exceptionService;

    @BeforeEach
    void setUp() {
        exceptionService = new ExceptionServiceImpl(exceptionRepository);
    }

    @Test
    @DisplayName("createException should persist exception record with timestamp")
    void testCreateException() {
        when(exceptionRepository.save(any(ExceptionEntity.class))).thenAnswer(i -> i.getArgument(0));

        ExceptionEntity exception = exceptionService.createException(
                "TX-100", "CUST-1", "RULE_HIGH", Severity.HIGH, "Exceeded limit");

        assertNotNull(exception);
        assertEquals("TX-100", exception.getTransactionId());
        assertEquals("CUST-1", exception.getCustomerId());
        assertEquals(Severity.HIGH, exception.getSeverity());
        assertNotNull(exception.getTimestamp());
        verify(exceptionRepository).save(any(ExceptionEntity.class));
    }

    @Test
    @DisplayName("getExceptions with specification should query repository")
    void testGetExceptionsWithFilters() {
        ExceptionEntity ex = new ExceptionEntity("TX-1", "CUST-1", "RULE", Severity.HIGH, "msg", null);
        Page<ExceptionEntity> page = new PageImpl<>(List.of(ex));

        when(exceptionRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(page);

        Page<ExceptionEntity> result = exceptionService.getExceptions("CUST-1", Severity.HIGH, "RULE", PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("getExceptionById should return record or throw")
    void testGetExceptionById() {
        ExceptionEntity ex = new ExceptionEntity("TX-1", "CUST-1", "RULE", Severity.HIGH, "msg", null);
        when(exceptionRepository.findById(1L)).thenReturn(Optional.of(ex));

        ExceptionEntity found = exceptionService.getExceptionById(1L);
        assertNotNull(found);
        assertEquals("TX-1", found.getTransactionId());
    }
}
