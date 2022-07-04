package core;

public interface NewConnectionCallBack {
    public void onNewConnection(RUDPSocket socket) throws InterruptedException;
}
