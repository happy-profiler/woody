package happy2b.woody.common.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 样本底座
 */
public class ProfilingSampleBase {

  private String resource;
  private String rsType;
  private String frameTypeIds;
  private List<String> stackTraces = new ArrayList<>(128);

  public String getResource() {
    return resource;
  }

  public void setResource(String resource) {
    this.resource = resource;
  }

  public String getRsType() {
    return rsType;
  }

  public void setRsType(String rsType) {
    this.rsType = rsType;
  }

  public String getFrameTypeIds() {
    return frameTypeIds;
  }

  public void setFrameTypeIds(String frameTypeIds) {
    this.frameTypeIds = frameTypeIds;
  }

  public List<String> getStackTraces() {
    return stackTraces;
  }

  public void setStackTraces(List<String> stackTraces) {
    this.stackTraces = stackTraces;
  }
}


