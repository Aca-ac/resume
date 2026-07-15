package com.resume.module.resume.service;

import com.resume.common.BusinessException;
import com.resume.config.ExportProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ExportStorageServiceTest {

    @TempDir
    Path tempDir;

    private ExportStorageService service;

    @BeforeEach
    void setUp() throws Exception {
        ExportProperties props = new ExportProperties();
        props.setExportDir(tempDir.toString());
        props.setFileTtlHours(24);
        service = new ExportStorageService(props);
    }

    @Test
    void store_and_requireOwned_roundTrip() throws Exception {
        var stored = service.store(1L, 10L, 20L, "pdf", "pdf-bytes".getBytes(), "resume.pdf");
        assertNotNull(stored.exportId());
        assertEquals("pdf", stored.format());

        var owned = service.requireOwned(1L, stored.exportId());
        assertEquals(stored.exportId(), owned.exportId());
        assertArrayEquals("pdf-bytes".getBytes(), service.read(owned));
        assertEquals("resume.pdf", owned.filename());
    }

    @Test
    void requireOwned_wrongUser_throws404() throws Exception {
        var stored = service.store(1L, 10L, 20L, "word", "doc".getBytes(), null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.requireOwned(99L, stored.exportId()));
        assertEquals(404, ex.getCode());
    }

    @Test
    void formatExpiresAt_formatsOrNull() {
        assertNull(service.formatExpiresAt(null));
        assertTrue(service.formatExpiresAt(LocalDateTime.of(2026, 7, 15, 10, 0, 0))
                .startsWith("2026-07-15"));
    }
}
