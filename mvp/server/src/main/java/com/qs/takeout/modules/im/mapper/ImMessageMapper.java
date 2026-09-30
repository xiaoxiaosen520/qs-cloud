package com.qs.takeout.modules.im.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qs.takeout.modules.im.entity.ImMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ImMessageMapper extends BaseMapper<ImMessage> {

    @Select("SELECT COUNT(*) FROM im_message WHERE session_id = #{sessionId} "
            + "AND id > #{afterId} AND sender_role <> #{excludeRole} AND sender_role <> 'SYSTEM'")
    int countUnread(@Param("sessionId") Long sessionId,
                    @Param("afterId") Long afterId,
                    @Param("excludeRole") String excludeRole);
}
