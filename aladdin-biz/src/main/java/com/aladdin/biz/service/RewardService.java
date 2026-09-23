package com.aladdin.biz.service;

import com.aladdin.biz.dao.BizBambooDao;
import com.aladdin.biz.dao.BizBambooLogDao;
import com.aladdin.biz.dao.BizCheckinRecordDao;
import com.aladdin.biz.dao.BizCoinDao;
import com.aladdin.biz.dao.BizCoinLogDao;
import com.aladdin.biz.dao.BizDiaryDao;
import com.aladdin.biz.dao.BizLetterDao;
import com.aladdin.biz.entity.BizBamboo;
import com.aladdin.biz.entity.BizBambooLog;
import com.aladdin.biz.entity.BizCheckinRecord;
import com.aladdin.biz.entity.BizCoin;
import com.aladdin.biz.entity.BizCoinLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 奖励服务：铜钱奖励(每日登录/连续签到/首信/首日记/完善资料/邀请)与文竹生长
 * 金额均来自 biz_config 配置表，后台可修改
 *
 * @author cles
 * @date 2026/09/20
 */
@Service
public class RewardService {

    @Autowired
    private BizCoinDao coinDao;

    @Autowired
    private BizCoinLogDao coinLogDao;

    @Autowired
    private BizCheckinRecordDao checkinDao;

    @Autowired
    private BizBambooDao bambooDao;

    @Autowired
    private BizBambooLogDao bambooLogDao;

    @Autowired
    private BizLetterDao letterDao;

    @Autowired
    private BizDiaryDao diaryDao;

    @Autowired
    private ConfigService configService;

    // ==================== 铜钱 ====================

    /** 给用户加铜钱并写流水(账户不存在则自动创建；amount<=0 忽略) */
    public void addCoins(Long userId, int amount, String logType, String remark) {
        if (userId == null || amount <= 0) {
            return;
        }
        ensureAccount(userId);
        if (coinDao.adjustBalance(userId, amount) == 0) {
            return;
        }
        BizCoin coin = coinDao.selectByUserId(userId);
        BizCoinLog log = new BizCoinLog();
        log.setUserId(userId);
        log.setChangeAmount(amount);
        log.setBalanceAfter(coin == null ? amount : coin.getBalance());
        log.setLogType(logType);
        log.setRemark(remark);
        log.setSys001(LocalDateTime.now());
        log.setSys005(1);
        log.setSys006("biz");
        coinLogDao.insert(log);
    }

    private void ensureAccount(Long userId) {
        if (coinDao.selectByUserId(userId) == null) {
            BizCoin coin = new BizCoin();
            coin.setUserId(userId);
            coin.setBalance(0);
            coin.setSys001(LocalDateTime.now());
            coin.setSys005(1);
            coin.setSys006("biz");
            try {
                coinDao.insert(coin);
            } catch (Exception ignored) {
                // 并发下已由其他请求创建
            }
        }
    }

    // ==================== 每日登录 + 连续签到 ====================

    /**
     * 每日首次活动签到：发放每日登录奖励，连续签到满7/30天发额外奖励
     * 由拦截器在用户当日首个业务请求时触发，每日限1次
     */
    public void doDailyCheckin(Long userId) {
        LocalDate today = LocalDate.now();
        if (checkinDao.countByUserAndDate(userId, today) > 0) {
            return;
        }
        int streak = computeStreak(userId, today);
        int daily = configService.getInt("reward.daily_login", 10);
        // 先写签到记录占位(当日唯一)，失败说明已签到
        BizCheckinRecord record = new BizCheckinRecord();
        record.setUserId(userId);
        record.setCheckinDate(today);
        record.setStreakDays(streak);
        record.setCoin(daily);
        record.setSys001(LocalDateTime.now());
        record.setSys005(1);
        record.setSys006("biz");
        try {
            checkinDao.insert(record);
        } catch (Exception e) {
            return;
        }
        if (daily > 0) {
            addCoins(userId, daily, "reward", "每日登录奖励");
        }
        if (streak == 7) {
            int bonus = configService.getInt("reward.streak_7", 50);
            if (bonus > 0) {
                addCoins(userId, bonus, "reward", "连续签到7天额外奖励");
            }
        } else if (streak == 30) {
            int bonus = configService.getInt("reward.streak_30", 100);
            if (bonus > 0) {
                addCoins(userId, bonus, "reward", "连续签到30天额外奖励");
            }
        }
    }

    /** 连续签到天数(含今天)：从昨天往前数连续签到记录 */
    private int computeStreak(Long userId, LocalDate today) {
        int streak = 0;
        LocalDate date = today.minusDays(1);
        while (streak < 30 && checkinDao.countByUserAndDate(userId, date) > 0) {
            streak++;
            date = date.minusDays(1);
        }
        return streak + 1;
    }

    // ==================== 写信 / 写日记 ====================

    /**
     * 寄出信件后调用：当日第一封信奖励 + 文竹按字数生长
     */
    public void onLetterSent(Long userId, Long letterId, String content) {
        try {
            LocalDateTime start = todayStart();
            if (letterDao.countSentToday(userId, start) == 1) {
                int reward = configService.getInt("reward.first_letter_daily", 10);
                if (reward > 0) {
                    addCoins(userId, reward, "reward", "当日第一封信奖励");
                }
            }
            int len = content == null ? 0 : content.length();
            int cm = letterBambooCm(len);
            if (cm > 0) {
                growBamboo(userId, cm, "letter", len, letterId);
            }
        } catch (Exception ignored) {
            // 奖励/生长失败不影响写信主流程
        }
    }

    /**
     * 新建日记后调用：当日写日记奖励 + 文竹生长(每天最多1cm)
     */
    public void onDiaryCreated(Long userId, Long diaryId, String content) {
        try {
            LocalDateTime start = todayStart();
            if (diaryDao.countToday(userId, start) == 1) {
                int reward = configService.getInt("reward.first_diary_daily", 10);
                if (reward > 0) {
                    addCoins(userId, reward, "reward", "当日写日记奖励");
                }
            }
            int threshold = configService.getInt("bamboo.diary.threshold", 100);
            int maxCm = configService.getInt("bamboo.diary.daily_max_cm", 1);
            int len = content == null ? 0 : content.length();
            if (maxCm > 0 && len >= threshold
                    && bambooLogDao.countDiaryGrowthToday(userId, start) == 0) {
                growBamboo(userId, maxCm, "diary", len, diaryId);
            }
        } catch (Exception ignored) {
            // 奖励/生长失败不影响写日记主流程
        }
    }

    /** 信件文竹生长：达到各字数档位各+1cm，单封最多 thresholds 档(默认1500字/5cm) */
    private int letterBambooCm(int len) {
        int[] thresholds = configService.getIntArray("bamboo.letter.thresholds",
                new int[]{100, 300, 600, 1000, 1500});
        int cm = 0;
        for (int threshold : thresholds) {
            if (len >= threshold) {
                cm++;
            }
        }
        return cm;
    }

    /** 文竹长高并写生长记录(仅后台可见) */
    private void growBamboo(Long userId, int cm, String type, int len, Long refId) {
        ensureBamboo(userId);
        if (bambooDao.grow(userId, cm) == 0) {
            return;
        }
        BizBambooLog log = new BizBambooLog();
        log.setUserId(userId);
        log.setBizType(type);
        log.setAddCm(cm);
        log.setContentLen(len);
        log.setRefId(refId);
        log.setSys001(LocalDateTime.now());
        log.setSys005(1);
        log.setSys006("biz");
        bambooLogDao.insert(log);
    }

    /** 确保用户有文竹(每个用户一株) */
    public void ensureBamboo(Long userId) {
        if (bambooDao.selectByUserId(userId) == null) {
            BizBamboo bamboo = new BizBamboo();
            bamboo.setUserId(userId);
            bamboo.setHeightCm(0);
            bamboo.setSys001(LocalDateTime.now());
            bamboo.setSys005(1);
            bamboo.setSys006("biz");
            try {
                bambooDao.insert(bamboo);
            } catch (Exception ignored) {
                // 并发下已由其他请求创建
            }
        }
    }

    private LocalDateTime todayStart() {
        return LocalDate.now().atStartOfDay();
    }
}
