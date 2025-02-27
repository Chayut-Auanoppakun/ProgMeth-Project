package application;

public class SharedState {
    private boolean broadcast;
    private boolean client;

    public synchronized boolean isBroadcast() {
        return broadcast;
    }

    public synchronized void setBroadcast(boolean broadcast) {
        this.broadcast = broadcast;
    }
    
    public synchronized boolean isClient() {
        return client;
    }

    public synchronized void setClient(boolean client) {
        this.client = client;
    }
}
