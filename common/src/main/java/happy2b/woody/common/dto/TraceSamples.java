package happy2b.woody.common.dto;

import java.util.List;
import java.util.Map;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/8/26
 */
public class TraceSamples {

    Map<String, ProfilingSampleBase> sampleBaseMap;
    Map<String, List<ProfilingSample>> eventSamples;

    public TraceSamples(Map<String, ProfilingSampleBase> sampleBaseMap, Map<String, List<ProfilingSample>> eventSamples) {
        this.sampleBaseMap = sampleBaseMap;
        this.eventSamples = eventSamples;
    }

    public Map<String, ProfilingSampleBase> getSampleBaseMap() {
        return sampleBaseMap;
    }

    public Map<String, List<ProfilingSample>> getEventSamples() {
        return eventSamples;
    }
}
