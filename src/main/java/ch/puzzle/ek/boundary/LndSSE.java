package ch.puzzle.ek.boundary;

import org.lightningj.lnd.wrapper.message.Invoice;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.enterprise.event.Observes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseBroadcaster;
import javax.ws.rs.sse.SseEventSink;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
@Path("/sse")
public class LndSSE {

    private Map<String, SseBroadcaster> broadcasters;
    private Sse sse;

    @PostConstruct
    public void init() {
        this.broadcasters = new ConcurrentHashMap<>();
    }

    @GET
    @Path("/invoices/{invoiceHash}")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void register(@Context Sse sse, @Context SseEventSink eventSink, @PathParam("invoiceHash") String invoiceHash) {
        this.sse = sse;
        if (broadcasters.get(invoiceHash) == null) {
            this.broadcasters.put(invoiceHash, sse.newBroadcaster());
        }
        this.broadcasters.get(invoiceHash).register(eventSink);
    }

    public void onInvoice(@Observes Invoice invoice) {
        String invoiceHash = Base64.getEncoder().encodeToString(invoice.getRHash());
        OutboundSseEvent outbound = this.sse.newEventBuilder().
                data(invoice).
                id("" + System.currentTimeMillis()).
                build();

        Optional.ofNullable(this.broadcasters.get(invoiceHash))
                .ifPresent(b -> b.broadcast(outbound));

    }

}
