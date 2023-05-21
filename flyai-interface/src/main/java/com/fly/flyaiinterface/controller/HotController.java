package com.fly.flyaiinterface.controller;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fly.flyapiclientsdk.model.WeboHot;
import com.google.gson.Gson;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping( "/hot" )
public class HotController {

    @GetMapping( "/get" )
    public List<WeboHot> getHot() throws IOException {
        String plantform ="微博";
        URL url = new URL("https://api.oioweb.cn/api/common/HotList");
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
        httpURLConnection.setRequestMethod("GET");
        httpURLConnection.connect();


        List<WeboHot> weboHotList = new ArrayList<>();
        int responseCode = httpURLConnection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            InputStream inputStream = httpURLConnection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder responseData = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                responseData.append(line);
            }
            reader.close();
            inputStream.close();
            String result = responseData.toString();
            // 处理返回的数据
            JSONObject jsonObject = new JSONObject(result);
            JSONObject data = jsonObject.getJSONObject("result");
            JSONArray jsonArray = data.getJSONArray(plantform);

            for (int i = 0; i < 10; i++) {
                JSONObject item = jsonArray.getJSONObject(i);
                String index = item.getStr("index");
                String title = item.getStr("title");
                String hot = item.getStr("hot");
                String href = item.getStr("href");
                WeboHot weboHot = new WeboHot(plantform,index, title, hot, href);
                weboHotList.add(weboHot);
            }
        }

        // 将weboHotList转换为JSON字符串
  /*      ObjectMapper objectMapper = new ObjectMapper();
        String json = "";
        try {
            json = objectMapper.writeValueAsString(weboHotList);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }*/

        return weboHotList;
    }
}
