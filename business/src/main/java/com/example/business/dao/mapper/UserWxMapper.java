package com.example.business.dao.mapper;

import com.example.business.pojo.UserWx;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserWxMapper {
    @Select("select count(*) from wx_user where open_id=#{open_id}")
    int ifExist(String open_id);

    @Update("update wx_user set token=#{token} where open_id=#{open_id}")
    int updateToken(String open_id, String token);

    @Insert("insert into wx_user " +
            "(open_id,session_key,token,city,province,country,avatar_url,gender,nick_name)" +
            " value(#{open_id},#{session_key},#{token},#{city},#{province},#{country},#{avatar_url},#{gender},#{nick_name})")
    int insertUser(UserWx userWx);
}
