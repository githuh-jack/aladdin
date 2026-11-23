package com.aladdin.biz.controller;

import com.aladdin.biz.entity.GoldFlow;
import com.aladdin.biz.entity.GoldPrice;
import com.aladdin.biz.entity.GoldSetting;
import com.aladdin.biz.entity.GoldTrade;
import com.aladdin.biz.service.GoldPanelService;
import com.aladdin.biz.service.GoldPriceService;
import com.aladdin.common.core.domain.R;
import com.aladdin.common.core.exception.BusinessException;
import com.aladdin.common.core.exception.GlobalErrorCode;
import com.aladdin.common.security.service.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 黄金投资预测
 *
 * @author cles
 * @date 2026/11/23
 */
@RestController
@RequestMapping("/gold")
public class GoldController {

    @Autowired
    private GoldPanelService panelService;
    @Autowired
    private GoldPriceService priceService;

    private Long uid() {
        Long userId = LoginService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(GlobalErrorCode.FAIL, "请先登录");
        }
        return userId;
    }

    /** 首页概览：走势、实际/预期收益、当日两种方式均价、历史投入 */
    @GetMapping("/overview")
    public R<Map<String, Object>> overview() {
        return R.ok(panelService.overview(uid()));
    }

    @GetMapping("/setting")
    public R<GoldSetting> setting() {
        return R.ok(panelService.getSetting(uid()));
    }

    @PostMapping("/setting")
    public R<GoldSetting> saveSetting(@RequestBody Map<String, Object> body) {
        return R.ok(panelService.saveSetting(uid(), body));
    }

    @GetMapping("/prices")
    public R<List<GoldPrice>> prices(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return R.ok(priceService.listPrices(startDate, endDate));
    }

    @GetMapping("/latest")
    public R<Map<String, Object>> latest() {
        return R.ok(priceService.latestQuote());
    }

    /** 从上海黄金交易所同步 Au99.99 行情，并重算收益 */
    @PostMapping("/sync")
    public R<Map<String, Object>> sync() {
        Map<String, Object> result = priceService.syncFromSge();
        Map<String, Object> replay = panelService.replay(uid());
        result.put("replay", replay);
        return R.ok(result);
    }

    @GetMapping("/flows")
    public R<List<GoldFlow>> flows(@RequestParam(defaultValue = "5000") int limit) {
        return R.ok(panelService.listFlows(uid(), limit));
    }

    @PostMapping("/flows")
    public R<GoldFlow> addFlow(@RequestBody Map<String, Object> body) {
        return R.ok(panelService.saveFlow(uid(), null, body));
    }

    @PutMapping("/flows/{id}")
    public R<GoldFlow> updateFlow(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return R.ok(panelService.saveFlow(uid(), id, body));
    }

    @DeleteMapping("/flows/{id}")
    public R<Void> clearFlow(@PathVariable Long id) {
        panelService.clearFlow(uid(), id);
        return R.ok();
    }

    @GetMapping("/trades")
    public R<List<GoldTrade>> trades(@RequestParam(defaultValue = "200") int limit) {
        return R.ok(panelService.listTrades(uid(), limit));
    }

    @PostMapping("/replay")
    public R<Map<String, Object>> replay() {
        return R.ok(panelService.replay(uid()));
    }

    /** 按选定方式记录今日买入 */
    @PostMapping("/buy")
    public R<Map<String, Object>> buy(@RequestBody Map<String, Object> body) {
        Object mode = body.get("buyMode");
        return R.ok(panelService.buyToday(uid(), mode == null ? null : String.valueOf(mode)));
    }
}
