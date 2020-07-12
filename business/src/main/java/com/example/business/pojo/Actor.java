package com.example.business.pojo;

import lombok.Data;

import java.io.Serializable;

@Data
public class Actor implements Serializable {
    private int aid;
    private String role;
    private String name;
    private String pic;
}
