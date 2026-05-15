package com.agenciahub.api.controller.sellerdashboard;

import com.agenciahub.api.application.sellerdashboard.BuildSellerDashboardUseCase;
import com.agenciahub.api.application.controllers.docs.SellerDashboardAPI;
import com.agenciahub.api.dto.seller.SellerDashboardResponse;
import com.agenciahub.api.entity.User;
import com.agenciahub.api.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class SellerDashboardController implements SellerDashboardAPI {

    private final BuildSellerDashboardUseCase buildSellerDashboardUseCase;
    private final UserService userService;

    @Override
    public SellerDashboardResponse myDashboard(@AuthenticationPrincipal User caller) {
        return buildSellerDashboardUseCase.execute(caller);
    }

    @Override
    @PreAuthorize("hasRole('OWNER')")
    public SellerDashboardResponse sellerDashboard(
            @PathVariable UUID sellerId,
            @AuthenticationPrincipal User caller) {
        User seller = userService.getEntityById(sellerId);
        return buildSellerDashboardUseCase.execute(seller);
    }
}
