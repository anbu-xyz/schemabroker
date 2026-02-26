package uk.anbu.schemabroker.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.anbu.schemabroker.service.LeaseService;
import uk.anbu.schemabroker.web.dto.StatusResponse;

@RestController
@RequestMapping("/api/v1/status")
public class StatusController {

    private final LeaseService leaseService;

    public StatusController(LeaseService leaseService) {
        this.leaseService = leaseService;
    }

    @GetMapping
    public ResponseEntity<StatusResponse> getStatus() {
        StatusResponse status = leaseService.getStatus();
        return ResponseEntity.ok(status);
    }
}