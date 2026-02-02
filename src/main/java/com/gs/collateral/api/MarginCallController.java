package com.gs.collateral.api;

import com.gs.collateral.api.dto.MarginCallResponse;
import com.gs.collateral.repository.MarginCallRepository;
import com.gs.collateral.service.MarginCallNoticeFormatter;
import com.gs.collateral.service.MarginCallService;
import com.gs.collateral.service.NotFoundException;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/margin-calls")
public class MarginCallController {

    private final MarginCallService service;
    private final MarginCallRepository repository;
    private final MarginCallNoticeFormatter noticeFormatter;

    public MarginCallController(MarginCallService service, MarginCallRepository repository,
                                MarginCallNoticeFormatter noticeFormatter) {
        this.service = service;
        this.repository = repository;
        this.noticeFormatter = noticeFormatter;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<MarginCallResponse> list() {
        return repository.findAll().stream().map(MarginCallResponse::new).collect(Collectors.toList());
    }

    @PostMapping("/run")
    public List<MarginCallResponse> runDailyCycle() {
        return service.runDailyCycle().stream().map(MarginCallResponse::new).collect(Collectors.toList());
    }

    @PostMapping("/{id}/agree")
    public MarginCallResponse agree(@PathVariable long id) {
        return new MarginCallResponse(service.agree(id));
    }

    @GetMapping(value = "/{id}/notice", produces = MediaType.TEXT_PLAIN_VALUE)
    @Transactional(readOnly = true)
    public String notice(@PathVariable long id) {
        return repository.findById(id).map(noticeFormatter::format)
                .orElseThrow(() -> new NotFoundException("Margin call " + id + " not found"));
    }
}
