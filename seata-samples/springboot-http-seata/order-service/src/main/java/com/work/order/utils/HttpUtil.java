package com.work.order.utils;

import java.io.IOException;

import io.seata.core.context.RootContext;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * Created by lanbing at 2022/11/11 18:26
 */

@Slf4j
public class HttpUtil {

  private static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json;charset=utf-8");

  private static OkHttpClient okHttpClient = new OkHttpClient();

  private static final String StockBaseUrl = "http://localhost:9092";

  public static void stockDeduct(String commodityCode, Integer count) {
    try {
      String stockDeduct = StockBaseUrl + "/stock/deduct";

      FormBody.Builder builder = new FormBody.Builder();
      builder.add("commodityCode", commodityCode);
      builder.add("count", count + "");
      FormBody requestBody = builder.build();

      // 创建请求
      Request request = new Request.Builder()
          .url(stockDeduct)
          .addHeader(RootContext.KEY_XID, RootContext.getXID())
          .post(requestBody)
          .build();

      Call call = okHttpClient.newCall(request);
      okhttp3.Response response = call.execute();
      if (response.isSuccessful()) {
        if (response.body() != null) {
          System.out.println(response.body().string());
          return;
        }
      }
      System.out.println(response);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  // private <Y> Y post(String url, Object param, Class<Y> clazz) {
  //     try {
  //         // String content = new JsonMapper().toJson(param);
  //         String content = JSONObject.toJSONString(param);
  //         okhttp3.RequestBody requestBody = okhttp3.RequestBody.create(MEDIA_TYPE_JSON, content);
  //         Request request = new Request.Builder().url(url).post(requestBody).build();
  //         Response response = okHttpClient.newCall(request).execute();
  //         String result = response.body().string();
  //         log.info("Post | url：{}, param: {}, result: {}", url, content, result);
  //     } catch (IOException e) {
  //         log.error("Post | url：{}, param: {}, error: ", url, param, e);
  //     }
  // }

}
