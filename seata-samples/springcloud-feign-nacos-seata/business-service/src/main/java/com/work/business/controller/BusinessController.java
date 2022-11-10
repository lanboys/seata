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
package com.work.business.controller;

import com.work.business.service.BusinessService;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("business")
public class BusinessController {

  @Resource
  private BusinessService businessService;

  @RequestMapping("/placeOrder/commit")
  public Boolean placeOrderCommit() {
    businessService.placeOrder("1", "product-1", 1);
    return true;
  }

  @RequestMapping("/placeOrder/commitOtherGlobalTx")
  public Boolean placeOrderCommitOtherGlobalTx() {
    businessService.placeOrderOtherGlobalTx("1", "product-1", 1);
    return true;
  }

  @RequestMapping("/placeOrder/rollback")
  public Boolean placeOrderRollback() {
    // product-2 扣库存时模拟了一个业务异常,
    businessService.placeOrder("1", "product-2", 1);
    return true;
  }

  @RequestMapping("/placeOrder")
  public Boolean placeOrder(String userId, String commodityCode, Integer count) {
    businessService.placeOrder("1", "product-1", 1);
    return true;
  }
}
