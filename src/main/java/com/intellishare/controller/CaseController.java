package com.intellishare.controller;

import com.intellishare.dto.cases.*;
import com.intellishare.model.User;
import com.intellishare.service.CaseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cases")
public class CaseController {
    private final CaseService service;
    public CaseController(CaseService service){this.service=service;}

    @GetMapping
    public CasePageResponse list(@AuthenticationPrincipal User actor,
        @RequestParam(required=false) String q,@RequestParam(required=false) String status,
        @RequestParam(required=false) String priority,@RequestParam(required=false) String type,
        @RequestParam(required=false) String city,@RequestParam(defaultValue="0") int page,
        @RequestParam(defaultValue="20") int size,@RequestParam(defaultValue="createdAt") String sort,
        @RequestParam(defaultValue="desc") String direction){
        return service.list(actor,q,status,priority,type,city,page,size,sort,direction);
    }

    @GetMapping("/{id}") public CaseResponse get(@AuthenticationPrincipal User actor,@PathVariable String id){return service.get(actor,id);}

    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyRole('Administrator','Officer')")
    public CaseResponse create(@AuthenticationPrincipal User actor,@Valid @RequestBody CreateCaseRequest request){return service.create(actor,request);}

    @PutMapping("/{id}") @PreAuthorize("hasAnyRole('Administrator','Officer')")
    public CaseResponse update(@AuthenticationPrincipal User actor,@PathVariable String id,@Valid @RequestBody UpdateCaseRequest request){return service.update(actor,id,request);}

    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasRole('Administrator')")
    public void delete(@AuthenticationPrincipal User actor,@PathVariable String id){service.delete(actor,id);}

    @PostMapping("/{id}/evidence") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyRole('Administrator','Officer')")
    public CaseResponse evidence(@AuthenticationPrincipal User actor,@PathVariable String id,@Valid @RequestBody CreateEvidenceRequest request){return service.addEvidence(actor,id,request);}

    @PostMapping("/{id}/timeline") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyRole('Administrator','Officer')")
    public CaseResponse timeline(@AuthenticationPrincipal User actor,@PathVariable String id,@Valid @RequestBody CreateTimelineRequest request){return service.addTimeline(actor,id,request);}
}
