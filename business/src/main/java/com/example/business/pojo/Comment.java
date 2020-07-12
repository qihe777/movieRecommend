package com.example.business.pojo;

import lombok.Data;

import java.io.Serializable;

@Data
public class Comment implements Serializable {
    private String comment;
    private int rating;
    private int vote_count;
    private String create_time;
    private int user_id;
    private String user_name;
    private String user_avatar;
}
