package com.fly.flyaiinterface.controller;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.fly.flyapiclientsdk.model.AI;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

@RestController
@RequestMapping( "/ai" )
public class AIController {

    @GetMapping( "/sendmsg" )
    public String getENTOZH(String sourceText) throws IOException {
        URL url = new URL("https://api.oioweb.cn/api/txt/QQFanyi?soucesText=" + sourceText);
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
        httpURLConnection.setRequestMethod("GET");
        httpURLConnection.connect();

        int responseCode = httpURLConnection.getResponseCode();
        AI ai = new AI();

        if (responseCode == 200) {
            InputStream inputStream = httpURLConnection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder responseData = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                responseData.append(line);
            }
            reader.close();
            inputStream.close();
            String   result = responseData.toString();
            System.out.println(result);


            JSONObject jsonObject = new JSONObject(result);
            JSONObject data = jsonObject.getJSONObject("result");

            System.out.println(data);
            JSONArray jsonArray = data.getJSONArray(sourceText);
            System.out.println(jsonArray);
        }
        return "ok";
    }
}
