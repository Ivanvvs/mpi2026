package com.exam.event;

import java.util.List;

public record ClassRankUpdatedEvent(List<Long> classIds) {
}
