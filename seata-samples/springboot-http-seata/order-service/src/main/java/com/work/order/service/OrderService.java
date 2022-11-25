/*
 *  Copyright 1999-2021 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.work.order.service;

import com.work.order.model.Order;
import com.work.order.repository.OrderDAO;
import com.work.order.utils.HttpUtil;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.math.BigDecimal;

/**
 * Program Name: springcloud-nacos-seata
 * <p>
 * Description:
 * <p>
 *
 * @author zhangjianwei
 * @version 1.0
 */
@Service
public class OrderService {

  @Resource
  private OrderDAO orderDAO;

  /**
   * 下单：创建订单、减库存，涉及到两个服务
   * <p>
   * https://mp.weixin.qq.com/s/fzlr-6pDPWKbwVuJlXe8sA
   * https://mp.weixin.qq.com/s/6DOtO5OQyCL8bR03Z-3q9A
   */
  //@GlobalTransactional 放到business
  @Transactional(rollbackFor = Exception.class)
  public void placeOrder(String userId, String commodityCode, Integer count, boolean throwStockEx, boolean throwOrderEx) {
    BigDecimal orderMoney = new BigDecimal(count).multiply(new BigDecimal(5));
    Order order = new Order().setUserId(userId).setCommodityCode(commodityCode).setCount(count).setMoney(orderMoney);

    orderDAO.insert(order);

    String result = HttpUtil.stockDeduct(commodityCode, count, throwStockEx);
    // 这里必须知道库存扣减是否成功，如果扣减失败就抛异常，如果扣减失败却不抛异常(可以注释掉试试)，那么最终结果数据不一致，成功下订单，库存扣减失败
    if (!"ok".equals(result)) {
      throw new RuntimeException(result);
    }

    if (throwOrderEx) {
      throw new RuntimeException("订单异常");
    }
  }

  private void sleep(int sec) {
    for (int i = 0; i < sec; i++) {
      try {
        System.out.println(">>>>>>>>>>>> sleep in " + i + " s <<<<<<<<<<<<");
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
}
