package com.vet_saas.core.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.vet_saas.core.exceptions.types.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

    private final Cloudinary cloudinary;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif");

    private static final Map<String, byte[]> MAGIC_BYTES = Map.of(
            "jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
            "png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47},
            "gif", new byte[]{0x47, 0x49, 0x46, 0x38},
            "webp", new byte[]{0x52, 0x49, 0x46, 0x46} // RIFF header
    );

    public String uploadFile(MultipartFile file, String folderName) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("El archivo de imagen esta vacio o es nulo");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("El archivo excede el tamano maximo de 10MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
            throw new BusinessException("Tipo de archivo no permitido: " + contentType
                    + ". Formatos aceptados: JPG, PNG, WEBP, GIF");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            String extension = getFileExtension(originalFilename).toLowerCase();
            if (!ALLOWED_EXTENSIONS.contains(extension)) {
                throw new BusinessException("Extension de archivo no permitida: " + extension);
            }
        }

        try {
            validateMagicBytes(file);

            String fileName = UUID.randomUUID().toString();
            String finalFolder = "vet-saas/" + (folderName.startsWith("/") ? folderName.substring(1) : folderName);

            Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", finalFolder,
                            "public_id", fileName,
                            "resource_type", "auto"));

            if (uploadResult == null || !uploadResult.containsKey("secure_url")) {
                throw new BusinessException("Cloudinary no devolvio una URL valida");
            }

            return uploadResult.get("secure_url").toString();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error subiendo imagen a Cloudinary: {}", e.getMessage(), e);
            throw new BusinessException("Error al subir la imagen a la nube");
        }
    }

    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.trim().isEmpty()) {
            return;
        }

        try {
            int vetSaasIndex = fileUrl.indexOf("vet-saas/");
            if (vetSaasIndex != -1) {
                String publicIdWithExt = fileUrl.substring(vetSaasIndex);
                String publicId = publicIdWithExt.contains(".")
                        ? publicIdWithExt.substring(0, publicIdWithExt.lastIndexOf('.'))
                        : publicIdWithExt;

                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            }
        } catch (Exception e) {
            log.warn("Failed to delete image from Cloudinary on cleanup: {}", e.getMessage());
        }
    }

    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex + 1);
    }

    private void validateMagicBytes(MultipartFile file) throws Exception {
        byte[] header = new byte[12];
        int bytesRead = file.getInputStream().read(header);
        if (bytesRead < 3) {
            return;
        }

        if (header[0] == (byte) 0xFF && header[1] == (byte) 0xD8 && header[2] == (byte) 0xFF) {
            return;
        }
        if (header[0] == (byte) 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47) {
            return;
        }
        if (header[0] == 0x47 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x38) {
            return;
        }
        if (header[0] == 0x52 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x46) {
            return;
        }

        throw new BusinessException("El contenido del archivo no coincide con una imagen valida");
    }
}