package uk.anbu.schemabroker.web;

import static j2html.TagCreator.a;
import static j2html.TagCreator.body;
import static j2html.TagCreator.div;
import static j2html.TagCreator.document;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.head;
import static j2html.TagCreator.html;
import static j2html.TagCreator.meta;
import static j2html.TagCreator.p;
import static j2html.TagCreator.span;
import static j2html.TagCreator.strong;
import static j2html.TagCreator.style;
import static j2html.TagCreator.table;
import static j2html.TagCreator.tbody;
import static j2html.TagCreator.td;
import static j2html.TagCreator.th;
import static j2html.TagCreator.thead;
import static j2html.TagCreator.title;
import static j2html.TagCreator.tr;

import j2html.tags.ContainerTag;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import uk.anbu.schemabroker.model.SchemaLease;
import uk.anbu.schemabroker.service.LeaseService;
import uk.anbu.schemabroker.web.dto.SchemaStatusDto;
import uk.anbu.schemabroker.web.dto.StatusResponse;

@RestController
public class WebController {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter
        .ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(java.time.ZoneId.of("UTC"));
    private static final String BASE_STYLES =
        "body { font-family: Arial, sans-serif; margin: 3rem; background: #f7f9fb; }"
            + "table { border-collapse: collapse; width: 100%; margin-top: 1rem; }"
            + "th, td { border: 1px solid #c6cbd3; padding: 0.5rem 0.75rem; text-align: left; }"
            + "th { background: #eef1f5; }"
            + "tr:nth-child(even) { background: #ffffff; }"
            + "tr:nth-child(odd) { background: #fdfefe; }"
            + ".status { font-weight: 600; }";
    private static final List<String> TABLE_HEADERS = List.of(
        "Schema",
        "Group",
        "Login User",
        "JDBC URL",
        "Enabled",
        "Status",
        "Lease ID",
        "Expires In",
        "Owner"
    );

    private final LeaseService leaseService;

    public WebController(LeaseService leaseService) {
        this.leaseService = leaseService;
    }

    @GetMapping(value = "/schemas", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> listSchemasPage() {
        StatusResponse status = leaseService.getStatus(Instant.now());
        return ResponseEntity.ok(renderHtml(status));
    }

    private static String renderHtml(StatusResponse status) {
        Instant now = Instant.now();
        String refreshed = TIMESTAMP_FORMATTER.format(now) + " UTC";
        ContainerTag<?> content = body().with(
            h1("Schema Broker Status"),
            div().with(p().withText("Lease TTL: ").with(strong(status.ttlSeconds() + " seconds"))),
            div().with(p().withText("Refreshed: ").with(strong(refreshed))),
            renderTable(status.schemas(), now)
        );
        return document(html().attr("lang", "en").with(
            head(
                meta().withCharset("UTF-8"),
                title("Schema Status"),
                style(BASE_STYLES)
            ),
            content
        ));
    }

    private static ContainerTag<?> renderTable(List<SchemaStatusDto> schemas, Instant now) {
        var head = thead();
        var headerRow = tr();
        TABLE_HEADERS.forEach(h -> headerRow.with(th(h)));
        head.with(headerRow);

        var body = tbody();
        if (schemas.isEmpty()) {
            body.with(
                tr().with(td("No schemas configured yet.").attr("colspan", TABLE_HEADERS.size())));
        } else {
            for (SchemaStatusDto schema : schemas) {
                body.with(tr()
                    .with(td(schema.getSchema()))
                    .with(td(schema.getGroupName()))
                    .with(td(schema.getLoginUser()))
                    .with(td(schema.getJdbcUrl()))
                    .with(td(Boolean.toString(schema.isEnabled())))
                    .with(td(schema.getStatus()).withClass("status"))
                    .with(td(renderLeaseLink(schema.getLeaseId())))
                    .with(td(formatRemaining(schema.getExpiresAt(), now)))
                    .with(td(schema.getOwner() == null ? "" : schema.getOwner())));
            }
        }
        return table().with(head,
            body);
    }

    @GetMapping(value = "/lease/{leaseId}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> leaseDetails(@PathVariable String leaseId) {
        return leaseService.getLeaseDetails(leaseId)
            .map(lease -> ResponseEntity.ok(renderLeaseDetails(lease)))
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("<html><body><h2>Lease not found</h2><p>Lease ID: " + leaseId
                    + "</p></body></html>"));
    }

    private static ContainerTag<?> renderLeaseLink(String leaseId) {
        if (leaseId == null || leaseId.isEmpty()) {
            return span("-");
        }
        return a(leaseId).withHref("/lease/" + leaseId);
    }

    private static String renderLeaseDetails(SchemaLease lease) {
        ContainerTag<?> content = body().with(
            h1("Lease Details"),
            h2("Lease ID: " + lease.getLeaseId()),
            table().with(tbody()
                .with(tr().with(th("Field"), th("Value")))
                .with(tr().with(td("Schema"), td(lease.getSchemaName())))
                .with(tr().with(td("JDBC URL"), td(lease.getJdbcUrl())))
                .with(tr().with(td("Login User"), td(lease.getLoginUser())))
                .with(tr().with(td("Status"), td(String.valueOf(lease.getStatus()))))
                .with(tr().with(td("Owner"), td(lease.getOwner())))
                .with(tr().with(td("Metadata"), td(String.valueOf(lease.getMetadata()))))
                .with(tr().with(td("Leased At"), td(String.valueOf(lease.getLeasedAt()))))
                .with(tr().with(td("Expires At"), td(String.valueOf(lease.getExpiresAt()))))
                .with(
                    tr().with(td("Last Heartbeat"), td(String.valueOf(lease.getLastHeartbeatAt()))))
                .with(tr().with(td("IP Address"), td(String.valueOf(lease.getIpAddress()))))
                .with(tr().with(td("Hostname"), td(String.valueOf(lease.getHostname()))))
            )
        );
        return document(html().attr("lang", "en").with(
            head(
                meta().withCharset("UTF-8"),
                title("Lease Details"),
                style(BASE_STYLES)
            ),
            content
        ));
    }

    private static String formatRemaining(Instant expiresAt, Instant now) {
        if (expiresAt == null) {
            return "";
        }
        long seconds = Duration.between(now, expiresAt).getSeconds();
        if (seconds < 0) {
            seconds = 0;
        }
        long minutesPart = seconds / 60;
        long secondsPart = seconds % 60;
        return String.format("%02d:%02d", minutesPart, secondsPart);
    }
}
