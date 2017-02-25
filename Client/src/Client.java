import utils.GatewayUtils;
import utils.UIUtils;

import java.io.*;
import java.util.concurrent.ExecutionException;

/**
 * Created by antonio on 12/05/16.
 */
public class Client {


    private static UIUtils uiUtils;

    private static GatewayUtils gatewayUtils;

    public static void main(String[] argv) throws ExecutionException, InterruptedException, IOException {
        if (argv.length <= 1) {
            System.out.println("Parameters incorrect! [gatewayAddress, gatewayPort]");
            System.exit(1);
        } else {
            String serverAddress = argv[0];
            int port = Integer.parseInt(argv[1]);

            gatewayUtils=GatewayUtils.getInstance(serverAddress,port);
            uiUtils=UIUtils.getInstance(gatewayUtils);

            String username;

            username=uiUtils.login();

            if(username!=null) {
                uiUtils.mainLoop();
                int res = gatewayUtils.logout(username);
                if (res == 200) {
                    System.out.println("Logged out");
                }
                System.exit(0);
            }else
                System.exit(1);
        }
    }
}
