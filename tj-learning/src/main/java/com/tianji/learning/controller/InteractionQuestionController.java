package com.tianji.learning.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionVO;
import com.tianji.learning.service.IInteractionQuestionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * <p>
 * 互动提问的问题表 前端控制器
 * </p>
 *
 * @author author
 * @since 2025-03-08
 */
@Api("问题相关接口")
@RestController
@RequiredArgsConstructor
@RequestMapping("/questions")
public class InteractionQuestionController {
    private final IInteractionQuestionService questionService;

    @ApiOperation("新增提问")
    @PostMapping
    public void saveQuestion(@Valid @RequestBody QuestionFormDTO questionDTO) {
        questionService.saveQuestion(questionDTO);
    }

    @ApiOperation("修改提问")
    @PutMapping("/{id}")
    public void updateQuestion(@PathVariable("id") Long id, QuestionFormDTO questionDTO) {
        questionService.updateQuestion(id, questionDTO);
    }

    @ApiOperation("分页查询")
    @GetMapping("/page")
    public PageDTO<QuestionVO> queryQuestionPage(QuestionPageQuery pageQuery) {
        return questionService.queryQuestionPage(pageQuery);
    }

    @ApiOperation("根据id查询问题详情")
    @GetMapping("/{id}")
    public QuestionVO queryQuestionById(@PathVariable("id") Long id) {
        return questionService.queryQuestionById(id);
    }

    @ApiOperation("根据id删除当前用户问题")
    @DeleteMapping("/{id}")
    public void deleteQuestionById(@PathVariable("id") Long id) {
        questionService.deleteQuestionById(id);
    }
}
