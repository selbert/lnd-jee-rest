package ch.puzzle.ek.control;

import io.grpc.netty.GrpcSslContexts;
import io.grpc.stub.StreamObserver;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.lightningj.lnd.wrapper.AsynchronousLndAPI;
import org.lightningj.lnd.wrapper.StatusException;
import org.lightningj.lnd.wrapper.SynchronousLndAPI;
import org.lightningj.lnd.wrapper.ValidationException;
import org.lightningj.lnd.wrapper.message.*;

import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import java.io.IOException;

import static ch.puzzle.ek.control.ConvertUtil.bytesToHex;
import static ch.puzzle.ek.control.ConvertUtil.hexToBytes;


@Stateless
public class LndService implements StreamObserver<org.lightningj.lnd.wrapper.message.Invoice> {

    private static final long CONNECTION_RETRY_TIMEOUT = 5000;

    @Inject
    @ConfigProperty(name = "LND_HOSTNAME", defaultValue = "localhost")
    String lndHostname;


    @Inject
    @ConfigProperty(name = "LND_HOSTNAME", defaultValue = "localhost")
    private int lndPort;

    @Inject
    @ConfigProperty(name = "LND_MACAROON_INVOICE")
    String invoiceMacaroon;

    @Inject
    @ConfigProperty(name = "LND_MACAROON_READONLY")
    String readonlyMacaroon;

    @Inject
    @ConfigProperty(name = "LND_CERT_PATH")
    private String certPath;

    @Inject
    Event<Invoice> newInvoiceEvent;

    private SynchronousLndAPI syncReadOnlyAPI;
    private SynchronousLndAPI syncInvoiceAPI;
    private AsynchronousLndAPI asyncAPI;

    public LndService() throws ValidationException, IOException, StatusException {
        subscribeToInvoices();
    }

    private void subscribeToInvoices() throws IOException, StatusException, ValidationException {
        InvoiceSubscription invoiceSubscription = new InvoiceSubscription();
        getAsyncApi().subscribeInvoices(invoiceSubscription, this);
    }

    private AsynchronousLndAPI getAsyncApi() throws IOException {
        if (asyncAPI == null) {
            asyncAPI = new AsynchronousLndAPI(
                    lndHostname,
                    lndPort,
                    getSslContext(),
                    () -> invoiceMacaroon
            );
        }
        return asyncAPI;
    }


    private SynchronousLndAPI getSyncInvoiceApi() throws IOException {
        if (syncInvoiceAPI == null) {
            syncInvoiceAPI = new SynchronousLndAPI(
                    lndHostname,
                    lndPort,
                    getSslContext(),
                    () -> invoiceMacaroon
            );
        }
        return syncInvoiceAPI;
    }

    private SynchronousLndAPI getSyncReadonlyApi() throws IOException {
        if (syncReadOnlyAPI == null) {
            syncReadOnlyAPI = new SynchronousLndAPI(
                    lndHostname,
                    lndPort,
                    getSslContext(),
                    () -> readonlyMacaroon
            );
        }
        return syncReadOnlyAPI;
    }

    public GetInfoResponse getInfo() throws IOException, StatusException, ValidationException {
        try {
            return getSyncReadonlyApi().getInfo();
        } catch (StatusException | ValidationException | IOException e) {
            resetSyncReadOnlyApi();
            return getSyncReadonlyApi().getInfo();
        }
    }

    public ListChannelsResponse getChannels() throws IOException, StatusException, ValidationException {
        try {
            return getSyncReadonlyApi().listChannels(true, false, true, false);
        } catch (StatusException | ValidationException | IOException e) {
            resetSyncReadOnlyApi();
            return getSyncReadonlyApi().listChannels(true, false, true, false);
        }
    }

    public NodeInfo getNodeInfo(String nodeId) throws IOException, StatusException, ValidationException {
        try {
            return getSyncReadonlyApi().getNodeInfo(nodeId);
        } catch (StatusException | ValidationException | IOException e) {
            resetSyncReadOnlyApi();
            return getSyncReadonlyApi().getNodeInfo(nodeId);
        }
    }

    AddInvoiceResponse addInvoice(Invoice invoice) throws IOException, StatusException, ValidationException {
        try {
            return getSyncInvoiceApi().addInvoice(invoice);
        } catch (StatusException | ValidationException | IOException e) {
            resetSyncInvoiceApi();
            return getSyncInvoiceApi().addInvoice(invoice);
        }
    }

    Invoice lookupInvoice(String hashHex) throws IOException, StatusException, ValidationException {
        PaymentHash paymentHash = new PaymentHash();
        byte[] rHash = hexToBytes(hashHex);
        paymentHash.setRHash(rHash);
        try {
            return getSyncInvoiceApi().lookupInvoice(paymentHash);
        } catch (StatusException | ValidationException | IOException e) {
            resetSyncInvoiceApi();
            return getSyncInvoiceApi().lookupInvoice(paymentHash);
        }

    }

    @Override
    public void onNext(Invoice invoice) {
        newInvoiceEvent.fire(invoice);
    }

    @Override
    public void onError(Throwable t) {
        try {
            resetAsyncApi();
            subscribeToInvoices();
        } catch (StatusException | ValidationException | IOException e) {
            try {
                Thread.sleep(CONNECTION_RETRY_TIMEOUT);
                onError(e);
            } catch (InterruptedException e1) {
            }
        }
    }

    private void resetSyncReadOnlyApi() {
        if (syncReadOnlyAPI != null) {
            try {
                syncReadOnlyAPI.close();
            } catch (StatusException e) {
            } finally {
                syncReadOnlyAPI = null;
            }
        }
    }

    private void resetSyncInvoiceApi() {
        if (syncInvoiceAPI != null) {
            try {
                syncInvoiceAPI.close();
            } catch (StatusException e) {
            } finally {
                syncInvoiceAPI = null;
            }
        }
    }

    private void resetAsyncApi() {
        if (asyncAPI != null) {
            try {
                asyncAPI.close();
            } catch (StatusException e) {
            } finally {
                asyncAPI = null;
            }
        }
    }

    @Override
    public void onCompleted() {

    }

    private SslContext getSslContext() throws IOException {
        return GrpcSslContexts
                .configure(SslContextBuilder.forClient(), SslProvider.OPENSSL)
                .trustManager(ClassLoader.getSystemClassLoader().getResourceAsStream(certPath))
                .build();
    }
}
