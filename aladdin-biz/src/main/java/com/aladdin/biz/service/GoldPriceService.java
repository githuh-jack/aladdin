package com.aladdin.biz.service;

import com.aladdin.biz.dao.GoldPriceDao;
import com.aladdin.biz.entity.GoldPrice;
import com.aladdin.common.core.exception.BusinessException;
import com.aladdin.common.core.exception.GlobalErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Au99.99 行情服务：上金所日行情同步 + 最新价（缓存 15 秒）
 *
 * @author cles
 * @date 2026/11/23
 */
@Slf4j
@Service
public class GoldPriceService {

    private static final String DAILY_URL = "https://www.sge.com.cn/graph/Dailyhq";
    private static final String QUOTE_URL = "https://www.sge.com.cn/graph/quotations?instid=Au99.99";
    private static final long QUOTE_CACHE_MILLIS = 15_000L;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GoldPriceService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        this.restTemplate = new RestTemplate(factory);
    }

    @Autowired
    private GoldPriceDao priceDao;

    private volatile Map<String, Object> quoteCache;
    private volatile long quoteCacheAt;

    /** 从上海黄金交易所同步 Au99.99 全量日行情，按交易日 upsert */
    public synchronized Map<String, Object> syncFromSge() {
        String body = postDaily();
        List<GoldPrice> rows = parseDaily(body);
        if (rows.isEmpty()) {
            throw new BusinessException(GlobalErrorCode.FAIL, "上金所返回行情为空");
        }
        Map<LocalDate, GoldPrice> exists = new HashMap<>();
        for (GoldPrice p : priceDao.selectListByQuery(QueryWrapper.create())) {
            exists.put(p.getTradeDate(), p);
        }
        LocalDateTime now = LocalDateTime.now();
        List<GoldPrice> inserts = new ArrayList<>();
        List<GoldPrice> updates = new ArrayList<>();
        for (GoldPrice row : rows) {
            GoldPrice old = exists.get(row.getTradeDate());
            if (old == null) {
                row.setSys001(now);
                row.setSys005(1);
                inserts.add(row);
            } else if (old.getClosePrice() == null || old.getClosePrice().compareTo(row.getClosePrice()) != 0
                    || !equalsPrice(old.getOpenPrice(), row.getOpenPrice())) {
                old.setOpenPrice(row.getOpenPrice());
                old.setClosePrice(row.getClosePrice());
                old.setLowPrice(row.getLowPrice());
                old.setHighPrice(row.getHighPrice());
                old.setSource(row.getSource());
                old.setSys002(now);
                updates.add(old);
            }
        }
        if (!inserts.isEmpty()) {
            priceDao.insertBatch(inserts);
        }
        for (GoldPrice p : updates) {
            priceDao.update(p);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("fetched", rows.size());
        result.put("inserted", inserts.size());
        result.put("updated", updates.size());
        result.put("syncedAt", now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return result;
    }

    /** 最新价：优先上金所 quotations，失败时回退本地最后一条日行情 */
    public Map<String, Object> latestQuote() {
        long nowMillis = System.currentTimeMillis();
        if (quoteCache != null && nowMillis - quoteCacheAt < QUOTE_CACHE_MILLIS) {
            return quoteCache;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            Map<String, Object> live = fetchLiveQuote();
            result.putAll(live);
            result.put("stale", false);
            quoteCache = result;
            quoteCacheAt = nowMillis;
            return result;
        } catch (Exception e) {
            log.warn("上金所最新价获取失败: {}", e.getMessage());
            GoldPrice latest = priceDao.selectLatest();
            if (latest == null) {
                throw new BusinessException(GlobalErrorCode.FAIL, "暂无法获取最新价，请先同步行情");
            }
            result.put("price", latest.getClosePrice());
            result.put("quoteTime", latest.getTradeDate().toString());
            result.put("delayStr", "");
            result.put("source", "LOCAL_DAILY");
            result.put("stale", true);
            quoteCache = result;
            quoteCacheAt = nowMillis;
            return result;
        }
    }

    public List<GoldPrice> listPrices(LocalDate startDate, LocalDate endDate) {
        QueryWrapper qw = QueryWrapper.create();
        if (startDate != null) {
            qw.where(new com.mybatisflex.core.query.QueryColumn("trade_date").ge(startDate));
        }
        if (endDate != null) {
            qw.where(new com.mybatisflex.core.query.QueryColumn("trade_date").le(endDate));
        }
        qw.orderBy(new com.mybatisflex.core.query.QueryColumn("trade_date").asc());
        return priceDao.selectListByQuery(qw);
    }

    public GoldPrice latestDaily() {
        return priceDao.selectLatest();
    }

    public void savePrice(GoldPrice price) {
        priceDao.insert(price);
    }

    public LocalDate earliestDate() {
        return priceDao.selectEarliestDate();
    }

    private Map<String, Object> fetchLiveQuote() throws Exception {
        HttpHeaders headers = defaultHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        String body = restTemplate.exchange(QUOTE_URL, org.springframework.http.HttpMethod.GET,
                entity, String.class).getBody();
        JsonNode root = objectMapper.readTree(body == null ? "{}" : body);
        BigDecimal price = decimalOrNull(root.get("now"));
        if (price == null && root.path("data").isArray() && root.path("data").size() > 0) {
            JsonNode last = root.path("data").get(root.path("data").size() - 1);
            if (last.isArray() && last.size() > 1) {
                price = decimalOrNull(last.get(1));
            }
        }
        if (price == null || price.signum() <= 0) {
            throw new IllegalStateException("quotations 无有效价格");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("price", price);
        result.put("quoteTime", root.path("delaystr").asText(""));
        result.put("delayStr", root.path("delaystr").asText(""));
        result.put("source", "SGE_QUOTATIONS");
        return result;
    }

    private String postDaily() {
        try {
            HttpHeaders headers = defaultHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("instid", "Au99.99");
            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);
            String body = restTemplate.postForObject(DAILY_URL, entity, String.class);
            if (body == null || body.trim().isEmpty()) {
                throw new IllegalStateException("空响应");
            }
            return body;
        } catch (Exception e) {
            throw new BusinessException(GlobalErrorCode.FAIL, "上金所日行情同步失败：" + e.getMessage());
        }
    }

    private List<GoldPrice> parseDaily(String body) {
        List<GoldPrice> rows = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode time = root.path("time");
            if (!time.isArray()) {
                return rows;
            }
            for (JsonNode item : time) {
                if (!item.isArray() || item.size() < 5) {
                    continue;
                }
                String dateStr = item.get(0).asText();
                if (dateStr.length() < 10) {
                    continue;
                }
                GoldPrice p = new GoldPrice();
                p.setTradeDate(LocalDate.parse(dateStr.substring(0, 10)));
                p.setOpenPrice(decimalOrNull(item.get(1)));
                p.setClosePrice(decimalOrNull(item.get(2)));
                p.setLowPrice(decimalOrNull(item.get(3)));
                p.setHighPrice(decimalOrNull(item.get(4)));
                if (p.getClosePrice() == null || p.getClosePrice().signum() <= 0) {
                    continue;
                }
                p.setSource("SGE");
                rows.add(p);
            }
            return rows;
        } catch (Exception e) {
            log.error("解析上金所日行情失败", e);
            throw new BusinessException(GlobalErrorCode.FAIL, "上金所日行情解析失败");
        }
    }

    private HttpHeaders defaultHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120 Safari/537.36");
        headers.set("Referer", "https://www.sge.com.cn/");
        headers.set("Accept", "application/json, text/plain, */*");
        return headers;
    }

    private BigDecimal decimalOrNull(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        try {
            String text = node.asText().trim();
            if (text.isEmpty()) {
                return null;
            }
            return new BigDecimal(text);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean equalsPrice(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return a == b;
        }
        return a.compareTo(b) == 0;
    }
}
