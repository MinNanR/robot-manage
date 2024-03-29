package site.minnan.robotmanage.entity.vo.question;

import lombok.Data;
import site.minnan.robotmanage.entity.aggregate.Question;

@Data
public class QuestionListVO {

    private Integer id;

    private String content;

    private Boolean share;

    private String groupId;

    private Integer answerCount;

    private String answer;

    private String updateTime;

    private String updater;

    public static QuestionListVO assemble(Question question) {
        QuestionListVO vo = new QuestionListVO();
        vo.id = question.getId();
        vo.groupId = question.getGroupId();
        vo.content = question.getContent();
        vo.share = question.getShare() == 1;
        vo.updateTime = question.getUpdateTime();
        vo.updater = question.getUpdater();
        return vo;
    }
}
