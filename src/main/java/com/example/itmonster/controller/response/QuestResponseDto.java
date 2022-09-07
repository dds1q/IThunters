package com.example.itmonster.controller.response;

import com.example.itmonster.domain.StackOfQuest;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QuestResponseDto { // 댓글 조회, 기술스택 추가해야됨 !!

    private Long questId;
    private String title;
    private String nickname;
    private String content;
    private Long duration;
    private boolean status;
    private ClassDto classes;
    private Long bookmarkCnt;
    private Long commentCnt; //<댓글 기능 추가 후>
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
    private List<String> stacks;
}

//
//    public QuestResponseDto(Quest quest){
//        this.questId = quest.getId();
//        this.title = quest.getTitle();
//        this.nickname = quest.getMember().getNickname();
//        this.content = quest.getContent();
//        this.duration = quest.getDuration();
//        this.status = quest.getStatus();
//
//        this.createdAt = quest.getCreatedAt();
//        this.modifiedAt = quest.getModifiedAt();
//    }
