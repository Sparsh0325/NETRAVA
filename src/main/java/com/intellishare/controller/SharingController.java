package com.intellishare.controller;

import com.intellishare.dto.sharing.CreateShareRequest;
import com.intellishare.dto.sharing.ShareResponse;
import com.intellishare.model.User;
import com.intellishare.service.SharingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/shares")
public class SharingController {
    private final SharingService sharing;
    public SharingController(SharingService sharing){this.sharing=sharing;}
    @GetMapping public List<ShareResponse> list(@AuthenticationPrincipal User actor){return sharing.list(actor);}
    @PostMapping("/cases/{caseKey}") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyRole('Administrator','Officer')")
    public ShareResponse create(@AuthenticationPrincipal User actor,@PathVariable String caseKey,@Valid @RequestBody CreateShareRequest request){return sharing.create(actor,caseKey,request);}
    @DeleteMapping("/{shareId}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasAnyRole('Administrator','Officer')")
    public void revoke(@AuthenticationPrincipal User actor,@PathVariable long shareId){sharing.revoke(actor,shareId);}
}
