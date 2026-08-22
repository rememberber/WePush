package com.fangxuele.wepush.next.core.api;

@FunctionalInterface
public interface SecretResolver {
    SecretValue resolve(SecretRef ref);
}
