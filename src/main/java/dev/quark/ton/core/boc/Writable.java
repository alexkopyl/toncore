package dev.quark.ton.core.boc;

@FunctionalInterface
public interface Writable {
    void writeTo(Builder builder);
}
