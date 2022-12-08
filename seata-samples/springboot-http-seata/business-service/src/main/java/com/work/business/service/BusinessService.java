package com.work.business.service;

import com.work.business.utils.HttpUtil;

import io.seata.core.context.RootContext;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class BusinessService {

  @GlobalTransactional
  public void placeOrderNoTransactional(String userId, String commodityCode, Integer count,
      boolean throwStockEx, boolean throwOrderEx, boolean throwBusinessEx) {

    doPlaceOrder(userId, commodityCode, count, throwStockEx, throwOrderEx, throwBusinessEx, false);
  }

  @GlobalTransactional
  public void placeOrder(String userId, String commodityCode, Integer count,
      boolean throwStockEx, boolean throwOrderEx, boolean throwBusinessEx) {

    doPlaceOrder(userId, commodityCode, count, throwStockEx, throwOrderEx, throwBusinessEx, true);
  }

  /**
   * 两个注解同时使用
   */
  @GlobalTransactional
  @Transactional
  public void placeOrderInBoth(String userId, String commodityCode, Integer count,
      boolean throwStockEx, boolean throwOrderEx, boolean throwBusinessEx) {

    doPlaceOrder(userId, commodityCode, count, throwStockEx, throwOrderEx, throwBusinessEx, true);
  }

  private void doPlaceOrder(String userId, String commodityCode, Integer count,
      boolean throwStockEx, boolean throwOrderEx, boolean throwBusinessEx, boolean inTransactional) {

    String result = HttpUtil.placeOrder(userId, commodityCode, count, throwStockEx, throwOrderEx, inTransactional);
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

    String result = HttpUtil.placeOrder(userId, commodityCode, count, throwStockEx, throwOrderEx, true);
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
