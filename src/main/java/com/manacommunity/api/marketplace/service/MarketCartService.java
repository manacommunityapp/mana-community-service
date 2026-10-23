package com.manacommunity.api.marketplace.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.marketplace.dto.MarketCartDto;
import com.manacommunity.api.marketplace.entity.MarketCart;
import com.manacommunity.api.marketplace.entity.MarketCartItem;
import com.manacommunity.api.marketplace.entity.MarketListing;
import com.manacommunity.api.marketplace.entity.MarketListingImage;
import com.manacommunity.api.marketplace.repository.MarketCartItemRepository;
import com.manacommunity.api.marketplace.repository.MarketCartRepository;
import com.manacommunity.api.marketplace.repository.MarketListingRepository;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarketCartService {

    private final MarketCartRepository cartRepository;
    private final MarketCartItemRepository cartItemRepository;
    private final MarketListingRepository listingRepository;

    public MarketCartDto.Response getCart(AppUser user) {
        MarketCart cart = cartRepository.findByUserId(user.getId())
                .orElseGet(() -> cartRepository.save(MarketCart.builder().user(user).items(new ArrayList<>()).build()));
        return toResponse(cart);
    }

    @Transactional
    public MarketCartDto.Response addItem(MarketCartDto.AddItemRequest req, AppUser user) {
        MarketCart cart = cartRepository.findByUserId(user.getId())
                .orElseGet(() -> cartRepository.save(MarketCart.builder().user(user).items(new ArrayList<>()).build()));

        MarketListing listing = listingRepository.findById(req.getListingId())
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with id: " + req.getListingId()));

        Optional<MarketCartItem> existing = cartItemRepository.findByCartIdAndListingId(cart.getId(), listing.getId());
        if (existing.isPresent()) {
            MarketCartItem item = existing.get();
            item.setQuantity(item.getQuantity() + req.getQuantity());
            cartItemRepository.save(item);
        } else {
            MarketCartItem newItem = MarketCartItem.builder()
                    .cart(cart)
                    .listing(listing)
                    .quantity(req.getQuantity())
                    .unitPrice(listing.getPrice())
                    .build();
            cart.getItems().add(cartItemRepository.save(newItem));
        }

        return toResponse(cartRepository.save(cart));
    }

    @Transactional
    public MarketCartDto.Response updateItem(Long itemId, MarketCartDto.UpdateItemRequest req, AppUser user) {
        MarketCartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        item.setQuantity(req.getQuantity());
        cartItemRepository.save(item);

        return getCart(user);
    }

    @Transactional
    public MarketCartDto.Response removeItem(Long itemId, AppUser user) {
        cartItemRepository.deleteById(itemId);
        return getCart(user);
    }

    @Transactional
    public void clearCart(AppUser user) {
        MarketCart cart = cartRepository.findByUserId(user.getId()).orElse(null);
        if (cart != null) {
            cartItemRepository.deleteByCartId(cart.getId());
            cart.getItems().clear();
            cartRepository.save(cart);
        }
    }

    private MarketCartDto.Response toResponse(MarketCart cart) {
        List<MarketCartDto.ItemResponse> items = cart.getItems() != null
                ? cart.getItems().stream().map(i -> {
            String img = (i.getListing().getImages() != null && !i.getListing().getImages().isEmpty())
                    ? i.getListing().getImages().get(0).getUrl() : null;
            BigDecimal total = i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity()));
            return MarketCartDto.ItemResponse.builder()
                    .id(i.getId())
                    .listingId(i.getListing().getId())
                    .listingTitle(i.getListing().getTitle())
                    .listingCategory(i.getListing().getCategory())
                    .imageUrl(img)
                    .unitPrice(i.getUnitPrice())
                    .quantity(i.getQuantity())
                    .totalPrice(total)
                    .sellerName(i.getListing().getSeller() != null ? i.getListing().getSeller().getFullName() : null)
                    .sellerId(i.getListing().getSeller() != null ? i.getListing().getSeller().getId() : null)
                    .build();
        }).collect(Collectors.toList())
                : List.of();

        BigDecimal subtotal = items.stream()
                .map(MarketCartDto.ItemResponse::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalQty = items.stream().mapToInt(MarketCartDto.ItemResponse::getQuantity).sum();

        return MarketCartDto.Response.builder()
                .id(cart.getId())
                .userId(cart.getUser().getId())
                .items(items)
                .subtotal(subtotal)
                .totalQuantity(totalQty)
                .build();
    }
}
