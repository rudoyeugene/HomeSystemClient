package com.rudyii.hsw.client.objects.internal;

import androidx.annotation.Keep;

import com.rudyii.hs.common.type.SystemModeType;
import com.rudyii.hs.common.type.SystemStateType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Keep
@Data
@Builder
@EqualsAndHashCode
@AllArgsConstructor
public class SystemModeAndState {
    private SystemModeType systemMode;
    private SystemStateType systemState;

}
