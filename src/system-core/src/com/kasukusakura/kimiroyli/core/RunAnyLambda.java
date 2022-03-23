package com.kasukusakura.kimiroyli.core;

interface RunAnyLambda<E> {
    void execute(E arg) throws Throwable;
}

interface RunAnyLambda2<E1, E2> {
    void execute(E1 a1, E2 a2) throws Throwable;
}

interface RunAnyRsp<R> {
    R execute() throws Throwable;
}