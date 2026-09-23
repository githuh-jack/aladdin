package com.aladdin.system.dao;

import com.aladdin.common.db.base.BaseDao;
import com.aladdin.system.entity.SysDictType;
import com.mybatisflex.core.query.QueryWrapper;

import static com.aladdin.system.entity.table.SysDictTypeTableDef.SYS_DICT_TYPE;

public interface SysDictTypeDao extends BaseDao<SysDictType> {

    /**
     * 按字典类型查询(sys005=1 由 flex 自动追加)
     */
    default SysDictType selectByDictType(String dictType) {
        return selectOneByQuery(QueryWrapper.create()
                .from(SYS_DICT_TYPE)
                .where(SYS_DICT_TYPE.DICT_TYPE.eq(dictType)));
    }
}
