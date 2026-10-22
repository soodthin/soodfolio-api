package com.soodfolio.api.service;

import com.soodfolio.api.dto.SnapshotDtos.RollbackResponseDto;
import com.soodfolio.api.dto.SnapshotDtos.SnapshotResponseDto;
import com.soodfolio.api.dto.SnapshotDtos.SnapshotSummaryDto;

import java.util.List;
import java.util.UUID;

public interface SnapshotService {

    SnapshotResponseDto publishSnapshot(UUID userId, String changelog);

    List<SnapshotSummaryDto> getSnapshots(UUID userId);

    SnapshotResponseDto getSnapshot(UUID userId, int version);

    RollbackResponseDto rollbackToSnapshot(UUID userId, int version);
}
