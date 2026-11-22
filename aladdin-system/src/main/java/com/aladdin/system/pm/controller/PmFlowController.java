package com.aladdin.system.pm.controller;

import com.aladdin.common.core.domain.PageQuery;
import com.aladdin.common.core.domain.PageResult;
import com.aladdin.common.core.domain.R;
import com.aladdin.common.core.exception.BusinessException;
import com.aladdin.common.core.exception.GlobalErrorCode;
import com.aladdin.system.pm.dao.PmBugDao;
import com.aladdin.system.pm.dao.PmFlowDefDao;
import com.aladdin.system.pm.dao.PmFlowInstanceDao;
import com.aladdin.system.pm.dao.PmFlowLogDao;
import com.aladdin.system.pm.dao.PmFlowNodeDao;
import com.aladdin.system.pm.dao.PmStoryDao;
import com.aladdin.system.pm.dao.PmUserDao;
import com.aladdin.system.pm.entity.PmBug;
import com.aladdin.system.pm.entity.PmFlowDef;
import com.aladdin.system.pm.entity.PmFlowInstance;
import com.aladdin.system.pm.entity.PmFlowLog;
import com.aladdin.system.pm.entity.PmFlowNode;
import com.aladdin.system.pm.entity.PmStory;
import com.aladdin.system.pm.entity.PmUser;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.aladdin.system.pm.entity.table.PmFlowDefTableDef.PM_FLOW_DEF;
import static com.aladdin.system.pm.entity.table.PmFlowInstanceTableDef.PM_FLOW_INSTANCE;
import static com.aladdin.system.pm.entity.table.PmFlowLogTableDef.PM_FLOW_LOG;
import static com.aladdin.system.pm.entity.table.PmFlowNodeTableDef.PM_FLOW_NODE;

/**
 * 项目管理系统-流程管理
 * 需求/缺陷的固定流程：开始 -> 审批/执行 -> 完成，支持 同意/拒绝/转发/完成 操作，
 * 流程定义与节点可配置，节点上可配置处理角色与进入节点时的业务状态回写。
 *
 * @author cles
 * @date 2026/11/03
 */
@RestController
@RequestMapping("/pm/flow")
public class PmFlowController {

    private final PmFlowDefDao flowDefDao;
    private final PmFlowNodeDao flowNodeDao;
    private final PmFlowInstanceDao flowInstanceDao;
    private final PmFlowLogDao flowLogDao;
    private final PmStoryDao pmStoryDao;
    private final PmBugDao pmBugDao;
    private final PmUserDao pmUserDao;
    private final PmAuthHelper auth;

    public PmFlowController(PmFlowDefDao flowDefDao, PmFlowNodeDao flowNodeDao,
                            PmFlowInstanceDao flowInstanceDao, PmFlowLogDao flowLogDao,
                            PmStoryDao pmStoryDao, PmBugDao pmBugDao, PmUserDao pmUserDao,
                            PmAuthHelper auth) {
        this.flowDefDao = flowDefDao;
        this.flowNodeDao = flowNodeDao;
        this.flowInstanceDao = flowInstanceDao;
        this.flowLogDao = flowLogDao;
        this.pmStoryDao = pmStoryDao;
        this.pmBugDao = pmBugDao;
        this.pmUserDao = pmUserDao;
        this.auth = auth;
    }

    // ==================== 流程定义(配置) ====================

    @GetMapping("/def/page")
    public R<PageResult<PmFlowDef>> defPage(PageQuery pageQuery,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String bizType) {
        QueryWrapper wrapper = QueryWrapper.create();
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(PM_FLOW_DEF.NAME.like(keyword).or(PM_FLOW_DEF.CODE.like(keyword)));
        }
        if (bizType != null && !bizType.isEmpty()) {
            wrapper.and(PM_FLOW_DEF.BIZ_TYPE.eq(bizType));
        }
        wrapper.orderBy(PM_FLOW_DEF.ID, false);
        Page<PmFlowDef> page = flowDefDao.paginate(pageQuery.getPage(), pageQuery.getLimit(), wrapper);
        for (PmFlowDef def : page.getRecords()) {
            def.setNodeCount(Math.toIntExact(flowNodeDao.selectCountByQuery(
                    QueryWrapper.create().where(PM_FLOW_NODE.DEF_ID.eq(def.getId())))));
        }
        return R.ok(new PageResult<>(page.getPageNumber(), page.getPageSize(), page.getTotalRow(), page.getRecords()));
    }

    @GetMapping("/def/list")
    public R<List<PmFlowDef>> defList(@RequestParam(required = false) String bizType) {
        QueryWrapper wrapper = QueryWrapper.create()
                .where(PM_FLOW_DEF.STATUS.eq(1))
                .orderBy(PM_FLOW_DEF.ID, false);
        if (bizType != null && !bizType.isEmpty()) {
            wrapper.and(PM_FLOW_DEF.BIZ_TYPE.eq(bizType));
        }
        return R.ok(flowDefDao.selectListByQuery(wrapper));
    }

    @PostMapping("/def")
    public R<Void> saveDef(@RequestBody PmFlowDef def) {
        auth.requireRole("admin");
        def.setId(null);
        def.setSys001(LocalDateTime.now());
        def.setSys005(1);
        if (def.getStatus() == null) {
            def.setStatus(1);
        }
        return flowDefDao.insert(def) > 0 ? R.ok() : R.fail();
    }

    @PostMapping("/def/edit")
    public R<Void> editDef(@RequestBody PmFlowDef def) {
        auth.requireRole("admin");
        return flowDefDao.update(def) > 0 ? R.ok() : R.fail();
    }

    @PostMapping("/def/remove/{id}")
    public R<Void> removeDef(@PathVariable Long id) {
        auth.requireRole("admin");
        Long running = flowInstanceDao.selectCountByQuery(QueryWrapper.create()
                .where(PM_FLOW_INSTANCE.DEF_ID.eq(id).and(PM_FLOW_INSTANCE.STATUS.eq("running"))));
        if (running != null && running > 0) {
            return R.fail(GlobalErrorCode.BAD_REQUEST.getCode(), "该流程存在运行中的实例，无法删除");
        }
        flowNodeDao.deleteByQuery(QueryWrapper.create().where(PM_FLOW_NODE.DEF_ID.eq(id)));
        return flowDefDao.deleteById(id) > 0 ? R.ok() : R.fail();
    }

    // ==================== 流程节点(配置) ====================

    @GetMapping("/node/list")
    public R<List<PmFlowNode>> nodeList(@RequestParam Long defId) {
        return R.ok(listNodes(defId));
    }

    @PostMapping("/node")
    public R<Void> saveNode(@RequestBody PmFlowNode node) {
        auth.requireRole("admin");
        node.setId(null);
        node.setSys001(LocalDateTime.now());
        node.setSys005(1);
        return flowNodeDao.insert(node) > 0 ? R.ok() : R.fail();
    }

    @PostMapping("/node/edit")
    public R<Void> editNode(@RequestBody PmFlowNode node) {
        auth.requireRole("admin");
        return flowNodeDao.update(node) > 0 ? R.ok() : R.fail();
    }

    @PostMapping("/node/remove/{id}")
    public R<Void> removeNode(@PathVariable Long id) {
        auth.requireRole("admin");
        return flowNodeDao.deleteById(id) > 0 ? R.ok() : R.fail();
    }

    // ==================== 流程运行 ====================

    /** 发起流程(bizType: story/bug) */
    @PostMapping("/start")
    @Transactional(rollbackFor = Exception.class)
    public R<Void> start(@RequestParam String bizType, @RequestParam Long bizId) {
        if (!"story".equals(bizType) && !"bug".equals(bizType)) {
            return R.fail(GlobalErrorCode.BAD_REQUEST.getCode(), "非法的业务类型");
        }
        checkBizOwnerPermission(bizType);
        if (!"story".equals(bizType) ? pmBugDao.selectOneById(bizId) == null
                : pmStoryDao.selectOneById(bizId) == null) {
            return R.fail(GlobalErrorCode.BAD_REQUEST.getCode(), "业务对象不存在");
        }
        Long running = flowInstanceDao.selectCountByQuery(QueryWrapper.create()
                .where(PM_FLOW_INSTANCE.BIZ_TYPE.eq(bizType)
                        .and(PM_FLOW_INSTANCE.BIZ_ID.eq(bizId))
                        .and(PM_FLOW_INSTANCE.STATUS.eq("running"))));
        if (running != null && running > 0) {
            return R.fail(GlobalErrorCode.BAD_REQUEST.getCode(), "该对象已有运行中的流程");
        }
        PmFlowDef def = flowDefDao.selectOneByQuery(QueryWrapper.create()
                .where(PM_FLOW_DEF.BIZ_TYPE.eq(bizType).and(PM_FLOW_DEF.STATUS.eq(1)))
                .orderBy(PM_FLOW_DEF.ID, true)
                .limit(1));
        if (def == null) {
            return R.fail(GlobalErrorCode.BAD_REQUEST.getCode(), "未启用对应业务类型的流程定义");
        }
        PmFlowNode start = startNode(def.getId());
        if (start == null || start.getApproveTo() == null || start.getApproveTo().isEmpty()) {
            return R.fail(GlobalErrorCode.BAD_REQUEST.getCode(), "流程定义缺少开始节点或开始节点未配置同意后节点");
        }
        PmFlowNode first = nodeByKey(def.getId(), start.getApproveTo());
        if (first == null) {
            return R.fail(GlobalErrorCode.BAD_REQUEST.getCode(), "流程定义配置错误：开始后节点不存在");
        }
        PmFlowInstance inst = new PmFlowInstance();
        inst.setDefId(def.getId());
        inst.setBizType(bizType);
        inst.setBizId(bizId);
        inst.setCurrentNode(first.getNodeKey());
        inst.setStatus("running");
        inst.setCreatedBy(auth.currentPmUserId());
        inst.setSys001(LocalDateTime.now());
        inst.setSys005(1);
        flowInstanceDao.insert(inst);
        applyBizStatus(bizType, bizId, first.getBizStatus());
        saveLog(inst.getId(), start.getNodeKey(), "start", null, first.getNodeKey(), "发起流程");
        return R.ok();
    }

    /** 查询某业务的流程实例(含当前节点、可用操作、操作日志) */
    @GetMapping("/instance")
    public R<Map<String, Object>> instance(@RequestParam String bizType, @RequestParam Long bizId) {
        PmFlowInstance inst = flowInstanceDao.selectOneByQuery(QueryWrapper.create()
                .where(PM_FLOW_INSTANCE.BIZ_TYPE.eq(bizType)
                        .and(PM_FLOW_INSTANCE.BIZ_ID.eq(bizId)))
                .orderBy(PM_FLOW_INSTANCE.ID, false)
                .limit(1));
        if (inst == null) {
            return R.ok(null);
        }
        fillInstances(List.of(inst));
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("instance", inst);
        PmFlowNode node = "running".equals(inst.getStatus())
                ? nodeByKey(inst.getDefId(), inst.getCurrentNode()) : null;
        result.put("currentNode", node);
        result.put("canHandle", node != null && canHandle(inst, node));
        result.put("logs", logList(inst.getId()));
        return R.ok(result);
    }

    /** 流程操作: approve同意/reject拒绝/forward转发/complete完成 */
    @PostMapping("/act")
    @Transactional(rollbackFor = Exception.class)
    public R<Void> act(@RequestBody Map<String, Object> body) {
        Long instanceId = Long.valueOf(String.valueOf(body.get("instanceId")));
        String action = String.valueOf(body.getOrDefault("action", ""));
        String remark = String.valueOf(body.getOrDefault("remark", ""));
        Long forwardTo = body.get("forwardTo") == null || String.valueOf(body.get("forwardTo")).isEmpty()
                ? null : Long.valueOf(String.valueOf(body.get("forwardTo")));
        PmFlowInstance inst = flowInstanceDao.selectOneById(instanceId);
        if (inst == null) {
            return R.fail(GlobalErrorCode.BAD_REQUEST.getCode(), "流程实例不存在");
        }
        if (!"running".equals(inst.getStatus())) {
            return R.fail(GlobalErrorCode.BAD_REQUEST.getCode(), "流程已结束");
        }
        PmFlowNode node = nodeByKey(inst.getDefId(), inst.getCurrentNode());
        if (node == null) {
            return R.fail(GlobalErrorCode.BAD_REQUEST.getCode(), "当前节点配置不存在，请联系管理员");
        }
        if (!canHandle(inst, node)) {
            throw new BusinessException(GlobalErrorCode.FORBIDDEN.getCode(), "您无权处理当前节点");
        }
        Set<String> actions = Arrays.stream(
                (node.getActions() == null || node.getActions().isEmpty() ? "" : node.getActions()).split(","))
                .map(String::trim).collect(Collectors.toSet());
        if (!actions.contains(action)) {
            return R.fail(GlobalErrorCode.BAD_REQUEST.getCode(), "当前节点不支持该操作");
        }
        String fromNode = node.getNodeKey();
        String toNodeKey = null;
        switch (action) {
            case "approve" -> {
                toNodeKey = node.getApproveTo();
                if (toNodeKey == null || toNodeKey.isEmpty()) {
                    return R.fail(GlobalErrorCode.BAD_REQUEST.getCode(), "当前节点未配置同意后节点");
                }
                PmFlowNode to = nodeByKey(inst.getDefId(), toNodeKey);
                if (to == null) {
                    return R.fail(GlobalErrorCode.BAD_REQUEST.getCode(), "同意后节点不存在，流程配置错误");
                }
                enterNode(inst, to);
            }
            case "reject" -> {
                toNodeKey = node.getRejectTo();
                if (toNodeKey == null || toNodeKey.isEmpty()) {
                    inst.setStatus("terminated");
                    flowInstanceDao.update(inst);
                } else {
                    PmFlowNode to = nodeByKey(inst.getDefId(), toNodeKey);
                    if (to == null) {
                        return R.fail(GlobalErrorCode.BAD_REQUEST.getCode(), "拒绝后节点不存在，流程配置错误");
                    }
                    enterNode(inst, to);
                }
            }
            case "forward" -> {
                if (forwardTo == null) {
                    return R.fail(GlobalErrorCode.BAD_REQUEST.getCode(), "请选择转发处理人");
                }
                if (pmUserDao.selectOneById(forwardTo) == null) {
                    return R.fail(GlobalErrorCode.BAD_REQUEST.getCode(), "转发处理人不存在");
                }
                inst.setCurrentAssignee(forwardTo);
                flowInstanceDao.update(inst);
            }
            case "complete" -> {
                // 完成本节点工作，流转到下一节点；未配置下一节点则结束流程
                toNodeKey = node.getApproveTo();
                if (toNodeKey == null || toNodeKey.isEmpty()) {
                    inst.setStatus("finished");
                    flowInstanceDao.update(inst);
                } else {
                    PmFlowNode to = nodeByKey(inst.getDefId(), toNodeKey);
                    if (to == null) {
                        return R.fail(GlobalErrorCode.BAD_REQUEST.getCode(), "下一节点不存在，流程配置错误");
                    }
                    enterNode(inst, to);
                }
            }
            default -> {
                return R.fail(GlobalErrorCode.BAD_REQUEST.getCode(), "非法的操作");
            }
        }
        saveLog(inst.getId(), fromNode, action, forwardTo, toNodeKey, remark);
        return R.ok();
    }

    /** 我的待办(当前用户需处理的流程) */
    @GetMapping("/todo")
    public R<List<PmFlowInstance>> todo() {
        List<PmFlowInstance> list = flowInstanceDao.selectListByQuery(QueryWrapper.create()
                .where(PM_FLOW_INSTANCE.STATUS.eq("running"))
                .orderBy(PM_FLOW_INSTANCE.ID, false));
        fillInstances(list);
        if (auth.isSysUser()) {
            return R.ok(list);
        }
        PmUser user = auth.currentPmUser();
        if (user == null) {
            return R.ok(list);
        }
        List<PmFlowInstance> result = new ArrayList<>();
        for (PmFlowInstance inst : list) {
            if (user.getId().equals(inst.getCurrentAssignee())) {
                result.add(inst);
                continue;
            }
            if (inst.getCurrentAssignee() == null) {
                PmFlowNode node = nodeByKey(inst.getDefId(), inst.getCurrentNode());
                String role = node == null ? null : node.getHandlerRole();
                if (role == null || role.isEmpty() || "*".equals(role) || role.equals(user.getRole())) {
                    result.add(inst);
                }
            }
        }
        return R.ok(result);
    }

    /** 全部流程实例(分页) */
    @GetMapping("/instances")
    public R<PageResult<PmFlowInstance>> instances(PageQuery pageQuery,
                                                   @RequestParam(required = false) String bizType,
                                                   @RequestParam(required = false) String status) {
        QueryWrapper wrapper = QueryWrapper.create();
        if (bizType != null && !bizType.isEmpty()) {
            wrapper.and(PM_FLOW_INSTANCE.BIZ_TYPE.eq(bizType));
        }
        if (status != null && !status.isEmpty()) {
            wrapper.and(PM_FLOW_INSTANCE.STATUS.eq(status));
        }
        wrapper.orderBy(PM_FLOW_INSTANCE.ID, false);
        Page<PmFlowInstance> page = flowInstanceDao.paginate(pageQuery.getPage(), pageQuery.getLimit(), wrapper);
        fillInstances(page.getRecords());
        return R.ok(new PageResult<>(page.getPageNumber(), page.getPageSize(), page.getTotalRow(), page.getRecords()));
    }

    // ==================== 私有方法 ====================

    private List<PmFlowNode> listNodes(Long defId) {
        return flowNodeDao.selectListByQuery(QueryWrapper.create()
                .where(PM_FLOW_NODE.DEF_ID.eq(defId))
                .orderBy(PM_FLOW_NODE.SORT, true));
    }

    private PmFlowNode startNode(Long defId) {
        return flowNodeDao.selectOneByQuery(QueryWrapper.create()
                .where(PM_FLOW_NODE.DEF_ID.eq(defId).and(PM_FLOW_NODE.NODE_TYPE.eq("start")))
                .limit(1));
    }

    private PmFlowNode nodeByKey(Long defId, String nodeKey) {
        return flowNodeDao.selectOneByQuery(QueryWrapper.create()
                .where(PM_FLOW_NODE.DEF_ID.eq(defId).and(PM_FLOW_NODE.NODE_KEY.eq(nodeKey)))
                .limit(1));
    }

    /** 进入节点：回写业务状态，实例指向新节点 */
    private void enterNode(PmFlowInstance inst, PmFlowNode to) {
        inst.setCurrentNode(to.getNodeKey());
        inst.setCurrentAssignee(null);
        if ("end".equals(to.getNodeType())) {
            inst.setStatus("finished");
        }
        flowInstanceDao.update(inst);
        applyBizStatus(inst.getBizType(), inst.getBizId(), to.getBizStatus());
    }

    /** 回写业务状态(story/bug的status字段) */
    private void applyBizStatus(String bizType, Long bizId, String bizStatus) {
        if (bizStatus == null || bizStatus.isEmpty()) {
            return;
        }
        if ("story".equals(bizType)) {
            PmStory story = pmStoryDao.selectOneById(bizId);
            if (story != null) {
                PmStory update = new PmStory();
                update.setId(bizId);
                update.setStatus(bizStatus);
                pmStoryDao.update(update);
            }
        } else {
            PmBug bug = pmBugDao.selectOneById(bizId);
            if (bug != null) {
                PmBug update = new PmBug();
                update.setId(bizId);
                update.setStatus(bizStatus);
                pmBugDao.update(update);
            }
        }
    }

    /** 发起权限：story需product角色，bug需tester角色 */
    private void checkBizOwnerPermission(String bizType) {
        if ("story".equals(bizType)) {
            auth.requireRole("product");
        } else {
            auth.requireRole("tester");
        }
    }

    /** 当前用户能否处理当前节点 */
    private boolean canHandle(PmFlowInstance inst, PmFlowNode node) {
        if (auth.isSysUser()) {
            return true;
        }
        PmUser user = auth.currentPmUser();
        if (user == null) {
            return false;
        }
        if ("admin".equals(user.getRole())) {
            return true;
        }
        if (inst.getCurrentAssignee() != null) {
            return inst.getCurrentAssignee().equals(user.getId());
        }
        String role = node.getHandlerRole();
        return role == null || role.isEmpty() || "*".equals(role) || role.equals(user.getRole());
    }

    private void saveLog(Long instanceId, String fromNode, String action, Long operatorId,
                         String toNode, String remark) {
        PmFlowLog flowLog = new PmFlowLog();
        flowLog.setInstanceId(instanceId);
        flowLog.setFromNode(fromNode);
        flowLog.setAction(action);
        flowLog.setOperatorId(operatorId != null ? operatorId : auth.currentPmUserId());
        flowLog.setToNode(toNode);
        flowLog.setRemark(remark);
        flowLog.setSys001(LocalDateTime.now());
        flowLog.setSys005(1);
        flowLogDao.insert(flowLog);
    }

    private List<PmFlowLog> logList(Long instanceId) {
        List<PmFlowLog> logs = flowLogDao.selectListByQuery(QueryWrapper.create()
                .where(PM_FLOW_LOG.INSTANCE_ID.eq(instanceId))
                .orderBy(PM_FLOW_LOG.ID, true));
        Map<Long, String> userNameMap = pmUserMap();
        List<PmFlowNode> nodes = null;
        for (PmFlowLog l : logs) {
            l.setOperatorName(userNameMap.get(l.getOperatorId()));
            if (nodes == null) {
                nodes = nodeListByDef(instanceDefId(l.getInstanceId()));
            }
            Map<String, String> nameByKey = nodes.stream()
                    .collect(Collectors.toMap(PmFlowNode::getNodeKey, PmFlowNode::getNodeName, (a, b) -> a));
            l.setFromNodeName(nameByKey.get(l.getFromNode()));
            l.setToNodeName(nameByKey.get(l.getToNode()));
        }
        return logs;
    }

    private Long instanceDefId(Long instanceId) {
        PmFlowInstance inst = flowInstanceDao.selectOneById(instanceId);
        return inst == null ? null : inst.getDefId();
    }

    private List<PmFlowNode> nodeListByDef(Long defId) {
        return defId == null ? List.of() : listNodes(defId);
    }

    private void fillInstances(List<PmFlowInstance> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Map<Long, String> userNameMap = pmUserMap();
        Map<Long, PmFlowDef> defMap = list.stream().map(PmFlowInstance::getDefId).filter(Objects::nonNull).distinct()
                .collect(Collectors.toMap(id -> id, id -> flowDefDao.selectOneById(id), (a, b) -> a));
        List<Long> storyIds = list.stream().filter(i -> "story".equals(i.getBizType()))
                .map(PmFlowInstance::getBizId).toList();
        List<Long> bugIds = list.stream().filter(i -> "bug".equals(i.getBizType()))
                .map(PmFlowInstance::getBizId).toList();
        Map<Long, String> storyTitleMap = storyIds.isEmpty() ? new HashMap<>()
                : pmStoryDao.selectListByIds(storyIds).stream()
                .collect(Collectors.toMap(PmStory::getId, PmStory::getTitle, (a, b) -> a));
        Map<Long, String> bugTitleMap = bugIds.isEmpty() ? new HashMap<>()
                : pmBugDao.selectListByIds(bugIds).stream()
                .collect(Collectors.toMap(PmBug::getId, PmBug::getTitle, (a, b) -> a));
        Map<Long, List<PmFlowNode>> nodeMap = new java.util.HashMap<>();
        for (PmFlowInstance inst : list) {
            PmFlowDef def = defMap.get(inst.getDefId());
            if (def != null) {
                inst.setDefName(def.getName());
            }
            if ("story".equals(inst.getBizType())) {
                inst.setBizTitle(storyTitleMap.get(inst.getBizId()));
            } else {
                inst.setBizTitle(bugTitleMap.get(inst.getBizId()));
            }
            List<PmFlowNode> nodes = nodeMap.computeIfAbsent(inst.getDefId(), this::nodeListByDef);
            PmFlowNode node = nodes.stream()
                    .filter(n -> n.getNodeKey().equals(inst.getCurrentNode()))
                    .findFirst().orElse(null);
            if (node != null) {
                inst.setCurrentNodeName(node.getNodeName());
                inst.setCurrentNodeActions(node.getActions());
                inst.setCurrentNodeHandlerRole(node.getHandlerRole());
            }
            inst.setAssigneeName(userNameMap.get(inst.getCurrentAssignee()));
            inst.setCreatedByName(userNameMap.get(inst.getCreatedBy()));
        }
    }

    private Map<Long, String> pmUserMap() {
        return pmUserDao.selectAll().stream()
                .collect(Collectors.toMap(PmUser::getId, PmUser::getRealName, (a, b) -> a));
    }
}
