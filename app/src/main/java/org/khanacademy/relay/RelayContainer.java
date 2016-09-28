package org.khanacademy.relay;

public interface RelayContainer<T> {
    String getQueryFragment();

    void renderScreen(Human data);
}
