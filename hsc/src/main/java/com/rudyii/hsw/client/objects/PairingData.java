package com.rudyii.hsw.client.objects;

import androidx.annotation.Keep;

import lombok.Builder;
import lombok.Data;

@Keep
@Data
@Builder
public class PairingData {
    private String serverAlias;
    private String serverKey;
    private String serverIp;
    private Integer serverPort;
}
