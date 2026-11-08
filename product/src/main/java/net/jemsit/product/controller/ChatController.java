package net.jemsit.product.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.jemsit.product.dto.chat.SendMessageRequestDTO;
import net.jemsit.product.dto.chat.StartChatRequestDTO;
import net.jemsit.product.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Listing chats. Every endpoint needs a signed-in user (SecurityConfig leaves this path authenticated)
 * and works on the caller's own conversations only. Live updates arrive on /api/event/stream as
 * {@code chat} events.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/product/v1/chat")
public class ChatController {

    private final ChatService chatService;

    @PostMapping("conversations")
    public ResponseEntity<?> start(@Valid @RequestBody StartChatRequestDTO request) {
        return ResponseEntity.ok(chatService.start(request.kind(), request.listingId()));
    }

    @GetMapping("conversations")
    public ResponseEntity<?> list() {
        return ResponseEntity.ok(chatService.list());
    }

    @GetMapping("conversations/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        return ResponseEntity.ok(chatService.get(id));
    }

    @GetMapping("conversations/{id}/messages")
    public ResponseEntity<?> messages(@PathVariable Long id, @RequestParam(required = false) Long before) {
        return ResponseEntity.ok(chatService.messages(id, before));
    }

    @PostMapping("conversations/{id}/messages")
    public ResponseEntity<?> send(@PathVariable Long id, @Valid @RequestBody SendMessageRequestDTO request) {
        return ResponseEntity.ok(chatService.send(id, request.body()));
    }

    @PostMapping("conversations/{id}/read")
    public ResponseEntity<?> markRead(@PathVariable Long id) {
        chatService.markRead(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("unread")
    public ResponseEntity<?> unread() {
        return ResponseEntity.ok(Map.of("count", chatService.unreadCount()));
    }
}
