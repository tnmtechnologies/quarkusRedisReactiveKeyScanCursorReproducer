/**
 *
 */
package org.acme;

import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.eventbus.EventBus;

/**
 *
 */
@ApplicationScoped
@Path("/api")
public class Resource  {


    @Inject
    EventBus eventBus;


    /**
     *
     */
    public Resource() {
        Log.info("Resource()");
    }


    @GET
    @Path("/ids")
    public Uni<JsonArray> getIds(@QueryParam("limit") final Integer limit, @QueryParam("type") final String type) {
        Log.info("getIds(limit, type)");

        return eventBus.<Set<String>>request(
                    "getIds"
                    , new JsonObject()
                            .put("limit", limit)
                            .put("type", type)
                    ).map(response -> {
                      final Set<String> ids = response.body();

                      final JsonArray body = new JsonArray();

                      ids.forEach(id -> body.add(new JsonObject().put("href", "/api/" + id)));

                      Log.infov(" body={0}", body);

                      return body;
                    });
    }


}
