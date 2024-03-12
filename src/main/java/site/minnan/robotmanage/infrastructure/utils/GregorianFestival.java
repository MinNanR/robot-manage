package site.minnan.robotmanage.infrastructure.utils;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.lang.Pair;
import cn.hutool.core.map.TableMap;
import org.hibernate.boot.jaxb.mapping.ManagedType;

import java.lang.management.ManagementFactory;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/**
 * 公历节日记录
 *
 * @author Minnan on 2024/03/11
 */
public class GregorianFestival {

    private static class Triple {
        Integer field1;
        Integer field2;
        Integer field3;

        private Triple(Integer a, Integer b, Integer c) {
            this.field1 = a;
            this.field2 = b;
            this.field3 = c;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Triple triple = (Triple) o;

            if (!Objects.equals(field1, triple.field1)) return false;
            if (!Objects.equals(field2, triple.field2)) return false;
            return Objects.equals(field3, triple.field3);
        }

        @Override
        public int hashCode() {
            int result = field1 != null ? field1.hashCode() : 0;
            result = 31 * result + (field2 != null ? field2.hashCode() : 0);
            result = 31 * result + (field3 != null ? field3.hashCode() : 0);
            return result;
        }
    }

    private static final TableMap<Pair<Integer, Integer>, String> M_FTV = new TableMap<>(16);

    private static final TableMap<Triple, String> W_FTV = new TableMap<>(16);


    static {
        M_FTV.put(new Pair<>(1, 1), "元旦");
        M_FTV.put(new Pair<>(1, 10), "中国人民警察节");
        M_FTV.put(new Pair<>(1, 21), "国际拥抱日");

        M_FTV.put(new Pair<>(2, 2), "世界湿地日");
        M_FTV.put(new Pair<>(2, 14), "情人节");

        M_FTV.put(new Pair<>(3, 5), "学雷锋纪念日");
        M_FTV.put(new Pair<>(3, 6), "世界青光眼日");
        M_FTV.put(new Pair<>(3, 8), "妇女节");
        M_FTV.put(new Pair<>(3, 12), "植树节");
        M_FTV.put(new Pair<>(3, 15), "国际消费者权益日");

        M_FTV.put(new Pair<>(4, 1), "愚人节");
        M_FTV.put(new Pair<>(4, 22), "世界地球日");
        M_FTV.put(new Pair<>(4, 23), "世界读书日");

        M_FTV.put(new Pair<>(5, 1), "国际劳动节");
        M_FTV.put(new Pair<>(5, 4), "中国青年节");
        M_FTV.put(new Pair<>(5, 8), "世界红十字日");
        M_FTV.put(new Pair<>(5, 12), "国际护士节");
        W_FTV.put(new Triple(5, 2, 1), "母亲节");

        M_FTV.put(new Pair<>(6, 1), "国际儿童节");
        M_FTV.put(new Pair<>(6, 23), "国际奥林匹克日");
        W_FTV.put(new Triple(6, 3, 1), "父亲节");

        M_FTV.put(new Pair<>(7, 1), "建党节,香港回归纪念日");

        M_FTV.put(new Pair<>(8, 1), "建军节");
        M_FTV.put(new Pair<>(8, 12), "国际青年节");
        M_FTV.put(new Pair<>(8, 19), "中国医师节");

        M_FTV.put(new Pair<>(9, 3), "中国人民抗日战争胜利纪念日");
        M_FTV.put(new Pair<>(9, 10), "教师节");
        M_FTV.put(new Pair<>(9, 30), "烈士纪念日");

        M_FTV.put(new Pair<>(10, 1), "国庆节");
        M_FTV.put(new Pair<>(10, 24), "联合国日,程序员节");
        M_FTV.put(new Pair<>(10, 31), "万圣夜");

        M_FTV.put(new Pair<>(11, 1), "万圣节");
        M_FTV.put(new Pair<>(11, 8), "中国记者日");
        W_FTV.put(new Triple(11, 4, 5), "感恩节");

        M_FTV.put(new Pair<>(12, 1), "世界艾滋病日");
        M_FTV.put(new Pair<>(12, 4), "国家宪法日");
        M_FTV.put(new Pair<>(12, 13), "南京大屠杀死难者国家公祭日");
        M_FTV.put(new Pair<>(12, 20), "澳门回归纪念日");
        M_FTV.put(new Pair<>(12, 24), "平安夜");
        M_FTV.put(new Pair<>(12, 25), "圣诞节");
    }

    /**
     * 获取节日列表
     *
     * @param date
     * @return
     */
    public static List<String> getFestivals(DateTime date) {
        Pair<Integer, Integer> pair = new Pair<>(date.monthBaseOne(), date.dayOfMonth());
        List<String> mFtv = M_FTV.getValues(pair);
        Triple triple = new Triple(date.month(), date.weekOfMonth(), date.dayOfWeek());
        List<String> wFtv = W_FTV.getValues(triple);
        mFtv.addAll(wFtv);
        return mFtv;
    }


}
