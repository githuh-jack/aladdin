package com.aladdin.biz.dao;

import com.aladdin.biz.entity.BizOrder;
import com.aladdin.common.db.base.BaseDao;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 订单DAO
 *
 * @author cles
 * @date 2026/09/15
 */
public interface BizOrderDao extends BaseDao<BizOrder> {

    @Select("<script>" +
            "SELECT o.*, u.nickname AS user_name FROM biz_order o " +
            "LEFT JOIN biz_user u ON o.user_id = u.id " +
            "WHERE o.sys005 = 1 " +
            "<if test='userId != null'>AND o.user_id = #{userId} </if>" +
            "<if test='itemType != null and itemType != \"\"'>AND o.item_type = #{itemType} </if>" +
            "<if test='status != null'>AND o.status = #{status} </if>" +
            "ORDER BY o.id DESC LIMIT #{limit} OFFSET #{offset}" +
            "</script>")
    List<BizOrder> selectListWithUser(@Param("userId") Long userId,
                                      @Param("itemType") String itemType,
                                      @Param("status") Integer status,
                                      @Param("offset") int offset,
                                      @Param("limit") int limit);

    @Select("<script>" +
            "SELECT COUNT(*) FROM biz_order o WHERE o.sys005 = 1 " +
            "<if test='userId != null'>AND o.user_id = #{userId} </if>" +
            "<if test='itemType != null and itemType != \"\"'>AND o.item_type = #{itemType} </if>" +
            "<if test='status != null'>AND o.status = #{status} </if>" +
            "</script>")
    long countListWithUser(@Param("userId") Long userId,
                           @Param("itemType") String itemType,
                           @Param("status") Integer status);
}
