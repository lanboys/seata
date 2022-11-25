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
package com.work.stock.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.work.stock.entity.Stock;
import com.work.stock.repository.StockDAO;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
@Service
public class StockService {

  @Resource
  private StockDAO stockDAO;

  /**
   * 减库存
   */
  @Transactional(rollbackFor = Exception.class)
  public void deduct(String commodityCode, int count, boolean throwStockEx) {
    QueryWrapper<Stock> wrapper = new QueryWrapper<>();
    wrapper.setEntity(new Stock().setCommodityCode(commodityCode));
    Stock stock = stockDAO.selectOne(wrapper);
    stock.setCount(stock.getCount() - count);

    stockDAO.updateById(stock);
    sleep(2);

    if (throwStockEx) {
      throw new RuntimeException("扣减库存异常");
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
