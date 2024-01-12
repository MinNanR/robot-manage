package site.minnan.robotmanage.entity.vo;


import lombok.Data;

import java.util.List;

/**
 * 通用列表查询结果
 *
 * @author Minnan on 2023/06/09
 */
public record ListQueryVO<T> (List<T> list, Long totalCount, Integer pageCount) {


}
