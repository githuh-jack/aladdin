package com.aladdin.biz.service;

import com.aladdin.biz.dao.BizCheckinRecordDao;
import com.aladdin.biz.dao.BizCoinDao;
import com.aladdin.biz.dao.BizCoinLogDao;
import com.aladdin.biz.dao.BizCreditLogDao;
import com.aladdin.biz.dao.BizDiaryDao;
import com.aladdin.biz.dao.BizLetterDao;
import com.aladdin.biz.dao.BizUserDao;
import com.aladdin.biz.entity.BizCheckinRecord;
import com.aladdin.biz.entity.BizCoin;
import com.aladdin.biz.entity.BizCoinLog;
import com.aladdin.biz.entity.BizCreditLog;
import com.aladdin.biz.entity.BizUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 奖励服务：铜钱奖励(每日登录/连续签到/首信/首日记/完善资料/邀请)与小星星(每寄一封信+1)
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
    private BizCreditLogDao creditLogDao;

    @Autowired
    private BizUserDao userDao;

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

    // ==================== 信用分 ====================

    /**
     * 调整信用分并写流水(delta可正可负，下限0；用户不存在或delta=0忽略)
     */
    public void adjustCredit(Long userId, int delta, String reason) {
        if (userId == null || delta == 0) {
            return;
        }
        BizUser user = userDao.selectOneById(userId);
        if (user == null) {
            return;
        }
        int current = user.getCreditScore() == null ? 100 : user.getCreditScore();
        int balance = Math.max(0, current + delta);
        BizUser update = new BizUser();
        update.setId(userId);
        update.setCreditScore(balance);
        userDao.update(update);
        BizCreditLog log = new BizCreditLog();
        log.setUserId(userId);
        log.setChangeVal(delta);
        log.setBalance(balance);
        log.setReason(reason);
        log.setSys001(LocalDateTime.now());
        log.setSys005(1);
        log.setSys006("biz");
        creditLogDao.insert(log);
    }

    // ==================== 每日登录 + 连续签到 ====================

    /**
     * 注册奖励：新用户注册后发放欢迎铜钱(金额取 biz_config 的 reward.register，默认100)
     */
    public void onRegistered(Long userId) {
        try {
            int bonus = configService.getInt("reward.register", 100);
            if (bonus > 0) {
                addCoins(userId, bonus, "register", "注册奖励");
            }
        } catch (Exception ignored) {
            // 奖励发放失败不影响注册主流程
        }
    }

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
     * 寄出信件后调用：当日第一封信奖励 + 小星星+1(每封信都有)
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
            userDao.addStar(userId);
        } catch (Exception ignored) {
            // 奖励失败不影响写信主流程
        }
    }

    /**
     * 新建日记后调用：当日写日记奖励
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
        } catch (Exception ignored) {
            // 奖励失败不影响写日记主流程
        }
    }

    private LocalDateTime todayStart() {
        return LocalDate.now().atStartOfDay();
    }
}
