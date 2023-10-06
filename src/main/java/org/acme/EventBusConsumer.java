/**
 *
 */
package org.acme;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.logging.Log;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.keys.KeyScanArgs;
import io.quarkus.redis.datasource.keys.ReactiveKeyCommands;
import io.quarkus.redis.datasource.keys.ReactiveKeyScanCursor;
import io.quarkus.redis.datasource.set.ReactiveSetCommands;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;

/**
 *
 */
@ApplicationScoped
public class EventBusConsumer {


    /**
     *
     */
    private static final String ID_NAMESPACE = "id:";
    private static final String TYPE_NAMESPACE = "type:";


    @Inject
    private ReactiveRedisDataSource rds;


    /**
     *
     */
    public EventBusConsumer() {
        Log.info("RedisEventBusConsumer()");
    }


    /**
     *
     */
    @ConsumeEvent("getIds")
    public Uni<Set<String>> getIds(final JsonObject message) {
        Log.infov("getIds(message={0})", message);

        final Long limit = Optional.ofNullable(message.getLong("limit")).orElse(Long.MAX_VALUE);
        final String type = message.getString("type");

        return Optional.ofNullable(type)
                    .map(t -> {
                        final ReactiveSetCommands<String,String> setCommands = rds.set(String.class);
                        return setCommands.smembers(TYPE_NAMESPACE + t);
                    }).orElseGet(() -> {
                        final ReactiveKeyCommands<String> keyCommands = rds.key(String.class);
                        final ReactiveKeyScanCursor<String> scanCursor = keyCommands.scan(new KeyScanArgs().match(ID_NAMESPACE + '*').count(Long.MAX_VALUE));
                        final boolean hasNext = scanCursor.hasNext();
                        return hasNext ? scanCursor.next() : Uni.createFrom().item(() -> Collections.emptySet());
                    }).onItem().transformToMulti(s -> Multi.createFrom().items(s::stream))
                    .select().first(limit)
                    .onItem().transform(s -> type != null ? s : s.substring(ID_NAMESPACE.length()))
                    .collect().asSet()
                    ;
    }


}
