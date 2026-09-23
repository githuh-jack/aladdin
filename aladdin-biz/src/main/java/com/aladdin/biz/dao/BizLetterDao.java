package com.aladdin.biz.dao;

import com.aladdin.biz.entity.BizLetter;
import com.aladdin.common.db.base.BaseDao;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 信件DAO
 *
 * @author cles
 * @date 2026/09/15
 */
public interface BizLetterDao extends BaseDao<BizLetter> {

    /** 用户当日寄出的信件数 */
    @Select("SELECT COUNT(*) FROM biz_letter WHERE sys005 = 1 AND sender_id = #{senderId} AND status = 1 AND sys001 >= #{start}")
    long countSentToday(@Param("senderId") Long senderId, @Param("start") LocalDateTime start);

    /**
     * 查询发件箱(带收件人昵称)
     */
    @Select("<script>" +
            "SELECT l.*, u.nickname AS receiver_name FROM biz_letter l " +
            "LEFT JOIN sys_user u ON l.receiver_id = u.id " +
            "WHERE l.sys005 = 1 AND l.sender_id = #{senderId} " +
            "<if test='status != null'>AND l.status = #{status} </if>" +
            "ORDER BY l.id DESC LIMIT #{limit} OFFSET #{offset}" +
            "</script>")
    List<BizLetter> selectSentList(@Param("senderId") Long senderId,
                                   @Param("status") Integer status,
                                   @Param("offset") int offset,
                                   @Param("limit") int limit);

    /**
     * 查询收件箱(带发件人昵称)
     */
    @Select("<script>" +
            "SELECT l.*, u.nickname AS sender_name FROM biz_letter l " +
            "LEFT JOIN sys_user u ON l.sender_id = u.id " +
            "WHERE l.sys005 = 1 AND l.receiver_id = #{receiverId} " +
            "<if test='status != null'>AND l.status = #{status} </if>" +
            "ORDER BY l.id DESC LIMIT #{limit} OFFSET #{offset}" +
            "</script>")
    List<BizLetter> selectInboxList(@Param("receiverId") Long receiverId,
                                    @Param("status") Integer status,
                                    @Param("offset") int offset,
                                    @Param("limit") int limit);

    @Select("SELECT l.*, su.nickname AS sender_name, ru.nickname AS receiver_name, " +
            "s.name AS stamp_name, e.name AS envelope_name " +
            "FROM biz_letter l " +
            "LEFT JOIN sys_user su ON l.sender_id = su.id " +
            "LEFT JOIN sys_user ru ON l.receiver_id = ru.id " +
            "LEFT JOIN biz_stamp s ON l.stamp_id = s.id " +
            "LEFT JOIN biz_envelope e ON l.envelope_id = e.id " +
            "WHERE l.id = #{id} AND l.sys005 = 1")
    BizLetter selectLetterDetail(@Param("id") Long id);

    @Update("UPDATE biz_letter SET status = #{status}, read_time = #{readTime} WHERE id = #{id}")
    int updateStatusAndReadTime(@Param("id") Long id, @Param("status") Integer status, @Param("readTime") LocalDateTime readTime);

    /**
     * 信件广场：最近写信人(仅昵称与头像，不含信件内容，游客可看)
     */
    @Select("SELECT su.id AS user_id, su.nickname AS sender_name, " +
            "CASE WHEN p.avatar_status = 2 THEN p.avatar_url ELSE NULL END AS avatar_url " +
            "FROM biz_letter l " +
            "LEFT JOIN sys_user su ON l.sender_id = su.id " +
            "LEFT JOIN biz_user_profile p ON p.user_id = su.id " +
            "WHERE l.sys005 = 1 AND l.status = 1 AND l.sender_id IS NOT NULL AND su.sys005 = 1 " +
            "GROUP BY su.id, su.nickname, p.avatar_url " +
            "ORDER BY MAX(l.send_time) DESC LIMIT #{limit}")
    List<Map<String, Object>> selectRecentWriters(@Param("limit") int limit);

    /**
     * 随机挑选一位可寄送的用户(排除自己、禁用用户及拉黑我的人)
     */
    @Select("SELECT u.id FROM sys_user u " +
            "WHERE u.sys005 = 1 AND u.status = 1 AND u.id != #{userId} " +
            "AND u.id NOT IN (SELECT friend_id FROM biz_friend WHERE sys005 = 1 AND user_id = #{userId} AND status = 4) " +
            "ORDER BY RAND() LIMIT 1")
    Long selectRandomReceiver(@Param("userId") Long userId);

    /**
     * 管理员: 查询所有信件
     */
    @Select("<script>" +
            "SELECT l.*, su.nickname AS sender_name, ru.nickname AS receiver_name, " +
            "s.name AS stamp_name, e.name AS envelope_name " +
            "FROM biz_letter l " +
            "LEFT JOIN sys_user su ON l.sender_id = su.id " +
            "LEFT JOIN sys_user ru ON l.receiver_id = ru.id " +
            "LEFT JOIN biz_stamp s ON l.stamp_id = s.id " +
            "LEFT JOIN biz_envelope e ON l.envelope_id = e.id " +
            "WHERE l.sys005 = 1 " +
            "<if test='senderId != null'>AND l.sender_id = #{senderId} </if>" +
            "<if test='receiverId != null'>AND l.receiver_id = #{receiverId} </if>" +
            "ORDER BY l.id DESC LIMIT #{limit} OFFSET #{offset}" +
            "</script>")
    List<BizLetter> selectAdminList(@Param("senderId") Long senderId,
                                    @Param("receiverId") Long receiverId,
                                    @Param("offset") int offset,
                                    @Param("limit") int limit);

    @Select("<script>" +
            "SELECT COUNT(*) FROM biz_letter l WHERE l.sys005 = 1 " +
            "<if test='senderId != null'>AND l.sender_id = #{senderId} </if>" +
            "<if test='receiverId != null'>AND l.receiver_id = #{receiverId} </if>" +
            "</script>")
    long countAdminList(@Param("senderId") Long senderId, @Param("receiverId") Long receiverId);
}
