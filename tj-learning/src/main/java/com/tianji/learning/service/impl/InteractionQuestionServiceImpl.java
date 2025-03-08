package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.cache.CategoryCache;
import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.client.search.SearchClient;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.course.CataSimpleInfoDTO;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.domain.query.QuestionAdminPageQuery;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionAdminVO;
import com.tianji.learning.domain.vo.QuestionVO;
import com.tianji.learning.enums.QuestionStatus;
import com.tianji.learning.mapper.InteractionQuestionMapper;
import com.tianji.learning.service.IInteractionQuestionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.learning.service.IInteractionReplyService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * <p>
 * 互动提问的问题表 服务实现类
 * </p>
 *
 * @author author
 * @since 2025-03-08
 */
@Service
@AllArgsConstructor
public class InteractionQuestionServiceImpl extends ServiceImpl<InteractionQuestionMapper, InteractionQuestion> implements IInteractionQuestionService {

    private final UserClient userClient;

    private final IInteractionReplyService replyService;

    private final SearchClient searchClient;

    private final CourseClient courseClient;

    private final CatalogueClient catalogueClient;

    private final CategoryCache categoryCache;

    @Override
    public void saveQuestion(QuestionFormDTO questionDTO) {
        Long userId = UserContext.getUser();
        InteractionQuestion question = BeanUtils.copyBean(questionDTO, InteractionQuestion.class);
        question.setUserId(userId);
        save(question);
    }

    @Override
    public void updateQuestion(Long id, QuestionFormDTO questionDTO) {
        Long userId = UserContext.getUser();
        InteractionQuestion questionDb = getById(id);
        if (questionDb == null) {
            throw new BadRequestException("问题不存在");
        }
        if (!questionDb.getUserId().equals(userId)) {
            throw new BadRequestException("无权修改他人的问题");
        }
        InteractionQuestion question = BeanUtils.copyBean(questionDTO, InteractionQuestion.class);
        question.setId(id);
        updateById(question);
    }

    @Override
    public PageDTO<QuestionVO> queryQuestionPage(QuestionPageQuery pageQuery) {
        Long userId = UserContext.getUser();
        // 参数校验，课程id和小节id不能都为空
        Long courseId = pageQuery.getCourseId();
        Long sectionId = pageQuery.getSectionId();
        if (courseId == null && sectionId == null) {
            throw new BadRequestException("课程id和小节id不能都为空");
        }
        // 查询分页信息
        Page<InteractionQuestion> page = lambdaQuery()
                .select(InteractionQuestion.class, tableFieldInfo -> !tableFieldInfo.getProperty().equals("description"))
                .eq(pageQuery.getOnlyMine(), InteractionQuestion::getUserId, userId)
                .eq(courseId != null, InteractionQuestion::getCourseId, courseId)
                .eq(sectionId != null, InteractionQuestion::getSectionId, sectionId)
                .eq(InteractionQuestion::getHidden, false)
                .page(pageQuery.toMpPageDefaultSortByCreateTimeDesc());
        List<InteractionQuestion> records = page.getRecords();
        if (records == null) {
            return PageDTO.empty(page);
        }
        // 根据id查询提问者和最近一次回答的信息
        Set<Long> userIds = new HashSet<>();
        Set<Long> answerIds = new HashSet<>();
        for (InteractionQuestion q : records) {
            if (!q.getAnonymity()) {
                userIds.add(q.getUserId());
            }
            answerIds.add(q.getLatestAnswerId());
        }
        // 根据id查询最近一次回答,并添加用户id信息
        answerIds.remove(null);
        Map<Long, InteractionReply> replyMap = new HashMap<>(answerIds.size());
        if (CollUtils.isNotEmpty(answerIds)) {
            List<InteractionReply> replies = replyService.lambdaQuery().in(InteractionReply::getId, answerIds).list();
            for (InteractionReply reply : replies) {
                replyMap.put(reply.getId(), reply);
                // 将回答不是匿名的userid加入
                if (!reply.getAnonymity()) {
                    userIds.add(reply.getUserId());
                }
            }
        }
        userIds.remove(null);
        Map<Long, UserDTO> userMap = new HashMap<>(userIds.size());
        if (CollUtils.isNotEmpty(userIds)) {
            userMap = userClient.queryUserByIds(userIds).stream().collect(Collectors.toMap(UserDTO::getId, c -> c));
        }
        // 获取最新回答信息
        // 封装VO
        List<QuestionVO> res = new ArrayList<>(records.size());
        for (InteractionQuestion r : records) {
            QuestionVO vo = BeanUtils.copyBean(r, QuestionVO.class);
            vo.setUserId(null);
            // 当不是匿名才设置用户信息
            if (!r.getAnonymity()) {
                UserDTO userDTO = userMap.get(r.getUserId());
                if (userDTO != null) {
                    vo.setUserId(userDTO.getId());
                    vo.setUserName(userDTO.getName());
                    vo.setUserIcon(userDTO.getIcon());
                }
            }
            // 当查看自己的
            if (pageQuery.getOnlyMine()) {
                UserDTO userDTO = userMap.get(r.getUserId());
                if (userDTO != null && userDTO.getId() == userId) {
                    vo.setUserId(userDTO.getId());
                }
            }
            InteractionReply reply = replyMap.get(r.getLatestAnswerId());
            if (reply != null) {
                // 当回答不是匿名才设置用户名
                if (!reply.getAnonymity()) {
                    UserDTO replyUserDTO = userMap.get(reply.getUserId());
                    if (replyUserDTO != null) {
                        vo.setLatestReplyUser(replyUserDTO.getName());
                    }
                }
                vo.setLatestReplyContent(reply.getContent());
            }
            res.add(vo);
        }
        return PageDTO.of(page, res);
    }

    @Override
    public QuestionVO queryQuestionById(Long id) {
        InteractionQuestion question = getById(id);
        if (question == null || question.getHidden()) {
            // 没有数据或者是被隐藏了
            return null;
        }
        QuestionVO vo = BeanUtils.copyBean(question, QuestionVO.class);
        vo.setUserId(null);
        if (!question.getAnonymity()) {
            UserDTO userDTO = userClient.queryUserById(question.getUserId());
            vo.setUserId(question.getUserId());
            vo.setUserName(userDTO.getUsername());
            vo.setUserIcon(userDTO.getIcon());
        }
        return vo;
    }

    @Override
    @Transactional
    public void deleteQuestionById(Long id) {
        InteractionQuestion question = getById(id);
        if (question == null) {
            return;
        }
        // 判断是否是当前用户的问题
        if (!question.getUserId().equals(UserContext.getUser())) {
            throw new BadRequestException("无权删除他人的问题");
        }
        // 删除问题和对应的回答
        removeById(id);
        // 删除答案
        replyService.remove(
                replyService.lambdaQuery().eq(InteractionReply::getQuestionId, question.getId())
        );
    }

    // 管理端
    @Override
    public PageDTO<QuestionAdminVO> queryQuestionPageAdmin(QuestionAdminPageQuery query) {
        // 1.处理课程名称，得到课程id
        List<Long> courseIds = null;
        if (StringUtils.isNotEmpty(query.getCourseName())) {
            courseIds = searchClient.queryCoursesIdByName(query.getCourseName());
            if (CollUtils.isEmpty(courseIds)) {
                return PageDTO.empty(0L, 0L);
            }
        }
        // 2.分页查询
        Integer status = query.getStatus();
        LocalDateTime begin = query.getBeginTime();
        LocalDateTime end = query.getEndTime();
        Page<InteractionQuestion> page = lambdaQuery()
                .in(courseIds != null, InteractionQuestion::getCourseId, courseIds)
                .eq(status != null, InteractionQuestion::getStatus, status)
                .gt(begin != null, InteractionQuestion::getCreateTime, begin)
                .lt(end != null, InteractionQuestion::getCreateTime, end)
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<InteractionQuestion> records = page.getRecords();
        if (CollUtils.isEmpty(records)) {
            return PageDTO.empty(page);
        }
        // 3.准备VO需要的数据：用户数据、课程数据、章节数据
        Set<Long> userIds = new HashSet<>();
        Set<Long> cIds = new HashSet<>();
        Set<Long> cataIds = new HashSet<>();
        for (InteractionQuestion q : records) {
            userIds.add(q.getUserId());
            cIds.add(q.getCourseId());
            cataIds.add(q.getChapterId());
            cataIds.add(q.getSectionId());
        }
        // 3.2.根据id查询用户
        List<UserDTO> users = userClient.queryUserByIds(userIds);
        Map<Long, UserDTO> userMap = new HashMap<>(users.size());
        if (CollUtils.isNotEmpty(users)) {
            userMap = users.stream().collect(Collectors.toMap(UserDTO::getId, u -> u));
        }
        // 3.3.根据id查询课程
        List<CourseSimpleInfoDTO> cInfos = courseClient.getSimpleInfoList(cIds);
        Map<Long, CourseSimpleInfoDTO> cInfoMap = new HashMap<>(cInfos.size());
        if (CollUtils.isNotEmpty(cInfos)) {
            cInfoMap = cInfos.stream().collect(Collectors.toMap(CourseSimpleInfoDTO::getId, c -> c));
        }
        // 3.4.根据id查询章节
        List<CataSimpleInfoDTO> catas = catalogueClient.batchQueryCatalogue(cataIds);
        Map<Long, String> cataMap = new HashMap<>(catas.size());
        if (CollUtils.isNotEmpty(catas)) {
            cataMap = catas.stream()
                    .collect(Collectors.toMap(CataSimpleInfoDTO::getId, CataSimpleInfoDTO::getName));
        }
        List<QuestionAdminVO> voList = new ArrayList<>(records.size());
        for (InteractionQuestion q : records) {
            QuestionAdminVO vo = BeanUtils.copyBean(q, QuestionAdminVO.class);
            voList.add(vo);
            // 用户信息，是否匿名都展示
            UserDTO user = userMap.get(q.getUserId());
            if (user != null) {
                vo.setUserName(user.getName() + (q.getAnonymity() ? "[匿名]" : ""));
            }
            // 课程分类
            CourseSimpleInfoDTO cInfo = cInfoMap.get(q.getCourseId());
            if (cInfo != null) {
                vo.setCourseName(cInfo.getName());
                // 在缓存中获取一二三分类名称
                vo.setCategoryName(categoryCache.getCategoryNames(cInfo.getCategoryIds()));
            }
            // 章节信息
            vo.setChapterName(cataMap.getOrDefault(q.getChapterId(), ""));
            vo.setSectionName(cataMap.getOrDefault(q.getSectionId(), ""));
        }
        // 4.封装VO
        return PageDTO.of(page, voList);
    }

    @Override
    public QuestionAdminVO queryQuestionByIdAdmin(Long id) {
        InteractionQuestion question = getById(id);
        if (question == null) {
            return null;
        }
        QuestionAdminVO vo = BeanUtils.copyBean(question, QuestionAdminVO.class);
        // 查询提问者信息
        UserDTO user = userClient.queryUserById(question.getUserId());
        if (user != null) {
            vo.setUserName(user.getName() + (question.getAnonymity() ? "[匿名]" : ""));
            vo.setUserIcon(user.getIcon());
        }
        // 查询课程信息
        CourseFullInfoDTO cInfo = courseClient.getCourseInfoById(question.getCourseId(), false, true);
        if (cInfo != null) {
            // 课程名称信息
            vo.setCourseName(cInfo.getName());
            // 分类信息
            vo.setCategoryName(categoryCache.getCategoryNames(cInfo.getCategoryIds()));
            // 教师信息
            List<UserDTO> teachers = userClient.queryUserByIds(cInfo.getTeacherIds());
            if (CollUtils.isNotEmpty(teachers)) {
                vo.setTeacherName(teachers.stream()
                        .map(UserDTO::getName).collect(Collectors.joining("/"))
                );
            }
        }
        // 查询章节信息
        List<CataSimpleInfoDTO> catas = catalogueClient.batchQueryCatalogue(List.of(question.getChapterId(), question.getSectionId()));
        Map<Long, String> cataMap = new HashMap<>(catas.size());
        if (CollUtils.isNotEmpty(catas)) {
            cataMap = catas.stream().collect(Collectors.toMap(CataSimpleInfoDTO::getId, CataSimpleInfoDTO::getName));
        }
        vo.setChapterName(cataMap.getOrDefault(question.getChapterId(), ""));
        vo.setSectionName(cataMap.getOrDefault(question.getSectionId(), ""));
        // 修改查看状态
        InteractionQuestion q = new InteractionQuestion();
        q.setId(id);
        q.setStatus(QuestionStatus.CHECKED);
        updateById(q);
        return vo;
    }

    @Override
    public void hiddenQuestion(Long id, Boolean hidden) {
        InteractionQuestion question = new InteractionQuestion();
        question.setId(id);
        question.setHidden(hidden);
        updateById(question);
    }
}
