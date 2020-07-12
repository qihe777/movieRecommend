package com.example.business.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class Rating {

    private int uid;

    private int mid;

    private double score;

    private long timestamp;
}
