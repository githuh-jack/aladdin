package com.aladdin.biz.controller;

import com.aladdin.biz.entity.BizOrder;
import com.aladdin.biz.service.ShopService;
import com.aladdin.common.core.domain.R;
import com.aladdin.common.core.exception.BusinessException;
import com.aladdin.common.core.exception.GlobalErrorCode;
import com.aladdin.common.security.service.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 商店控制器：购买邮票/信封
 *
 * @author cles
 * @date 2026/09/15
 */
@RestController
@RequestMapping({"/shop", "/biz/shop"})
public class ShopController {

    @Autowired
    private ShopService shopService;

    /**
     * 购买商品
     * body: {itemType: 'stamp'|'envelope', itemId, quantity}
     */
    @PostMapping("/buy")
    public R<BizOrder> buy(@RequestBody Map<String, Object> body) {
        Long userId = LoginService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }
        String itemType = (String) body.get("itemType");
        Long itemId = Long.valueOf(body.get("itemId").toString());
        int quantity = Integer.parseInt(body.getOrDefault("quantity", "1").toString());
        BizOrder order = shopService.buy(userId, itemType, itemId, quantity);
        return R.ok("购买成功", order);
    }
}
