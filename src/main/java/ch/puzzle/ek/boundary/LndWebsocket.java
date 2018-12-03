package ch.puzzle.ek.boundary;

import org.lightningj.lnd.wrapper.message.Invoice;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.websocket.EncodeException;
import javax.websocket.OnClose;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@ServerEndpoint("/ws/invoice/{invoiceHash}")
@ApplicationScoped
public class LndWebsocket {
    private Map<String, List<Session>> invoiceToSession;
    private Map<String, List<String>> sessionToInvoice;


    @PostConstruct
    public void init() {
        invoiceToSession = new ConcurrentHashMap<>();
        sessionToInvoice = new ConcurrentHashMap<>();
    }

    @OnOpen
    public void open(Session session, @PathParam("invoiceHash") String invoiceHash) {
        invoiceToSession.putIfAbsent(invoiceHash, new CopyOnWriteArrayList<>());
        invoiceToSession.get(invoiceHash).add(session);

        sessionToInvoice.putIfAbsent(session.getId(), new CopyOnWriteArrayList<>());
        sessionToInvoice.get(session.getId()).add(invoiceHash);
    }

    @OnClose
    public void close(Session session) {

        sessionToInvoice.get(session.getId())
                .forEach(invoiceHash ->
                        invoiceToSession
                                .get(invoiceHash)
                                .removeIf(s -> s.getId().equals(session.getId())));
    }

    public void onInvoice(@Observes Invoice invoice) {
        String invoiceHash = Base64.getEncoder().encodeToString(invoice.getRHash());
        invoiceToSession.getOrDefault(invoiceHash, Collections.emptyList())
                .forEach(session -> {
                    try {
                        session.getBasicRemote().sendObject(invoice);
                    } catch (IOException | EncodeException e) {
                        e.printStackTrace();
                    }
                });
    }

}
