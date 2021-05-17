package io.rong.imkit.plugin.location;

import java.util.List;

public interface IRealTimeLocationStateListener {

    void onParticipantChanged(List<String> userIdList);

    void onErrorException();
}
