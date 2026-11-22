package com.safwat.hr.controller.hr.entitlements.allowance;

import java.util.List;

public record AllowanceSyncResult(
        List<String> addedCodes,
        List<String> addedNames,
        List<String> removedCodes,
        List<String> removedNames
) {
}