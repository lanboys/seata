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
  public String deduct(String commodityCode, Integer count, boolean throwStockEx) {
    try {
      stockService.deduct(commodityCode, count, throwStockEx);
      return "ok";
    } catch (Exception e) {
      e.printStackTrace();
      return e.getLocalizedMessage();
    }
  }
}
