package com.agenciahub.api.application.controller;

import com.agenciahub.api.application.usecases.sellerdashboard.buildsellerdashboard.BuildSellerDashboardUseCase;
import com.agenciahub.api.application.controller.doc.SellerDashboardAPI;
import com.agenciahub.api.application.usecases.user.retrieve.entity.GetUserEntityByIdUseCase;
import com.agenciahub.api.application.usecases.sellerdashboard.buildsellerdashboard.SellerDashboardResponseDTO;
import com.agenciahub.api.entity.User;
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
    private final GetUserEntityByIdUseCase getUserEntityByIdUseCase;

    @Override
    public SellerDashboardResponseDTO myDashboard(@AuthenticationPrincipal User caller) {
        return buildSellerDashboardUseCase.execute(caller);
    }

    @Override
    @PreAuthorize("hasRole('OWNER')")
    public SellerDashboardResponseDTO sellerDashboard(
            @PathVariable UUID sellerId,
            @AuthenticationPrincipal User caller) {
        User seller = getUserEntityByIdUseCase.execute(sellerId);
        return buildSellerDashboardUseCase.execute(seller);
    }
}
