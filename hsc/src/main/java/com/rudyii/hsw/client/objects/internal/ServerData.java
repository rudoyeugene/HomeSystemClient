package com.rudyii.hsw.client.objects.internal;

import androidx.annotation.Keep;

import com.rudyii.hs.common.type.NotificationType;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Keep
@Data
@Builder
@EqualsAndHashCode
public class ServerData {
    private String serverKey;
    private String serverAlias;
    private String serverIp;
    private Integer serverPort;
    @Builder.Default
    private NotificationType notificationType = NotificationType.ALL;
    private boolean hourlyReportEnabled;
    private boolean notificationsMuted;
}
