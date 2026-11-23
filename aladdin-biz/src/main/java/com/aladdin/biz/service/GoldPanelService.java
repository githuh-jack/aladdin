package com.aladdin.biz.service;

import com.aladdin.biz.dao.GoldDailyDao;
import com.aladdin.biz.dao.GoldFlowDao;
import com.aladdin.biz.dao.GoldSettingDao;
import com.aladdin.biz.dao.GoldTradeDao;
import com.aladdin.biz.entity.GoldDaily;
import com.aladdin.biz.entity.GoldFlow;
import com.aladdin.biz.entity.GoldPrice;
import com.aladdin.biz.entity.GoldSetting;
import com.aladdin.biz.entity.GoldTrade;
import com.aladdin.common.core.exception.BusinessException;
import com.aladdin.common.core.exception.GlobalErrorCode;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 黄金投资预测：设置、历史投入、每日回放、收益预测
 *
 * @author cles
 * @date 2026/11/23
 */
@Slf4j
@Service
public class GoldPanelService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final int SCALE = 4;
    private static final int RATE_SCALE = 8;

    @Autowired
    private GoldSettingDao settingDao;
    @Autowired
    private GoldFlowDao flowDao;
    @Autowired
    private GoldTradeDao tradeDao;
    @Autowired
    private GoldDailyDao dailyDao;
    @Autowired
    private GoldPriceService priceService;

    /** 读取设置，不存在则初始化默认值 */
    public GoldSetting getSetting(Long userId) {
        GoldSetting setting = settingDao.selectOneByQuery(
                QueryWrapper.create().where(new QueryColumn("user_id").eq(userId)));
        if (setting != null) {
            return setting;
        }
        setting = new GoldSetting();
        setting.setUserId(userId);
        setting.setBuyMode("GRAM");
        setting.setUnitAmount(new BigDecimal("50"));
        setting.setGramAmount(new BigDecimal("0.05"));
        setting.setDailyAmount(new BigDecimal("50"));
        LocalDate earliest = priceService.earliestDate();
        setting.setStartDate(earliest != null ? earliest : LocalDate.now());
        setting.setInitialCapital(ZERO);
        setting.setFeeRate(ZERO);
        setting.setExpectedAnnualRate(new BigDecimal("0.08"));
        setting.setHorizonDays(365);
        setting.setAutoDeposit(true);
        setting.setSys001(LocalDateTime.now());
        setting.setSys005(1);
        settingDao.insert(setting);
        return setting;
    }

    @Transactional(rollbackFor = Exception.class)
    public GoldSetting saveSetting(Long userId, Map<String, Object> body) {
        GoldSetting setting = getSetting(userId);
        if (body.get("buyMode") != null) {
            String mode = String.valueOf(body.get("buyMode")).toUpperCase();
            if (!"GRAM".equals(mode) && !"UNIT".equals(mode)) {
                throw new BusinessException(GlobalErrorCode.FAIL, "买入方式只能是 GRAM 或 UNIT");
            }
            setting.setBuyMode(mode);
        }
        setting.setUnitAmount(positive(body.get("unitAmount"), setting.getUnitAmount()));
        setting.setGramAmount(positive(body.get("gramAmount"), setting.getGramAmount()));
        setting.setDailyAmount(positive(body.get("dailyAmount"), setting.getDailyAmount()));
        setting.setInitialCapital(nonNegative(body.get("initialCapital"), setting.getInitialCapital()));
        setting.setFeeRate(nonNegative(body.get("feeRate"), setting.getFeeRate()));
        setting.setExpectedAnnualRate(nonNegative(body.get("expectedAnnualRate"), setting.getExpectedAnnualRate()));
        if (body.get("horizonDays") != null) {
            setting.setHorizonDays(Math.max(1, Integer.parseInt(String.valueOf(body.get("horizonDays")))));
        }
        if (body.get("autoDeposit") != null) {
            setting.setAutoDeposit(Boolean.parseBoolean(String.valueOf(body.get("autoDeposit"))));
        }
        if (body.get("startDate") != null && !String.valueOf(body.get("startDate")).trim().isEmpty()) {
            setting.setStartDate(LocalDate.parse(String.valueOf(body.get("startDate")).trim()));
        } else if (setting.getStartDate() == null) {
            LocalDate earliest = priceService.earliestDate();
            setting.setStartDate(earliest != null ? earliest : LocalDate.now());
        }
        setting.setSys002(LocalDateTime.now());
        settingDao.update(setting);
        replay(userId);
        return setting;
    }

    /** 今日按指定方式买入：追加今日投入并重新回放 */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> buyToday(Long userId, String buyMode) {
        String mode = buyMode == null ? null : buyMode.toUpperCase();
        if (!"GRAM".equals(mode) && !"UNIT".equals(mode)) {
            throw new BusinessException(GlobalErrorCode.FAIL, "请选择按克数买入或按份数买入");
        }
        GoldSetting setting = getSetting(userId);
        BigDecimal price;
        try {
            Map<String, Object> quote = priceService.latestQuote();
            price = new BigDecimal(String.valueOf(quote.get("price")));
        } catch (Exception e) {
            GoldPrice latest = priceService.latestDaily();
            if (latest == null) {
                throw new BusinessException(GlobalErrorCode.FAIL, "暂无行情，请先同步上金所行情");
            }
            price = latest.getClosePrice();
        }
        LocalDate today = LocalDate.now();
        ensureTodayPrice(today, price);

        BigDecimal amount = "GRAM".equals(mode)
                ? setting.getGramAmount().multiply(price).setScale(2, RoundingMode.HALF_UP)
                : setting.getUnitAmount();
        GoldFlow flow = flowDao.selectOneByQuery(QueryWrapper.create()
                .where(new QueryColumn("user_id").eq(userId))
                .where(new QueryColumn("flow_date").eq(today)));
        if (flow == null) {
            flow = new GoldFlow();
            flow.setUserId(userId);
            flow.setFlowDate(today);
            flow.setAmount(amount);
            flow.setMemo("今日买入（" + ("GRAM".equals(mode) ? "按克数" : "按份数") + "）");
            flow.setSys001(LocalDateTime.now());
            flow.setSys005(1);
            flowDao.insert(flow);
        } else {
            flow.setAmount(flow.getAmount().add(amount));
            flow.setMemo("今日买入（" + ("GRAM".equals(mode) ? "按克数" : "按份数") + "）");
            flow.setSys002(LocalDateTime.now());
            flowDao.update(flow);
        }
        setting.setBuyMode(mode);
        setting.setSys002(LocalDateTime.now());
        settingDao.update(setting);
        replay(userId);
        return buildOverview(userId);
    }

    public List<GoldFlow> listFlows(Long userId, int limit) {
        return flowDao.selectListByQuery(QueryWrapper.create()
                .where(new QueryColumn("user_id").eq(userId))
                .orderBy(new QueryColumn("flow_date").desc())
                .limit(limit));
    }

    @Transactional(rollbackFor = Exception.class)
    public GoldFlow saveFlow(Long userId, Long id, Map<String, Object> body) {
        if (body.get("flowDate") == null) {
            throw new BusinessException(GlobalErrorCode.FAIL, "请选择投入日期");
        }
        LocalDate date = LocalDate.parse(String.valueOf(body.get("flowDate")).trim());
        BigDecimal amount = nonNegative(body.get("amount"), ZERO);
        String memo = body.get("memo") == null ? "" : String.valueOf(body.get("memo"));
        GoldFlow flow;
        if (id == null) {
            flow = flowDao.selectOneByQuery(QueryWrapper.create()
                    .where(new QueryColumn("user_id").eq(userId))
                    .where(new QueryColumn("flow_date").eq(date)));
            if (flow == null) {
                flow = new GoldFlow();
                flow.setUserId(userId);
                flow.setFlowDate(date);
                flow.setSys001(LocalDateTime.now());
                flow.setSys005(1);
            }
        } else {
            flow = flowDao.selectOneById(id);
            if (flow == null || !userId.equals(flow.getUserId())) {
                throw new BusinessException(GlobalErrorCode.FAIL, "投资记录不存在");
            }
            flow.setFlowDate(date);
        }
        flow.setAmount(amount);
        flow.setMemo(memo);
        flow.setSys002(LocalDateTime.now());
        if (flow.getId() == null) {
            flowDao.insert(flow);
        } else {
            flowDao.update(flow);
        }
        replay(userId);
        return flow;
    }

    /** 删除历史投入等价于当天投入置 0，避免自动投入日被重新生成 */
    @Transactional(rollbackFor = Exception.class)
    public void clearFlow(Long userId, Long id) {
        GoldFlow flow = flowDao.selectOneById(id);
        if (flow == null || !userId.equals(flow.getUserId())) {
            throw new BusinessException(GlobalErrorCode.FAIL, "投资记录不存在");
        }
        flow.setAmount(ZERO);
        flow.setMemo("跳过该日");
        flow.setSys002(LocalDateTime.now());
        flowDao.update(flow);
        replay(userId);
    }

    public List<GoldTrade> listTrades(Long userId, int limit) {
        return tradeDao.selectListByQuery(QueryWrapper.create()
                .where(new QueryColumn("user_id").eq(userId))
                .orderBy(new QueryColumn("trade_date").desc())
                .orderBy(new QueryColumn("id").desc())
                .limit(limit));
    }

    /** 按历史行情回放整个投资过程，重建买入流水与每日快照 */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> replay(Long userId) {
        GoldSetting setting = getSetting(userId);
        LocalDate start = setting.getStartDate();
        LocalDate earliest = priceService.earliestDate();
        if (start == null) {
            start = earliest != null ? earliest : LocalDate.now();
        }
        if (earliest != null && start.isBefore(earliest)) {
            start = earliest;
        }
        List<GoldPrice> prices = priceService.listPrices(start, null);
        dailyDao.deleteAllByUser(userId);
        tradeDao.deleteAllByUser(userId);
        if (prices.isEmpty()) {
            return buildEmptyReplayResult("暂无行情数据，请先同步上金所行情");
        }

        // 自动投入：为缺失的历史交易日补一笔默认投入（可随时编辑）
        List<GoldFlow> allFlows = flowDao.selectListByQuery(QueryWrapper.create()
                .where(new QueryColumn("user_id").eq(userId)));
        Map<LocalDate, GoldFlow> flowMap = allFlows.stream()
                .collect(Collectors.toMap(GoldFlow::getFlowDate, f -> f, (a, b) -> a));
        if (Boolean.TRUE.equals(setting.getAutoDeposit())) {
            List<GoldFlow> seeds = new ArrayList<>();
            for (GoldPrice p : prices) {
                if (p.getTradeDate().isBefore(start) || flowMap.containsKey(p.getTradeDate())) {
                    continue;
                }
                GoldFlow f = new GoldFlow();
                f.setUserId(userId);
                f.setFlowDate(p.getTradeDate());
                f.setAmount(setting.getDailyAmount());
                f.setMemo("每日自动投入");
                f.setSys001(LocalDateTime.now());
                f.setSys005(1);
                seeds.add(f);
                flowMap.put(f.getFlowDate(), f);
            }
            if (!seeds.isEmpty()) {
                flowDao.insertBatch(seeds);
            }
        }

        BigDecimal cash = setting.getInitialCapital();
        BigDecimal totalInvested = setting.getInitialCapital();
        BigDecimal grams = ZERO;
        BigDecimal cumulativeBuy = ZERO;
        LocalDateTime now = LocalDateTime.now();

        List<GoldTrade> trades = new ArrayList<>();
        List<GoldDaily> dailies = new ArrayList<>();

        for (GoldPrice p : prices) {
            BigDecimal price = p.getClosePrice();
            if (price == null || price.signum() <= 0) {
                continue;
            }
            GoldFlow flow = flowMap.get(p.getTradeDate());
            BigDecimal deposit = flow == null ? ZERO : flow.getAmount();
            if (deposit.signum() > 0) {
                cash = cash.add(deposit);
                totalInvested = totalInvested.add(deposit);
            }

            BigDecimal buyAmount = ZERO;
            BigDecimal buyGrams = ZERO;
            BigDecimal buyUnits = ZERO;
            if (cash.signum() > 0) {
                if ("UNIT".equals(setting.getBuyMode())) {
                    buyAmount = cash.min(setting.getUnitAmount());
                    buyUnits = buyAmount.divide(setting.getUnitAmount(), SCALE, RoundingMode.DOWN);
                    buyGrams = buyAmount.divide(price, SCALE, RoundingMode.DOWN);
                } else {
                    BigDecimal maxGrams = cash.divide(price, SCALE, RoundingMode.DOWN);
                    buyGrams = maxGrams.min(setting.getGramAmount());
                    buyAmount = buyGrams.multiply(price).setScale(2, RoundingMode.HALF_UP);
                    buyUnits = buyAmount.divide(setting.getUnitAmount(), SCALE, RoundingMode.DOWN);
                }
            }
            if (buyAmount.signum() > 0) {
                BigDecimal fee = buyAmount.multiply(setting.getFeeRate()).setScale(2, RoundingMode.HALF_UP);
                if (buyAmount.add(fee).compareTo(cash) > 0) {
                    buyAmount = cash.subtract(fee).max(ZERO);
                    buyGrams = buyAmount.divide(price, SCALE, RoundingMode.DOWN);
                    buyUnits = buyAmount.divide(setting.getUnitAmount(), SCALE, RoundingMode.DOWN);
                    fee = buyAmount.multiply(setting.getFeeRate()).setScale(2, RoundingMode.HALF_UP);
                }
                cash = cash.subtract(buyAmount).subtract(fee);
                grams = grams.add(buyGrams);
                cumulativeBuy = cumulativeBuy.add(buyAmount);

                GoldTrade trade = new GoldTrade();
                trade.setUserId(userId);
                trade.setTradeDate(p.getTradeDate());
                trade.setBuyMode(setting.getBuyMode());
                trade.setPrice(price);
                trade.setGrams(buyGrams);
                trade.setAmount(buyAmount);
                trade.setUnits(buyUnits);
                trade.setFee(fee);
                trade.setSource("AUTO");
                trade.setNote("每日自动买入");
                trade.setSys001(now);
                trade.setSys005(1);
                trades.add(trade);
            }

            BigDecimal marketValue = grams.multiply(price).setScale(2, RoundingMode.HALF_UP);
            BigDecimal equity = cash.add(marketValue).setScale(2, RoundingMode.HALF_UP);
            BigDecimal profit = equity.subtract(totalInvested).setScale(2, RoundingMode.HALF_UP);
            BigDecimal profitRate = totalInvested.signum() > 0
                    ? profit.divide(totalInvested, RATE_SCALE, RoundingMode.HALF_UP)
                    : ZERO;

            GoldDaily daily = new GoldDaily();
            daily.setUserId(userId);
            daily.setTradeDate(p.getTradeDate());
            daily.setPrice(price);
            daily.setDeposit(deposit);
            daily.setBuyAmount(buyAmount);
            daily.setGrams(buyGrams);
            daily.setCumulativeGrams(grams.setScale(SCALE, RoundingMode.HALF_UP));
            daily.setCumulativeInvested(totalInvested.setScale(2, RoundingMode.HALF_UP));
            daily.setMarketValue(marketValue);
            daily.setEquity(equity);
            daily.setProfit(profit);
            daily.setProfitRate(profitRate);
            daily.setSys001(now);
            daily.setSys005(1);
            dailies.add(daily);
        }

        if (!trades.isEmpty()) {
            tradeDao.insertBatch(trades);
        }
        if (!dailies.isEmpty()) {
            dailyDao.insertBatch(dailies);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("days", dailies.size());
        result.put("trades", trades.size());
        result.put("cumulativeGrams", grams.setScale(SCALE, RoundingMode.HALF_UP));
        result.put("totalInvested", totalInvested.setScale(2, RoundingMode.HALF_UP));
        result.put("cash", cash.setScale(2, RoundingMode.HALF_UP));
        result.put("cumulativeBuy", cumulativeBuy.setScale(2, RoundingMode.HALF_UP));
        return result;
    }

    /** 首页概览 */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> overview(Long userId) {
        ensureReplay(userId);
        return buildOverview(userId);
    }

    private void ensureReplay(Long userId) {
        GoldPrice latestPrice = priceService.latestDaily();
        GoldDaily last = dailyDao.selectOneByQuery(QueryWrapper.create()
                .where(new QueryColumn("user_id").eq(userId))
                .orderBy(new QueryColumn("trade_date").desc())
                .limit(1));
        if (latestPrice == null) {
            return;
        }
        if (last == null || last.getTradeDate().isBefore(latestPrice.getTradeDate())) {
            replay(userId);
        }
    }

    private Map<String, Object> buildOverview(Long userId) {
        GoldSetting setting = getSetting(userId);
        GoldDaily last = dailyDao.selectOneByQuery(QueryWrapper.create()
                .where(new QueryColumn("user_id").eq(userId))
                .orderBy(new QueryColumn("trade_date").desc())
                .limit(1));

        Map<String, Object> latest = new LinkedHashMap<>();
        BigDecimal livePrice;
        String quoteTime = "";
        boolean stale = true;
        try {
            Map<String, Object> quote = priceService.latestQuote();
            livePrice = new BigDecimal(String.valueOf(quote.get("price")));
            quoteTime = String.valueOf(quote.get("quoteTime") == null ? "" : quote.get("quoteTime"));
            stale = Boolean.TRUE.equals(quote.get("stale"));
        } catch (Exception e) {
            GoldPrice p = priceService.latestDaily();
            livePrice = p == null ? ZERO : p.getClosePrice();
            quoteTime = p == null ? "" : p.getTradeDate().toString();
        }
        latest.put("price", livePrice);
        latest.put("quoteTime", quoteTime);
        latest.put("stale", stale);

        List<GoldFlow> flows = flowDao.selectListByQuery(QueryWrapper.create()
                .where(new QueryColumn("user_id").eq(userId)));
        LocalDate startDate = setting.getStartDate();
        BigDecimal flowTotal = flows.stream()
                .filter(f -> startDate == null || !f.getFlowDate().isBefore(startDate))
                .map(GoldFlow::getAmount)
                .reduce(ZERO, BigDecimal::add);
        BigDecimal totalInvested = setting.getInitialCapital().add(flowTotal)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal grams;
        BigDecimal cash;
        BigDecimal avgCost;
        if (last != null) {
            grams = last.getCumulativeGrams();
            cash = last.getEquity().subtract(last.getMarketValue());
            avgCost = grams.signum() > 0
                    ? totalInvested.subtract(cash).divide(grams, 2, RoundingMode.HALF_UP)
                    : ZERO;
        } else {
            grams = ZERO;
            cash = setting.getInitialCapital();
            avgCost = ZERO;
        }
        BigDecimal marketValue = grams.multiply(livePrice).setScale(2, RoundingMode.HALF_UP);
        BigDecimal equity = cash.add(marketValue).setScale(2, RoundingMode.HALF_UP);
        BigDecimal actualProfit = equity.subtract(totalInvested).setScale(2, RoundingMode.HALF_UP);
        BigDecimal actualReturn = totalInvested.signum() > 0
                ? actualProfit.divide(totalInvested, RATE_SCALE, RoundingMode.HALF_UP)
                : ZERO;

        int horizonDays = setting.getHorizonDays() == null ? 365 : setting.getHorizonDays();
        double annual = setting.getExpectedAnnualRate() == null ? 0.08 : setting.getExpectedAnnualRate().doubleValue();
        BigDecimal projectedPrice = BigDecimal.valueOf(
                livePrice.doubleValue() * Math.pow(1 + annual, horizonDays / 365.0))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal projectedMarketValue = grams.multiply(projectedPrice).setScale(2, RoundingMode.HALF_UP);
        BigDecimal expectedEquity = cash.add(projectedMarketValue).setScale(2, RoundingMode.HALF_UP);
        BigDecimal expectedProfit = expectedEquity.subtract(totalInvested).setScale(2, RoundingMode.HALF_UP);
        BigDecimal expectedReturn = totalInvested.signum() > 0
                ? expectedProfit.divide(totalInvested, RATE_SCALE, RoundingMode.HALF_UP)
                : ZERO;

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalInvested", totalInvested);
        summary.put("cash", cash.setScale(2, RoundingMode.HALF_UP));
        summary.put("marketValue", marketValue);
        summary.put("equity", equity);
        summary.put("grams", grams);
        summary.put("avgCost", avgCost);
        summary.put("actualProfit", actualProfit);
        summary.put("actualReturn", actualReturn);
        summary.put("expectedProfit", expectedProfit);
        summary.put("expectedReturn", expectedReturn);
        summary.put("projectedPrice", projectedPrice);
        summary.put("horizonDays", horizonDays);
        summary.put("expectedAnnualRate", setting.getExpectedAnnualRate());
        summary.put("buyMode", setting.getBuyMode());

        return assemble(userId, setting, latest, summary, flows);
    }

    private Map<String, Object> assemble(Long userId, GoldSetting setting, Map<String, Object> latest,
                                         Map<String, Object> summary, List<GoldFlow> flows) {
        GoldPrice latestPrice = priceService.latestDaily();
        LocalDate today = latestPrice == null ? LocalDate.now() : latestPrice.getTradeDate();
        BigDecimal price = latestPrice == null
                ? new BigDecimal(String.valueOf(latest.get("price")))
                : latestPrice.getClosePrice();

        List<GoldTrade> todayTrades = tradeDao.selectListByQuery(QueryWrapper.create()
                .where(new QueryColumn("user_id").eq(userId))
                .where(new QueryColumn("trade_date").eq(today)));

        Map<String, Object> todayMap = new LinkedHashMap<>();
        todayMap.put("date", today.toString());
        todayMap.put("price", price);
        todayMap.put("activeMode", setting.getBuyMode());
        List<Map<String, Object>> modes = new ArrayList<>();
        modes.add(modeStat("GRAM", "按克数买入", todayTrades, setting, price));
        modes.add(modeStat("UNIT", "按份数买入", todayTrades, setting, price));
        todayMap.put("modes", modes);

        int trendLimit = 730;
        List<GoldPrice> all = priceService.listPrices(
                setting.getStartDate() == null ? null : setting.getStartDate(), null);
        if (all.size() > trendLimit) {
            all = all.subList(all.size() - trendLimit, all.size());
        }
        List<GoldTrade> trades = tradeDao.selectListByQuery(QueryWrapper.create()
                .where(new QueryColumn("user_id").eq(userId))
                .orderBy(new QueryColumn("trade_date").asc()));
        List<String> dates = all.stream().map(p -> p.getTradeDate().toString()).collect(Collectors.toList());
        List<BigDecimal> closes = all.stream().map(GoldPrice::getClosePrice).collect(Collectors.toList());
        List<Map<String, Object>> buys = new ArrayList<>();
        for (GoldTrade t : trades) {
            Map<String, Object> b = new LinkedHashMap<>();
            b.put("date", t.getTradeDate().toString());
            b.put("price", t.getPrice());
            b.put("grams", t.getGrams());
            b.put("amount", t.getAmount());
            buys.add(b);
        }
        Map<String, Object> trend = new LinkedHashMap<>();
        trend.put("dates", dates);
        trend.put("closes", closes);
        trend.put("buys", buys);

        List<Map<String, Object>> flowList = flows.stream()
                .sorted(Comparator.comparing(GoldFlow::getFlowDate).reversed())
                .limit(5000)
                .map(f -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", f.getId());
                    m.put("flowDate", f.getFlowDate().toString());
                    m.put("amount", f.getAmount());
                    m.put("memo", f.getMemo());
                    return m;
                }).collect(Collectors.toList());

        List<Map<String, Object>> tradeList = listTrades(userId, 200).stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("tradeDate", t.getTradeDate().toString());
            m.put("buyMode", t.getBuyMode());
            m.put("price", t.getPrice());
            m.put("grams", t.getGrams());
            m.put("amount", t.getAmount());
            m.put("units", t.getUnits());
            m.put("fee", t.getFee());
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("latest", latest);
        result.put("setting", setting);
        result.put("summary", summary);
        result.put("today", todayMap);
        result.put("trend", trend);
        result.put("flows", flowList);
        result.put("trades", tradeList);
        return result;
    }

    private Map<String, Object> modeStat(String mode, String label, List<GoldTrade> todayTrades,
                                         GoldSetting setting, BigDecimal price) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("buyMode", mode);
        m.put("label", label);
        BigDecimal actualGrams = ZERO;
        BigDecimal actualAmount = ZERO;
        BigDecimal actualUnits = ZERO;
        for (GoldTrade t : todayTrades) {
            if (mode.equals(t.getBuyMode())) {
                actualGrams = actualGrams.add(t.getGrams());
                actualAmount = actualAmount.add(t.getAmount());
                actualUnits = actualUnits.add(t.getUnits());
            }
        }
        boolean bought = actualAmount.signum() > 0;
        if (!bought) {
            if ("UNIT".equals(mode)) {
                actualAmount = setting.getUnitAmount();
                actualUnits = BigDecimal.ONE;
                actualGrams = price.signum() > 0
                        ? actualAmount.divide(price, SCALE, RoundingMode.DOWN) : ZERO;
            } else {
                actualGrams = setting.getGramAmount();
                actualAmount = actualGrams.multiply(price).setScale(2, RoundingMode.HALF_UP);
                actualUnits = actualAmount.divide(setting.getUnitAmount(), SCALE, RoundingMode.DOWN);
            }
        }
        m.put("bought", bought);
        m.put("grams", actualGrams.setScale(SCALE, RoundingMode.HALF_UP));
        m.put("amount", actualAmount.setScale(2, RoundingMode.HALF_UP));
        m.put("units", actualUnits.setScale(SCALE, RoundingMode.HALF_UP));
        m.put("gramAvgPrice", actualGrams.signum() > 0
                ? actualAmount.divide(actualGrams, 2, RoundingMode.HALF_UP) : price);
        m.put("unitAvgPrice", actualUnits.signum() > 0
                ? actualAmount.divide(actualUnits, 2, RoundingMode.HALF_UP) : setting.getUnitAmount());
        return m;
    }

    private void ensureTodayPrice(LocalDate today, BigDecimal price) {
        GoldPrice existing = priceService.latestDaily();
        if (existing != null && today.equals(existing.getTradeDate())) {
            return;
        }
        List<GoldPrice> same = priceService.listPrices(today, today);
        if (!same.isEmpty()) {
            return;
        }
        GoldPrice p = new GoldPrice();
        p.setTradeDate(today);
        p.setClosePrice(price);
        p.setOpenPrice(price);
        p.setLowPrice(price);
        p.setHighPrice(price);
        p.setSource("MANUAL");
        p.setSys001(LocalDateTime.now());
        p.setSys005(1);
        priceService.savePrice(p);
    }

    private Map<String, Object> buildEmptyReplayResult(String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("days", 0);
        m.put("message", message);
        return m;
    }

    private BigDecimal positive(Object value, BigDecimal fallback) {
        BigDecimal v = decimal(value);
        return v == null || v.signum() <= 0 ? fallback : v;
    }

    private BigDecimal nonNegative(Object value, BigDecimal fallback) {
        BigDecimal v = decimal(value);
        return v == null || v.signum() < 0 ? (v == null ? fallback : ZERO) : v;
    }

    private BigDecimal decimal(Object value) {
        if (value == null || String.valueOf(value).trim().isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
