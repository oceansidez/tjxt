package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.constants.Constant;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.ReplyDTO;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.domain.query.ReplyPageQuery;
import com.tianji.learning.domain.vo.ReplyVO;
import com.tianji.learning.enums.QuestionStatus;
import com.tianji.learning.mapper.InteractionReplyMapper;
import com.tianji.learning.service.IInteractionQuestionService;
import com.tianji.learning.service.IInteractionReplyService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static com.tianji.common.constants.Constant.DATA_FIELD_NAME_CREATE_TIME;

/**
 * <p>
 * 互动问题的回答或评论 服务实现类
 * </p>
 *
 * @author author
 * @since 2025-03-08
 */
@Service

public class InteractionReplyServiceImpl extends ServiceImpl<InteractionReplyMapper, InteractionReply> implements IInteractionReplyService {

    private final IInteractionQuestionService questionService;
    private final UserClient userClient;

    public InteractionReplyServiceImpl(@Lazy IInteractionQuestionService questionService, UserClient userClient) {
        this.questionService = questionService;
        this.userClient = userClient;
    }

    @Override
    @Transactional
    public void saveReply(ReplyDTO replyDTO) {
        Long userId = UserContext.getUser();
        InteractionReply reply = BeanUtils.copyBean(replyDTO, InteractionReply.class);
        reply.setUserId(userId);
        save(reply);
        if (replyDTO.getAnswerId() != null) {
            // 是评论，则需要更新上级回答的评论数量
            lambdaUpdate()
                    .setSql("reply_times = reply_times + 1")
                    .eq(InteractionReply::getId, replyDTO.getAnswerId())
                    .update();
        }
        // 更新问题中的最新回答等信息
        questionService.lambdaUpdate()
                .set(replyDTO.getAnswerId() == null, InteractionQuestion::getLatestAnswerId, reply.getId())
                .setSql("answer_times = answer_times + 1")
                .set(replyDTO.getIsStudent(), InteractionQuestion::getStatus, QuestionStatus.UN_CHECK)
                .eq(InteractionQuestion::getId, replyDTO.getQuestionId())
                .update();
        // todo 添加积分
    }

    @Override
    public PageDTO<ReplyVO> queryReplyPage(ReplyPageQuery query, boolean forAdmin) {
        Long questionId = query.getQuestionId();
        Long answerId = query.getAnswerId();
        if (questionId == null && answerId == null) {
            throw new BadRequestException("问题或回答id不能都为空");
        }
        // 标记当前是查询问题下的回答
        boolean isQueryAnswer = questionId != null;
        // 分页查询
        Page<InteractionReply> page = lambdaQuery()
                .eq(isQueryAnswer, InteractionReply::getQuestionId, questionId)
                .eq(!isQueryAnswer, InteractionReply::getAnswerId, answerId)
                .eq(!forAdmin, InteractionReply::getHidden, false)
                .page(query.toMpPage(// 先根据点赞数排序，点赞数相同，再按照创建时间排序
                        new OrderItem(Constant.DATA_FIELD_NAME_LIKED_TIME, false),
                        new OrderItem(DATA_FIELD_NAME_CREATE_TIME, true)
                ));
        List<InteractionReply> records = page.getRecords();
        if (CollUtils.isEmpty(records)) {
            return PageDTO.empty(page);
        }
        // 数据处理，需要查询：提问者信息、回复目标信息、当前用户是否点赞
        Set<Long> userIds = new HashSet<>();
        Set<Long> targetReplyIds = new HashSet<>();
        for (InteractionReply r : records) {
            if (!r.getAnonymity() || forAdmin) {
                userIds.add(r.getUserId());
            }
            targetReplyIds.add(r.getTargetReplyId());
        }
        // 查询目标回复，如果目标回复不是匿名，则需要查询出目标回复的用户信息
        targetReplyIds.remove(0L);
        targetReplyIds.remove(null);
        if (targetReplyIds.size() > 0) {
            List<InteractionReply> targetReplies = listByIds(targetReplyIds);
            List<Long> targetUserIds = targetReplies.stream()
                    .filter(interactionReply -> !interactionReply.getAnonymity() || forAdmin)
                    .map(InteractionReply::getUserId)
                    .collect(Collectors.toList());
            userIds.addAll(targetUserIds);
        }
        // 查询用户
        Map<Long, UserDTO> userMap = new HashMap<>(userIds.size());
        if (userIds.size() > 0) {
            List<UserDTO> users = userClient.queryUserByIds(userIds);
            userMap = users.stream().collect(Collectors.toMap(UserDTO::getId, u -> u));
        }
        // todo 查询用户点赞状态
        List<ReplyVO> list = new ArrayList<>(records.size());
        for (InteractionReply r : records) {
            ReplyVO v = BeanUtils.toBean(r, ReplyVO.class);
            list.add(v);
            // 回复人信息
            if (!r.getAnonymity() || forAdmin) {
                UserDTO userDTO = userMap.get(r.getUserId());
                if (userDTO != null) {
                    v.setUserIcon(userDTO.getIcon());
                    v.setUserName(forAdmin && r.getAnonymity() ? userDTO.getName() + "[匿名]" : userDTO.getName());
                    v.setUserType(userDTO.getType());
                }
            }
            // 如果存在评论的目标，则需要设置目标用户信息
            if (r.getTargetReplyId() != null) {
                UserDTO targetUser = userMap.get(r.getTargetUserId());
                if (targetUser != null) {
                    v.setTargetUserName(targetUser.getName());
                }
            }
            // todo 点赞状态
        }
        return PageDTO.of(page, list);
    }

    // 管理端
    @Override
    @Transactional
    public void hiddenReply(Long id, Boolean hidden) {
        InteractionReply old = getById(id);
        if (old == null) {
            return;
        }
        //隐藏回答
        InteractionReply reply = new InteractionReply();
        reply.setId(id);
        reply.setHidden(hidden);
        updateById(reply);
        // 隐藏评论，先判断是否是回答，回答才需要隐藏下属评论
        if (old.getAnswerId() != null && old.getAnswerId() != 0) {
            // 有answerId，说明自己是评论，无需处理
            return;
        }
        lambdaUpdate()
                .set(InteractionReply::getHidden, hidden)
                .eq(InteractionReply::getAnswerId, id)
                .update();
    }

    @Override
    public ReplyVO queryReplyById(Long id) {

        return null;
    }
}
