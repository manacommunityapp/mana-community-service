package com.manacommunity.api.email;

import com.manacommunity.api.dto.email.EmailTemplateManagementDtos.*;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.*;
import com.manacommunity.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class EmailTemplateManagementService {

    private static final Pattern RAW_TOKEN = Pattern.compile("\\{\\{\\{\\s*([A-Za-z0-9_.-]+)\\s*}}}");
    private static final Pattern ESCAPED_TOKEN = Pattern.compile("\\{\\{\\s*([A-Za-z0-9_.-]+)\\s*}}");

    private final CommunityRepository communityRepository;
    private final CommunityBrandingRepository brandingRepository;
    private final EmailHeaderRepository headerRepository;
    private final EmailFooterRepository footerRepository;
    private final EmailTemplateDefinitionRepository templateRepository;
    private final EmailTemplateVersionRepository versionRepository;
    private final EmailDynamicFieldRepository dynamicFieldRepository;

    @Transactional(readOnly = true)
    public BrandingResponse getBranding(Long communityId) {
        Community community = requireCommunity(communityId);
        return brandingRepository.findByCommunity_Id(communityId)
                .map(this::toBrandingResponse)
                .orElseGet(() -> new BrandingResponse(
                        null,
                        community.getId(),
                        community.getName(),
                        null,
                        "#2563eb",
                        "#0f172a",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                ));
    }

    @Transactional
    public BrandingResponse saveBranding(Long communityId, BrandingRequest request) {
        Community community = requireCommunity(communityId);
        CommunityBranding branding = brandingRepository.findByCommunity_Id(communityId)
                .orElseGet(() -> CommunityBranding.builder().community(community).build());

        branding.setLogoUrl(request.logoUrl());
        branding.setPrimaryColor(defaultIfBlank(request.primaryColor(), "#2563eb"));
        branding.setSecondaryColor(defaultIfBlank(request.secondaryColor(), "#0f172a"));
        branding.setFontFamily(request.fontFamily());
        branding.setButtonStyle(request.buttonStyle());
        branding.setBannerUrl(request.bannerUrl());
        branding.setFacebookUrl(request.facebookUrl());
        branding.setInstagramUrl(request.instagramUrl());
        branding.setWebsiteUrl(request.websiteUrl());
        branding.setSupportEmail(request.supportEmail());
        branding.setSupportPhone(request.supportPhone());

        return toBrandingResponse(brandingRepository.save(branding));
    }

    @Transactional(readOnly = true)
    public List<HeaderFooterResponse> listHeaders(Long communityId) {
        requireCommunity(communityId);
        return headerRepository.findByCommunity_IdOrderByNameAscIdAsc(communityId)
                .stream()
                .map(this::toHeaderResponse)
                .toList();
    }

    @Transactional
    public HeaderFooterResponse createHeader(Long communityId, HeaderFooterRequest request) {
        Community community = requireCommunity(communityId);
        EmailHeader header = EmailHeader.builder()
                .community(community)
                .name(request.name())
                .htmlContent(request.htmlContent())
                .layoutJson(request.layoutJson())
                .active(request.active() == null || request.active())
                .createdBy(request.createdBy())
                .version(1)
                .build();
        return toHeaderResponse(headerRepository.save(header));
    }

    @Transactional
    public HeaderFooterResponse updateHeader(Long communityId, Long headerId, HeaderFooterRequest request) {
        EmailHeader header = headerRepository.findByIdAndCommunity_Id(headerId, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Email header", headerId));
        header.setName(request.name());
        header.setHtmlContent(request.htmlContent());
        header.setLayoutJson(request.layoutJson());
        header.setActive(request.active() == null || request.active());
        header.setCreatedBy(request.createdBy());
        header.setVersion(header.getVersion() == null ? 1 : header.getVersion() + 1);
        return toHeaderResponse(headerRepository.save(header));
    }

    @Transactional(readOnly = true)
    public List<HeaderFooterResponse> listFooters(Long communityId) {
        requireCommunity(communityId);
        return footerRepository.findByCommunity_IdOrderByNameAscIdAsc(communityId)
                .stream()
                .map(this::toFooterResponse)
                .toList();
    }

    @Transactional
    public HeaderFooterResponse createFooter(Long communityId, HeaderFooterRequest request) {
        Community community = requireCommunity(communityId);
        EmailFooter footer = EmailFooter.builder()
                .community(community)
                .name(request.name())
                .htmlContent(request.htmlContent())
                .layoutJson(request.layoutJson())
                .active(request.active() == null || request.active())
                .createdBy(request.createdBy())
                .version(1)
                .build();
        return toFooterResponse(footerRepository.save(footer));
    }

    @Transactional
    public HeaderFooterResponse updateFooter(Long communityId, Long footerId, HeaderFooterRequest request) {
        EmailFooter footer = footerRepository.findByIdAndCommunity_Id(footerId, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Email footer", footerId));
        footer.setName(request.name());
        footer.setHtmlContent(request.htmlContent());
        footer.setLayoutJson(request.layoutJson());
        footer.setActive(request.active() == null || request.active());
        footer.setCreatedBy(request.createdBy());
        footer.setVersion(footer.getVersion() == null ? 1 : footer.getVersion() + 1);
        return toFooterResponse(footerRepository.save(footer));
    }

    @Transactional(readOnly = true)
    public List<TemplateSummaryResponse> listTemplates(Long communityId) {
        requireCommunity(communityId);
        return templateRepository.findByCommunity_IdOrderByTemplateCodeAsc(communityId)
                .stream()
                .map(this::toTemplateSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TemplateSummaryResponse> listAllTemplates() {
        return templateRepository.findAllByOrderByCommunity_IdAscTemplateCodeAsc()
                .stream()
                .map(this::toTemplateSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public TemplateDetailResponse getTemplate(Long communityId, String templateCode) {
        return templateRepository.findByCommunity_IdAndTemplateCodeIgnoreCase(communityId, templateCode)
                .map(this::toTemplateDetail)
                .orElseThrow(() -> new ResourceNotFoundException("Email template", "templateCode", templateCode));
    }

    @Transactional
    public TemplateDetailResponse createTemplate(Long communityId, TemplateRequest request) {
        Community community = requireCommunity(communityId);
        templateRepository.findByCommunity_IdAndTemplateCodeIgnoreCase(communityId, request.templateCode())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Template code already exists for this community: " + request.templateCode());
                });

        EmailTemplateDefinition template = EmailTemplateDefinition.builder()
                .community(community)
                .templateCode(normalizeCode(request.templateCode()))
                .templateName(request.templateName())
                .category(request.category())
                .subject(request.subject())
                .htmlContent(request.htmlContent())
                .layoutJson(request.layoutJson())
                .header(resolveHeader(communityId, request.headerId()))
                .footer(resolveFooter(communityId, request.footerId()))
                .status(request.status() == null ? EmailTemplateStatus.DRAFT : request.status())
                .version(1)
                .createdBy(request.createdBy())
                .build();

        EmailTemplateDefinition saved = templateRepository.save(template);
        snapshot(saved, request.notes());
        return toTemplateDetail(saved);
    }

    @Transactional
    public TemplateDetailResponse updateTemplate(Long communityId, Long templateId, TemplateRequest request) {
        EmailTemplateDefinition template = templateRepository.findByIdAndCommunity_Id(templateId, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Email template", templateId));

        String nextCode = normalizeCode(request.templateCode());
        templateRepository.findByCommunity_IdAndTemplateCodeIgnoreCase(communityId, nextCode)
                .filter(existing -> !existing.getId().equals(templateId))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Template code already exists for this community: " + nextCode);
                });

        template.setTemplateCode(nextCode);
        template.setTemplateName(request.templateName());
        template.setCategory(request.category());
        template.setSubject(request.subject());
        template.setHtmlContent(request.htmlContent());
        template.setLayoutJson(request.layoutJson());
        template.setHeader(resolveHeader(communityId, request.headerId()));
        template.setFooter(resolveFooter(communityId, request.footerId()));
        template.setStatus(request.status() == null ? EmailTemplateStatus.DRAFT : request.status());
        template.setCreatedBy(request.createdBy());
        template.setVersion(template.getVersion() == null ? 1 : template.getVersion() + 1);

        EmailTemplateDefinition saved = templateRepository.save(template);
        snapshot(saved, request.notes());
        return toTemplateDetail(saved);
    }

    @Transactional(readOnly = true)
    public List<DynamicFieldResponse> listDynamicFields() {
        return dynamicFieldRepository.findByActiveTrueOrderByCategoryAscSortOrderAscFieldKeyAsc()
                .stream()
                .map(field -> new DynamicFieldResponse(
                        field.getCategory(),
                        field.getFieldKey(),
                        field.getDisplayName(),
                        field.getSampleValue(),
                        field.getDescription()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public PreviewResponse previewActiveTemplate(Long communityId, String templateCode, Map<String, Object> data) {
        EmailTemplateDefinition template = templateRepository
                .findByCommunity_IdAndTemplateCodeIgnoreCaseAndStatus(communityId, templateCode, EmailTemplateStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Active email template", "templateCode", templateCode));
        return render(template, data);
    }

    @Transactional(readOnly = true)
    public PreviewResponse previewTemplate(Long communityId, Long templateId, Map<String, Object> data) {
        EmailTemplateDefinition template = templateRepository.findByIdAndCommunity_Id(templateId, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Email template", templateId));
        return render(template, data);
    }

    private PreviewResponse render(EmailTemplateDefinition template, Map<String, Object> data) {
        Map<String, Object> variables = new HashMap<>();
        if (data != null) {
            variables.putAll(data);
        }
        applyCommunityVariables(template.getCommunity(), variables);

        EmailHeader header = template.getHeader() != null
                ? template.getHeader()
                : headerRepository.findFirstByCommunity_IdAndActiveTrueOrderByUpdatedAtDesc(template.getCommunity().getId()).orElse(null);
        EmailFooter footer = template.getFooter() != null
                ? template.getFooter()
                : footerRepository.findFirstByCommunity_IdAndActiveTrueOrderByUpdatedAtDesc(template.getCommunity().getId()).orElse(null);

        String subject = replaceTokens(template.getSubject(), variables, false);
        StringBuilder html = new StringBuilder();
        if (header != null) {
            html.append(replaceTokens(header.getHtmlContent(), variables, false));
        }
        html.append(replaceTokens(template.getHtmlContent(), variables, false));
        if (footer != null) {
            html.append(replaceTokens(footer.getHtmlContent(), variables, false));
        }
        return new PreviewResponse(subject, html.toString());
    }

    private void applyCommunityVariables(Community community, Map<String, Object> variables) {
        variables.putIfAbsent("communityName", community.getName());
        variables.putIfAbsent("communityCity", community.getCity());
        brandingRepository.findByCommunity_Id(community.getId()).ifPresent(branding -> {
            putIfPresent(variables, "logoUrl", branding.getLogoUrl());
            putIfPresent(variables, "primaryColor", branding.getPrimaryColor());
            putIfPresent(variables, "secondaryColor", branding.getSecondaryColor());
            putIfPresent(variables, "fontFamily", branding.getFontFamily());
            putIfPresent(variables, "buttonStyle", branding.getButtonStyle());
            putIfPresent(variables, "bannerUrl", branding.getBannerUrl());
            putIfPresent(variables, "facebookUrl", branding.getFacebookUrl());
            putIfPresent(variables, "instagramUrl", branding.getInstagramUrl());
            putIfPresent(variables, "websiteUrl", branding.getWebsiteUrl());
            putIfPresent(variables, "supportEmail", branding.getSupportEmail());
            putIfPresent(variables, "supportPhone", branding.getSupportPhone());
        });
    }

    private String replaceTokens(String content, Map<String, Object> variables, boolean raw) {
        if (content == null || content.isBlank()) {
            return "";
        }

        String rawReplaced = replaceWithPattern(content, RAW_TOKEN, variables, true);
        return replaceWithPattern(rawReplaced, ESCAPED_TOKEN, variables, raw);
    }

    private String replaceWithPattern(String content, Pattern pattern, Map<String, Object> variables, boolean raw) {
        Matcher matcher = pattern.matcher(content);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = variables.get(key);
            String replacement = value == null ? "" : String.valueOf(value);
            if (!raw) {
                replacement = HtmlUtils.htmlEscape(replacement);
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private void snapshot(EmailTemplateDefinition template, String notes) {
        EmailTemplateVersion version = EmailTemplateVersion.builder()
                .template(template)
                .version(template.getVersion())
                .subject(template.getSubject())
                .htmlContent(template.getHtmlContent())
                .layoutJson(template.getLayoutJson())
                .header(template.getHeader())
                .footer(template.getFooter())
                .status(template.getStatus())
                .createdBy(template.getCreatedBy())
                .notes(notes)
                .build();
        versionRepository.save(version);
    }

    private Community requireCommunity(Long communityId) {
        return communityRepository.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community", communityId));
    }

    private EmailHeader resolveHeader(Long communityId, Long headerId) {
        if (headerId == null) {
            return null;
        }
        return headerRepository.findByIdAndCommunity_Id(headerId, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Email header", headerId));
    }

    private EmailFooter resolveFooter(Long communityId, Long footerId) {
        if (footerId == null) {
            return null;
        }
        return footerRepository.findByIdAndCommunity_Id(footerId, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Email footer", footerId));
    }

    private BrandingResponse toBrandingResponse(CommunityBranding branding) {
        Community community = branding.getCommunity();
        return new BrandingResponse(
                branding.getId(),
                community.getId(),
                community.getName(),
                branding.getLogoUrl(),
                branding.getPrimaryColor(),
                branding.getSecondaryColor(),
                branding.getFontFamily(),
                branding.getButtonStyle(),
                branding.getBannerUrl(),
                branding.getFacebookUrl(),
                branding.getInstagramUrl(),
                branding.getWebsiteUrl(),
                branding.getSupportEmail(),
                branding.getSupportPhone()
        );
    }

    private HeaderFooterResponse toHeaderResponse(EmailHeader header) {
        return new HeaderFooterResponse(
                header.getId(),
                header.getCommunity().getId(),
                header.getName(),
                header.getHtmlContent(),
                header.getLayoutJson(),
                header.isActive(),
                header.getVersion(),
                header.getCreatedAt(),
                header.getUpdatedAt()
        );
    }

    private HeaderFooterResponse toFooterResponse(EmailFooter footer) {
        return new HeaderFooterResponse(
                footer.getId(),
                footer.getCommunity().getId(),
                footer.getName(),
                footer.getHtmlContent(),
                footer.getLayoutJson(),
                footer.isActive(),
                footer.getVersion(),
                footer.getCreatedAt(),
                footer.getUpdatedAt()
        );
    }

    private TemplateSummaryResponse toTemplateSummary(EmailTemplateDefinition template) {
        return new TemplateSummaryResponse(
                template.getId(),
                template.getCommunity().getId(),
                template.getTemplateCode(),
                template.getTemplateName(),
                template.getCategory(),
                template.getSubject(),
                template.getStatus(),
                template.getVersion(),
                template.getUpdatedAt()
        );
    }

    private TemplateDetailResponse toTemplateDetail(EmailTemplateDefinition template) {
        return new TemplateDetailResponse(
                template.getId(),
                template.getCommunity().getId(),
                template.getTemplateCode(),
                template.getTemplateName(),
                template.getCategory(),
                template.getSubject(),
                template.getHtmlContent(),
                template.getLayoutJson(),
                template.getHeader() != null ? template.getHeader().getId() : null,
                template.getFooter() != null ? template.getFooter().getId() : null,
                template.getStatus(),
                template.getVersion(),
                template.getCreatedAt(),
                template.getUpdatedAt()
        );
    }

    private void putIfPresent(Map<String, Object> variables, String key, String value) {
        if (value != null && !value.isBlank()) {
            variables.putIfAbsent(key, value);
        }
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Template code is required");
        }
        return code.trim().toUpperCase().replaceAll("[^A-Z0-9_\\-]", "_");
    }
}
