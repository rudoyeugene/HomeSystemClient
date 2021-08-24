package com.rudyii.hsw.client.objects;

import androidx.annotation.Keep;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Keep
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IamBack {
    private String serverKey;
    private String email;
}
