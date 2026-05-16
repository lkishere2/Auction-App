package com.auction.app.domains.inventory;

import java.util.List;

import org.springframework.stereotype.Service;

import com.ltnc.auction.domain.exceptions.InventoryItemNotFoundException;
import com.ltnc.auction.domain.exceptions.UnauthorizedActionException;
import com.ltnc.auction.domain.user.User;
import com.ltnc.auction.domain.user.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InventoryService {
    private final UserRepository userRepository;
    private final ItemStorageRepository itemStorageRepository;

    public List<InventoryItemResponse> getMyInventory(Long userId) {
        return itemStorageRepository.findByOwnerUserIdWithOwner(userId)
            .stream()
            .map(InventoryItemResponse::from)
            .toList();
    }
    public List<InventoryItemResponse> getAvailableItems(Long userId) {
        return itemStorageRepository.findByOwnerUserIdAndStatusWithOwner(userId, ItemStatus.AVAILABLE)
            .stream()
            .map(InventoryItemResponse::from)
            .toList();
    }

    @Transactional
    public InventoryItemResponse addItem(Long userId, ItemRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Item item = new Item();
        item.setOwner(user);
        item.setName(request.name());
        item.setDescription(request.description());
        item.setCategory(request.category());
        item.setStatus(ItemStatus.AVAILABLE);
        itemStorageRepository.save(item);
        return InventoryItemResponse.from(item);
        }

    @Transactional
    public InventoryItemResponse updateItem(Long userId, Long itemId, ItemRequest request) {
        Item item = itemStorageRepository.findByIdAndOwnerUserId(itemId, userId)
                .orElseThrow(() -> new InventoryItemNotFoundException("Item not found"));
        if (item.getStatus() != ItemStatus.AVAILABLE) {
            throw new UnauthorizedActionException("Cannot edit an item that is listed or transferred");
        }
        item.setName(request.name());
        item.setDescription(request.description());
        item.setCategory(request.category());

        return InventoryItemResponse.from(itemStorageRepository.save(item));
    }

    @Transactional
    public void deleteItem(Long userId, Long itemId) {
    Item item = itemStorageRepository.findByIdAndOwnerUserId(itemId, userId)
            .orElseThrow(() -> new RuntimeException("Item not found"));

    if (item.getStatus() != ItemStatus.AVAILABLE) {
        throw new RuntimeException("Cannot delete a listed item");
    }

        itemStorageRepository.delete(item);
}
}
