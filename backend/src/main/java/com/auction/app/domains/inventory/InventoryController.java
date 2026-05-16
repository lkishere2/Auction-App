package com.auction.app.domains.inventory;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ltnc.auction.domain.user.User;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;


@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {
    private final InventoryService inventoryService;

    @PostMapping("/add")
    public ResponseEntity<InventoryItemResponse> addItem(
        @AuthenticationPrincipal User user,
        @RequestBody @Valid ItemRequest request) {
        return ResponseEntity.ok(inventoryService.addItem(user.getUserId(), request));
    }

    @PutMapping("/update/{itemId}")
    public ResponseEntity<InventoryItemResponse> updateItem(
        @AuthenticationPrincipal User user,
        @PathVariable Long itemId,
        @RequestBody @Valid ItemRequest request) {
        return ResponseEntity.ok(inventoryService.updateItem(user.getUserId(), itemId, request));
    }

    @DeleteMapping("/delete/{itemId}")
    public ResponseEntity<Void> deleteItem(
        @AuthenticationPrincipal User user,
        @PathVariable Long itemId) {
        inventoryService.deleteItem(user.getUserId(), itemId);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/all")
    public ResponseEntity<List<InventoryItemResponse>> getInventory(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(inventoryService.getMyInventory(user.getUserId()));
    }

    @GetMapping("/available")
    public ResponseEntity<List<InventoryItemResponse>> getAvailableItems(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(inventoryService.getAvailableItems(user.getUserId()));
    }
}
