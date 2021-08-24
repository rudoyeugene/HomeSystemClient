package com.rudyii.hsw.client.objects;

import androidx.annotation.Keep;

import com.rudyii.hsw.client.objects.types.NotificationType;

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
    private NotificationType notificationType;
    private boolean hourlyReportMuted;
    private boolean notificationsMuted;
}
