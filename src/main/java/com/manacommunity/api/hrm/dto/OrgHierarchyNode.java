package com.manacommunity.api.hrm.dto;

import lombok.*;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OrgHierarchyNode {
    private Long id;
    private String employeeCode;
    private String firstName;
    private String lastName;
    private String designation;
    private String department;
    private List<OrgHierarchyNode> directReports;
}
