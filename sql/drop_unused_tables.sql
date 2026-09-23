-- ================================================================
-- 清理脚本：删除 8 张暂未使用的系统表
-- 适用库：localhost:3306/aladdin（auth 数据源）
-- 说明：这 8 张表(公告/租户/站内信/开放API应用/用户组)当前无业务代码依赖，
--       TableInitRunner 已回退不再创建；若此前启动过程中已被建出，执行本脚本清理。
-- 重复执行安全(IF NOT EXISTS / IF EXISTS 均幂等)。
-- ================================================================

DROP TABLE IF EXISTS `sys_notice`;
DROP TABLE IF EXISTS `sys_tenant`;
DROP TABLE IF EXISTS `sys_message`;
DROP TABLE IF EXISTS `sys_message_receiver`;
DROP TABLE IF EXISTS `sys_open_api_app`;
DROP TABLE IF EXISTS `sys_user_group`;
DROP TABLE IF EXISTS `sys_user_group_role`;
DROP TABLE IF EXISTS `sys_user_group_user`;
