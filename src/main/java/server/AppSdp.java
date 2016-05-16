package server;

/**
 * Created by antonio on 09/05/16.
 */

import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.ApplicationPath;

@ApplicationPath("/appsdp")
public class AppSdp extends ResourceConfig {

    public AppSdp() {}
}