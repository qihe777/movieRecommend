package com.example.business.pojo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
public class Movie implements Serializable {
    private int mid;
    private String pic;
    private String name;
    private int year;
    private String regions;
    private int duration;
    private String imdb_id;
    private double score;
    private int votes;
    private String storyline;
    private String showname;
    private double five;
    private double four;
    private double three;
    private double two;
    private double one;
}
