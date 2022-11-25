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
package com.work.business.service;

import com.work.business.utils.HttpUtil;

import io.seata.core.context.RootContext;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BusinessService {

  @GlobalTransactional
  public void placeOrder(String userId, String commodityCode, Integer count,
      boolean throwStockEx, boolean throwOrderEx, boolean throwBusinessEx) {

    String result = HttpUtil.placeOrder(userId, commodityCode, count, throwStockEx, throwOrderEx);
    if (!"ok".equals(result)) {
      throw new RuntimeException(result);
    }
    sleep(1);

    if (throwBusinessEx) {
      sleep(5);// 增大时间查看undo_log
      throw new RuntimeException("业务异常");
    }
  }

  /**
   * 跟上面的方法同时调用，虽然可以开启一个新的全局事务，但是仍然无法提交，因为有全局锁，具体怎么实现，以后再看
   */
  @GlobalTransactional
  public void placeOrderOtherGlobalTx(String userId, String commodityCode, Integer count,
      boolean throwStockEx, boolean throwOrderEx) {

    String result = HttpUtil.placeOrder(userId, commodityCode, count, throwStockEx, throwOrderEx);
    if (!"ok".equals(result)) {
      throw new RuntimeException(result);
    }
  }

  private void sleep(int sec) {
    for (int i = 0; i < sec; i++) {
      try {
        String xid = RootContext.getXID();
        log.info(">>>>>>>>>>>> xid: {}, sleep in {} s <<<<<<<<<<<<", xid, i);
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
}
