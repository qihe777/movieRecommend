package com.example.business.dao;

import org.apache.ibatis.jdbc.SQL;

import java.util.Map;

public class MovieCategoryProvider {
    /*
    SELECT m.mid,m.name,m.pic,m.score from movie m
WHERE locate("中国",regions)
and m.year between 2000 and 2010
and m.mid in(select mid from genres_union where name="动作")
and m.mid in(select mid from tag_union where name="犯罪")
order by m.votes desc*/
    public String joinSql(Map<String,Object> map){
        return new SQL(){{
            SELECT("m.mid,m.name,m.score,m.pic");
            FROM("movie m");
            if(map.containsKey("place"))
                WHERE(String.format("locate('%s',regions)",map.get("place")));
            if(map.containsKey("beginyear")){
                AND();
                WHERE(String.format("m.year between %d and %d",((Integer)map.get("beginyear")),((Integer)map.get("endyear"))));
            }
            if(map.containsKey("genres")){
                AND();
                WHERE(String.format("m.mid in(select mid from genres_union where name='%s')",map.get("genres")));
            }
            if(map.containsKey("tag")){
                AND();
                WHERE(String.format("m.mid in(select mid from tag_union where name='%s')",map.get("tag")));
            }
            ORDER_BY(map.get("sort").toString());
        }}.toString();
    }

    //select uid,gender,age,occupation from data_users
    // where gender= and age= and occupation=
    public String joinUserSql(Map<String,String> map){
        return new SQL(){
            {
                SELECT("uid,gender,age,occupation");
                FROM("data_users");
                WHERE("TO_DAYS(NOW())-TO_DAYS(lastlogin)>1");
                if(map.containsKey("gender")){
                    AND();
                    WHERE(String.format("gender=%s",map.get("gender")));
                }
                if(map.containsKey("age")){
                    AND();
                    WHERE(String.format("age=%s",map.get("age")));
                }
                if(map.containsKey("occupation")){
                    AND();
                    WHERE(String.format("occupation=%s",map.get("occupation")));
                }
            }
        }.toString();
    }
}
