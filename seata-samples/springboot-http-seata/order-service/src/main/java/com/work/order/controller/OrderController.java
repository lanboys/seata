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
package com.work.order.controller;

import com.work.order.service.OrderService;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * Program Name: springcloud-nacos-seata
 * <p>
 * Description:
 * <p>
 *
 * @author zhangjianwei
 * @version 1.0
 */
@RestController
@RequestMapping("/order")
public class OrderController {

  @Resource
  private OrderService orderService;

  /**
   * 下单：插入订单表、扣减库存，模拟回滚
   */
  @RequestMapping("/placeOrder")
  public String placeOrder(String userId, String commodityCode, Integer count,
      boolean throwStockEx, boolean throwOrderEx, boolean inTransactional) {
    try {
      orderService.placeOrder(userId, commodityCode, count, throwStockEx, throwOrderEx, inTransactional);
      return "ok";
    } catch (Exception e) {
      e.printStackTrace();
      return e.getLocalizedMessage();
    }
  }
}
