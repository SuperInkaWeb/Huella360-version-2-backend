package com.vet_saas.core.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.vet_saas.core.exceptions.types.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StorageServiceTest {

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    @Mock
    private MultipartFile file;

    private StorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new StorageService(cloudinary);
        lenient().when(cloudinary.uploader()).thenReturn(uploader);
    }

    @Test
    void uploadFile_nullFile_throwsBusinessException() {
        assertThrows(BusinessException.class, () -> storageService.uploadFile(null, "test"));
    }

    @Test
    void uploadFile_emptyFile_throwsBusinessException() throws IOException {
        when(file.isEmpty()).thenReturn(true);
        assertThrows(BusinessException.class, () -> storageService.uploadFile(file, "test"));
    }

    @Test
    void uploadFile_oversizedFile_throwsBusinessException() throws IOException {
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(11 * 1024 * 1024L);

        assertThrows(BusinessException.class, () -> storageService.uploadFile(file, "test"));
    }

    @Test
    void uploadFile_invalidContentType_throwsBusinessException() throws IOException {
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L);
        when(file.getContentType()).thenReturn("application/pdf");

        assertThrows(BusinessException.class, () -> storageService.uploadFile(file, "test"));
    }

    @Test
    void uploadFile_nullContentType_throwsBusinessException() throws IOException {
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L);
        when(file.getContentType()).thenReturn(null);

        assertThrows(BusinessException.class, () -> storageService.uploadFile(file, "test"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void uploadFile_validJpeg_returnsUrl() throws Exception {
        byte[] jpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10};
        InputStream jpegStream = new ByteArrayInputStream(jpegBytes);

        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn((long) jpegBytes.length);
        when(file.getContentType()).thenReturn("image/jpeg");
        when(file.getOriginalFilename()).thenReturn("photo.jpg");
        when(file.getInputStream()).thenReturn(jpegStream);
        when(file.getBytes()).thenReturn(jpegBytes);
        when(uploader.upload(any(byte[].class), any(Map.class)))
                .thenReturn(Map.of("secure_url", "https://res.cloudinary.com/test/image/upload/v1/photo.jpg"));

        String url = storageService.uploadFile(file, "mascotas");

        assertEquals("https://res.cloudinary.com/test/image/upload/v1/photo.jpg", url);
    }

    @Test
    void uploadFile_cloudinaryReturnsNull_throwsBusinessException() throws Exception {
        byte[] pngBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A};
        InputStream pngStream = new ByteArrayInputStream(pngBytes);

        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn((long) pngBytes.length);
        when(file.getContentType()).thenReturn("image/png");
        when(file.getOriginalFilename()).thenReturn("photo.png");
        when(file.getInputStream()).thenReturn(pngStream);
        when(file.getBytes()).thenReturn(pngBytes);
        when(uploader.upload(any(byte[].class), any(Map.class))).thenReturn(null);

        assertThrows(BusinessException.class, () -> storageService.uploadFile(file, "test"));
    }

    @Test
    void deleteFile_nullUrl_doesNotThrow() {
        assertDoesNotThrow(() -> storageService.deleteFile(null));
    }

    @Test
    void deleteFile_emptyUrl_doesNotThrow() {
        assertDoesNotThrow(() -> storageService.deleteFile("  "));
    }

    @Test
    void deleteFile_validUrl_callsCloudinaryDestroy() throws Exception {
        when(uploader.destroy(anyString(), any(Map.class))).thenReturn(Map.of("result", "ok"));
        storageService.deleteFile("https://res.cloudinary.com/test/vet-saas/mascotas/photo.jpg");
        verify(uploader, times(1)).destroy(eq("vet-saas/mascotas/photo"), any(Map.class));
    }

    @Test
    void deleteFile_urlWithoutVetsaas_doesNotCallCloudinary() throws Exception {
        storageService.deleteFile("https://example.com/other/file.jpg");
        verify(uploader, never()).destroy(anyString(), any(Map.class));
    }
}
