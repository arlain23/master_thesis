package masters.features;

import java.util.List;

import javax.management.RuntimeErrorException;

import org.apache.log4j.Logger;

public class FeatureContainer implements Feature {
  private static final long serialVersionUID = -2607718850680822924L;
  
  private List<Feature> features;
  private int featureIndex;
  
  public FeatureContainer(List<Feature> features, int featureIndex) {
    this.featureIndex = featureIndex;
    this.features = features;
  }
  
	@Override
	public Object getValue() {
	  _log.error("get value not implemented for FeatureContainer");
	  throw new RuntimeErrorException(null);
		//return 0;
	}

	@Override
	public void setValue(Object value) {
	  _log.error("set value not implemented for FeatureContainer");
	}

	@Override
	public double getDifference(Feature otherFeature) {
		_log.error("get difference not implemented for FeatureContainer");
	  return 0;
	}

	@Override
	public int getFeatureIndex() {
		return featureIndex;
	}
	@Override
	public boolean isOutOfBounds() {
		return false;
	}
	public List<Feature> getFeatures() {
	  return this.features;
	}
	private static transient Logger _log = Logger.getLogger(FeatureContainer.class);

}
