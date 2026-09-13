package com.safwat.hr.network.auth.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private String type;
    private String username;

    private String displayName;   // ✅ جديد
    private String jobTitle;      // ✅ جديد

}