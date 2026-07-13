package com.manacommunity.api.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/admin")
public class ApiCatalogController {

    private final RequestMappingHandlerMapping handlerMapping;

    // Actuator adds a second RequestMappingHandlerMapping bean
    // (controllerEndpointHandlerMapping), so inject the MVC one by name to
    // avoid an ambiguous "2 beans found" dependency error.
    public ApiCatalogController(
            @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping) {
        this.handlerMapping = handlerMapping;
    }

    public record ApiEndpoint(String method, String path, String handler) {}
    public record ApiGroup(String tag, List<ApiEndpoint> endpoints) {}

    @GetMapping("/api-catalog")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<ApiGroup>> getApiCatalog() {
        Map<String, List<ApiEndpoint>> grouped = new TreeMap<>();

        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry :
                handlerMapping.getHandlerMethods().entrySet()) {

            try {
                RequestMappingInfo info = entry.getKey();
                HandlerMethod method = entry.getValue();

                String controllerName = method.getBeanType().getSimpleName();
                if (controllerName.equals("BasicErrorController") ||
                    controllerName.equals("ApiCatalogController")) continue;

                String tag = controllerName.replace("Controller", "");

                Set<String> patterns = info.getPatternValues();
                Set<String> methods = info.getMethodsCondition().getMethods().stream()
                        .map(Enum::name).collect(Collectors.toSet());
                if (methods.isEmpty()) methods = Set.of("GET");

                for (String pattern : patterns) {
                    for (String httpMethod : methods) {
                        String handlerName = method.getMethod().getName();
                        grouped.computeIfAbsent(tag, k -> new ArrayList<>())
                                .add(new ApiEndpoint(httpMethod, pattern, handlerName));
                    }
                }
            } catch (Exception ex) {
                log.warn("Skipping unmappable handler: {}", ex.getMessage());
            }
        }

        grouped.values().forEach(list ->
                list.sort(Comparator.comparing(ApiEndpoint::path)
                        .thenComparing(ApiEndpoint::method)));

        List<ApiGroup> result = grouped.entrySet().stream()
                .map(e -> new ApiGroup(e.getKey(), e.getValue()))
                .toList();

        return ResponseEntity.ok(result);
    }
}
