package site.minnan.robotmanage.entity.aggregate;

import jakarta.persistence.Id;

/**
 * 排名查询内容与查询地址映射
 *
 * @author Minnan on 2024/01/15
 */
public class QueryMap {

    @Id
    private Integer id;

    //查询地址
    private String queryUrl;

    //查询内容
    private String queryContent;
}
