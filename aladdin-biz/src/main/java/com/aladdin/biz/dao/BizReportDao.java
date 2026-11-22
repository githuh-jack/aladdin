package com.aladdin.biz.dao;

import com.aladdin.biz.entity.BizReport;
import com.aladdin.common.db.base.BaseDao;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 信件举报DAO
 *
 * @author cles
 * @date 2026/10/14
 */
public interface BizReportDao extends BaseDao<BizReport> {

    /** 我的举报列表(最新在前，按目标类型取信件/日记标题) */
    @Select("SELECT r.*, " +
            "CASE WHEN r.target_type = 'diary' THEN d.title ELSE l.title END AS target_title, " +
            "CASE WHEN r.target_type = 'diary' THEN d.title ELSE l.title END AS letter_title " +
            "FROM biz_report r " +
            "LEFT JOIN biz_letter l ON r.target_type = 'letter' AND r.letter_id = l.id AND l.sys005 = 1 " +
            "LEFT JOIN biz_diary d ON r.target_type = 'diary' AND r.letter_id = d.id AND d.sys005 = 1 " +
            "WHERE r.sys005 = 1 AND r.reporter_id = #{userId} " +
            "ORDER BY r.id DESC LIMIT 100")
    List<BizReport> selectMyList(@Param("userId") Long userId);

    /** 管理员: 举报列表(待处理在前，按目标类型取信件/日记标题内容/双方昵称编号) */
    @Select("<script>" +
            "SELECT r.*, " +
            "CASE WHEN r.target_type = 'diary' THEN d.title ELSE l.title END AS target_title, " +
            "CASE WHEN r.target_type = 'diary' THEN d.content ELSE l.content END AS target_content, " +
            "u1.nickname AS reporter_name, u1.user_no AS reporter_no, " +
            "u2.nickname AS reported_name, u2.user_no AS reported_no " +
            "FROM biz_report r " +
            "LEFT JOIN biz_letter l ON r.target_type = 'letter' AND r.letter_id = l.id AND l.sys005 = 1 " +
            "LEFT JOIN biz_diary d ON r.target_type = 'diary' AND r.letter_id = d.id AND d.sys005 = 1 " +
            "LEFT JOIN biz_user u1 ON r.reporter_id = u1.id AND u1.sys005 = 1 " +
            "LEFT JOIN biz_user u2 ON r.reported_id = u2.id AND u2.sys005 = 1 " +
            "WHERE r.sys005 = 1 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (l.title LIKE CONCAT('%',#{keyword},'%') " +
            "OR d.title LIKE CONCAT('%',#{keyword},'%') " +
            "OR r.description LIKE CONCAT('%',#{keyword},'%') " +
            "OR u1.nickname LIKE CONCAT('%',#{keyword},'%') " +
            "OR u1.user_no LIKE CONCAT('%',#{keyword},'%') " +
            "OR u2.nickname LIKE CONCAT('%',#{keyword},'%') " +
            "OR u2.user_no LIKE CONCAT('%',#{keyword},'%')) </if>" +
            "<if test='status != null'>AND r.status = #{status} </if>" +
            "ORDER BY r.status ASC, r.id DESC LIMIT #{limit} OFFSET #{offset}" +
            "</script>")
    List<BizReport> selectAdminList(@Param("keyword") String keyword,
                                    @Param("status") Integer status,
                                    @Param("offset") int offset,
                                    @Param("limit") int limit);

    @Select("<script>" +
            "SELECT COUNT(*) FROM biz_report r " +
            "LEFT JOIN biz_letter l ON r.target_type = 'letter' AND r.letter_id = l.id AND l.sys005 = 1 " +
            "LEFT JOIN biz_diary d ON r.target_type = 'diary' AND r.letter_id = d.id AND d.sys005 = 1 " +
            "LEFT JOIN biz_user u1 ON r.reporter_id = u1.id AND u1.sys005 = 1 " +
            "LEFT JOIN biz_user u2 ON r.reported_id = u2.id AND u2.sys005 = 1 " +
            "WHERE r.sys005 = 1 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (l.title LIKE CONCAT('%',#{keyword},'%') " +
            "OR d.title LIKE CONCAT('%',#{keyword},'%') " +
            "OR r.description LIKE CONCAT('%',#{keyword},'%') " +
            "OR u1.nickname LIKE CONCAT('%',#{keyword},'%') " +
            "OR u1.user_no LIKE CONCAT('%',#{keyword},'%') " +
            "OR u2.nickname LIKE CONCAT('%',#{keyword},'%') " +
            "OR u2.user_no LIKE CONCAT('%',#{keyword},'%')) </if>" +
            "<if test='status != null'>AND r.status = #{status} </if>" +
            "</script>")
    long countAdminList(@Param("keyword") String keyword, @Param("status") Integer status);

    /** 同一目标同一举报人是否已举报过(唯一性兜底，target_id复用letter_id列) */
    @Select("SELECT COUNT(*) FROM biz_report WHERE sys005 = 1 " +
            "AND target_type = #{targetType} AND letter_id = #{targetId} AND reporter_id = #{reporterId}")
    long countByTargetAndReporter(@Param("targetType") String targetType,
                                  @Param("targetId") Long targetId,
                                  @Param("reporterId") Long reporterId);

    /** 按发给被举报人的处罚邮件ID查举报(用于查看邮件时启动禁闭) */
    @Select("SELECT * FROM biz_report WHERE sys005 = 1 AND reported_mail_id = #{mailId} LIMIT 1")
    BizReport selectByReportedMailId(@Param("mailId") Long mailId);
}
