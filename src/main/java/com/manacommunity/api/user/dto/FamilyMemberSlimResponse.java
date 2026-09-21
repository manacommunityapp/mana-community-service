package com.manacommunity.api.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamilyMemberSlimResponse {

    private Long id;
    private String name;
    private String gothram;
    private String relation;
    private String phone;
    private String gender;
    private Integer age;
    private String dob;
    private String email;

    public FamilyMemberSlimResponse(Long id, String name, String gothram,
                                    String relation, String phone, String gender) {
        this.id = id;
        this.name = name;
        this.gothram = gothram;
        this.relation = relation;
        this.phone = phone;
        this.gender = gender;
    }
}

