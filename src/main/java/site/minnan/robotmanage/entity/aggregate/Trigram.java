package site.minnan.robotmanage.entity.aggregate;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 卦象
 *
 * @author Minnan on 2024/01/16
 */
@Entity
@Table(name = "trigram")
@Data
public class Trigram {

    @Id
    private Integer id;

    /**
     * 卦象缩写
     */
    @Column(name = "short_name")
    private String shortName;

    /**
     * 卦象全名
     */
    @Column(name = "whole_name")
    private String wholeName;

    /**
     * 卦象描述
     */
    private String description;

    /**
     * 卦象质量（上上卦，中上卦之类的）
     */
    private String quality;

    /**
     * 卦象解析
     */
    private String explanation;

    /**
     * 卦象索引
     */
    private Integer indez;
}
