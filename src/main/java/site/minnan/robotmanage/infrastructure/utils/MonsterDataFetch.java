package site.minnan.robotmanage.infrastructure.utils;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.ReUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import site.minnan.robotmanage.entity.dao.MonsterRepository;
import site.minnan.robotmanage.infrastructure.config.ProxyConfig;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.net.Proxy;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class MonsterDataFetch {

    public static final String baseUrl = "https://maplestory.fandom.com";

    static Proxy proxy;

    static Map<String, Integer> sacCache;


    static {
        proxy = new ProxyConfig().proxy();
        sacCache = new HashMap<>();
    }

    public void fetchMonsterData(String url, Iterator<Integer> idItr) throws InterruptedException {
//        Proxy proxy = new ProxyConfig().proxy();
//        this.proxy = proxy;
//        String url = "https://maplestory.fandom.com/wiki/Monster/Level_281_-_290";
//        String url = "https://maplestory.fandom.com/wiki/Despairing_Hunter#Carcion";
        HttpResponse res = HttpUtil.createGet(url).setProxy(proxy).execute();
        String html = res.body();
        Document doc = Jsoup.parse(html);
        Elements tableBody = doc.selectXpath("//*[@id=\"mw-content-text\"]/div/table[2]/tbody");
        Iterator<Element> lineItr = tableBody.get(0).children().iterator();
        lineItr.next();
        lineItr.next();

        ArrayList<Integer> dataIndex = ListUtil.toList(0, 1, 2, 6);

        while (lineItr.hasNext()) {
            Element line = lineItr.next();
            Elements children = line.children();
//            StringBuilder sb = new StringBuilder(name);
            if (children.first().text().contains("Boss")) {
                continue;
            }
            Element nameA = line.selectXpath("td[1]/b/a").first();
            String name = nameA.text();
            String href = nameA.attr("href");
            String imgUrl = line.selectXpath("td[1]/a").first().attr("href");
//            saveImg(imgUrl, nameA.text());

            String lv = children.get(1).text();
            String hp = children.get(2).text().replaceAll(",", "");
            String exp = children.get(6).text().replaceAll(",", "");

//            String s = dataIndex.stream().map(children::get).map(Element::text).collect(Collectors.joining(","));
//            String s = children.stream().map(e -> e.text()).collect(Collectors.joining(","));
            String locations = getLocation(href);
//            s = s + "," + locations;

            Integer id = idItr.next();
            String lineString = "%d,%s,%s,%s,%s,%s".formatted(id, name, lv, hp, exp, locations);
            System.out.println(lineString);
            Thread.sleep(1000);
        }
    }

    public String getLocation(String url) {
        url = baseUrl + url;
        HttpResponse res = HttpUtil.createGet(url).setProxy(proxy).execute();
        String html = res.body();
        Document doc = Jsoup.parse(html);
        Elements table = doc.selectXpath("//*[@id=\"mw-content-text\"]/div/table[1]/tbody");
        Elements lines = table.first().children();
        Optional<Element> locationOpt = lines.stream().filter(e -> e.children().first().text().contains("Locations"))
                .findFirst();
        Element locationTr = locationOpt.get();
        Elements lis = locationTr.select("li");
        List<String> locations = new ArrayList<>();
        for (Element locationEle : lis) {
            String location = locationEle.text();
            Elements hrefEle = locationEle.select("a");
            if (!hrefEle.isEmpty()) {
                String href = hrefEle.get(0).attr("href");
                int sac = getLocationSac(href, location);
                location = "%s(%d)".formatted(location, sac);
            }
            locations.add(location);
        }
        return String.join("、", locations);
    }

    public int getLocationSac(String mapUrl, String name) {
        if (sacCache.containsKey(name)) {
            return sacCache.get(name);
        }
        String url = baseUrl + mapUrl;
        HttpResponse res = HttpUtil.createGet(url).setProxy(proxy).execute();
        String html = res.body();
        Document doc = Jsoup.parse(html);

        Pattern pattern = Pattern.compile("Sacred Power/Authentic Force: (\\d+)");
        Elements lines = doc.selectXpath("//*[@id=\"mw-content-text\"]/div/table[1]/tbody/tr");
        Optional<String> text = lines.stream()
                .flatMap(e -> e.children().stream())
                .filter(e -> ReUtil.isMatch(pattern, e.text()))
                .findFirst()
                .map(e -> e.text());
        String s = text.get();
        String sacNeed = ReUtil.getGroup1(pattern, s);
        int sac = Integer.parseInt(sacNeed);
        sacCache.put(name, sac);
        return sac;
    }

    public void saveImg(String url, String name) {
        HttpResponse response = HttpUtil.createGet(url).setProxy(proxy).execute();
        InputStream stream = response.bodyStream();
        BufferedImage img = ImgUtil.read(stream);
        ImgUtil.write(img, new File("F:\\pdf\\monster\\%s.png".formatted(name)));
    }

    public static void main(String[] args) throws InterruptedException {
        String url = "https://maplestory.fandom.com/wiki/Monster/Level_271_-_280";
        MonsterDataFetch m = new MonsterDataFetch();
//        m.saveImg("https://static.wikia.nocookie.net/maplestory/images/f/fe/Mob_Despairing_Hunter.png/revision/latest?cb=20230803094035", "Despairing Hunter");
        Iterator<Integer> idItr = Stream.iterate(15, i -> i + 1).iterator();
        m.fetchMonsterData(url, idItr);
//        String location = m.getLocation("/wiki/Shadowy_Black_Panther#Carcion");
//        System.out.println(location);
//        m.readMonster("F:\\Minnan\\怪物数据.xlsx", null);
    }
}
