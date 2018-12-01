package ch.puzzle.ek.boundary;

import ch.puzzle.ek.control.LndService;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.lightningj.lnd.wrapper.message.GetInfoResponse;
import org.lightningj.lnd.wrapper.message.ListChannelsResponse;
import org.lightningj.lnd.wrapper.message.NodeInfo;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

@Path("/api/lnd")
public class LndResource {

    @Inject
    private LndService lndService;

    @GET
    @Timed
    public GetInfoResponse getInfo() throws Exception {
        return lndService.getInfo();
    }

    @GET
    @Path("/channels")
    @Timed
    public ListChannelsResponse getChannels() throws Exception {
        return lndService.getChannels();
    }

    @GET
    @Path("/nodeinfo/{nodeId}")
    @Timed
    public NodeInfo getNodeInfo(@QueryParam("nodeId") String nodeId) throws Exception {
        return lndService.getNodeInfo(nodeId);
    }
}
