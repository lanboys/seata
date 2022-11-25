package com.work.business.utils;

import java.io.IOException;

import io.seata.core.context.RootContext;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * Created by lanbing at 2022/11/11 18:26
 */

@Slf4j
public class HttpUtil {

  private static OkHttpClient okHttpClient = new OkHttpClient();

  private static final String OrderBaseUrl = "http://localhost:9091";

  public static String placeOrder(String userId, String commodityCode, Integer count, boolean throwStockEx,
      boolean throwOrderEx) {

    try {
      String stockDeduct = OrderBaseUrl + "/order/placeOrder";

      FormBody.Builder builder = new FormBody.Builder();
      builder.add("userId", userId);
      builder.add("commodityCode", commodityCode);
      builder.add("count", count + "");
      builder.add("throwStockEx", throwStockEx + "");
      builder.add("throwOrderEx", throwOrderEx + "");
      FormBody requestBody = builder.build();

      // 创建请求
      Request request = new Request.Builder()
          .url(stockDeduct)
          .addHeader(RootContext.KEY_XID, RootContext.getXID())
          .post(requestBody)
          .build();

      Call call = okHttpClient.newCall(request);
      okhttp3.Response response = call.execute();
      if (response.isSuccessful() && response.body() != null) {
        String string = response.body().string();
        response.close();
        return string;
      }

      System.out.println("http请求结果：" + response);
      response.close();
      throw new RuntimeException("http请求结果异常");
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException(e.getLocalizedMessage());
    }
  }

}
