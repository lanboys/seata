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

import io.seata.spring.annotation.GlobalLock;

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
  public void deduct(String commodityCode, int count, int sleepSec, boolean throwStockEx) {
    QueryWrapper<Stock> wrapper = new QueryWrapper<>();
    wrapper.setEntity(new Stock().setCommodityCode(commodityCode));
    Stock stock = stockDAO.selectOne(wrapper);
    stock.setCount(stock.getCount() - count);

    stockDAO.updateById(stock);
    sleep(sleepSec);

    if (throwStockEx) {
      throw new RuntimeException("扣减库存异常");
    }
  }

  /**
   * 测试 没事务注解还会不会注册分支事务，答案是会的，只要有提交就会进行注册，前提是是在全局事务中，也就是 有xid
   * <p>
   * AbstractDMLBaseExecutor类中 搜索 executeAutoCommitTrue，没有事务注解，每执行一条 DML sql，会提交一次，每提交一次就会注册一次事务分支
   */
  public void deductNoTransactional(String commodityCode, int count, boolean throwStockEx) {
    QueryWrapper<Stock> wrapper = new QueryWrapper<>();
    wrapper.setEntity(new Stock().setCommodityCode(commodityCode));
    Stock stock = stockDAO.selectOne(wrapper);

    stock.setCount(stock.getCount() - count);
    stockDAO.updateById(stock);// 第一次提交, 注册分支 一

    for (int i = 0; i < 90; i++) {
      System.out.print(" =");
    }
    System.out.println();

    stock.setCount(stock.getCount() - count);
    stockDAO.updateById(stock);// 第二次提交, 注册分支 二

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
