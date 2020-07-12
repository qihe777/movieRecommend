package com.example.business.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
public class BriefMovie implements Serializable {

    private Integer mid;

    private String name;

    private double score;

    private String pic;
}
