package com.fly.flyaiinterface.controller;

import cn.hutool.json.JSONUtil;
import com.fly.flyapiclientsdk.model.Picture;
import com.fly.flyapiclientsdk.model.Query.PictureRequest;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 获取相应文本的图片
 * 通过jsoup进行到bing上抓取，注意版本需要时1.15.3，不能换成1.16
 */
@RestController
@RequestMapping( "/picture" )
public class PictureController {

    @PostMapping( "/getPicture" )
    public String getBingPicture(@RequestBody PictureRequest pictureRequest) {
        String searchText = pictureRequest.getSearchText();
        int pageSize = pictureRequest.getPageSize();
        Random random1 =new Random();
        pageSize = random1.nextInt(10);
        String url = String.format("https://www.bing.com/images/search?q=%s&first=%s", searchText, pageSize);
        Document doc = null;
        try {
            doc = Jsoup.connect(url)
                    .header("Referer", "https://www.bing.com/")
                    .timeout(1000)
                    .get();
        } catch (IOException e) {
            throw new RuntimeException("输入错误");
        }
        List<Picture> list = new ArrayList<>();
        int count =0;
        Elements select = doc.select(".iuscp.isv ");
        for (Element element : select) {
            // 图片地址
            String s = element.select(".iusc").get(0).attr("m");
            Map map = JSONUtil.toBean(s, Map.class);
            String murl = (String) map.get("murl");

            String title = element.select(".inflnk").get(0).attr("aria-label");

            Picture newPicture = new Picture();
            newPicture.setTitle(title);
            newPicture.setUrl(murl);
            list.add(newPicture);
            count++;
        }
        Random random =new Random();
        int randomIndex = random.nextInt(count);
        return list.get(randomIndex).getUrl();
    }
}
