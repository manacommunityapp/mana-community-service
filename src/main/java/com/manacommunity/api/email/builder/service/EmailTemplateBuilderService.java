package com.manacommunity.api.email.builder.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.email.builder.dto.AssetUploadResponse;
import com.manacommunity.api.email.builder.dto.EmailTemplateRequest;
import com.manacommunity.api.email.builder.entity.CustomEmailTemplate;
import com.manacommunity.api.email.builder.entity.TemplateStatus;
import com.manacommunity.api.email.builder.repository.CustomEmailTemplateRepository;
import com.manacommunity.api.exception.InvalidFileUploadException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailTemplateBuilderService {

    private final CustomEmailTemplateRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/png", "image/jpeg", "image/gif", "image/webp", "image/svg+xml"
    );
    private static final long MAX_ASSET_SIZE = 5 * 1024 * 1024;

    @Value("${app.email.assets.upload-dir:uploads/email-assets}")
    private String uploadDir;

    @Value("${app.email.assets.base-url:/api/email/templates/assets/}")
    private String assetBaseUrl;

    public List<CustomEmailTemplate> listByCommunity(Long communityId) {
        return repository.findByCommunityIdOrderByUpdatedAtDesc(communityId);
    }

    @Transactional
    public CustomEmailTemplate save(EmailTemplateRequest request) {
        CustomEmailTemplate entity;

        if (request.id() != null) {
            entity = repository.findById(request.id())
                    .orElseThrow(() -> new ResourceNotFoundException("Email template", request.id()));
            entity.setVersion(entity.getVersion() + 1);
        } else {
            entity = CustomEmailTemplate.builder()
                    .version(1)
                    .build();
        }

        String name = request.templateName() != null ? request.templateName() : request.subject();
        entity.setCommunityId(request.communityId());
        entity.setName(name);
        entity.setSubject(request.subject());
        entity.setHtml(request.html());
        entity.setCss(request.css());
        entity.setLayoutJson(serializeJson(request.jsonLayout() != null ? request.jsonLayout() : request.layoutJson()));
        entity.setGeneratedCss(request.generatedCss());
        entity.setStatus(request.status() != null ? request.status() : TemplateStatus.DRAFT);
        entity.setCategory(request.category());
        entity.setTags(request.tags());
        entity.setModuleKey(request.moduleKey());
        entity.setMenuKey(request.menuKey());
        entity.setMenuLabel(request.menuLabel());
        entity.setSubMenuKey(request.subMenuKey());
        entity.setSubMenuLabel(request.subMenuLabel());
        entity.setUseCase(request.useCase());
        entity.setTriggerKey(request.triggerKey());
        entity.setTemplateKey(request.templateKey());
        entity.setThemeName(request.themeName());
        entity.setThemeJson(request.themeJson());

        return repository.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Email template", id);
        }
        repository.deleteById(id);
    }

    public AssetUploadResponse uploadAsset(MultipartFile file, Long communityId) {
        if (file.isEmpty()) {
            throw new InvalidFileUploadException("File is empty");
        }
        if (file.getSize() > MAX_ASSET_SIZE) {
            throw new InvalidFileUploadException("File exceeds maximum size of 5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new InvalidFileUploadException("Only image files (PNG, JPEG, GIF, WebP, SVG) are allowed");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        String key = communityId + "/" + UUID.randomUUID() + extension;

        try {
            Path targetPath = Paths.get(uploadDir, key);
            Files.createDirectories(targetPath.getParent());
            Files.write(targetPath, file.getBytes());
        } catch (IOException e) {
            throw new InvalidFileUploadException("Failed to save uploaded file");
        }

        String url = assetBaseUrl + key;
        return new AssetUploadResponse(url, key);
    }

    public Path resolveAssetPath(String key) {
        Path path = Paths.get(uploadDir, key).normalize();
        if (!path.startsWith(Paths.get(uploadDir).normalize())) {
            throw new ResourceNotFoundException("Asset", "key", key);
        }
        if (!Files.exists(path)) {
            throw new ResourceNotFoundException("Asset", "key", key);
        }
        return path;
    }

    private String serializeJson(Object obj) {
        if (obj == null) return null;
        if (obj instanceof String s) return s;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return null;
        }
    }
}
