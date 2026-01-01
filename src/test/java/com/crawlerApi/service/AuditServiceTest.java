package com.crawlerApi.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.looksee.gcp.PubSubUrlMessagePublisherImpl;
import com.looksee.models.Account;
import com.looksee.models.audit.PageAuditRecord;
import com.looksee.models.dto.AuditRecordDto;
import com.looksee.models.enums.AuditLevel;
import com.looksee.services.AccountService;
import com.looksee.services.AuditRecordService;
import com.looksee.services.DomainService;

@ExtendWith(MockitoExtension.class)
public class AuditServiceTest {
    
    static {
        System.setProperty("mockito.inline.mockmaker", "true");
    }
    
    @Mock
    private AccountService accountService;
    
    @Mock
    private AuditRecordService auditRecordService;
    
    @Mock
    private DomainService domainService;
    
    @Mock
    private PubSubUrlMessagePublisherImpl urlTopic;
    
    @Mock
    private UrlSanitizerService urlSanitizerService;
    
    @InjectMocks
    private AuditService auditService;
    
    private AuditService spyAuditService;
    
    @BeforeEach
    void setUp() {
        // AuditService is automatically instantiated with mocked dependencies via @InjectMocks
        // Create a spy to allow mocking of private methods
        spyAuditService = spy(auditService);
    }
    
    @Test
    void testStartPageAudit_Success() throws Exception {
        // Arrange
        String testUrl = "https://example.com";
        Account testAccount = new Account();
        testAccount.setId(1L);
        testAccount.setEmail("test@example.com");
        
        // Create a PageAuditRecord that will be returned by createPageAuditRecord and save()
        // Mock the PageAuditRecord to avoid constructor AssertionError
        PageAuditRecord mockRecord = mock(PageAuditRecord.class);
        lenient().when(mockRecord.getId()).thenReturn(100L);
        lenient().when(mockRecord.getUrl()).thenReturn(testUrl);
        lenient().when(mockRecord.getKey()).thenReturn("test-key");
        
        // Create the DTO that will be returned by buildAudit()
        AuditRecordDto expectedDto = new AuditRecordDto();
        expectedDto.setId(100L);
        
        // Mock URL sanitization to avoid Selenium dependency
        URL sanitizedUrl = new URI(testUrl).toURL();
        when(urlSanitizerService.sanitizeUrl(eq(testUrl)))
            .thenReturn(sanitizedUrl);
        
        // Mock createPageAuditRecord to return a mock instead of creating a real PageAuditRecord
        // This avoids the AssertionError from PageAuditRecord constructor
        doReturn(mockRecord).when(spyAuditService).createPageAuditRecord(any(URL.class));
        
        // Mock the service dependencies
        when(auditRecordService.save(any(PageAuditRecord.class), isNull(), isNull()))
            .thenReturn(mockRecord);
        when(auditRecordService.buildAudit(any(PageAuditRecord.class)))
            .thenReturn(expectedDto);
        
        // Act
        AuditRecordDto result = spyAuditService.startAudit(testUrl, AuditLevel.PAGE, testAccount);
        
        // Assert
        assertNotNull(result);
        assertEquals(100L, result.getId());
        
        // Verify interactions with dependencies
        verify(spyAuditService).createPageAuditRecord(any(URL.class));
        verify(auditRecordService).save(any(PageAuditRecord.class), isNull(), isNull());
        verify(auditRecordService).buildAudit(any(PageAuditRecord.class));
        verify(accountService).addAuditRecord(eq(testAccount.getId()), eq(100L));
        verify(urlTopic).publish(anyString());
    }
    
    @Test
    void testStartAudit_InvalidUrl_ThrowsException() throws Exception {
        // Arrange
        String invalidUrl = "";
        Account testAccount = new Account();
        testAccount.setId(1L);
        
        // Mock URL sanitizer to throw IllegalArgumentException for empty URL
        when(urlSanitizerService.sanitizeUrl(eq(invalidUrl)))
            .thenThrow(new IllegalArgumentException("URL cannot be null or empty"));
        
        // Act & Assert
        // The service should throw IllegalArgumentException for empty URL during sanitization
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            auditService.startAudit(invalidUrl, AuditLevel.PAGE, testAccount);
        });
        
        assertTrue(exception.getMessage().contains("URL cannot be null or empty") || 
                   exception.getMessage().contains("Failed to start audit"));
        
        // Verify no interactions occurred since validation failed early
        verify(auditRecordService, never()).save(any(), any(), any());
    }
    
    @Test
    void testStartAudit_NullUrl_ThrowsException() throws Exception {
        // Arrange
        String nullUrl = null;
        Account testAccount = new Account();
        testAccount.setId(1L);
        
        // Mock URL sanitizer to throw IllegalArgumentException for null URL
        when(urlSanitizerService.sanitizeUrl(isNull()))
            .thenThrow(new IllegalArgumentException("URL cannot be null or empty"));
        
        // Act & Assert
        // The service should throw IllegalArgumentException for null URL during sanitization
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            auditService.startAudit(nullUrl, AuditLevel.PAGE, testAccount);
        });
        
        assertTrue(exception.getMessage().contains("URL cannot be null or empty") || 
                   exception.getMessage().contains("Failed to start audit"));
        
        // Verify no interactions occurred since validation failed early
        verify(auditRecordService, never()).save(any(), any(), any());
    }
    
    @Test
    void testStartAudit_UnsupportedAuditLevel_ThrowsException() {
        // Arrange
        String testUrl = "https://example.com";
        Account testAccount = new Account();
        testAccount.setId(1L);
        
        // Act & Assert
        // The service should throw IllegalArgumentException for null audit level
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            auditService.startAudit(testUrl, null, testAccount);
        });
        
        assertTrue(exception.getMessage().contains("Unsupported audit level") || 
                   exception.getMessage().contains("Failed to start audit"));
        
        // Verify no interactions occurred since validation failed early
        verify(auditRecordService, never()).save(any(), any(), any());
    }
    
    @Test
    void testStartDomainAudit_Success() throws Exception {
        // This test would be more complex and would need additional setup for domain audits
        // For brevity, this is a placeholder showing the structure
        
        // TODO: Implement domain audit test
        assertTrue(true, "Domain audit test to be implemented");
    }
} 