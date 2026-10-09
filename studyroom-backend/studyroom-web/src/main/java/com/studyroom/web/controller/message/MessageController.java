package com.studyroom.web.controller.message;

import com.studyroom.common.annotation.CurrentUser;
import com.studyroom.common.annotation.RateLimiter;
import com.studyroom.common.login.LoginUser;
import com.studyroom.common.result.PageResult;
import com.studyroom.common.result.Result;
import com.studyroom.entity.dto.user.SubscribeMessageDTO;
import com.studyroom.entity.vo.message.MessageVO;
import com.studyroom.service.message.MessageService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.concurrent.TimeUnit;

/**
 * 用户端-消息中心 Controller
 *
 * <h3>接口清单</h3>
 * <table border="1">
 *   <tr><th>方法</th><th>路径</th><th>说明</th><th>FR</th></tr>
 *   <tr><td>GET</td><td>/api/message/list</td><td>消息列表（分类）</td><td>FR-UC-047</td></tr>
 *   <tr><td>GET</td><td>/api/message/unread-count</td><td>未读数（角标）</td><td>FR-UC-047</td></tr>
 *   <tr><td>POST</td><td>/api/message/{id}/read</td><td>单条已读</td><td>FR-UC-047</td></tr>
 *   <tr><td>POST</td><td>/api/message/read-all</td><td>全部已读</td><td>FR-UC-047</td></tr>
 *   <tr><td>DELETE</td><td>/api/message/{id}</td><td>删除消息（左滑）</td><td>FR-UC-047</td></tr>
 *   <tr><td>POST</td><td>/api/message/subscribe</td><td>订阅消息授权上报</td><td>FR-UC-044</td></tr>
 * </table>
 *
 * @author studyroom
 */
@RestController
@RequestMapping("/message")
@RequiredArgsConstructor
@Api(tags = "用户端-消息中心")
public class MessageController {

    private final MessageService messageService;

    /**
     * 消息列表 —— FR-UC-047（0全部 1系统通知 2订单消息 3活动消息）
     */
    @GetMapping("/list")
    @ApiOperation("消息列表（分类筛选）")
    public Result<PageResult<MessageVO>> list(
            @CurrentUser LoginUser user,
            @ApiParam(value = "分类：0全部 1系统通知 2订单消息 3活动消息", defaultValue = "0")
            @RequestParam(defaultValue = "0") Integer type,
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") Long page,
            @ApiParam(value = "每页条数", defaultValue = "15") @RequestParam(defaultValue = "15") Long size) {
        return Result.ok(messageService.listMessages(user.getId(), type, page, size));
    }

    /**
     * 未读消息数 —— FR-UC-047（TabBar 角标轮询）
     */
    @GetMapping("/unread-count")
    @ApiOperation("未读消息数")
    public Result<Long> unreadCount(@CurrentUser LoginUser user) {
        return Result.ok(messageService.unreadCount(user.getId()));
    }

    /**
     * 单条标记已读 —— FR-UC-047
     */
    @PostMapping("/{id}/read")
    @ApiOperation("单条标记已读")
    public Result<Void> read(
            @CurrentUser LoginUser user,
            @PathVariable("id") Long messageId) {
        messageService.readMessage(user.getId(), messageId);
        return Result.ok();
    }

    /**
     * 全部已读 —— FR-UC-047
     */
    @PostMapping("/read-all")
    @ApiOperation("全部已读")
    public Result<Void> readAll(@CurrentUser LoginUser user) {
        messageService.readAll(user.getId());
        return Result.ok();
    }

    /**
     * 删除单条消息 —— FR-UC-047（列表左滑删除）
     */
    @DeleteMapping("/{id}")
    @ApiOperation("删除消息（逻辑删除）")
    public Result<Void> delete(
            @CurrentUser LoginUser user,
            @PathVariable("id") Long messageId) {
        messageService.deleteMessage(user.getId(), messageId);
        return Result.ok();
    }

    /**
     * 订阅消息授权上报 —— FR-UC-044
     * <p>前端 uni.requestSubscribeMessage 回调后上报；限流防刷。</p>
     */
    @PostMapping("/subscribe")
    @ApiOperation("订阅消息授权上报（6 场景）")
    @RateLimiter(value = "message:subscribe", permits = 10, interval = 1,
            unit = TimeUnit.MINUTES, key = RateLimiter.KeyType.USER)
    public Result<Void> subscribe(
            @CurrentUser LoginUser user,
            @Valid @RequestBody SubscribeMessageDTO dto) {
        messageService.reportSubscribe(user.getId(), dto);
        return Result.ok();
    }
}
