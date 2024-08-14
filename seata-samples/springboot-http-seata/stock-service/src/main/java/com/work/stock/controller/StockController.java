package com.work.stock.controller;

import com.work.stock.service.StockService;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/stock")
public class StockController {

  @Resource
  private StockService stockService;

  /**
   * 减库存
   *
   * @param commodityCode 商品代码
   * @param count         数量
   */
  @RequestMapping(path = "/deduct")
  public String deduct(String commodityCode, Integer count, int sleepSec, boolean throwStockEx, boolean inTransactional) {
    try {
      if (inTransactional) {
        stockService.deduct(commodityCode, count, sleepSec, throwStockEx);
      } else {
        stockService.deductNoTransactional(commodityCode, count, throwStockEx);
      }
      return "ok";
    } catch (Exception e) {
      e.printStackTrace();
      return e.getLocalizedMessage();
    }
  }
}
