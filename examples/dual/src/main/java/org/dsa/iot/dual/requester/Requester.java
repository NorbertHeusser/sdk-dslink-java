package org.dsa.iot.dual.requester;

import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.DSLinkHandler;
import org.dsa.iot.dslink.methods.requests.InvokeRequest;
import org.dsa.iot.dslink.methods.requests.ListRequest;
import org.dsa.iot.dslink.methods.requests.SetRequest;
import org.dsa.iot.dslink.methods.responses.InvokeResponse;
import org.dsa.iot.dslink.methods.responses.ListResponse;
import org.dsa.iot.dslink.methods.responses.SetResponse;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.actions.table.Row;
import org.dsa.iot.dslink.node.actions.table.Table;
import org.dsa.iot.dslink.node.value.SubscriptionValue;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.util.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author Samuel Grenier
 */
public class Requester extends DSLinkHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(Requester.class);

    /**
     * Initializes the requester link.
     *
     * @param link Requester link to initialize.
     */
    public static void init(DSLink link) {
        setNodeValue(link);
        listValuesChildren(link);
        subscribe(link);
        invoke(link);
        invokeError(link);
    }

    /**
     * Sets the node value on "/conns/dual/values/settable" as provided by the
     * responder to "Hello world!"
     *
     * @param link Requester link used to communicate to the endpoint.
     * @see org.dsa.iot.dual.responder.Responder#initSettableNode
     */
    private static void setNodeValue(DSLink link) {
        Value value = new Value("Hello world!");
        String path = link.getPath() + "/values/settable";
        SetRequest request = new SetRequest(path, value);
        link.getRequester().set(request, new Handler<SetResponse>() {
            @Override
            public void handle(SetResponse event) {
                LOGGER.info("Successfully set the new value on the responder");
            }
        });
    }

    /**
     * Lists the children of the "/conns/dual/values" path.
     *
     * @param link Requester link used to communicate to the endpoint.
     */
    private static void listValuesChildren(DSLink link) {
        String path = link.getPath() + "/values";
        ListRequest request = new ListRequest(path);
        link.getRequester().list(request, new Handler<ListResponse>() {
            @Override
            public void handle(ListResponse event) {
                Map<Node, Boolean> updates = event.getUpdates();
                if (updates != null) {
                    for (Map.Entry<Node, Boolean> entry : updates.entrySet()) {
                        String msg = "Child node at ";
                        msg += entry.getKey().getPath() + " was ";
                        msg += entry.getValue() ? "removed" : "added";
                        LOGGER.info(msg);
                    }
                }
            }
        });
    }

    /**
     * Subscribes to the dynamic node on the responder to demonstrate
     * subscriptions.
     *
     * @param link Requester link used to communicate to the endpoint.
     */
    private static void subscribe(DSLink link) {
        String path = link.getPath() + "/values/dynamic";
        link.getRequester().subscribe(path, new Handler<SubscriptionValue>() {
            @Override
            public void handle(SubscriptionValue event) {
                int val = event.getValue().getNumber().intValue();
                LOGGER.info("Received new dynamic value of {}", val);
            }
        });
    }

    /**
     * Invokes the action on "/conns/dual/values/action".
     *
     * @param link Requester link used to communicate to the endpoint.
     * @see org.dsa.iot.dual.responder.Responder#initActionNode
     */
    private static void invoke(DSLink link) {
        String path = link.getPath() + "/values/action";
        InvokeRequest request = new InvokeRequest(path);
        link.getRequester().invoke(request, new Handler<InvokeResponse>() {
            @Override
            public void handle(InvokeResponse event) {
                LOGGER.info("Successfully invoked the responder action");

                Table t = event.getTable();
                Row row = t.getRows().get(0);
                Value value = row.getValues().get(0);
                LOGGER.info("Received response: {}", value.toString());
            }
        });
    }

    private static void invokeError(DSLink link) {
        String path = link.getPath() + "/non_existent_node";
        InvokeRequest req = new InvokeRequest(path);
        link.getRequester().invoke(req, new Handler<InvokeResponse>() {
            @Override
            public void handle(InvokeResponse event) {
                if (!event.hasError()) {
                    return;
                }
                String m = event.getError().getMessage();
                String d = event.getError().getDetail();
                LOGGER.info("Invocation error (as desired): \nmsg: {}\ndetail: {}", m, d);
            }
        });
    }
}
