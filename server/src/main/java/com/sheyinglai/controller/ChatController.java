package com.sheyinglai.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.sheyinglai.common.Result;
import com.sheyinglai.dto.Views;
import com.sheyinglai.entity.ChatMessage;
import com.sheyinglai.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/** 我的私信（聊一聊）。新消息通过 /ws/chat 实时推送，这里是收发和历史记录 */
@SaCheckLogin
@RestController
@RequestMapping("/me/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    public record TextReq(String content) {
    }

    /** 在别人主页点「聊一聊」：找到或创建和 TA 的会话 */
    @PostMapping("/open")
    public Result<Views.ChatConversationView> open(@RequestParam Long peerId) {
        return Result.ok(chatService.open(peerId));
    }

    @GetMapping
    public Result<List<Views.ChatConversationView>> list() {
        return Result.ok(chatService.list());
    }

    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        return Result.ok(chatService.unreadCount());
    }

    @GetMapping("/{id}")
    public Result<Views.ChatConversationView> get(@PathVariable Long id) {
        return Result.ok(chatService.get(id));
    }

    /** 历史消息，before 为空时取最新的 */
    @GetMapping("/{id}/messages")
    public Result<List<ChatMessage>> history(@PathVariable Long id, @RequestParam(required = false) Long before,
                                             @RequestParam(defaultValue = "30") int size) {
        return Result.ok(chatService.history(id, before, size));
    }

    @PostMapping("/{id}/messages")
    public Result<ChatMessage> sendText(@PathVariable Long id, @RequestBody TextReq req) {
        return Result.ok(chatService.sendText(id, req.content()));
    }

    @PostMapping("/{id}/images")
    public Result<ChatMessage> sendImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) throws IOException {
        return Result.ok(chatService.sendImage(id, file));
    }

    @PostMapping("/{id}/read")
    public Result<Void> read(@PathVariable Long id) {
        chatService.read(id);
        return Result.ok();
    }
}
