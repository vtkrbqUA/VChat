package ua.vclient;

@FunctionalInterface
public interface Callback {
    void callback(Object... args);
}
