package io.rong.imkit.manager;


abstract class IAudioState {
    void enter() {

    }

    abstract void handleMessage(AudioStateMessage message);
}
