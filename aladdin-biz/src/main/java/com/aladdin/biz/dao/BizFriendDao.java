package com.aladdin.biz.dao;

import com.aladdin.biz.entity.BizFriend;
import com.aladdin.common.db.base.BaseDao;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 好友关系DAO
 *
 * @author cles
 * @date 2026/09/15
 */
public interface BizFriendDao extends BaseDao<BizFriend> {

    /**
     * 查询我的好友列表(带好友昵称/头像)
     */
    @Select("SELECT f.*, u.nickname AS friend_name, u.avatar AS friend_avatar " +
            "FROM biz_friend f " +
            "INNER JOIN sys_user u ON f.friend_id = u.id " +
            "WHERE f.sys005 = 1 AND f.user_id = #{userId} AND f.status = 1 " +
            "ORDER BY f.add_time DESC")
    List<BizFriend> selectMyFriends(@Param("userId") Long userId);

    /**
     * 查询我收到的好友申请
     */
    @Select("SELECT f.*, u.nickname AS friend_name, u.avatar AS friend_avatar " +
            "FROM biz_friend f " +
            "INNER JOIN sys_user u ON f.user_id = u.id " +
            "WHERE f.sys005 = 1 AND f.friend_id = #{userId} AND f.status = 0 " +
            "ORDER BY f.id DESC")
    List<BizFriend> selectPendingApplies(@Param("userId") Long userId);

    /**
     * 查询某条好友关系(双向)
     */
    @Select("SELECT * FROM biz_friend WHERE sys005 = 1 " +
            "AND ((user_id = #{userId} AND friend_id = #{friendId}) " +
            "OR (user_id = #{friendId} AND friend_id = #{userId})) " +
            "AND status = 1 LIMIT 1")
    BizFriend selectFriendRelation(@Param("userId") Long userId, @Param("friendId") Long friendId);

    /**
     * 查询我拉黑的用户列表(带昵称)
     */
    @Select("SELECT f.*, u.nickname AS friend_name, u.avatar AS friend_avatar " +
            "FROM biz_friend f " +
            "INNER JOIN sys_user u ON f.friend_id = u.id " +
            "WHERE f.sys005 = 1 AND f.user_id = #{userId} AND f.status = 4 " +
            "ORDER BY f.id DESC")
    List<BizFriend> selectBlacklist(@Param("userId") Long userId);

    /**
     * 查询指定用户是否被某用户拉黑(user_id=owner 拉黑了 target)
     */
    @Select("SELECT * FROM biz_friend WHERE sys005 = 1 " +
            "AND user_id = #{ownerId} AND friend_id = #{targetId} AND status = 4 LIMIT 1")
    BizFriend selectBlackRelation(@Param("ownerId") Long ownerId, @Param("targetId") Long targetId);

    /**
     * 查询我与某用户之间我方的任意关系记录(不限状态)
     */
    @Select("SELECT * FROM biz_friend WHERE sys005 = 1 " +
            "AND user_id = #{userId} AND friend_id = #{friendId} LIMIT 1")
    BizFriend selectMyRelationTo(@Param("userId") Long userId, @Param("friendId") Long friendId);

    /** 管理员: 查询所有好友关系 */
    @Select("SELECT * FROM biz_friend WHERE sys005 = 1 ORDER BY id DESC")
    List<BizFriend> selectAllList();
}
