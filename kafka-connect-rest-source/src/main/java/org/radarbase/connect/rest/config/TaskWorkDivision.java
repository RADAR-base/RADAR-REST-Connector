package org.radarbase.connect.rest.config;

import java.util.List;
import java.util.Map;

public interface TaskWorkDivision extends RestSourceTool {
  List<Map<String, String>> taskConfigs(int maxTasks);
}
