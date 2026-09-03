package com.manacommunity.api.user.dto;

public class FamilyMemberSlimResponse {

    private Long id;
    private String name;
    private String gothram;
    private String relation;
    private String phone;
    private String gender;

    public FamilyMemberSlimResponse(Long id, String name, String gothram,
                                    String relation, String phone, String gender) {
        this.id = id;
        this.name = name;
        this.gothram = gothram;
        this.relation = relation;
        this.phone = phone;
        this.gender = gender;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getGothram() { return gothram; }
    public String getRelation() { return relation; }
    public String getPhone() { return phone; }
    public String getGender() { return gender; }
}
