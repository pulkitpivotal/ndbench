package com.netflix.ndbench.plugin.dyno;

import com.google.inject.Singleton;
import com.netflix.dyno.connectionpool.Host;
import com.netflix.dyno.connectionpool.HostSupplier;
import com.netflix.dyno.jedis.DynoJedisClient;
import com.netflix.ndbench.api.plugin.DataGenerator;
import com.netflix.ndbench.api.plugin.NdBenchClient;
import com.netflix.ndbench.api.plugin.annotations.NdBenchClientPlugin;

import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This pluging performs GET/SET inside a pipeline of size MAX_PIPE_KEYS against
 * Dynomite.
 * 
 * @author ipapapa
 *
 */
@Singleton
@NdBenchClientPlugin("DynoGetSetPipeline")
public class DynoJedisGetSetPipeline implements NdBenchClient {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(DynoJedisGetSetPipeline.class);

    private static final int MIN_PIPE_KEYS = 3;
    private static final int MAX_PIPE_KEYS = 10;

    private static final String ClusterName = "dynomite_redis";

    private final AtomicReference<DynoJedisClient> jedisClient = new AtomicReference<DynoJedisClient>(null);

    private DataGenerator dataGenerator;

    @Override
    public void shutdown() throws Exception {
        if (jedisClient.get() != null) {
            jedisClient.get().stopClient();
            jedisClient.set(null);
        }
    }

    @Override
    public String getConnectionInfo() throws Exception {
        return String.format("Cluster Name - %s", ClusterName);
    }

    @Override
    public void init(DataGenerator dataGenerator) throws Exception {
        this.dataGenerator = dataGenerator;
        if (jedisClient.get() != null) {
            return;
        }

        logger.info("Initing dyno jedis client");

        logger.info("\nDynomite Cluster: " + ClusterName);

        HostSupplier hSupplier = new HostSupplier() {

            @Override
            public Collection<Host> getHosts() {

                List<Host> hosts = new ArrayList<Host>();
                hosts.add(new Host("localhost", 8102, Host.Status.Up).setRack("local-dc"));

                return hosts;
            }

        };

        DynoJedisClient jClient = new DynoJedisClient.Builder().withApplicationName(ClusterName)
                .withDynomiteClusterName(ClusterName).withHostSupplier(hSupplier).build();

        jedisClient.set(jClient);

    }

    @Override
    public String runWorkFlow() throws Exception {
        return null;
    }

    @Override
    public String readSingle(String key) throws Exception {
        DynoJedisUtils jedisUtils = new DynoJedisUtils(jedisClient);
        return jedisUtils.pipelineRead(key, MAX_PIPE_KEYS, MIN_PIPE_KEYS);
    }

    @Override
    public String writeSingle(String key) throws Exception {
        DynoJedisUtils jedisUtils = new DynoJedisUtils(jedisClient);
        return jedisUtils.pipelineWrite(key, dataGenerator, MAX_PIPE_KEYS, MIN_PIPE_KEYS);
    }

}