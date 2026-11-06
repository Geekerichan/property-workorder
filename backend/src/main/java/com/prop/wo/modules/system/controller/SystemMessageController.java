package com.prop.wo.modules.system.controller;

import com.prop.wo.common.context.LoginUserHolder;
import com.prop.wo.common.entity.PageQuery;
import com.prop.wo.common.result.PageResult;
import com.prop.wo.common.result.Result;
import com.prop.wo.modules.system.entity.SysMessage;
import com.prop.wo.modules.system.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 站内信（管理端与作业端共用逻辑，路径区分）
 */
@Tag(name = "系统-站内信")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SystemMessageController {

    private final MessageService messageService;

    @Operation(summary = "消息分页")
    @GetMapping({"/admin/system/message/page", "/app/message/page"})
    public Result<PageResult<SysMessage>> page(@RequestParam(required = false) Integer readFlag, PageQuery query) {
        return Result.ok(messageService.page(LoginUserHolder.userId(), readFlag, query));
    }

    @Operation(summary = "未读数量")
    @GetMapping({"/admin/system/message/unread", "/app/message/unread"})
    public Result<Long> unread() {
        return Result.ok(messageService.unreadCount(LoginUserHolder.userId()));
    }

    @Operation(summary = "标记已读")
    @PostMapping({"/admin/system/message/read/{id}", "/app/message/read/{id}"})
    public Result<Void> read(@PathVariable Long id) {
        messageService.markRead(LoginUserHolder.userId(), id);
        return Result.ok();
    }

    @Operation(summary = "全部已读")
    @PostMapping({"/admin/system/message/read-all", "/app/message/read-all"})
    public Result<Void> readAll() {
        messageService.markAllRead(LoginUserHolder.userId());
        return Result.ok();
    }
}
