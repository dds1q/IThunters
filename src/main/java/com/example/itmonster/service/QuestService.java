package com.example.itmonster.service;

import com.example.itmonster.controller.request.QuestRequestDto;
import com.example.itmonster.controller.response.QuestResponseDto;
import com.example.itmonster.controller.response.SearchResponseDto;
import com.example.itmonster.domain.*;
import com.example.itmonster.exceptionHandler.CustomException;
import com.example.itmonster.exceptionHandler.ErrorCode;
import com.example.itmonster.repository.*;
import com.example.itmonster.security.UserDetailsImpl;
import com.example.itmonster.utils.SearchPredicate;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestService {

    private final QuestRepository questRepository;
    private final FolioRepository folioRepository;
    private final SquadRepository squadRepository;
    private final BookmarkRepository bookmarkRepository;
    private final CommentRepository commentRepository;
    private final StackOfQuestRepository stackOfQuestRepository;

    @PersistenceContext
    private EntityManager em;

    @Transactional // 게시글 작성 // 기술스택 추가해야됨 !!
    public boolean createQuest(QuestRequestDto questRequestDto, UserDetailsImpl userDetails) {
        Member member = userDetails.getMember();
        Quest quest = Quest.builder()
                .member(member)
                .title(questRequestDto.getTitle())
                .content(questRequestDto.getContent())
                .frontend(questRequestDto.getFrontend())
                .backend(questRequestDto.getBackend())
                .fullstack(questRequestDto.getFullstack())
                .designer(questRequestDto.getDesigner())
                .status(false)
                .duration(questRequestDto.getDuration())
                .build();
        questRepository.save(quest);

        // 퀘스트의 스택 저장
        String stacks = questRequestDto.getStacks().trim() ;
        String[] stackList = questRequestDto.getStacks().split(" ");
        for( String stack : stackList ){
            stackOfQuestRepository.save(
                StackOfQuest.builder()
                    .stackName( stack )
                    .quest( quest )
                    .build()
            );
        }

        squadRepository.save(Squad.builder()  // 본인을 포함하여 Squad 생성
                .quest(quest)
                .member(member)
                .build());

        // 빈 포트폴리오 생성
        folioRepository.save(Folio.builder()
                .title(member.getNickname() + "님의 포트폴리오입니다.")
                .member(member)
                .build());

        return true;
    }

    @Transactional(readOnly = true) // 모든 게시글 최신순 조회 // 기술스택 추가해야됨 !!
    public List<QuestResponseDto> readAllQuest() {
        List<Quest> quests = questRepository.findAllByOrderByModifiedAtDesc();
        List<QuestResponseDto> result = new ArrayList<>();
        for (Quest quest : quests) {
            result.add(QuestResponseDto.builder()
                    .questId(quest.getId())
                    .title(quest.getTitle())
                    .nickname(quest.getMember().getNickname())
                    .content(quest.getContent())
                    .duration(quest.getDuration())
                    .status(quest.getStatus())
                    .frontend(quest.getFrontend())
                    .backend(quest.getBackend())
                    .fullstack(quest.getFullstack())
                    .designer(quest.getDesigner())
                    .bookmarkCnt(bookmarkRepository.countAllByQuest(quest))
                    .commentCnt(commentRepository.countAllByQuest(quest)) // 댓글 추가후
                    .createdAt(quest.getCreatedAt())
                    .modifiedAt(quest.getModifiedAt())
                    .build());
        }
        return result;
    }

    @Transactional(readOnly = true) // 메인페이지용 게시글 최신순 3개 조회 // 기술스택 추가해야됨 !!
    public List<QuestResponseDto> readTop3Quest() {
        List<Quest> quests = questRepository.findTop3ByOrderByModifiedAtDesc();
        List<QuestResponseDto> result = new ArrayList<>();
        for (Quest quest : quests) {
            result.add(QuestResponseDto.builder()
                    .questId(quest.getId())
                    .title(quest.getTitle())
                    .nickname(quest.getMember().getNickname())
                    .content(quest.getContent())
                    .duration(quest.getDuration())
                    .status(quest.getStatus())
                    .frontend(quest.getFrontend())
                    .backend(quest.getBackend())
                    .fullstack(quest.getFullstack())
                    .designer(quest.getDesigner())
                    .bookmarkCnt(bookmarkRepository.countAllByQuest(quest))
                    .commentCnt(commentRepository.countAllByQuest(quest)) // 댓글 추가후
                    .createdAt(quest.getCreatedAt())
                    .modifiedAt(quest.getModifiedAt())
                    .build());
        }
        return result;
    }

    @Transactional(readOnly = true) // 게시글 상세 조회 // 댓글조회, 기술스택 추가해야됨 !!
    public QuestResponseDto readQuest(Long questId) {
        Quest quest = validateQuest(questId);
        return QuestResponseDto.builder()
                .questId(quest.getId())
                .title(quest.getTitle())
                .nickname(quest.getMember().getNickname())
                .content(quest.getContent())
                .duration(quest.getDuration())
                .status(quest.getStatus())
                .frontend(quest.getFrontend())
                .backend(quest.getBackend())
                .fullstack(quest.getFullstack())
                .designer(quest.getDesigner())
                .bookmarkCnt(bookmarkRepository.countAllByQuest(quest))
                .commentCnt(commentRepository.countAllByQuest(quest))
                .createdAt(quest.getCreatedAt())
                .modifiedAt(quest.getModifiedAt())
                .build();
    }

    @Transactional // 게시글 수정 // 기술스택 추가해야됨 !!
    public boolean updateQuest(Long questId, QuestRequestDto questRequestDto,
                               UserDetailsImpl userDetails) {
        Member member = userDetails.getMember();
        Quest quest = validateQuest(questId);
        if (validateAuthor(member, quest)) {
            quest.updateQuest(questRequestDto.getTitle(), questRequestDto.getContent(),
                    questRequestDto.getFrontend(), questRequestDto.getBackend(),
                    questRequestDto.getFullstack(), questRequestDto.getDesigner(),
                    questRequestDto.getDuration());
        }
        return true;
    }

    @Transactional // 게시글 삭제
    public boolean deleteQuest(Long questId, UserDetailsImpl userDetails) {
        Member member = userDetails.getMember();
        Quest quest = validateQuest(questId);
        if (validateAuthor(member, quest)) {
            questRepository.deleteById(questId);
        }
        return true;
    }

    @Transactional // 게시글 북마크
    public boolean bookmarkQuest(Long questId, UserDetailsImpl userDetails) {
        Member member = userDetails.getMember();
        Quest quest = validateQuest(questId);
        if (!bookmarkRepository.existsByMarkedMemberAndQuest(member, quest)) {
            bookmarkRepository.save(Bookmark.builder()
                    .markedMember(member)
                    .quest(quest)
                    .build());
            return true;
        }
        bookmarkRepository.deleteByMarkedMemberAndQuest(member, quest);
        return false;
    }

    public Quest validateQuest(Long questId) {
        return questRepository.findById(questId)
                .orElseThrow(() -> new CustomException(ErrorCode.QUEST_NOT_FOUND));
    }

    public boolean validateAuthor(Member member, Quest quest) { // 수정,삭제 권한 확인(글쓴이인지 확인)
        if (!member.getId().equals(quest.getMember().getId())) {
            throw new CustomException(ErrorCode.INVALID_AUTHORITY);
        }
        return true;
    }

    // 필터링된 검색결과 가져오기
    @Transactional(readOnly = true)
    public List<SearchResponseDto> searchQuests(MultiValueMap<String, String> allParameters) {

        JPAQueryFactory jpaQueryFactory = new JPAQueryFactory(em);

        BooleanBuilder searchBuilder = SearchPredicate.filter(allParameters , jpaQueryFactory );

        List<Quest> results = jpaQueryFactory.selectFrom(QQuest.quest)
                .where(searchBuilder)
                .orderBy(QQuest.quest.createdAt.desc()).fetch();

        List<SearchResponseDto> questResponseDtos = new ArrayList<>();

        results.forEach(result -> questResponseDtos.add( new SearchResponseDto(result ,
            stackOfQuestRepository.findAllByQuest( result ) ) ) );
        long totalCount = results.size();

        return questResponseDtos;

    }
}