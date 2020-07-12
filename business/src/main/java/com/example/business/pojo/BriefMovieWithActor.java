package com.example.business.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
public class BriefMovieWithActor implements Serializable {
    private Integer mid;

    private String name;

    private double score;

    private String pic;

    private String actor;
}
