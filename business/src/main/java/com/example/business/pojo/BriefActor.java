package com.example.business.pojo;

import lombok.Data;

import java.io.Serializable;

@Data
public class BriefActor implements Serializable {
    private int aid;
    private String name;
    private String pic;
}
