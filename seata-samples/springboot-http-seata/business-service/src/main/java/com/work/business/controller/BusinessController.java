package com.work.business.controller;

import com.work.business.service.BusinessService;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/business")
public class BusinessController {

  @Resource
  private BusinessService businessService;

  /**
   * 测试库存模块 不添加事务注解，代理类 StatementProxy AbstractConnectionProxy 等执行流程
   * <p>
   * 搜索 executeAutoCommitTrue
   */
  @RequestMapping("/placeOrder/commitNoTransactional")
  public String placeOrderCommitNoTransactional() {
    try {
      businessService.placeOrderNoTransactional("1", "product", 1, false, false, false);
      return "ok";
    } catch (Exception e) {
      e.printStackTrace();
      return e.getLocalizedMessage();
    }
  }

  @RequestMapping("/placeOrder/commit")
  public String placeOrderCommit() {
    try {
      businessService.placeOrder("1", "product", 1, false, false, false);
      return "ok";
    } catch (Exception e) {
      e.printStackTrace();
      return e.getLocalizedMessage();
    }
  }

  /**
   * 两个注解同时使用
   */
  @RequestMapping("/placeOrder/commitInBoth")
  public String placeOrderCommitInBoth() {
    try {
      businessService.placeOrderInBoth("1", "product", 1, false, false, false);
      return "ok";
    } catch (Exception e) {
      e.printStackTrace();
      return e.getLocalizedMessage();
    }
  }

  @RequestMapping("/placeOrder/commitOtherGlobalTx")
  public String placeOrderCommitOtherGlobalTx() {
    try {
      businessService.placeOrderOtherGlobalTx("1", "product", 1, false, false);
      return "ok";
    } catch (Exception e) {
      e.printStackTrace();
      return e.getLocalizedMessage();
    }
  }

  @RequestMapping("/placeOrder/rollback/stock")
  public String placeOrderRollbackStock() {
    try {
      businessService.placeOrder("1", "product", 1, true, false, false);
      return "ok";
    } catch (Exception e) {
      e.printStackTrace();
      return e.getLocalizedMessage();
    }
  }

  @RequestMapping("/placeOrder/rollback/order")
  public String placeOrderRollbackOrder() {
    try {
      businessService.placeOrder("1", "product", 1, false, true, false);
      return "ok";
    } catch (Exception e) {
      e.printStackTrace();
      return e.getLocalizedMessage();
    }
  }

  @RequestMapping("/placeOrder/rollback/business")
  public String placeOrderRollbackBusiness() {
    try {
      businessService.placeOrder("1", "product", 1, false, false, true);
      return "ok";
    } catch (Exception e) {
      e.printStackTrace();
      return e.getLocalizedMessage();
    }
  }
}
