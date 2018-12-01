package ch.puzzle.ek.boundary;

import org.lightningj.lnd.wrapper.message.Invoice;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.websocket.EncodeException;
import javax.websocket.OnClose;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/invoices")
@ApplicationScoped
public class LndWebsocket {
    Map<String, Session> sessions = new ConcurrentHashMap<>();

    @OnOpen
    public void open(Session session) {
        sessions.put(session.getId(), session);
    }

    @OnClose
    public void close(Session session) {
        sessions.remove(session.getId());
    }

    public void onInvoice(@Observes Invoice invoice) {
        sessions.values()
                .forEach(session -> {
                    try {
                        session.getBasicRemote().sendObject(invoice);
                    } catch (IOException | EncodeException e) {
                        e.printStackTrace();
                    }
                });
    }

}
