package com.rudyii.hsw.client.objects.internal;

import androidx.annotation.Keep;

import com.rudyii.hs.common.objects.settings.CameraSettings;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Keep
@Data
@Builder
@EqualsAndHashCode
@AllArgsConstructor
public class CameraSettingsInternal {
    public static final String CAMERA_SETTINSG_EXTRA_DATA_NAME = "cameraSettings";
    private CameraSettings cameraSettings;
    private String cameraName;
}
