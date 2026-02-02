package com.gs.collateral.api;

import com.gs.collateral.api.dto.AgreementRequest;
import com.gs.collateral.api.dto.AgreementResponse;
import com.gs.collateral.api.dto.PositionRequest;
import com.gs.collateral.domain.Agreement;
import com.gs.collateral.domain.CollateralPosition;
import com.gs.collateral.repository.AgreementRepository;
import com.gs.collateral.service.ExposureService;
import com.gs.collateral.service.NotFoundException;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agreements")
public class AgreementController {

    private final AgreementRepository repository;
    private final ExposureService exposureService;

    public AgreementController(AgreementRepository repository, ExposureService exposureService) {
        this.repository = repository;
        this.exposureService = exposureService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<AgreementResponse> list() {
        return repository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @GetMapping("/{reference}")
    @Transactional(readOnly = true)
    public AgreementResponse get(@PathVariable String reference) {
        return repository.findByReference(reference).map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("Agreement " + reference + " not found"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public AgreementResponse create(@Valid @RequestBody AgreementRequest request) {
        Agreement agreement = new Agreement(request.getReference(), request.getCounterparty(),
                request.getCurrency(), request.getThreshold(), request.getMinimumTransferAmount());
        agreement.setExposure(request.getExposure());
        for (PositionRequest p : request.getPositions()) {
            agreement.addPosition(new CollateralPosition(p.getAsset(), p.getAssetClass(),
                    p.getMarketValue(), p.getHaircutBps()));
        }
        return toResponse(repository.save(agreement));
    }

    private AgreementResponse toResponse(Agreement agreement) {
        return new AgreementResponse(agreement, exposureService.collateralHeld(agreement));
    }
}
