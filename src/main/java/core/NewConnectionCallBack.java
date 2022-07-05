package core;

import java.io.IOException;

public interface NewConnectionCallBack {
    public void onNewConnection(RUDPSocket socket) throws InterruptedException, IOException;
}
