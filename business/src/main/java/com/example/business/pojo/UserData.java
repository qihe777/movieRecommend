package com.example.business.pojo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
public class UserData {
    private int uid;
    private String gender;
    private String age;
    private String occupation;
}
