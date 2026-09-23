package com.aladdin.system.dao;

import com.aladdin.common.db.base.BaseDao;
import com.aladdin.system.entity.SysDictData;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;

import java.util.List;

import static com.aladdin.system.entity.table.SysDictDataTableDef.SYS_DICT_DATA;

public interface SysDictDataDao extends BaseDao<SysDictData> {

    /**
     * 查询字典数据(主表 sys005=1 由 flex 自动追加；原 SQL 对 sys_dict_type 无 sys005 过滤，JOIN 保持一致)
     */
    default List<SysDictData> selectByDictType(String dictType) {
        return selectListByQuery(QueryWrapper.create()
                .from(SYS_DICT_DATA)
                .innerJoin("sys_dict_type")
                .on(SYS_DICT_DATA.DICT_TYPE_ID.eq(new QueryColumn("sys_dict_type", "id")))
                .where(new QueryColumn("sys_dict_type", "dict_type").eq(dictType))
                .orderBy(SYS_DICT_DATA.SORT.asc()));
    }
}
