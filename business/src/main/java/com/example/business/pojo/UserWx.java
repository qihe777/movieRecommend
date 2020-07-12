package com.example.business.pojo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserWx {
    private String open_id;
    private String session_key;
    private String token;
    private String city;
    private String province;
    private String country;
    private String avatar_url;
    private int gender;
    private String nick_name;
}
